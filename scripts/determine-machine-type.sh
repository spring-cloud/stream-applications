#!/bin/bash
function check_env() {
    eval ev='$'$1
    if [ "$ev" == "" ]; then
      echo "$1 not defined"
      exit 1
    fi
}
check_env CLUSTER_NAME
check_env REGION
set +e
MACHINE_TYPE=$(gcloud container clusters describe $CLUSTER_NAME --region $REGION "--format=table(nodeConfig.machineType)" 2>/dev/null | grep -v "MACHINE_TYPE")
RC=$?
if ((RC > 0)); then
    exit 0
fi
echo "$MACHINE_TYPE"
