#!/bin/bash

if [ "$#" -ne 5 ]; then
    echo "Please specify the tag, functions next, core next, apps next and aggregator next versions"
    exit
fi

TAG=$1
FUNCTIONS_NEXT=$2
CORE_NEXT=$3
APPS_NEXT=$4
AGGREGATOR_NEXT=$5 

pushd ../..

git tag $TAG
git push upstream $TAG

echo "Waiting for 20 seconds..."
sleep 20

./mvnw -Ddisable.checks=true -f functions versions:set -DnewVersion=$FUNCTIONS_NEXT -DgenerateBackupPoms=false

./mvnw -Ddisable.checks=true -f applications/stream-applications-core versions:set -DnewVersion=$CORE_NEXT -DgenerateBackupPoms=false -DupdateMatchingVersions=false

cd applications/stream-applications-core
sed -i '' 's/<java-functions.version>.*/<java-functions.version>'"$2"'<\/java-functions.version>/g' pom.xml
cd ../..

./mvnw -Ddisable.checks=true -f applications/stream-applications-core clean install -DskipTests

./mvnw -Ddisable.checks=true -f applications/source versions:set -DnewVersion=$APPS_NEXT -DgenerateBackupPoms=false
./mvnw -Ddisable.checks=true -f applications/source versions:update-parent -DparentVersion=$CORE_NEXT -Pspring -DgenerateBackupPoms=false

cd applications/source

for folder in *; do
    [ -d "${folder}" ] || continue # if not a directory, skip
    echo "FOLDER NAME - ${folder}"
    pushd ${folder}
    ../../../mvnw -Ddisable.checks=true versions:set -DnewVersion=$APPS_NEXT -DgenerateBackupPoms=false
    ../../../mvnw -Ddisable.checks=true versions:update-parent -DparentVersion=$CORE_NEXT -Pspring -DgenerateBackupPoms=false
    popd
done

cd ../../

./mvnw -Ddisable.checks=true -f applications/sink versions:set -DnewVersion=$APPS_NEXT -DgenerateBackupPoms=false 
./mvnw -Ddisable.checks=true -f applications/sink versions:update-parent -DparentVersion=$CORE_NEXT -Pspring -DgenerateBackupPoms=false

cd applications/sink

for folder in *; do
    [ -d "${folder}" ] || continue # if not a directory, skip
    echo "FOLDER NAME - ${folder}"
    pushd ${folder}
    ../../../mvnw -Ddisable.checks=true versions:set -DnewVersion=$APPS_NEXT -DgenerateBackupPoms=false
    ../../../mvnw -Ddisable.checks=true versions:update-parent -DparentVersion=$CORE_NEXT -Pspring -DgenerateBackupPoms=false
    popd
done

cd ../../

./mvnw -Ddisable.checks=true -f applications/processor versions:set -DnewVersion=$APPS_NEXT -DgenerateBackupPoms=false
./mvnw -Ddisable.checks=true -f applications/processor versions:update-parent -DparentVersion=$CORE_NEXT -Pspring -DgenerateBackupPoms=false

cd applications/processor

for folder in *; do
    [ -d "${folder}" ] || continue # if not a directory, skip
    echo "FOLDER NAME - ${folder}"
    pushd ${folder}
    ../../../mvnw -Ddisable.checks=true versions:set -DnewVersion=$APPS_NEXT -DgenerateBackupPoms=false
    ../../../mvnw -Ddisable.checks=true versions:update-parent -DparentVersion=$CORE_NEXT -Pspring -DgenerateBackupPoms=false
    popd
done

cd ../../

cd applications/stream-applications-build
sed -i '' 's/<apps.version>.*/<apps.version>'"$APPS_NEXT"'<\/apps.version>/g' pom.xml
cd ../.. 

./mvnw -Ddisable.checks=true -f applications/stream-applications-build versions:set -DnewVersion=$AGGREGATOR_NEXT -DgenerateBackupPoms=false
./mvnw -Ddisable.checks=true -f applications/stream-applications-build versions:update-parent -DparentVersion=$CORE_NEXT -Pspring -DgenerateBackupPoms=false

popd

echo "Committing and pushing the changes, but waiting for 30 seconds before doing so..."
sleep 60

COMMIT_MSG="Next version updates

  Functions: $FUNCTIONS_NEXT
  Core Apps: $CORE_NEXT
  Apps: $APPS_NEXT
  Aggregate Next:$AGGREGATOR_NEXT"

git commit -am"$COMMIT_MSG"
git push origin master && git push upstream master

