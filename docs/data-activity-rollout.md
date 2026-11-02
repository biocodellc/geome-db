# Data activity and sorting rollout

1. Complete the preflight and full backup below. As the owner of the public and network tables, run `PGOPTIONS='-c lock_timeout=10s' psql -X -v ON_ERROR_STOP=1 -f scripts/updateDataActivity.sql`. The script is repeatable and installs tracking before backfilling audit history. It scans history only during migration; API reads use stored summaries.
2. Deploy the backend, rerun the repeatable migration to reconcile any network/entity tables created during the deployment window, then deploy geome-ui. New API fields are additive: `created`, `latestDataModification` (UTC ISO 8601), `latestDataModificationUsername`; expedition statistics also include `expeditionId`, `ownerUsername`, `ownerUserId`.
3. Verify a member upload, a deletion and a record move update matching date/user pairs; verify settings edits leave activity unchanged. Compare anonymous and member-visible rows.
4. Unknown historical actors remain null; system audit actors retain their system labels. Existing project timestamps newer than retained audit history are preserved with no inferred username. Summary maintenance may update the legacy general-purpose `modified` column; it is not used for data-only activity.

Automatic photo derivative processing remains attributed to the system. User photo metadata uploads are attributed in their authenticated request; asynchronous dataset validation preserves its initiating actor with a scoped thread context.

Rollback: roll back the UI and backend first; leave the additive nullable columns and tracking in place. Restore old triggers only as a deliberate database rollback, because they cannot retain accurate timestamp/actor pairs. No production database is modified by building or testing these changes.

## Preflight and full backup

- Record the backend and geome-ui commit IDs and retain the previous deployed artifacts. Include the new, currently untracked migration, helper SQL, actor class, and tests when preparing the backend commit. Ship `scripts/updateDataActivity.sql` and `scripts/dataActivityFunctions.sql` from the same revision; the migration loads its helper using a relative `\ir`.
- Use an explicitly configured database connection (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and a password file or approved credential mechanism). Confirm the database name, server and migration role before running SQL. The role needs ownership of the affected public and network tables and the ability to create or replace the tracking functions; application API credentials alone are insufficient.
- **Do not use `scripts/backupDatabase.sh` as the only pre-migration backup.** It excludes `network_1.audit_table` data, which this migration needs for historical attribution. Take a full backup including all network audit tables and verify it can be restored on a separate disposable PostgreSQL server.
- **Do not run `fimsCreateTables.sql` against an existing database.** It contains table drops and is only for the disposable fresh-install check below.

With the intended source connection already configured and a durable backup directory outside the checkout:

```sh
: "${PGHOST:?Set the intended database host explicitly}"
: "${PGDATABASE:?Set the intended database name explicitly}"
: "${PGUSER:?Set the approved database role explicitly}"
: "${GEOME_ACTIVITY_BACKUP_DIR:?Set a durable backup directory outside the checkout}"
psql -X -v ON_ERROR_STOP=1 -c "SELECT current_database(), session_user, current_setting('TimeZone'), version();"
umask 077
mkdir -p "$GEOME_ACTIVITY_BACKUP_DIR"
activity_backup="$GEOME_ACTIVITY_BACKUP_DIR/data-activity-before-$(date -u +%Y%m%dT%H%M%SZ).dump"
pg_dump --format=custom --file="$activity_backup"
pg_restore --list "$activity_backup" > "$activity_backup.contents"
rg 'TABLE DATA .*audit_table' "$activity_backup.contents"
```

Use PostgreSQL client tools compatible with the source server. The archive listing verifies that audit data is included; it does not replace a restore check. Restore and rehearse the migration using the disposable server's own connection settings, then record its runtime and confirm enough storage is available for the backup and audit-history scan.

Run the production migration in a scheduled maintenance window. The DDL takes locks, and backfilling can hold project and expedition row locks until its transaction commits. The lock timeout above fails a blocked attempt instead of waiting indefinitely; investigate contention before retrying. The script commits trigger installation before backfilling, so a backfill failure does not undo the already-installed tracking. Inspect the error and rerun the same reviewed migration to finish; do not remove the new columns as an automatic rollback.

