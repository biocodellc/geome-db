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
```
pg_restore --disable-triggers -d bcid bcid.pgsql
pg_restore --disable-triggers -d biscicol biscicol_all.pgsql
```

# Restoring Other Connections
We want to restore crontab and nginx configuration files.  These configurations are stored
under deploy/production

