#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$2" = "" ]; then
  echo "VERSION and RELEASE_TRAIN_VERSION required as arguments"
  exit 1
fi
VERSION=$1
RELEASE_TRAIN_VERSION=$2
$SCDIR/mvnw versions:set \
  -DnewVersion="$VERSION" \
  -s .settings.xml \
  --offline \
  -DprocessAllModules=true \
  -DgenerateBackupPoms=false \
  -Dartifactory.publish.artifacts=false \
  -B $VERBOSE
$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -s .settings.xml \
  --offline \
  -DgenerateBackupPoms=false \
  -Dproperty=stream-apps-core.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE
$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -s .settings.xml \
  --offline \
  -DgenerateBackupPoms=false \
  -Dproperty=java-functions.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE
$SCDIR/mvnw versions:set -f stream-applications-release-train \
  -DnewVersion="$RELEASE_TRAIN_VERSION" \
  --offline \
  -s .settings.xml \
  -DprocessFromLocalAggregationRoot=false
  -DprocessParent=false \
  -DgenerateBackupPoms=false \
  -Dartifactory.publish.artifacts=false \
  -B $VERBOSE
$SCDIR/mvnw versions:set-property -f stream-applications-release-train \
  -s .settings.xml \
  --offline \
  -DgenerateBackupPoms=false \
  -Dproperty=apps.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE
