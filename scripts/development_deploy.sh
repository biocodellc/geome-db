#!/usr/bin/env bash

git checkout src/main/web/apidocs

git fetch
git checkout develop
git pull

./gradlew clean &&
./scripts/generateDevelopRestApiDocs.sh
./gradlew -Penvironment=development fatWar &&

sudo cp /home/deploy/code/dev/geome-db/dist/geome-db-fat.war /opt/web/dev/webapps/geome-db.war
sudo /bin/touch /opt/web/dev/webapps/geome-db.xml