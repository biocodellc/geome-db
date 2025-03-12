# commands for restoring database using the backup that i used
# bcid
psql -U postgres -d bcid -c "SET session_replication_role = 'replica';"
pg_restore -U postgres -d bcid --clean --if-exists opt/backups/archives/offsite/bcid.pgsql > bcid_restore_output.log 2> bcid_restore_error.log
psql -U postgres -d bcid -c "SET session_replication_role = 'origin';"

# biscicol
psql -U postgres -d biscicol -c "SET session_replication_role = 'replica';"
pg_restore -U postgres -d biscicol --clean --if-exists opt/backups/archives/offsite/biscicol_all.pgsql > biscicol_restore_output.log 2> biscicol_restore_error.log
psql -U postgres -d biscicol -c "SET session_replication_role = 'origin';"
