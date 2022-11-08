#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

(return 0 2>/dev/null) && sourced=1 || sourced=0
if (( sourced == 0 )); then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
function usage() {
    echo "Usage is $0 <cluster>"
    if [ $sourced -eq 0 ]; then
      exit 1
    else
      return 1
    fi
}

if [ "$1" == "" ]; then
  usage
fi
export CLUSTER_NAME="$1"
tmc configure --management-cluster-name aws-hosted --provisioner-name scdf-provisioner
tmc cluster auth kubeconfig get $CLUSTER_NAME > /dev/null
