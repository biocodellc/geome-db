#!/usr/bin/env bash

git checkout src/main/web/apidocs

git fetch
git checkout develop
git pull

./gradlew clean &&
./scripts/generateDevelopRestApiDocs.sh
./gradlew -Penvironment=development war &&

sudo cp /home/deploy/code/prod/geome-db/dist/geome-db.war /opt/web/prod/webapps/geome-db.war
sudo /bin/touch /opt/web/prod/webapps/geome-db.xml
