#!/usr/bin/env bash

# script to generate api docs for the develop branch
# we rely on published jar files for biocode-fims-& subprojects
# to generate the api docs. This will publish a new version to
# you're local mvn repository and generate the docs using that
# version.

set -e

scriptDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function publishLocal()
{
    cmd='/{print $3}'
    cmd="/$1 $cmd"
    local dir=$(cat ${scriptDir}/../gradle.properties | awk -F '[ ]+' "$cmd")

    if [ ! -z "$dir" ]; then
        cd "$dir"
        local version=$(./gradlew publishToMavenLocal | awk -F '[ ]+' '/Reckoned version: /{print $3}' | tail -n 1)
        echo "$version"
    fi;
}

echo "$(tput setaf 1)attempting to run publishToMavenLocal task for biocode-fims-commons subproject$(tput sgr 0)"
commonsVersion=$(publishLocal "biocodeFimsCommonsDir")
echo "$(tput setaf 1)attempting to run publishToMavenLocal task for biocode-fims-sequences subproject$(tput sgr 0)"
sequencesVersion=$(publishLocal "biocodeFimsSequencesDir")
echo "$(tput setaf 1)attempting to run publishToMavenLocal task for biocode-fims-photos subproject$(tput sgr 0)"
photosVersion=$(publishLocal "biocodeFimsPhotosDir")

cd "${scriptDir}/../"

args="-PforceJars=true"

if [ ! -z "$commonsVersion" ]; then
    args="$args -PfimsCommonsVersion=$commonsVersion"
fi;

if [ ! -z "$sequencesVersion" ]; then
    args="$args -PfimsSequencesVersion=$sequencesVersion"
fi;

if [ ! -z "$photosVersion" ]; then
    args="$args -PfimsPhotosVersion=$photosVersion"
fi;

echo "running    ->    ./gradlew ${args} generateRestApiDocs"
echo ""
echo ""

./gradlew ${args} generateRestApiDocs
