#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath "$SCDIR")
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
  MAVEN_OPT="-B -U -T 1C"
else
  MAVEN_OPT="$MAVEN_OPT -B -U -T 1C"
fi
if [ "$1" = "" ]; then
  MAVEN_GOAL="install"
else
  MAVEN_GOAL="$*"
fi
$SCDIR/build-folder.sh stream-applications-build "$MAVEN_GOAL"
$SCDIR/build-folder.sh functions "$MAVEN_GOAL"
$SCDIR/build-folder.sh applications/stream-applications-core "$MAVEN_GOAL"
