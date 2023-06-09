#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
$SCDIR/build-core.sh "-T 0.5C install -P-snapshot -P-integration -DskipTests -q"
$SCDIR/build-folder.sh ./applications/processor,./applications/sink,./applications/source,./applications/stream-applications-integration-tests,stream-applications-release-train "-T 0.5C install -P-snapshot -P-integration -DskipTests -q"
