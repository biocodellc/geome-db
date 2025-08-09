#!/usr/bin/env node
'use strict';

/**
 * Fix imageProcessingErrors when the image URL is actually valid.
 *
 * - Looks in schema PGSCHEMA (default: network_1) tables: event_photo, sample_photo
 * - Only considers rows where:
 *       data.imageProcessingErrors has some value (string/array/etc.)
 *       AND lower(data->>'processed') = 'true'
 * - Checks a representative URL (HEAD with GET fallback, retries, timeout, UA)
 * - If it returns image content, removes imageProcessingErrors from data and updates modified
 * - Emails summary + attaches CSV and a SQL "plan" file
 *
 * Flag:
 *   --dry-run   Preview SQL, write plan files & email, but DO NOT update DB.
 */

const fs = require('fs');
const path = require('path');
require('dotenv').config();

const { Pool } = require('pg');
const nodemailer = require('nodemailer');

const argv = {
  dryRun: process.argv.includes('--dry-run'),
};

// ---------- config from env ----------
const PG = {
  host: process.env.PGHOST,
  port: Number(process.env.PGPORT || 5432),
  user: process.env.PGUSER,
  password: process.env.PGPASSWORD,
  database: process.env.PGDATABASE,
  ssl: /^true$/i.test(process.env.PGSSL || ''),
  schema: process.env.PGSCHEMA || 'network_1',
};

const MAIL = {
  user: process.env.MAIL_USER,
  pass: process.env.MAIL_PASS,
  to: process.env.MAIL_TO,
  from: process.env.MAIL_FROM || process.env.MAIL_USER,
};

// ---------- small helpers ----------
const nowStamp = () => {
  const d = new Date(); const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}_${p(d.getHours())}-${p(d.getMinutes())}-${p(d.getSeconds())}`;
};
const qident = (s) => `"${String(s).replace(/"/g, '""')}"`;
const qtable = (schema, table) => `${qident(schema)}.${qident(table)}`;

