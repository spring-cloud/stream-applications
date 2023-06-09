#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
(return 0 2>/dev/null) && sourced=1 || sourced=0
if (( sourced == 0 )); then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
# get the target release version and type
export BUILD_VERSION=$(cat ./version/RELEASE_VERSION)
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
export CUR_VERSION=$($SCDIR/mvn-get-version.sh)
echo "CUR_VERSION: $CUR_VERSION"

# is build a version change?
if [ "$CUR_VERSION" = "$BUILD_VERSION" ]; then
    export IS_VERSION_CHANGE="false"
else
    export IS_VERSION_CHANGE="true"
fi
echo "IS_VERSION_CHANGE: $IS_VERSION_CHANGE"

# get the next dev version
export NEXT_DEV_VERSION=$(cat ./version/NEXT_DEV_VERSION)
echo "NEXT_DEV_VERSION: $NEXT_DEV_VERSION"
