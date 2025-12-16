#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
: "${STREAM_APPS_VERSION:=$($SCDIR/mvn-get-version.sh)}"
$SCDIR/build-core.sh "-T 0.5C install -Psnapshot -Pintegration -Dspring.cloud.stream.applications.version=$STREAM_APPS_VERSION"
$SCDIR/build-folder.sh ./applications/processor,./applications/sink,./applications/source,./applications/stream-applications-integration-tests,stream-applications-release-train "-T 0.5C install -Psnapshot -Pintegration -Dspring.cloud.stream.applications.version=$STREAM_APPS_VERSION"
