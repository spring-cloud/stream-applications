#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
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
set -e
check_env CLUSTER_NAME
check_env K8S
check_env NODES

sed -i 's/name-changeme/'"$CLUSTER_NAME"'/g' .github/tmc/dataflow-tmc-template-mod.yaml
sed -i 's/version-changeme/'"$K8S"'/g' .github/tmc/dataflow-tmc-template-mod.yaml
sed -i 's/nodes-changeme/'"$NODES"'/g' .github/tmc/dataflow-tmc-template-mod.yaml
tmc cluster create -f .github/tmc/dataflow-tmc-template-mod.yaml
