#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$2" = "" ]; then
  echo "VERSION and RELEASE_TRAIN_VERSION required as arguments"
  exit 1
fi
function find_version() {
    for v in $1; do
      VER=$v
    done
    echo $VER
}
VERSION=$1
RELEASE_TRAIN_VERSION=$2
OLD_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2> /dev/null)
OLD_VERSION=$(find_version "$OLD_VERSION")
OLD_RT_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -f stream-applications-release-train 2> /dev/null)
OLD_RT_VERSION=$(find_version "$OLD_RT_VERSION")
if [ "$VERBOSE" = "" ]; then
  VERBOSE=-q
fi
echo "Version:[$OLD_VERSION] -> [$VERSION]"
echo "Release Train Version: [$OLD_RT_VERSION] -> [$RELEASE_TRAIN_VERSION]"

$SCDIR/mvnw versions:set \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -Dartifactory.publish.artifacts=false -B $VERBOSE \
  -DoldVersion="$OLD_VERSION" -DnewVersion="$VERSION" -DprocessAllModules=true

$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=stream-apps-core.version -DnewVersion="$VERSION"

$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=java-functions.version -DnewVersion="$VERSION"
$SCDIR/mvnw versions:set-property -pl :stream-applications-release-train \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=apps.version -DnewVersion="$VERSION" \

echo "Update versions for stream-applications-release-train -> $RELEASE_TRAIN_VERSION"
$SCDIR/mvnw versions:set -f stream-applications-release-train \
  -DskipResolution=true -s .settings.xml -DgenerateBackupPoms=false -Dartifactory.publish.artifacts=false -B $VERBOSE \
  -DoldVersion="$OLD_RT_VERSION" -DnewVersion="$RELEASE_TRAIN_VERSION" \

