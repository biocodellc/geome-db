#!/usr/bin/env bash

git checkout src/main/web/apidocs

git fetch
git checkout develop
git pull

./gradlew clean &&
./scripts/generateDevelopRestApiDocs.sh
./gradlew -Penvironment=development war &&

sudo cp /home/deploy/code/dev/geome-db/dist/geome-db.war /opt/web/dev/webapps/geome-db.war
sudo /bin/touch /opt/web/dev/webapps/geome-db.xml
