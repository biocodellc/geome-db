#!/usr/bin/env node
'use strict';

/**
 * Postgres Photo Audit with speed, diagnostics, filtered output, filtered pruning, dry-run,
 * hardened URL checks, and hardened PG pool.
 */

const fs = require('fs');
const path = require('path');
require('dotenv').config();

const { Pool } = require('pg');
const yargs = require('yargs/yargs');
const { hideBin } = require('yargs/helpers');
const nodemailer = require('nodemailer');

if (typeof fetch !== 'function') {
  console.error('This script expects Node 18+ (built-in fetch).');
  process.exit(1);
}

const argv = yargs(hideBin(process.argv))
  // core
  .option('tables', { type: 'string', default: 'event,sample', describe: 'Comma list: event,sample' })
  .option('schema', { type: 'string', default: process.env.PGSCHEMA || 'network_1', describe: 'Schema containing photo tables' })
  .option('join', { type: 'boolean', default: true, describe: 'Include expeditions LEFT JOIN (use --no-join to skip)' })
  .option('check-urls', { type: 'boolean', default: false, describe: 'HEAD/GET a representative URL on suspect rows' })
  .option('email', { type: 'boolean', default: false, describe: 'Email JSON/CSV via Gmail' })
  .option('concurrency', { type: 'number', default: 10, describe: 'URL-check concurrency' })

  // pruning
  .option('prune-errors', { type: 'boolean', default: false, describe: 'Delete rows that contain backend imageProcessingErrors' })
  .option('prune-all-flagged', { type: 'boolean', default: false, describe: 'Delete all rows flagged for any reason' })
  .option('prune-filtered', { type: 'boolean', default: false, describe: 'Delete only rows included in the filtered report (and flagged)' })
  .option('dry-run', { type: 'boolean', default: false, describe: 'Show what would be deleted; do not execute deletes' })

  // speed/diagnostics
  .option('limit', { type: 'number', default: 0, describe: 'LIMIT rows per table (0 = no limit)' })
  .option('since', { type: 'string', describe: 'Filter by created/modified >= this ISO date (e.g. 2025-01-01)' })
  .option('where', { type: 'string', describe: 'Extra WHERE clause (raw SQL; appended with AND)' })
  .option('progress', { type: 'boolean', default: true, describe: 'Show heartbeat logs' })

  // output filters
  .option('only-flagged', { type: 'boolean', default: false, describe: 'Report only flagged rows' })
  .option('reasons', {
    type: 'string',
    describe: 'Comma list filter: invalid_json, processed_false, processing_errors, non_jpeg, url_failed, url_not_image'
  })

  // URL check hardening
  .option('url-timeout', { type: 'number', default: 8000, describe: 'Per-request timeout (ms) for URL checks' })
  .option('url-retries', { type: 'number', default: 2, describe: 'Retry count for URL checks' })
  .option('user-agent',  { type: 'string', default: 'PhotoAuditBot/1.0 (+you@domain)', describe: 'User-Agent for URL checks' })
  .option('assume-image-by-ext', { type: 'boolean', default: false, describe: 'If Content-Type is missing, treat .jpg/.jpeg URLs as image/*' })
  .option('url-method', { type: 'string', choices: ['auto','get'], default: 'auto', describe: 'auto = HEAD then GET fallback; get = force GET only' })
  .strict()
  .argv;

const PG = {
  host: process.env.PGHOST,
  port: Number(process.env.PGPORT || 5432),
  user: process.env.PGUSER,
  password: process.env.PGPASSWORD,
  database: process.env.PGDATABASE,
  ssl: /^true$/i.test(process.env.PGSSL || ''),
};

const MAIL = {
  user: process.env.MAIL_USER,
  pass: process.env.MAIL_PASS,
  to: process.env.MAIL_TO,
  from: process.env.MAIL_FROM || process.env.MAIL_USER,
};

const TABLES_MAP = { event: 'event_photo', sample: 'sample_photo' };

