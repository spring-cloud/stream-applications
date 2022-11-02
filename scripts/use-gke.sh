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
    if (( sourced != 0 )); then
      exit 1
    else
      return 1
    fi
}

if [ "$1" == "" ]; then
  usage
fi
export CLUSTER_NAME="$1"
echo "Determining region for $CLUSTER_NAME"
export REGION=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $2}')
if [ "$REGION" == "" ]; then
  echo "Cannot find $CLUSTER_NAME"
  if (( sourced != 0 )); then
      exit 1
    else
      return 1
    fi
fi
echo "Region:$REGION"
