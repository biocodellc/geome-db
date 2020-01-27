#!/usr/bin/env bash

ssh deploy@biscicol3.acis.ufl.edu <<'ENDSSH'

    cd code/prod/geome-db

    git fetch
    git checkout master
    git pull

    sudo cp /home/deploy/code/prod/geome-db/deploy/production/crontab.conf /etc/cron.d/geome_production
    sudo chmod 600 /etc/cron.d/geome_production

    ./gradlew clean &&
    ./gradlew -PforceJars=true -Penvironment=production war &&
    sudo cp /home/deploy/code/prod/geome-db/dist/geome-db.war /opt/web/prod/webapps/geome-db.war &&
    sudo /bin/touch /opt/web/prod/webapps/geome-db.xml

ENDSSH