The database migration must finish before the new backend starts, because its entity mappings and statistics SQL reference the new columns. Existing backend/UI versions can continue using the additive schema during the deployment sequence. Rerun the migration after backend deployment, then deploy the corresponding UI revision. Confirm every network audit table has an enabled `set_data_activity` trigger and no entity tables retain the legacy `set_project_last_modified` trigger.

For the post-deployment smoke check, use a designated test project. Confirm an authenticated member upload, deletion, and move update both the timestamp and username; confirm title/description edits do not. Check anonymous and unrelated-user responses exclude member-only expeditions, while members and the project owner can see them. Do not modify real collection records solely for rollout testing.

## Local verification

Use disposable databases only. The SQL tests create fixture tables and data.

```sh
createdb geome_activity_test
psql -v ON_ERROR_STOP=1 -d geome_activity_test -f scripts/tests/dataActivity.sql
createdb geome_activity_fresh
psql -v ON_ERROR_STOP=1 -d geome_activity_fresh -f fimsCreateTables.sql
psql -v ON_ERROR_STOP=1 -d geome_activity_fresh -f scripts/tests/freshDataActivity.sql
./gradlew test --tests biocode.fims.repositories.DataActivityActorTest --tests biocode.fims.repositories.DataActivitySerializationTest --tests biocode.fims.rest.services.ProjectActivityAccessTest
```

The fresh-schema check also verifies statistics SQL, owner names and member-only expedition visibility. The migration check covers repeatability, historical deletion/move attribution, no-op updates, timestamp/event ordering, settings edits, and cascaded deletion. Fresh-schema installation now creates project configurations before projects, fixing their foreign-key dependency order.

## Local verification record: 2026-11-02

Validated the current backend working tree on PostgreSQL 16.15 and Java 8:

- Migration fixture, repeatability, and fresh-schema/statistics checks passed in a disposable cluster with TCP listening disabled.
- All nine targeted Java tests passed. The full unit suite passed with 373 tests successful and five skipped.
- `./gradlew --offline --no-daemon -Penvironment=production test war` succeeded and produced `build/libs/geome-db.war`. The artifact was not deployed.
- A full custom-format backup of the fresh fixture restored successfully to a separate disposable database, retained its audit row, and accepted migration replay. This checks the procedure, not production-scale backup or backfill runtime.

SQL logs and the fixture backup are retained locally in `/private/tmp/geome-activity.IH8BVW`; the temporary cluster has been stopped. No production database was accessed or modified for these checks. Production backup/restore verification, migration-role checks, and validation of the corresponding geome-ui revision remain rollout prerequisites. Rerun the relevant checks if the implementation changes after this verification.

## Production migration record: 2026-11-02

At the user's subsequent request, `scripts/updateDataActivity.sql` and its matching helper were applied successfully to production database `biscicol` on `149.165.170.158` through SSH as PostgreSQL role `postgres`, with `lock_timeout=10s`. The application role owns the affected tables but lacks `CREATE` on the public schema; its privileges were not changed.

Before applying the migration:

- Took a full custom-format backup including audit history (168,544,377 bytes; SHA-256 `3da01a0229b2569bda683a3aa589c8c1e616a0133a2b229b53cb873a942f35b3`).
- Copied it locally, verified the checksum, restored it into an isolated PostgreSQL 16 cluster, and confirmed 455 projects, 1,860 expeditions, and 604,725 audit rows.
- Successfully rehearsed the migration against the restored production data and ran the same post-migration verification SQL. The backfill rehearsal took approximately two seconds; the local cluster was stopped afterward.

Production verification confirmed all five added columns, the enabled `network_1.audit_table` activity trigger and expedition cascade-attribution trigger, absence of legacy `set_project_last_modified` triggers, and no usernames without corresponding timestamps. There are 561 expedition activity summaries with actors and 45 project summaries with actors; legacy project timestamps without a retained matching actor remain preserved. The MDBC project summary attributes its latest data activity to `heathershull`, and LGOM attributes its latest activity to `stweedt`.

Backup, reviewed SQL fingerprints, and execution logs are retained in both locations:

- Database host: `/home/exouser/geome-data-activity-backups/20261102T145500Z/`
- Local copy: `/Users/jdeck/Downloads/geome-data-activity-backups/20261102T145500Z/`

Only the database migration was performed. Backend and geome-ui deployment, the post-backend reconciliation rerun, and the designated-test-project smoke checks remain pending.
