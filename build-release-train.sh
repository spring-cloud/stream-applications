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

if [ "$1" = "" ]; then
  MAVEN_GOAL="install"
else
  MAVEN_GOAL="$*"
fi

$SCDIR/build-folder.sh stream-applications-release-train "$MAVEN_GOAL"
