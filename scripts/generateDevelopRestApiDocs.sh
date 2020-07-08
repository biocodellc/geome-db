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

cd "${scriptDir}/../"

args="-PforceJars=true"

echo "running    ->    ./gradlew ${args} generateRestApiDocs"
echo ""
echo ""

./gradlew ${args} generateRestApiDocs
