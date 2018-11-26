#!/usr/bin/env bash

# Loops through all project_configurations, uses jq to update the config json
# (this case adding "type" = "Tissue" if entity.conceptAlias = "Tissue"), and updates the config
set -e

# loop through each line
while read -r result
do

  # split query result into id & config vars ("1|{}")
  IFS='|' read id config <<< "$result"

  # use jq to update the config as required
  updated_config=$(jq '.entities |= map( if .conceptAlias=="Tissue" then .type |= "Tissue" else . end)' <<< $config)

  # persist the updated config
  psql -X -A -t -d biscicoldev -c "update project_configurations set config = '$updated_config' where id = $id;"
done <<< $(psql -X -A -t -d biscicoldev -c 'select id, config from project_configurations where id = 1;')
