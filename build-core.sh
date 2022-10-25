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
else
  MAVEN_OPT=-q
fi
if [ "$LOCAL" == "true" ]; then
  MAVEN_GOAL="install"
else
  MAVEN_GOAL="install deploy"
fi
./mvnw $MAVEN_OPT -s ./.settings.xml $MAVEN_GOAL -f stream-applications-build -U
./mvnw $MAVEN_OPT -s ./.settings.xml $MAVEN_GOAL -f functions -U
./mvnw $MAVEN_OPT -s ./.settings.xml $MAVEN_GOAL -f functions/function-dependencies -U
./mvnw $MAVEN_OPT -s ./.settings.xml $MAVEN_GOAL -f applications/stream-applications-core -U
