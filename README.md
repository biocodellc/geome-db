This code drives the [GeOMe database](http://geome-db.org/).

The GeOMe database validates field data and tracks physical samples and collecting events, using globally unique and persistent identifiers, for reference in collections management systems, laboratory information management systems, seqence repositories, and publications.

# Getting Started:
Tools used are gradle, java/jetty, postgresql.

# Production Names/Services
The following end-points / repos are associated with this codebase and are used to get things running...
 * api.geome-db.org (The backend for the GEOME database) github repo: https://github.com/biocodellc/geome-db
 * bcid.geome-db.org  (Manage minting identifiers) github repo: https://github.com/biocodellc/bcid
 * photos.geome-db.org (Manage photo handling) github repo: https://github.com/biocodellc/biocode-fims-photos


The GEOME database is built using Gradle scripts (contained in this repository) and running on Jetty Server that stores data in a postgres database. GEOME is supported from the Gordon and Betty Moore Foundation as part of the Moorea Biocode Project (http://biocode.berkeley.edu/), the National Science Foundation as part of the Diversity of the IndoPacific (http://indopacificnetwork.wikispaces.com/), the Barcode of Wildlife Project (http://www.barcodeofwildlife.org/), the Smithsonian Museum of Natural History (http://www.mnh.si.edu/),  the Berkeley Natural History Museums (http://bnhm.berkeley.edu/), and the Autonomous Reef Monitoring Network (website coming soon).

# Restoring database
The dump file is stored in a non-text format, which reduces the size of the dump and allows us to 
temporarily disable triggers during reoload.
```
pg_restore --disable-triggers -d bcid bcid.pgsql
pg_restore --disable-triggers -d biscicol biscicol_all.pgsql
```

# Creating a development seed
Use `scripts/dev-seed/create-dev-seed.sh` to turn a production backup archive
into a smaller, scrubbed local seed for onboarding or local development:
```
scripts/dev-seed/create-dev-seed.sh ~/Downloads/2026-08-17-backups.tar.gz dev-seed.tar.gz
```

The script requires PostgreSQL 16 tools. It starts a temporary PostgreSQL 16
server, restores `bcid.pgsql` and `biscicol_all.pgsql`, trims/scrubs the
restored databases, writes `dev-seed-bcid.sql` and `dev-seed-biscicol.sql`,
packages them as `dev-seed.tar.gz`, and removes the temporary server data
directory. The generated seed archive and extracted SQL files are ignored by git.

Defaults:
- keeps 5 small public/discoverable projects
- keeps at most 3 non-empty expeditions per retained project
- replaces all GeOMe users with one local user
- replaces OAuth and BCID client secrets with deterministic fake values
- removes OAuth tokens/nonces, invites, SRA submissions, and config history
- drops accidental backup tables in `network_1`

Local credentials created by the seed:
```
username: dev
password: password
oauth client id: geome-dev-client
oauth client secret: geome-dev-secret
bcid client id: geome-dev-client
bcid client secret: geome-dev-secret
```

To keep specific projects instead of the automatic small-project selection:
```
DEV_SEED_PROJECT_IDS=650,692 scripts/dev-seed/create-dev-seed.sh ~/Downloads/2026-08-17-backups.tar.gz dev-seed.tar.gz
```

To change the automatic size:
```
DEV_SEED_PROJECT_LIMIT=10 DEV_SEED_EXPEDITIONS_PER_PROJECT=5 \
  scripts/dev-seed/create-dev-seed.sh ~/Downloads/2026-08-17-backups.tar.gz dev-seed.tar.gz
```

Restore the generated seed into PostgreSQL 16 with:
```
tar -xzf dev-seed.tar.gz
/usr/local/opt/postgresql@16/bin/psql -f dev-seed-bcid.sql
/usr/local/opt/postgresql@16/bin/psql -f dev-seed-biscicol.sql
```

# Restoring Other Connections
We want to restore crontab and nginx configuration files.  These configurations are stored
under deploy/production

# Deploy scripts
The root deploy wrapper auto-detects the current environment:
```
./deploy.sh
```

Use explicit scripts when needed:
```
./deploy-local.sh
./deploy-server.sh
```

Local defaults target Homebrew Jetty at `/usr/local/opt/jetty/libexec` and build with
`-Penvironment=local`. Server defaults target `/usr/share/jetty9`, build with
`-Penvironment=production`, install the WAR with `sudo`, and restart `jetty9`.

# Local startup (Jetty)
This app runs as a WAR in Jetty. The local flow is: use remote database credentials, build the WAR, deploy it into Jetty's `webapps`, then run Jetty.

Remote database notes:
- A local Postgres instance is not required if `src/main/environment/local/biocode-fims-database.properties` points to a remote `bcidUrl`.
- Current local env uses remote DB URL: `jdbc:postgresql://149.165.170.158:5432/biscicol`.
- If needed, confirm DB access before startup:
```
psql -h 149.165.170.158 -p 5432 -U biscicol -d biscicol -c "select 1;"
```

1) Build the WAR (uses `environment = local` from `gradle.properties`):
```
./gradlew -Penvironment=local war
```

2) Create a Jetty context file (only needed once; avoids an empty `geome-db.xml` parse error):
```
cat <<'EOF' > /usr/local/opt/jetty/libexec/webapps/geome-db.xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">
<Configure class="org.eclipse.jetty.webapp.WebAppContext">
  <Set name="contextPath">/geome-db</Set>
  <Set name="war">/usr/local/opt/jetty/libexec/webapps/geome-db.war</Set>
</Configure>
EOF
```

3) Deploy the WAR:
```
cp build/libs/geome-db.war /usr/local/opt/jetty/libexec/webapps/geome-db.war
```

4) Run Jetty (use `start.jar` with `servlets` module):
```
java -jar /usr/local/opt/jetty/libexec/start.jar \
  --module=servlets \
  jetty.base=/usr/local/opt/jetty/libexec \
  jetty.http.port=8081
```

5) Check app health:
```
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/geome-db/
```

Notes:
- First startup can take 1-2 minutes while Spring initializes.
- Logs are under `/usr/local/opt/jetty/libexec/logs/geome-db.log`.
- `403` from `/geome-db/` is acceptable and usually means the app is up and auth is being enforced.
- If you see warnings about Jersey resources, they are not fatal to startup.

# Generate report.json for geome-configurations
Use the reports endpoint to generate a JSON report file that can be consumed by geome-configurations.
Detailed service docs: `docs/reporting-service.md`.

Local (Jetty on `8081`):
```
curl -sS "http://127.0.0.1:8081/geome-db/reports/summary?includePublic=true&includePrivate=true" > report.json
```

Production API:
```
curl -sS "https://api.geome-db.org/geome-db/reports/summary?includePublic=true&includePrivate=true&access_token=YOUR_TOKEN" > report.json
```

Optional filters:
- `teamId=<project_configurations.id>`
- `projectId=<projects.id>`
- `includePrivate=true|false` (default `true`; requires authenticated user to include private/member projects)
- `topUsersLimit=<int>` (default `25`, max `500`)
- `fieldLimit=<int>`

Example with team filter:
```
curl -sS "https://api.geome-db.org/geome-db/reports/summary?includePublic=true&includePrivate=true&teamId=321&topUsersLimit=100&access_token=YOUR_TOKEN" > report.json
```

`projectSummary` rows include:
- `latestDataModification` (ISO timestamp from `projects.latest_data_modification`)
- `public` (project visibility flag)
