#!/usr/bin/env bash

# script to checkout a specific branch for current project &
# all child project if present. The GIT_BRANCH env variable is
# used to specify the branch. If not found, we default to the
# current branch of the parent project.
# version.

set -e

scriptDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

git fetch

if [ ! -z "$GIT_BRANCH" ]; then
    branch=$(echo $GIT_BRANCH)
    git checkout $GIT_BRANCH
else
    branch=$(git rev-parse --abbrev-ref HEAD)
fi

git checkout src/main/web/apidocs
git pull

function checkout()
{
    cmd='/{print $3}'
    cmd="/$1 $cmd"
    local dir=$(cat ${scriptDir}/../gradle.properties | awk -F '[ ]+' "$cmd")

    if [ ! -z "$dir" ]; then
        echo $dir
        cd "$dir"
        git fetch
        git checkout $branch
        git pull
    fi;
}

checkout "biocodeFimsCommonsDir"
checkout "biocodeFimsTissuesDir"
checkout "biocodeFimsPhotosDir"
checkout "biocodeFimsEvolutionDir"
