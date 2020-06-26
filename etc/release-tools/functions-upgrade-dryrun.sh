#!/bin/bash

# The script takes one argument - release version

if [ "$#" -ne 1 ]; then
    echo "Please specify the release version"
    exit
fi

pushd ../..

VERSION=$1
./mvnw -f functions versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false

if [[ $VERSION =~ M[0-9]|RC[0-9] ]]; then
 lines=$(find functions -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
 if [ $lines -eq 0 ]; then
   echo "All good"
 else
   echo "Snapshots found. Exiting build"
   find functions -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex
   git checkout -f
   exit 1
 fi
else 
  lines=$(find functions -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
  if [ $lines -eq 0 ]; then
    echo "All good"
  else
   echo "Non Release versions found. Exiting build"
   find functions -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex
   git checkout -f
   exit 1
  fi
fi

#git checkout -f

popd
