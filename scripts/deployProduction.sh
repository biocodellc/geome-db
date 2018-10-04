#!/usr/bin/env bash

ssh deploy@biscicol3.acis.ufl.edu <<'ENDSSH'

    cd code/prod/geome-db

    git fetch
    git checkout master
    git pull

    ./gradlew clean &&
    ./gradlew -PforceJars=true -Penvironment=production fatWar &&
    sudo cp /home/deploy/code/prod/geome-db/dist/geome-db-fat.war /opt/web/prod/webapps/geome-db.war &&
    sudo /bin/touch /opt/web/prod/webapps/geome-db.xml

ENDSSH