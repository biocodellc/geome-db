# Development Seed

Use `scripts/dev-seed/create-dev-seed.sh` to turn a production backup archive into a
smaller, scrubbed local seed:

```bash
scripts/dev-seed/create-dev-seed.sh ~/Downloads/2026-08-17-backups.tar.gz dev-seed.tar.gz
```

The script requires PostgreSQL 16 tools. It starts a temporary PostgreSQL 16 server, restores `bcid.pgsql` and
`biscicol_all.pgsql`, trims/scrubs the restored databases, writes
`dev-seed-bcid.sql` and `dev-seed-biscicol.sql`, packages them as
`dev-seed.tar.gz`, and removes the temporary server data directory.

Defaults:

- keep 5 small public/discoverable projects
- keep at most 3 non-empty expeditions per retained project
- replace all GeOMe users with one local user
- replace OAuth and BCID client secrets with deterministic fake values
- remove OAuth tokens/nonces, invites, SRA submissions, and config history
- drop accidental backup tables in `network_1`

Local credentials created by the seed:

```text
username: dev
password: password
oauth client id: geome-dev-client
oauth client secret: geome-dev-secret
bcid client id: geome-dev-client
bcid client secret: geome-dev-secret
```

To keep specific projects instead of the automatic small-project selection:

```bash
DEV_SEED_PROJECT_IDS=650,692 scripts/dev-seed/create-dev-seed.sh ~/Downloads/2026-08-17-backups.tar.gz dev-seed.tar.gz
```

To change the automatic size:

```bash
DEV_SEED_PROJECT_LIMIT=10 DEV_SEED_EXPEDITIONS_PER_PROJECT=5 \
  scripts/dev-seed/create-dev-seed.sh ~/Downloads/2026-08-17-backups.tar.gz dev-seed.tar.gz
```

Restore the generated seed into PostgreSQL 16 with:

```bash
tar -xzf dev-seed.tar.gz
/usr/local/opt/postgresql@16/bin/psql -f dev-seed-bcid.sql
/usr/local/opt/postgresql@16/bin/psql -f dev-seed-biscicol.sql
```
