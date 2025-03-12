# backup databases
#pg_dump --no-password -U biscicol -h 127.0.0.1 -p5432 biscicol > /opt/backups/archives/offsite/biscicol.pgsql
#pg_dump --clean --if-exists --lock-wait-timeout=5000 --no-password -U biscicol -h 127.0.0.1 -p 5432 -Fc -n public biscicol > /opt/backups/archives/offsite/biscicol_public.pgsql
#pg_dump --clean --if-exists --lock-wait-timeout=5000 --no-password --exclude-table-data=network_1.audit_table -Fc -U biscicol -h 127.0.0.1 -p 5432 -n network_1 biscicol > /opt/backups/archives/offsite/biscicol_network_1.pgsql
#pg_dump --clean --if-exists --lock-wait-timeout=5000 --no-password -U bcid -h 127.0.0.1 -p 5432 bcid -Fc > /opt/backups/archives/offsite/bcid.pgsql
# Dump `biscicol` database with both `public` and `network_1` schemas (excluding data for audit_table)
pg_dump --clean --if-exists --lock-wait-timeout=5000 --no-password \
  -U biscicol -h 127.0.0.1 -p 5432 -Fc \
  -n public -n network_1 --exclude-table-data=network_1.audit_table biscicol \
  > /opt/backups/archives/offsite/biscicol_all.pgsql

# Dump `bcid` database
pg_dump --clean --if-exists --lock-wait-timeout=5000 --no-password \
  -U bcid -h 127.0.0.1 -p 5432 -Fc \
  bcid > /opt/backups/archives/offsite/bcid.pgsql


# tar and zip PGSQL backup files 
tar -czvf /opt/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /opt/backups/archives/offsite/*.pgsql

# cleanup
rm /opt/backups/archives/offsite/*.pgsql

# daily backups saved every day
cp /opt/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /opt/backups/archives/daily/

# weekly backups saved on saturdays
dayOfWeek=`date +"%u"`
if [ "$dayOfWeek" = "6" ]; then
	cp /opt/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /opt/backups/archives/weekly/
fi
# monthly backupks saved on the first day of the month
dayOfMonth=`date +"%d"`
if [ "$dayOfMonth" = "01" ]; then
	cp /opt/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /opt/backups/archives/monthly/
fi

# offsite backup directory contains backups for just 1 day
# 1380 minutes is 23 hours, so will effectively just keep 1 copy 
find /opt/backups/archives/offsite/ -type f -mmin +1380 -delete

# daily backup directory contains backups for 14 days
find /opt/backups/archives/daily/ -type f -mtime +14 -delete

# weekly backup directory contains backups for 8 weeks
find /opt/backups/archives/weekly/ -type f -mtime +56 -delete

# monthly  directory contains backups for 12 months
find /opt/backups/archives/monthly/ -type f -mtime +365 -delete

