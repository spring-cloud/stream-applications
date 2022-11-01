#!/usr/bin/env bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "$1 not defined"
    if (( sourced != 0 )); then
      return 1
    else
      exit 1
    fi
  fi
}
check_env CI_DEPLOY_USERNAME
check_env CI_DEPLOY_PASSWORD

if [ "$VERBOSE" == "true" ]; then
  MAVEN_OPT=--debug
fi
if [ "$MAVEN_OPT" == "" ]; then
  MAVEN_OPT=-B
else
  MAVEN_OPT="$MAVEN_OPT -B"
fi

if [ "$LOCAL" == "true" ]; then
  MAVEN_GOAL="install"
else
  MAVEN_GOAL="install deploy"
fi
# -T 2C
./mvnw $MAVEN_OPT -s ./.settings.xml $MAVEN_GOAL -T 1C -f stream-applications-build
./mvnw $MAVEN_OPT -s ./.settings.xml $MAVEN_GOAL -T 1C -f functions
./mvnw $MAVEN_OPT -s ./.settings.xml $MAVEN_GOAL -T 1C -f applications/stream-applications-core
