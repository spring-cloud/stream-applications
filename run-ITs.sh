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
shift
CONTAINERS="s3-source sftp-source http-request-processor log-sink jdbc-source time-source http-source tcp-sink mongodb-sink"
BROKERS="rabbit kafka"
for container in $CONTAINERS; do
  for broker in $BROKERS; do
    echo "Pulling springcloudstream/${container}-${broker}:$VERSION"
    docker pull "springcloudstream/${container}-${broker}:$VERSION"
    docker tag "springcloudstream/${container}-${broker}:$VERSION" "springcloudstream/${container}-${broker}:latest"
  done
done
echo "Using version:$VERSION"
$SCDIR/mvnw $MVN_OPT -pl :stream-applications-integration-tests -am install -DskipTests
$SCDIR/mvnw $MVN_OPT $@ -pl :stream-applications-integration-tests -Pintegration -Psnapshot test integration-test -Dspring.cloud.stream.applications.version=$VERSION


