#!/usr/bin/env bash

url="$(curl -sg 'https://api.geome-db.org/records/Sample/csv?q=_select_:[Event,Tissue]%20_projects_:1' | jq -r '.url')"

wget -q -O /opt/ipt/data/dipnet.csv.zip "$url"
unzip -p /opt/ipt/data/dipnet.csv.zip > /opt/ipt/data/dipnet.csv

python3 /opt/ipt/data/scripts/dipnet_ipt_transformer.py /opt/ipt/data/dipnet.csv

sqlite3 /opt/ipt/data/ipt.db "drop table if exists dipnet;" ".mode csv" ".import /opt/ipt/data/dipnet.csv dipnet" ".exit"

rm -f /opt/ipt/data/dipnet.csv.zip
rm -f /opt/ipt/data/dipnet.csv
