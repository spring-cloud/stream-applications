#!/bin/bash

# The script takes one argument - release version

if [ "$#" -ne 1 ]; then
    echo "Please specify the release version"
    exit
fi

pushd /tmp

git clone git@github.com:spring-cloud-stream-app-starters/core.git
cd core

./mvnw versions:set -DnewVersion=$1 -DgenerateBackupPoms=false -U
./mvnw versions:set -DnewVersion=$1 -DgenerateBackupPoms=false -pl :app-starters-core-dependencies -U

sed -i '' 's/<app-starters-core-dependencies.version>.*/<app-starters-core-dependencies.version>'"$1"'<\/app-starters-core-dependencies.version>/g' pom.xml

snapshotlines=$(find . -type f -name pom.xml | xargs grep SNAPSHOT | wc -l)
rclines=$(find . -type f -name pom.xml | xargs grep .RC | wc -l)
milestonelines=$(find . -type f -name pom.xml | xargs grep version | grep .M | wc -l)

if [ $snapshotlines -eq 0 ] && [ $rclines -eq 0 ] && [$milestonelines -eq 0 ]; then
        echo "All clear"
	git commit -am"Update version to $1"
	git push origin master
else
        echo "Snapshots found."
        echo "SNAPSHOTS: " $snapshotlines
        echo "Milestones: " $milestonelines
        echo "RC: " $rclines
	exit 1
fi

cd ..
rm -rf core

popd
