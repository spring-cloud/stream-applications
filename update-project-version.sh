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
NEW_VERSION=$1
RELEASE_TRAIN_VERSION=$2
OLD_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2> /dev/null)
OLD_VERSION=$(find_version "$OLD_VERSION")
OLD_RT_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -f stream-applications-release-train 2> /dev/null)
OLD_RT_VERSION=$(find_version "$OLD_RT_VERSION")

if [ "$VERBOSE" = "" ]; then
  VERBOSE=-q
fi

echo "Version:[$OLD_VERSION] -> [$NEW_VERSION]"
echo "Release Train Version: [$OLD_RT_VERSION] -> [$RELEASE_TRAIN_VERSION]"
set +e

$SCDIR/mvnw clean install -DskipTests -T 1C -ntp -Dmaven.javadoc.skip=true
$SCDIR/mvnw clean
find $SCDIR -name apps -type d -exec rm -rf '{}' \;

$SCDIR/mvnw versions:set \
  -s .settings.xml -DgenerateBackupPoms=false -Dartifactory.publish.artifacts=false -B $VERBOSE \
  -DoldVersion="$OLD_VERSION" -DnewVersion="$NEW_VERSION" -DprocessAllModules=true -Dmaven.version.ignore="${OLD_RT_VERSION/\./\\.}"

$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=stream-apps-core.version -DnewVersion="$NEW_VERSION"

$SCDIR/mvnw versions:set-property -f applications/stream-applications-core \
  -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=java-functions.version -DnewVersion="$NEW_VERSION"

$SCDIR/mvnw versions:set-property -pl :stream-applications-release-train \
  -s .settings.xml -DgenerateBackupPoms=false -B $VERBOSE \
  -Dproperty=apps.version -DnewVersion="$NEW_VERSION"

echo "Release Train Version: [$OLD_RT_VERSION] -> [$RELEASE_TRAIN_VERSION]"
echo "Update versions for stream-applications-release-train -> $RELEASE_TRAIN_VERSION"

$SCDIR/mvnw versions:set -pl ":stream-applications-release-train,:stream-applications-descriptor,:stream-applications-docs" \
  -s .settings.xml -DgenerateBackupPoms=false -Dartifactory.publish.artifacts=false -B $VERBOSE -DnewVersion="$RELEASE_TRAIN_VERSION"


FOUND_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2> /dev/null)
FOUND_VERSION=$(find_version "$FOUND_VERSION")
if [ "$NEW_VERSION" != "$FOUND_VERSION" ]; then
  echo "Expected stream-applications version to be $NEW_VERSION not $FOUND_VERSION"
  exit 1
fi
echo "Version updated: stream-applications: $FOUND_VERSION"
PROJECTS="stream-applications-release-train stream-applications-descriptor stream-applications-docs"
for proj in $PROJECTS; do
  NEW_RT_VERSION=$($SCDIR/mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -pl ":$proj" 2> /dev/null)
  NEW_RT_VERSION=$(find_version "$NEW_RT_VERSION")
  if [ "$NEW_RT_VERSION" != "$RELEASE_TRAIN_VERSION" ]; then
    echo "Expected $proj version to be $RELEASE_TRAIN_VERSION not $NEW_RT_VERSION"
    exit 1
  fi
  echo "Version updated: $proj: $NEW_RT_VERSION"
done
