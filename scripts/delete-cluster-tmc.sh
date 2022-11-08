#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "$1 not defined"
    exit 0
  fi
}
if [ "$1" != "" ]; then
  export CLUSTER_NAME="$1"
fi
check_env CLUSTER_NAME
set +e
COUNT=$(tmc cluster list | grep -c -F "$CLUSTER_NAME")
if ((COUNT > 0)); then
  echo "Deleting cluster: $CLUSTER_NAME"
  set +e
  tmc cluster delete "$CLUSTER_NAME"
  COUNT=$(tmc cluster list | grep -c -F "$CLUSTER_NAME")
  while ((COUNT > 0)); do
    set +e
    STATUS=$(tmc cluster get $CLUSTER_NAME --output json | jq '.status.phase')
    RC=$?
    if ((RC != 0)); then
      echo "Cannot find cluster"
      exit 0
    fi
    echo "Waiting to delete $CLUSTER_NAME. Status=$STATUS"
    sleep 30
    set +e
    COUNT=$(tmc cluster list | grep -c -F "$CLUSTER_NAME")
  done
fi
