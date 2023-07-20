#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$2" = "" ]; then
  echo "VERSION and RELEASE_TRAIN_VERSION required as arguments"
  exit 1
fi
VERSION=$1
RELEASE_TRAIN_VERSION=$2
OLD_VERSION=$(SCDIR/mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q | sed 's/\"//g' | sed 's/version=//g')
OLD_RT_VERSION=$(SCDIR/mvnw exec:exec -f stream-applications-release-train -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q | sed 's/\"//g' | sed 's/version=//g')
$SCDIR/mvnw versions:set \
  -DskipResolution=true \
  -DnewVersion="$VERSION" \
  -DoldVersion="$OLD_VERSION" \
  -s .settings.xml \
  -DprocessAllModules=true \
  -DgenerateBackupPoms=false \
  -Dartifactory.publish.artifacts=false \
  -B $VERBOSE
$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -DskipResolution=true \
  -s .settings.xml \
  -DgenerateBackupPoms=false \
  -Dproperty=stream-apps-core.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE
$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -DskipResolution=true \
  -s .settings.xml \
  -DgenerateBackupPoms=false \
  -Dproperty=java-functions.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE

# $SCDIR/mvnw install -DskipTests -o -T 1C
$SCDIR/mvnw versions:set-property -pl :stream-applications-release-train \
  -DskipResolution=true \
  -s .settings.xml \
  -DgenerateBackupPoms=false \
  -Dproperty=apps.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE
$SCDIR/mvnw versions:set -f stream-applications-release-train \
  -DskipResolution=true \
  -DoldVersion="$OLD_RT_VERSION" \
  -DnewVersion="$RELEASE_TRAIN_VERSION" \
  -s .settings.xml \
  -DprocessFromLocalAggregationRoot=false \
  -DprocessParent=false \
  -DgenerateBackupPoms=false \
  -Dartifactory.publish.artifacts=false \
  -B $VERBOSE
$SCDIR/mvnw versions:update-parent -pl :stream-applications-descriptor,:stream-applications-docs \
  -DskipResolution=true \
  -s .settings.xml \
  -DgenerateBackupPoms=false \
  -DparentVersion="$RELEASE_TRAIN_VERSION" \
  -DallowSnapshots=true \
  -B $VERBOSE
