#!/bin/bash
# backup databases

pg_dump --clean --if-exists --lock-wait-timeout=5000 --no-password \
  -U biscicol -h 127.0.0.1 -p 5432 -Fc \
  -n public -n network_1 --exclude-table-data=network_1.audit_table biscicol \
  > /media/volume/Elastic/backups/archives/offsite/biscicol_all.pgsql

# Dump `bcid` database
pg_dump --clean --if-exists --lock-wait-timeout=5000 --no-password \
  -U bcid -h 127.0.0.1 -p 5432 -Fc \
  bcid > /media/volume/Elastic/backups/archives/offsite/bcid.pgsql


# tar and zip PGSQL backup files 
tar -czvf /media/volume/Elastic/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /media/volume/Elastic/backups/archives/offsite/*.pgsql

# cleanup
rm /media/volume/Elastic/backups/archives/offsite/*.pgsql

# daily backups saved every day
cp /media/volume/Elastic/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /media/volume/Elastic/backups/archives/daily/

# weekly backups saved on saturdays
dayOfWeek=`date +"%u"`
if [ "$dayOfWeek" = "6" ]; then
	cp /media/volume/Elastic/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /media/volume/Elastic/backups/archives/weekly/
fi
# monthly backupks saved on the first day of the month
dayOfMonth=`date +"%d"`
if [ "$dayOfMonth" = "01" ]; then
	cp /media/volume/Elastic/backups/archives/offsite/"`date +"%Y-%m-%d"`"-backups.tar.gz /media/volume/Elastic/backups/archives/monthly/
fi

# offsite backup directory contains backups for just 1 day
# 1380 minutes is 23 hours, so will effectively just keep 1 copy 
find /media/volume/Elastic/backups/archives/offsite/ -type f -mmin +1380 -delete

# daily backup directory contains backups for 14 days
find /media/volume/Elastic/backups/archives/daily/ -type f -mtime +14 -delete

# weekly backup directory contains backups for 8 weeks
find /media/volume/Elastic/backups/archives/weekly/ -type f -mtime +56 -delete

# monthly  directory contains backups for 12 months
find /media/volume/Elastic/backups/archives/monthly/ -type f -mtime +365 -delete

