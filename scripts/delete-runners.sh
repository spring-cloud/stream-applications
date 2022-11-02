#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

export CLUSTER_NAME="stream-apps-gh-runners"
set +e
echo "Deleting stream-apps-gh-runners"
RDEPLOY=$(kubectl get rdeploy --output=json | jq '.items | .[] | .metadata.name' | sed 's/\"//g')
for item in $RDEPLOY; do
  kubectl delete rdeploy $item
done
HRA=$(kubectl get HorizontalRunnerAutoscaler --output=json | jq '.items | .[] | .metadata.name' | sed 's/\"//g')
for item in $HRA; do
  kubectl delete HorizontalRunnerAutoscaler $item
done
$SCDIR/delete-k8s-ns.sh actions-runner-system
$SCDIR/delete-k8s-ns.sh cert-manager
exit 0
