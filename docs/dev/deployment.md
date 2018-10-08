# Production Deployment

### Package releases

The following steps need to happen for each package (commons, photos, sequences, etc). Complete all steps for each 
package before moving on to the next. You'll also need to start with the most depended upon package first (biocode-fims-commons)
and work your way up the dependency tree.

1. Update any package versions needed in `build.gradle`. Ex. `fimsCommonsVersion` variable in `biocode-fims-photos`

2. Merge `develop` into `master`

       git checkout master
       git merge --no-ff develop
       
3. Create a release       

Versions are handled automatically using semver

**scope** can be one of `major, minor, patch`

**stage** can be one of `snapshot, beta, rc, final`
  
      ./gradlew release -Preckon.scope=major -Preckon.stage=final


### GeOMe release

1. Update `fims*Version` variables in `build.gradle`

2. Merge `develop` into `master`

       git checkout master
       git merge --no-ff develop

3. Re-generate api docs

       ./gradlew generateRestApiDocs

4. Push changes

       git push
       
5. Tag the release

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
