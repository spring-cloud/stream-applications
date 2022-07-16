#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Please specify the release and core parent versions"
    exit
fi

pushd ../..

VERSION=$1
PARENT_VERSION=$2

function iterate_through_apps_folders_and_update {

  ./mvnw -Ddisable.checks=true -f $BASE_DIR versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false

  cd $BASE_DIR

  # Remove any lingering apps folders
  find . -name "apps" -type d -exec rm -r "{}" \;

  for folder in *; do
    [ -d "${folder}" ] || continue # if not a directory, skip
    echo "FOLDER NAME - ${folder}"
    pushd ${folder}
    ../../../mvnw -Ddisable.checks=true versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
    #../../../mvnw -Ddisable.checks=true versions:update-parent -DparentVersion=$PARENT_VERSION -Pspring -DgenerateBackupPoms=false
    # only used after a release for updating parent versions.
    sed -i '' 's/<version>3.2.2-SNAPSHOT/<version>'4.0.0-SNAPSHOT'/g' pom.xml
    popd
  done

  cd ../../

#  if [[ $VERSION =~ M[0-9]|RC[0-9] ]]; then
#    lines=$(find $BASE_DIR -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v "<!--"| grep -v regex | wc -l)
#    if [ $lines -eq 0 ]; then
#     echo "All good"
#    else
#     echo "Snapshots found under $BASE_DIR. Exiting build"
#     find $BASE_DIR -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex
#     git checkout -f
#     exit 1
#    fi
#  else
#    lines=$(find $BASE_DIR -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
#    if [ $lines -eq 0 ]; then
#     echo "All good"
#    else
#     echo "Non Release versions found under $BASE_DIR. Exiting build"
#     find $BASE_DIR -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex
#     git checkout -f
#     exit 1
#    fi
#  fi

}

BASE_DIR=applications/source
iterate_through_apps_folders_and_update

BASE_DIR=applications/sink
iterate_through_apps_folders_and_update

BASE_DIR=applications/processor
iterate_through_apps_folders_and_update

popd
