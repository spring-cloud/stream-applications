#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$2" = "" ]; then
  echo "VERSION and RELEASE_TRAIN_VERSION required as arguments"
  exit 1
fi
VERSION=$1
RELEASE_TRAIN_VERSION=$2
OLD_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | sed 's/\"//g' | sed 's/version=//g')
OLD_RT_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -f stream-applications-release-train | sed 's/\"//g' | sed 's/version=//g')
if [ "$VERBOSE" = "" ]; then
  VERBOSE=-q
fi
echo "Version:[$OLD_VERSION] -> [$VERSION]"
echo "Release Train Version: [$OLD_RT_VERSION] -> [$RELEASE_TRAIN_VERSION]"

$SCDIR/mvnw versions:set \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -Dartifactory.publish.artifacts=false -B $VERBOSE \
  -DnewVersion="$VERSION" -DprocessAllModules=true

$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=stream-apps-core.version -DnewVersion="$VERSION"

$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=java-functions.version -DnewVersion="$VERSION"

$SCDIR/mvnw versions:set-property -pl :stream-applications-release-train \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=apps.version -DnewVersion="$VERSION" \

$SCDIR/mvnw versions:set -f stream-applications-release-train \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -Dartifactory.publish.artifacts=false -B $VERBOSE \
  -DnewVersion="$RELEASE_TRAIN_VERSION" \
  -DprocessFromLocalAggregationRoot=false -DprocessParent=false

$SCDIR/mvnw versions:update-parent -pl :stream-applications-descriptor,:stream-applications-docs \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -DgenerateBackupPoms=false -B $VERBOSE \
  -DparentVersion="$RELEASE_TRAIN_VERSION" -DallowSnapshots=true