// ---------- helpers ----------
const nowStamp = () => {
  const d = new Date(); const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}_${p(d.getHours())}-${p(d.getMinutes())}-${p(d.getSeconds())}`;
};
const tick = (msg) => argv.progress && console.log(`[${new Date().toISOString()}] ${msg}`);

const coerceBool = (v) => typeof v === 'boolean' ? v : String(v ?? '').trim().toLowerCase() === 'true';
const safeJsonParse = (s) => { try { return JSON.parse(s); } catch { return null; } };
const parseProcessingErrors = (raw) => {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  if (typeof raw === 'string') {
    const parsed = safeJsonParse(raw.trim());
    return Array.isArray(parsed) ? parsed : [raw.trim()];
  }
  return [];
};

const pickRepresentativeUrl = (d) => {
  const order = ['img128', 'img2048','img1024','img512','img256'];
  for (const k of order) if (d?.[k] && /^https?:\/\//i.test(d[k])) return d[k];
  if (d?.originalUrl && /^https?:\/\//i.test(d.originalUrl) && d.originalUrl.toLowerCase() !== 'none') return d.originalUrl;
  const cands = Object.values(d || {}).filter(v => typeof v === 'string' && /^https?:\/\//i.test(v));
  return cands[0] || null;
};
const hasJpegExtension = (d) => {
  const vals = [];
  if (d?.filename) vals.push(d.filename);
  ['img128','img256','img512','img1024','img2048','originalUrl'].forEach(k => d?.[k] && vals.push(d[k]));
  return vals.some(s => typeof s === 'string' && /\.(jpe?g)(\?|#|$)/i.test(s));
};

const buildReasonList = (f) => {
  const r = [];
  if (f.badJson) r.push('data: invalid JSON');
  if (f.processedFalse) r.push('processed != true');
  if (f.processingErrors) r.push('imageProcessingErrors present');
  if (f.nonJpeg) r.push('not JPG/JPEG');
  if (f.urlFailed) r.push(`url failed (${f.urlStatus || 'n/a'})`);
  if (f.urlNotImage) r.push(`url content-type not image/* (${f.urlContentType || 'unknown'})`);
  return r;
};

// simple concurrency limiter (no ESM)
function makeLimiter(concurrency = 10) {
  let active = 0; const queue = [];
  const next = () => { active--; if (queue.length) queue.shift()(); };
  return (fn) => (...args) => new Promise((resolve, reject) => {
    const run = () => { active++; Promise.resolve(fn(...args)).then(resolve, reject).finally(next); };
    if (active < concurrency) run(); else queue.push(run);
  });
}

const qident = (s) => `"${String(s).replace(/"/g, '""')}"`;
const qtable = (schema, table) => `${qident(schema)}.${qident(table)}`;

// Hardened URL checker with retry/backoff and options
async function headOrGetWithRetry(url) {
  const timeoutMs = argv['url-timeout'] || 8000;
  const retries = argv['url-retries'] || 0;
  const methodMode = argv['url-method'] || 'auto';
  const headers = {};
  if (argv['user-agent']) headers['User-Agent'] = argv['user-agent'];

  function withTimeout(method) {
    const controller = new AbortController();
    const t = setTimeout(() => controller.abort(), timeoutMs);
    return fetch(url, { method, redirect: 'follow', signal: controller.signal, headers })
      .finally(() => clearTimeout(t));
  }

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      let res;
      if (methodMode === 'get') {
        res = await withTimeout('GET');
      } else {
        // auto: try HEAD then GET if needed
        res = await withTimeout('HEAD');
        if (!res.ok || !res.headers.get('content-type')) {
          res = await withTimeout('GET');
        }
      }

      const ok = res.ok;
      let ct = res.headers.get('content-type') || '';

      // If CT missing but URL looks jpeg and user opted-in, assume image/jpeg
      if (!ct && argv['assume-image-by-ext'] && /\.(jpe?g)(\?|#|$)/i.test(url)) {
        ct = 'image/jpeg';
      }

      return { ok, status: res.status, contentType: ct };
    } catch (e) {
      if (attempt < retries) {
        await new Promise(r => setTimeout(r, 250 * (attempt + 1))); // tiny backoff
        continue;
      }
      return { ok: false, status: 0, error: String(e) };
    }
  }
}

// ---------- main ----------
(async function main() {
  // env sanity
  ['host','user','password','database'].forEach(k => { if (!PG[k]) console.error(`Missing PG ${k} in .env`); });
  if (argv.email) ['user','pass','to'].forEach(k => { if (!MAIL[k]) console.error(`Missing MAIL_${k.toUpperCase()} in .env`); });

  const pool = new Pool({
    ...PG,
    keepAlive: true,
    connectionTimeoutMillis: 10000,
    idleTimeoutMillis: 10000,
  });
  // prevent idle errors from crashing the process
  pool.on('error', (err) => {
    console.warn('PG pool idle client error:', err.message);
  });

  const schema = argv.schema;

  const wanted = argv.tables.split(',').map(s => s.trim().toLowerCase()).filter(Boolean);
  const tableNames = wanted.map(w => TABLES_MAP[w]).filter(Boolean);
  if (!tableNames.length) { console.error('No valid tables. Use --tables event,sample'); process.exit(1); }

  const t0 = Date.now();
  const rowsAll = [];

  for (const table of tableNames) {
    tick(`Querying ${schema}.${table}...`);

    const selectCols = `
      p.id, p.local_identifier, p.expedition_id, p.data, p.tsv, p.created, p.modified, p.parent_identifier
      ${argv.join ? `,
        e.expedition_code, e.expedition_title, e.identifier AS expedition_identifier
      ` : ''}
    `;

    const joinSql = argv.join
      ? `LEFT JOIN ${qtable('public','expeditions')} AS e ON e.id = p.expedition_id`
      : '';

    const whereClauses = [];
    const params = [];

    if (argv.since) {
      params.push(argv.since);
      // include if created OR modified >= since
      whereClauses.push(`(p.created >= $${params.length} OR p.modified >= $${params.length})`);
    }
    if (argv.where) {
      whereClauses.push(`(${argv.where})`); // raw SQL, use carefully
    }
    const whereSql = whereClauses.length ? `WHERE ${whereClauses.join(' AND ')}` : '';
    const limitSql = argv.limit ? `LIMIT ${Number(argv.limit)}` : '';

    const sql = `
      SELECT ${selectCols}
      FROM ${qtable(schema, table)} AS p
      ${joinSql}
      ${whereSql}
      ${limitSql}
    `;

    const { rows } = await pool.query(sql, params);
    tick(`Fetched ${rows.length} rows from ${schema}.${table}`);
    for (const r of rows) rowsAll.push({ ...r, _table: table });
  }
  tick(`Total fetched: ${rowsAll.length} rows in ${((Date.now()-t0)/1000).toFixed(1)}s`);

  // analyze
  tick('Analyzing rows...');
  const results = [];
  for (const r of rowsAll) {
    const base = {
      table: r._table,
      id: r.id,
      local_identifier: r.local_identifier,
      expedition_id: r.expedition_id,
      expedition_code: r.expedition_code || null,
      expedition_title: r.expedition_title || null,
      expedition_identifier: r.expedition_identifier || null,
      created: r.created,
      modified: r.modified,
      parent_identifier: r.parent_identifier,
    };

    let d = null, badJson = false;
    if (r.data && typeof r.data === 'object') d = r.data;
    else if (r.data && typeof r.data === 'string') { d = safeJsonParse(r.data); if (!d) badJson = true; }

    const processedFalse = d ? !coerceBool(d.processed) : true;
    const processingErrorsArr = parseProcessingErrors(d?.imageProcessingErrors);
    const processingErrors = processingErrorsArr.length > 0;
    const nonJpeg = d ? !hasJpegExtension(d) : true;
    const repUrl = d ? pickRepresentativeUrl(d) : null;

    results.push({
      ...base,
      processed: d ? coerceBool(d.processed) : false,
      filename: d?.filename || null,
      representativeUrl: repUrl,
      processingErrors,
      processingErrorMessages: processingErrorsArr,
      nonJpeg,
      badJson,
      urlChecked: false,
      urlOk: null,
      urlStatus: null,
      urlContentType: null,
      reasons: [],
    });
  }

  // URL checks (suspects only)
  let heartbeat;
  if (argv['check-urls']) {
    const suspects = results
      .filter(r => r.badJson || r.processingErrors || r.nonJpeg || !r.processed)
      .filter(r => r.representativeUrl);
    tick(`Checking URLs for ${suspects.length} suspect rows (concurrency ${argv.concurrency})...`);
    const limit = makeLimiter(argv.concurrency || 10);
    let done = 0;

    if (argv.progress) {
      heartbeat = setInterval(() => {
        process.stdout.write(`\rURL checks: ${done}/${suspects.length} complete`);
      }, 1000);
    }

    await Promise.all(suspects.map(row =>
      limit(async () => {
        const check = await headOrGetWithRetry(row.representativeUrl);
        row.urlChecked = true;
        row.urlOk = !!check.ok;
        row.urlStatus = check.status || null;
        row.urlContentType = check.contentType || null;
        if (!row.urlOk) row._urlFailed = true;
        else if (!/^image\//i.test(row.urlContentType || '')) row._urlNotImage = true;
        done++;
      })()
    ));

    if (heartbeat) clearInterval(heartbeat);
    if (argv.progress) process.stdout.write('\n');
  }

  // reasons + flagged
  results.forEach((row) => {
    const flags = {
      badJson: row.badJson,
      processedFalse: !row.processed,
      processingErrors: row.processingErrors,
      nonJpeg: row.nonJpeg,
      urlFailed: !!row._urlFailed,
      urlNotImage: !!row._urlNotImage,
      urlStatus: row.urlStatus,
      urlContentType: row.urlContentType,
    };
    row.reasons = buildReasonList(flags);
    row.flagged = row.reasons.length > 0;
  });

  // --- Output filtering: only-flagged + reasons ---
  const wantedReasons = (argv.reasons || '')
    .split(',')
    .map(s => s.trim().toLowerCase())
    .filter(Boolean);

  function matchesReasonFilter(row) {
    if (!wantedReasons.length) return true;
    const fr = {
      invalid_json: row.badJson,
      processed_false: !row.processed,
      processing_errors: row.processingErrors,
      non_jpeg: row.nonJpeg,
      url_failed: !!row._urlFailed,
      url_not_image: !!row._urlNotImage,
    };
    return wantedReasons.some(k => fr[k]);
  }

  let outputResults = results;
  if (argv['only-flagged'] || wantedReasons.length) {
    outputResults = results.filter(r => (argv['only-flagged'] ? r.flagged : true) && matchesReasonFilter(r));
  }

  // write reports
  tick('Writing reports...');
  const outDir = path.join(process.cwd(), 'reports');
  await fs.promises.mkdir(outDir, { recursive: true });
  const stamp = nowStamp();
  const jsonPath = path.join(outDir, `photo_audit_${stamp}.json`);
  const csvPath = path.join(outDir, `photo_audit_${stamp}.csv`);

  const total = results.length;
  const flagged = results.filter(r => r.flagged).length;
  const written = outputResults.length;

  const byReasonAll = {};
  results.forEach(r => r.reasons.forEach(reason => { byReasonAll[reason] = (byReasonAll[reason] || 0) + 1; }));

  const byReasonWritten = {};
  outputResults.forEach(r => r.reasons.forEach(reason => { byReasonWritten[reason] = (byReasonWritten[reason] || 0) + 1; }));

  await fs.promises.writeFile(jsonPath, JSON.stringify({
    generatedAt: new Date().toISOString(),
    db: { host: PG.host, database: PG.database, schema },
    options: {
      tables: tableNames,
      checkUrls: !!argv['check-urls'],
      pruneErrors: !!argv['prune-errors'],
      pruneAllFlagged: !!argv['prune-all-flagged'],
      pruneFiltered: !!argv['prune-filtered'],
      dryRun: !!argv['dry-run'],
      limit: argv.limit,
      since: argv.since || null,
      join: !!argv.join,
      where: argv.where || null,
      concurrency: argv.concurrency,
      onlyFlagged: !!argv['only-flagged'],
      reasons: wantedReasons,
      urlTimeout: argv['url-timeout'],
      urlRetries: argv['url-retries'],
      userAgent: argv['user-agent'],
      assumeImageByExt: !!argv['assume-image-by-ext'],
      urlMethod: argv['url-method'],
    },
    summary: {
      total, flagged,
      written,
      byReasonAll,
      byReasonWritten
    },
    results: outputResults,
  }, null, 2), 'utf8');

  // CSV
  const header = [
    'table','id','local_identifier',
    'expedition_id','expedition_code','expedition_title','expedition_identifier',
    'created','modified','parent_identifier',
    'processed','processingErrors','nonJpeg','badJson',
    'urlChecked','urlOk','urlStatus','urlContentType',
    'representativeUrl','filename','reasons'
  ];
  const toCsv = (v) => `"${(v ?? '').toString().replace(/"/g, '""')}"`;
  const lines = [header.join(',')];
  for (const r of outputResults) {
    lines.push([
      r.table,
      r.id,
      r.local_identifier,
      r.expedition_id,
      r.expedition_code,
      r.expedition_title,
      r.expedition_identifier,
      r.created,
      r.modified,
      r.parent_identifier,
      String(r.processed),
      String(r.processingErrors),
      String(r.nonJpeg),
      String(r.badJson),
      String(r.urlChecked),
      r.urlOk === null ? '' : String(r.urlOk),
      r.urlStatus === null ? '' : String(r.urlStatus),
      r.urlContentType || '',
      r.representativeUrl || '',
      r.filename || '',
      (r.reasons || []).join('|'),
    ].map(toCsv).join(','));
  }
  await fs.promises.writeFile(csvPath, lines.join('\n'), 'utf8');

  // --- pruning (supports --prune-filtered and --dry-run) ---
  const deletions = [];
  const pruneMode = argv['prune-filtered']
    ? 'filtered'
    : argv['prune-errors']
      ? 'errors'
      : argv['prune-all-flagged']
        ? 'all_flagged'
        : null;

  const resultIndex = new Map(results.map(r => [`${r.table}|${String(r.id)}`, r]));

  let toDelete = [];
  if (pruneMode === 'filtered') {
    if (!written) console.warn('⚠️  prune-filtered requested but filtered report is empty; nothing to delete.');
    toDelete = outputResults.filter(r => r.flagged).map(r => ({ table: r.table, id: r.id }));
  } else if (pruneMode === 'errors') {
    results.forEach(r => { if (r.processingErrors) toDelete.push({ table: r.table, id: r.id }); });
  } else if (pruneMode === 'all_flagged') {
    results.forEach(r => { if (r.flagged) toDelete.push({ table: r.table, id: r.id }); });
  }

  // dry-run plan
  if (pruneMode && toDelete.length && argv['dry-run']) {
    tick(`DRY RUN: planning to prune ${toDelete.length} rows... (no changes made)`);
    const planCsv = path.join(outDir, `planned_deletions_${stamp}.csv`);
    const planHeader = ['table','id','expedition_id','local_identifier','reasons'];
    const planLines = [planHeader.join(',')];
    for (const x of toDelete) {
      const r = resultIndex.get(`${x.table}|${String(x.id)}`) || {};
      const reasons = (r.reasons || []).join('|');
      planLines.push([x.table, x.id, r.expedition_id ?? '', r.local_identifier ?? '', reasons].map(toCsv).join(','));
    }
    await fs.promises.writeFile(planCsv, planLines.join('\n'), 'utf8');
    console.log(`DRY RUN plan written: ${planCsv}`);
  }

  // execute deletes
  if (pruneMode && toDelete.length && !argv['dry-run']) {
    const client = await pool.connect();
    try {
      tick(`Pruning (${pruneMode}) ${toDelete.length} rows...`);
      await client.query('BEGIN');
      const grouped = toDelete.reduce((acc, x) => { (acc[x.table] ||= []).push(String(x.id)); return acc; }, {});
      for (const [table, ids] of Object.entries(grouped)) {
        const sql = `DELETE FROM ${qtable(schema, table)} WHERE id::text = ANY($1::text[])`;
        const res = await client.query(sql, [ids]);
        deletions.push({ table, count: res.rowCount || 0 });
      }
      await client.query('COMMIT');
    } catch (e) {
      await client.query('ROLLBACK');
      throw e;
    } finally {
      client.release();
    }
  } else if (pruneMode && !toDelete.length) {
    console.log(`Nothing to prune for mode=${pruneMode}.`);
  }

  // console summary
  console.log('--- Photo Audit (PG) ---');
  console.log(`Schema:  ${schema}`);
  console.log(`Tables:  ${tableNames.join(', ')}`);
  console.log(`Scanned: ${total}`);
  console.log(`Flagged: ${flagged}`);
  console.log(`Written (after filters): ${written}`);
  console.log('By reason (all):', byReasonAll);
  console.log('By reason (written):', byReasonWritten);
  if (pruneMode) console.log(`Prune mode: ${pruneMode}${argv['dry-run'] ? ' (dry-run)' : ''}`);
  if (deletions.length) console.log('Pruning executed:', deletions);
  console.log(`JSON: ${jsonPath}`);
  console.log(`CSV:  ${csvPath}`);
  console.log(`Elapsed: ${((Date.now()-t0)/1000).toFixed(1)}s`);

  // email
  if (argv.email) {
    const transporter = nodemailer.createTransport({
      service: 'gmail',
      auth: { user: MAIL.user, pass: MAIL.pass },
    });
    const prunedCount = deletions.reduce((s, d) => s + d.count, 0);
    const subject = `Photo Audit (PG ${schema}) — ${flagged}/${total} flagged; reported ${written}`
      + (argv['dry-run'] ? ' [DRY RUN]' : prunedCount ? `; pruned ${prunedCount}` : '');

    const body = [
      `DB: ${PG.host} / ${PG.database} (schema ${schema})`,
      `Tables: ${tableNames.join(', ')}`,
      `Scanned: ${total}`,
      `Flagged: ${flagged}`,
      `Reported (after filters): ${written}`,
      `Reasons (all):`,
      ...Object.entries(byReasonAll).map(([k,v]) => `  - ${k}: ${v}`),
      `Reasons (reported):`,
      ...Object.entries(byReasonWritten).map(([k,v]) => `  - ${k}: ${v}`),
      pruneMode ? `Prune mode: ${pruneMode}${argv['dry-run'] ? ' (DRY RUN)' : ''}` : '',
      !argv['dry-run'] && prunedCount ? `Pruned: ${prunedCount}` : '',
      argv['dry-run'] ? 'NOTE: DRY RUN — no deletions executed. See planned_deletions CSV if present.' : '',
      '',
      argv.limit ? `NOTE: LIMIT ${argv.limit} applied.` : '',
      argv.join ? '' : 'NOTE: Expeditions join was skipped (--no-join).',
      argv.since ? `NOTE: --since=${argv.since}` : '',
      argv.where ? `NOTE: Extra WHERE: ${argv.where}` : '',
      argv['only-flagged'] ? 'NOTE: Only flagged rows were included in the report.' : '',
      wantedReasons.length ? `NOTE: reasons filter: ${wantedReasons.join(', ')}` : '',
      argv['prune-filtered'] ? 'NOTE: prune-filtered was used.' : '',
      `URL check: concurrency=${argv.concurrency}, timeout=${argv['url-timeout']}ms, retries=${argv['url-retries']}, method=${argv['url-method']}, assumeByExt=${argv['assume-image-by-ext']}`
    ].filter(Boolean).join('\n');

    const attachments = [
      { filename: path.basename(jsonPath), path: jsonPath },
      { filename: path.basename(csvPath), path: csvPath },
    ];
    const planCsv = path.join(outDir, `planned_deletions_${stamp}.csv`);
    try { await fs.promises.access(planCsv); attachments.push({ filename: path.basename(planCsv), path: planCsv }); } catch {}

    await transporter.sendMail({ from: MAIL.from, to: MAIL.to, subject, text: body, attachments });
    console.log('📧 Email sent to', MAIL.to);
  }

  await pool.end();
})().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});

