#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
export MAVEN_THREADS=1C
export LOCAL=true
$SCDIR/mvnw install -DskipTests -T 1C -P-snapshot -P-integration -DskipTests
