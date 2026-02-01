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

# Restoring Other Connections
We want to restore crontab and nginx configuration files.  These configurations are stored
under deploy/production

# Local startup (Jetty)
This app runs as a WAR in Jetty. The local flow is: build the WAR, deploy it into Jetty's `webapps`, then run Jetty.

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

4) Run Jetty:
```
/usr/local/bin/jetty run jetty.http.port=8080
```

5) Visit:
```
http://localhost:8080/geome-db/
```

Notes:
- First startup can take 1-2 minutes while Spring initializes.
- Logs are under `/usr/local/opt/jetty/libexec/logs/geome-db.log`.
- If you see warnings about Jersey resources, they are not fatal to startup.
