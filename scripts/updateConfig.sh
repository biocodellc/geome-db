#!/usr/bin/env bash

# WARNING: This is not the recommended way to update a config. If you are going to use this
# make sure to verify the script updates as expected. And backup the project_configurations
# table before updating.


# Loops through all project_configurations, uses jq to update the config json
# (this case adding "type" = "Tissue" if entity.conceptAlias = "Tissue"), and updates the config
set -e

# update network_1.tissue t left join expeditions e on e.id = t.expedition_id set data = data || jsonb_build_object('urn:tissueID', data->>'urn:materialSampleID') where e.project_id = 2;

#jq_query='.entities |= map( if .conceptAlias=="Tissue" then .additionalProps |= {"generateID": true} | .uniqueKey |= "tissueID" | .attributes += [{"uri": "urn:tissueID"}] else . end)'
jq_query='.entities |= map( if .conceptAlias=="Sample" then .attributes |= map( if .uri=="urn:quantityDected" then .uri |= "urn:quantityDetected" else . end) else . end)'

# loop through each line
while read -r result
do

  # split query result into id & config vars ("1|{}")
  IFS='|' read id config <<< "$result"

  # use jq to update the config as required
  updated_config=$(jq "$jq_query" <<< $config)

  echo $updated_config

  # persist the updated config
#  psql -X -A -t -d biscicoldev -c "update project_configurations set config = '$updated_config' where id = $id;"
done <<< $(psql -X -A -t -d biscicoldev -c "select id, config from project_configurations where config::text like '%urn:quantityDected%' limit 1;")
