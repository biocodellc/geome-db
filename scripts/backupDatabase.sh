# backup databases
pg_dump --no-password -U biscicol -h 127.0.0.1 -p5432 biscicol > /opt/backups/archives/offsite/biscicol.pgsql
pg_dump --no-password -U bcid -h 127.0.0.1 -p5432 bcid > /opt/backups/archives/offsite/bcid.pgsql
tar -czvf /opt/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /opt/backups/archives/offsite/*.pgsql
rm /opt/backups/archives/offsite/*.pgsql
# delete files older than 20 days
find /opt/backups/archives/offsite/ -type f -mtime +20 -delete

