#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
(return 0 2>/dev/null) && sourced=1 || sourced=0
if (( sourced == 0 )); then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
# get the target release version and type
BUILD_VERSION=$(jq -r '.release_version' ./versions.json)
echo "BUILD_VERSION: $BUILD_VERSION"
set +e
IS_SNAPSHOT=$(echo $BUILD_VERSION | grep -E "^.*-SNAPSHOT$")
IS_MILESTONE=$(echo $BUILD_VERSION | grep -E "^.*-(M|RC)[0-9]+$")
IS_GA=$(echo $BUILD_VERSION | grep -E "^.*\.[0-9]+$")

if [ -n "$IS_MILESTONE" ]; then
  export BUILD_VERSION_TYPE="milestone"
elif [ -n "$IS_SNAPSHOT" ]; then
  export BUILD_VERSION_TYPE="snapshot"
elif [ -n "$IS_GA" ]; then
  export BUILD_VERSION_TYPE="ga"
else
  echo "Bad version format: $BUILD_VERSION"
  exit 1
fi

echo "BUILD_VERSION_TYPE: $BUILD_VERSION_TYPE"

# get the current version from pom.xml
CUR_VERSION=$($SCDIR/mvn-get-version.sh)
echo "CUR_VERSION: $CUR_VERSION"

# is build a version change?
if [ "$CUR_VERSION" = "$BUILD_VERSION" ]; then
    IS_VERSION_CHANGE="false"
else
    IS_VERSION_CHANGE="true"
fi
echo "IS_VERSION_CHANGE: $IS_VERSION_CHANGE"

# get the next dev version
NEXT_DEV_VERSION=$(jq -r '.next_dev_version' ./versions.json)
echo "NEXT_DEV_VERSION: $NEXT_DEV_VERSION"

RELEASE_TRAIN_VERSION=$(jq -r '.release_train.release_version' ./versions.json)
echo "RELEASE_TRAIN_VERSION=$RELEASE_TRAIN_VERSION"
RELEASE_TRAIN_NEXT_DEV_VERSION=$(jq -r '.release_train.next_dev_version' ./versions.json)
echo "RELEASE_TRAIN_NEXT_DEV_VERSION=$RELEASE_TRAIN_NEXT_DEV_VERSION"

export BUILD_VERSION
export BUILD_VERSION_TYPE
export IS_GA
export IS_MILESTONE
export IS_SNAPSHOT
export IS_VERSION_CHANGE
export CUR_VERSION
export RELEASE_TRAIN_VERSION
export RELEASE_TRAIN_NEXT_DEV_VERSION
