#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

(return 0 2>/dev/null) && sourced=1 || sourced=0
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "$1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}

if [ "$1" != "" ]; then
  CLUSTER_NAME=$1
fi
check_env CLUSTER_NAME

PROVIDER=$($SCDIR/determine-default.sh $CLUSTER_NAME "provider")
if [ ! -f "$SCDIR/use-${PROVIDER}.sh" ]; then
  echo "Invalid provider: $PROVIDER. Use one of gke, tmc"
  exit 1
fi
export PROVIDER
echo "$PROVIDER"
