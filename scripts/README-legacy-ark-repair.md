# Legacy Moorea Biocode ARK repair

`legacy_r2_ark_repair.py` maps old ARKs preserved in GeOMe project 75 records
to their current record pages, then optionally creates exact EZID records.
N2T uses those records to resolve old links to the individual specimen or tissue.

| Prefix | GeOMe entity | Main legacy identifier fields |
| --- | --- | --- |
| R2 (default) | Sample | `catalogNumber`, `otherCatalogNumbers`, `occurrenceID` |
| Q2 | Tissue | `tissueCatalogNumber`, `tissueOtherCatalogNumbers` |

The tissue mapping uses the tissue's own `bcid`, including its `.1`, `.2`, etc.
suffix. It does not infer identifiers from specimen identifiers. An old ARK
found on multiple current records is excluded and written to the conflicts CSV.

## Inventory

```bash
python3 scripts/legacy_r2_ark_repair.py --legacy-prefix Q2
```

The default is a dry run. Files are written under
`scripts/output/legacy-q2-ark-repair/` (or `legacy-r2-ark-repair/` for specimens):

- `legacy_q2_mapping.csv`: unambiguous mappings with source evidence.
- `legacy_q2_conflicts.csv`: excluded mappings requiring review.
- `legacy_q2_ezid_payloads.anvl`: proposed EZID metadata.
- `legacy_q2_ezid_results.csv`: dry-run or publication results.
- `legacy_q2_ezid_journal.jsonl`: append-only, flushed per-response publication log.

Generated audit files are ignored by Git. Preserve them locally before starting
a fresh inventory. A new completed run replaces the results CSV; the journal
retains responses from earlier publication runs.

## Publish a reviewed inventory

Set `EZID_USER` and `EZID_PASS` in the process environment, or pass
`--credentials-file /path/to/local/config`. Supported configuration keys are
`EZID_USER`/`EZID_PASS` or `ezidUser`/`ezidPass` (case-insensitive).
The file is read as key/value data; it is never executed. Do not commit credentials.

```bash
python3 scripts/legacy_r2_ark_repair.py \
  --legacy-prefix Q2 \
  --input-mapping scripts/output/legacy-q2-ark-repair/legacy_q2_mapping.csv \
  --apply --skip-existing-exact --workers 4
```

Authentication is checked before any identifier is written. CSV rows are checked
for entity, project, prefix, source evidence and target consistency. Use
`--only-ark ark:/21547/Q2MBIO3418.1` to process one reviewed mapping first.
`--workers` defaults to one and allows at most eight concurrent workers to limit
EZID load. Higher concurrency caused EZID concurrency-limit errors during the
Q2 repair; these rows need retrying with existing-record checks.

Existing exact EZID records are skipped with `--skip-existing-exact`.
Review skipped records whose existing target differs from the intended target.
Without that flag, differing existing targets are still protected unless
`--allow-existing-target-change` is explicitly supplied.

`--verify-after` follows N2T redirects after each successful publication and records
the final HTTP status and URL. A different URL is reported as
`published_verify_mismatch`; investigate or recheck propagation before reporting
that identifier as fixed. Existing records skipped on resume are also verified
when their stored EZID target matches the intended target.
For large inventories, run publication first and use `--only-ark` with
`--verify-after` for representative checks after the batch. Bulk N2T traffic can
produce HTTP 403/429 responses. If either is encountered, the script pauses
new resolver requests for the remainder of that run and marks later verification
as deferred; EZID publication continues. Allow a cooldown before checking again.

Network failures are logged as `failed`; a timed-out write may have succeeded on
the server. Resume using the reviewed CSV and `--skip-existing-exact` to avoid
rewriting records that already exist. Recheck failed and mismatched rows separately.
Authentication/input errors stop the run; row failures require inspecting the
results CSV or journal even if the batch process finishes normally.

`bash scripts/apply_remaining_legacy_q2_arks.sh` is a convenience wrapper for
Q2 publication with existing-record checks. It accepts the same options, including
`--input-mapping` and `--credentials-file`. The R2 wrapper remains available.

## Validation

```bash
python3 -m unittest discover -s scripts -p 'test_legacy_ark_repair.py' -v
```

Tests cover Chris Meyer's tissue example, R2 compatibility, tissue suffixes,
conflict exclusion, input scope, authentication failures, protected existing
targets, network failure journaling and redirect mismatches.

## Q2 repair completed October 6, 2026

The inventory covered 39,556 tissue records and 39,554 distinct legacy Q2 ARKs.
39,552 mappings were unambiguous. Two ARKs were present on both `.1` and `.2`
tissue records; the user explicitly selected the matching `.1` destination:

| Old ARK | Current tissue ARK |
| --- | --- |
| `ark:/21547/Q2MBIO24949.1` | `ark:/21547/CVL2MBIO24949.1` |
| `ark:/21547/Q2MBIO34899.1` | `ark:/21547/CYC2MBIO34899.1` |

All 39,554 targets were published or confirmed at EZID, with no outstanding
publication errors. Final N2T spot checks passed for 34 identifiers, covering all
31 expeditions, Chris's example and both reviewed conflicts. Chris's original
HTTP link resolves to `https://geome-db.org/record/ark:/21547/CVL2MBIO3418.1`.
These were representative resolver checks, not a full final check of every ARK.

The local output directory contains `legacy_q2_complete_mapping.csv`,
`legacy_q2_complete_results.csv`, `repair_summary.json`,
`verification_results.json`, and `conflict_review_decision.json`.
The original conflict CSV is retained as evidence. This repair changed EZID
records; it did not correct the duplicated catalog-number fields in GeOMe.
