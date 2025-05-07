#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

MVN_OPT=
if [ "$VERBOSE" == "true" ]; then
  MVN_OPT="-X"
fi

if [ "$STREAM_APPS_VERSION" == "" ]; then
  STREAM_APPS_VERSION=$($SCDIR/mvn-get-version.sh)
fi
CONTAINERS="s3-source sftp-source http-request-processor log-sink jdbc-source time-source http-source tcp-sink mongodb-sink"
BROKERS="rabbit kafka"
for container in $CONTAINERS; do
  for broker in $BROKERS; do
    echo "Pulling springcloudstream/${container}-${broker}:$STREAM_APPS_VERSION"
    docker pull "springcloudstream/${container}-${broker}:$STREAM_APPS_VERSION"
    docker tag "springcloudstream/${container}-${broker}:$STREAM_APPS_VERSION" "springcloudstream/${container}-${broker}:latest"
  done
done
echo "Using STREAM_APPS_VERSION=$STREAM_APPS_VERSION"
$SCDIR/mvnw $MVN_OPT -pl :stream-applications-integration-tests -am install -DskipTests
set +e
$SCDIR/mvnw $MVN_OPT $@ -pl :stream-applications-integration-tests -Pintegration -Psnapshot test integration-test -Dspring.cloud.stream.applications.version=$STREAM_APPS_VERSION
RC=$?
LOG_FILE=$SCDIR/stream-applications-integration-tests/test.log
if [ -f $LOG_FILE ]; then
  cat $LOG_FILE
fi
exit $RC
