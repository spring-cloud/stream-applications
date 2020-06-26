#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Please specify the release version and java-functions.version property"
    exit
fi

pushd ../..

VERSION=$1
./mvnw -f applications/stream-applications-core versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false -DupdateMatchingVersions=false

cd applications/stream-applications-core
sed -i '' 's/<java-functions.version>.*/<java-functions.version>'"$2"'<\/java-functions.version>/g' pom.xml
cd ../..

if [[ $VERSION =~ M[0-9]|RC[0-9] ]]; then
 lines=$(find applications/stream-applications-core -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
 if [ $lines -eq 0 ]; then
   echo "All good"
 else
   echo "Snapshots found. Exiting build"
   find applications/stream-applications-core -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex
   git checkout -f
   exit 1
 fi
else 
  lines=$(find applications/stream-applications-core -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
  if [ $lines -eq 0 ]; then
   echo "All good"
  else
   echo "Non Release versions found. Exiting build"
   find applications/stream-applications-core -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex
   git checkout -f
   exit 1
  fi
fi

#git checkout -f

popd
