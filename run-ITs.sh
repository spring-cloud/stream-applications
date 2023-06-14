#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

MVN_OPT=
if [ "$VERBOSE" == "true" ]; then
  MVN_OPT="-X"
fi
VERSION=$1
if [ "$VERSION" == "" ]; then
  VERSION=$($SCDIR/mvn-get-version.sh)
fi
$SCDIR/mvnw $MVN_OPT -pl applications/stream-applications-integration-tests -am install -DskipTests
$SCDIR/mvnw $MVN_OPT -pl applications/stream-applications-integration-tests verify -Pintegration -Psnapshot -Dspring.cloud.stream.applications.version=$VERSION
