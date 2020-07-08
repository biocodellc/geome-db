# Production Deployment

### GeOMe release

1. Merge `develop` into `master`

       git checkout master
       git merge --no-ff develop

2. Re-generate api docs

       ./gradlew generateRestApiDocs

3. Push changes

       git push
       
4. Tag the release

       ./gradlew reckonTagCreate -Preckon.scope=major -Preckon.stage=final
       git push --tags
       
       
### Deploy

note: `scripts/deployProduction.sh` automates the following steps

1. log into server

2. `su` as `deploy` user

       sudo su deploy
       
3. `cd` to codebase

       cd ~/code/prod/geome-db
       
4. checkout latest changes

       git checkout master
       git pull
       
5. update any props files as necessary

6. build

        ./gradlew clean
        ./gradlew -PforceJars=true -Penvironment=production war
        sudo cp /home/deploy/code/prod/geome-db/dist/geome-db.war /opt/web/prod/webapps/geome-db.war
        
7. deploy

        sudo /bin/touch /opt/web/prod/webapps/geome-db.xml
        
    or
    
        sudo service jetty restart
