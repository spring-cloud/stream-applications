#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Please specify the release and core parent versions"
    exit
fi

VERSION=$1

function git_commit_push {
 echo "in git commit"
 git commit -am"Source Applications: Release - $VERSION"
 git push origin master && git push upstream master
}

pushd ../..

./mvnw -f applications/source versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false -DupdateMatchingVersions=false
./mvnw -f applications/source versions:update-parent -DparentVersion=$2 -Pspring -DgenerateBackupPoms=false

if [[ $VERSION =~ M[0-9]|RC[0-9] ]]; then
 lines=$(find applications/source -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
 if [ $lines -eq 0 ]; then
  echo "All good"
  git_commit_push 
 else
   echo "Snapshots found. Exiting build"
   find applications/source -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex
   git checkout -f
 fi
else 
  lines=$(find applications/source -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
  if [ $lines -eq 0 ]; then
   echo "All good"
   git_commit_push 
  else
   echo "Non Release versions found. Exiting build"
   find applications/source -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex
   git checkout -f
  fi
fi

popd