function pickRepresentativeUrl(d) {
  const order = ['img2048','img1024','img512','img256','img128'];
  for (const k of order) if (d?.[k] && /^https?:\/\//i.test(d[k])) return d[k];
  if (d?.originalUrl && /^https?:\/\//i.test(d.originalUrl) && d.originalUrl.toLowerCase() !== 'none') return d.originalUrl;
  const cands = Object.values(d || {}).filter(v => typeof v === 'string' && /^https?:\/\//i.test(v));
  return cands[0] || null;
}

function hasImageExtension(url) {
  return typeof url === 'string' && /\.(jpe?g|png|gif|webp|tiff?)(\?|#|$)/i.test(url);
}

// lightweight concurrency limiter (no deps)
function makeLimiter(concurrency = 6) {
  let active = 0; const queue = [];
  const next = () => { active--; if (queue.length) queue.shift()(); };
  return (fn) => (...args) => new Promise((resolve, reject) => {
    const run = () => { active++; Promise.resolve(fn(...args)).then(resolve, reject).finally(next); };
    if (active < concurrency) run(); else queue.push(run);
  });
}

// Robust URL check: HEAD then GET fallback, retries, timeout, UA; accept missing CT if extension is image
async function checkImageUrl(url, { timeoutMs = 15000, retries = 2, userAgent = 'PhotoAuditBot/1.0 (+you@domain)' } = {}) {
  const headers = { 'User-Agent': userAgent };

  function withTimeout(method) {
    const controller = new AbortController();
    const t = setTimeout(() => controller.abort(), timeoutMs);
    return fetch(url, { method, redirect: 'follow', signal: controller.signal, headers })
      .finally(() => clearTimeout(t));
  }

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      let res = await withTimeout('HEAD');
      if (!res.ok || !res.headers.get('content-type')) {
        res = await withTimeout('GET');
      }
      const ok = res.ok;
      let ct = res.headers.get('content-type') || '';

      if (!ct && hasImageExtension(url)) ct = 'image/unknown-by-ext'; // treat as image if extension indicates so

      const isImage = /^image\//i.test(ct) || ct === 'image/unknown-by-ext';
      return { ok, status: res.status, contentType: ct, isImage };
    } catch (e) {
      if (attempt < retries) {
        await new Promise(r => setTimeout(r, 250 * (attempt + 1)));
        continue;
      }
      return { ok: false, status: 0, error: String(e), isImage: false };
    }
  }
}

// Extract a readable error presence from JSON
function hasProcessingErrorsValue(data) {
  if (!data || typeof data !== 'object') return false;
  if (!Object.prototype.hasOwnProperty.call(data, 'imageProcessingErrors')) return false;
  const v = data.imageProcessingErrors;
  if (v == null) return false;
  if (Array.isArray(v)) return v.length > 0;
  if (typeof v === 'string') return v.trim().length > 0 && v.trim() !== '[]';
  if (typeof v === 'object') return Object.keys(v).length > 0;
  return true;
}

function isProcessedTrue(data) {
  const v = data?.processed;
  if (typeof v === 'boolean') return v === true;
  if (v == null) return false;
  return String(v).trim().toLowerCase() === 'true';
}

// ---------- main ----------
(async function main() {
  if (!PG.host || !PG.user || !PG.password || !PG.database) {
    console.error('Missing Postgres env vars. Need PGHOST, PGUSER, PGPASSWORD, PGDATABASE.');
    process.exit(1);
  }
  if (!MAIL.user || !MAIL.pass || !MAIL.to) {
    console.error('Missing mail env vars. Need MAIL_USER, MAIL_PASS, MAIL_TO.');
    process.exit(1);
  }

  const pool = new Pool({
    host: PG.host, port: PG.port, user: PG.user, password: PG.password, database: PG.database, ssl: PG.ssl,
    keepAlive: true, connectionTimeoutMillis: 10000, idleTimeoutMillis: 10000,
  });
  pool.on('error', (err) => console.warn('PG pool idle client error:', err.message));

  const schema = PG.schema;
  const tables = ['event_photo', 'sample_photo'];

  const t0 = Date.now();
  const outDir = path.join(process.cwd(), 'reports');
  await fs.promises.mkdir(outDir, { recursive: true });
  const stamp = nowStamp();

  const rowsByTable = {};
  const candidatesByTable = {};
  const fixesByTable = {};

  // fetch only likely candidates (processed true AND has some imageProcessingErrors text)
  for (const table of tables) {
    const sql = `
      SELECT id, local_identifier, expedition_id, data
      FROM ${qtable(schema, table)} AS p
      WHERE (p.data ? 'imageProcessingErrors')
        AND COALESCE(NULLIF(TRIM(p.data->>'imageProcessingErrors'), ''), '') <> ''
        AND LOWER(COALESCE(p.data->>'processed','false')) = 'true'
    `;
    const { rows } = await pool.query(sql);
    rowsByTable[table] = rows;
    // further JS-level filter for odd shapes
    candidatesByTable[table] = rows.filter(r => hasProcessingErrorsValue(r.data) && isProcessedTrue(r.data));
  }

  // URL checks
  const limit = makeLimiter(6);
  for (const table of tables) fixesByTable[table] = [];
  const checks = [];

  for (const table of tables) {
    for (const r of candidatesByTable[table]) {
      checks.push(limit(async () => {
        const d = r.data || {};
        const url = pickRepresentativeUrl(d);
        if (!url) return; // nothing to check

        const res = await checkImageUrl(url);
        if (res.ok && res.isImage) {
          // mark for fix (remove imageProcessingErrors)
          fixesByTable[table].push({ id: r.id, local_identifier: r.local_identifier, expedition_id: r.expedition_id, url, status: res.status, contentType: res.contentType });
        }
      })());
    }
  }
  await Promise.all(checks);

  // Prepare SQL plan files + CSV
  const planSqlPath = path.join(outDir, `fix_plan_${stamp}.sql`);
  const csvPath     = path.join(outDir, `fix_results_${stamp}.csv`);

  const planLines = [];
  const csvHeader = ['table','id','local_identifier','expedition_id','url','status','content_type'];
  const csvLines = [csvHeader.join(',')];

  function toCsv(v) { return `"${(v ?? '').toString().replace(/"/g, '""')}"`; }

  for (const table of tables) {
    const ids = fixesByTable[table].map(x => String(x.id));
    if (!ids.length) continue;

    // batched SQL (safe, id::text match)
    const batchSize = 1000;
    for (let i = 0; i < ids.length; i += batchSize) {
      const chunk = ids.slice(i, i + batchSize).map(id => `'${id.replace(/'/g, "''")}'`).join(',');
      planLines.push(
        `UPDATE ${qtable(schema, table)}\n` +
        `SET data = (data - 'imageProcessingErrors'), modified = now()\n` +
        `WHERE id::text IN (${chunk});\n`
      );
    }

    // CSV rows
    for (const x of fixesByTable[table]) {
      csvLines.push([table, x.id, x.local_identifier, x.expedition_id, x.url, x.status, x.contentType].map(toCsv).join(','));
    }
  }

  await fs.promises.writeFile(planSqlPath, planLines.join('\n'), 'utf8');
  await fs.promises.writeFile(csvPath, csvLines.join('\n'), 'utf8');

  // Apply updates if not dry-run
  const applied = [];
  if (!argv.dryRun) {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      for (const table of tables) {
        const ids = fixesByTable[table].map(x => String(x.id));
        if (!ids.length) continue;
        const sql = `
          UPDATE ${qtable(schema, table)}
          SET data = (data - 'imageProcessingErrors'), modified = now()
          WHERE id::text = ANY($1::text[])
        `;
        const res = await client.query(sql, [ids]);
        applied.push({ table, count: res.rowCount || 0 });
      }
      await client.query('COMMIT');
    } catch (e) {
      await client.query('ROLLBACK');
      throw e;
    } finally {
      client.release();
    }
  }

  // Email results
  const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: { user: MAIL.user, pass: MAIL.pass },
  });

  const counts = {
    scanned: {
      event_photo: rowsByTable['event_photo']?.length || 0,
      sample_photo: rowsByTable['sample_photo']?.length || 0,
    },
    candidates: {
      event_photo: candidatesByTable['event_photo']?.length || 0,
      sample_photo: candidatesByTable['sample_photo']?.length || 0,
    },
    fixes: {
      event_photo: fixesByTable['event_photo']?.length || 0,
      sample_photo: fixesByTable['sample_photo']?.length || 0,
    },
  };

  const subjBits = [
    `Fix imageProcessingErrors ${argv.dryRun ? '(DRY RUN)' : ''}`.trim(),
    `event:${counts.fixes.event_photo}`,
    `sample:${counts.fixes.sample_photo}`
  ];
  const subject = subjBits.join(' | ');

  const body = [
    `DB: ${PG.host} / ${PG.database} (schema ${schema})`,
    `Mode: ${argv.dryRun ? 'DRY RUN (no updates applied)' : 'APPLIED'}`,
    '',
    `Scanned rows — event_photo: ${counts.scanned.event_photo}, sample_photo: ${counts.scanned.sample_photo}`,
    `Candidates (processed=true & has imageProcessingErrors) — event_photo: ${counts.candidates.event_photo}, sample_photo: ${counts.candidates.sample_photo}`,
    `Valid images (will fix) — event_photo: ${counts.fixes.event_photo}, sample_photo: ${counts.fixes.sample_photo}`,
    '',
    argv.dryRun
      ? 'See attached SQL plan (fix_plan_*.sql).'
      : `Applied updates: ${applied.map(a => `${a.table}:${a.count}`).join(', ') || 'none'}`,
  ].join('\n');

  const attachments = [
    { filename: path.basename(csvPath),     path: csvPath },
    { filename: path.basename(planSqlPath), path: planSqlPath },
  ];

  await transporter.sendMail({
    from: MAIL.from,
    to: MAIL.to,
    subject,
    text: body,
    attachments,
  });

  console.log('Done.', { counts, applied, csvPath, planSqlPath, elapsed_s: ((Date.now()-t0)/1000).toFixed(1) });

  await pool.end();
})().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});

