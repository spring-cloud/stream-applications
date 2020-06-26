#!/bin/bash

if [ "$#" -ne 3 ]; then
    echo "Please specify the release, core parent and apps versions"
    exit
fi

VERSION=$1

function git_commit_push {
 echo "in git commit"
 git commit -am"Aggregating docs/descriptors : Release - $VERSION"
 git push origin master && git push upstream master
}

pushd ../..


cd applications/stream-applications-build
sed -i '' 's/<apps.version>.*/<apps.version>'"$2"'<\/apps.version>/g' pom.xml
cd -

./mvnw -f applications/stream-applications-build versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
./mvnw -f applications/stream-applications-build versions:update-parent -DparentVersion=$2 -Pspring -DgenerateBackupPoms=false

if [[ $VERSION =~ M[0-9]|RC[0-9] ]]; then
 lines=$(find applications/stream-applications-build -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
 if [ $lines -eq 0 ]; then
  echo "All good"
  git_commit_push 
 else
   echo "Snapshots found. Exiting build"
   find applications/stream-applications-build -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex
   git checkout -f
 fi
else 
  lines=$(find applications/stream-applications-build -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
  if [ $lines -eq 0 ]; then
   echo "All good"
   git_commit_push 
  else
   echo "Non Release versions found. Exiting build"
   find applications/stream-applications-build -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex
   git checkout -f
  fi
fi

popd
