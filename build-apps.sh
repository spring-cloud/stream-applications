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
RC=0
set +e
$SCDIR/create-matrices.sh
PROCESSORS=$(jq -c '.processors | .[]' matrix.json | sed 's/\"//g')
SINKS=$(jq -c '.sinks | .[]' matrix.json | sed 's/\"//g')
SOURCES=$(jq -c '.sources | .[]' matrix.json | sed 's/\"//g')
for app in $PROCESSORS; do
  $SCDIR/build-app.sh . "applications/processor/$app"
  RCC=$?
  if ((RCC!=0)); then
    RC=$RCC
  fi
done
for app in $SOURCES; do
  $SCDIR/build-app.sh . "applications/source/$app"
  RCC=$?
  if ((RCC!=0)); then
    RC=$RCC
  fi
done
for app in $SINKS; do
  $SCDIR/build-app.sh . "applications/sink/$app"
  RCC=$?
  if ((RCC!=0)); then
    RC=$RCC
  fi
done
exit $RC
