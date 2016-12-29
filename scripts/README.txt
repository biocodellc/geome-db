#Contained here is a script for dumping data from DIPNet and loading into a mysql table,
#which an installed IPT can reference

# create the table -- first time only
mysql -u {user} -p{pass} < create_table.sql

# generate the dump
python dumper.py -u {user} -p {pass} -o /tmp/dump.csv

# load it up!
mysql -u {user} -p{pass} < load_table.sql
