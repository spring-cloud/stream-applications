#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

(return 0 2>/dev/null) && sourced=1 || sourced=0
if (( sourced == 0 )); then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
export CLUSTER_NAME=stream-apps-gh-runners
set +e
PROVIDER=$($SCDIR/determine-provider.sh "$CLUSTER_NAME")
source $SCDIR/use-${PROVIDER}.sh $CLUSTER_NAME
source $SCDIR/kubeconfig-${PROVIDER}.sh $CLUSTER_NAME
