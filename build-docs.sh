#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
MAVEN_GOALS="install deploy"
if [ "$1" != "" ]; then
  MAVEN_GOALS="$*"
fi
$SCDIR/build-folder.sh stream-applications-release-train/stream-applications-docs "$MAVEN_GOALS"
