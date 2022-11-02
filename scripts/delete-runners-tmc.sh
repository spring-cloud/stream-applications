#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

export CLUSTER_NAME="stream-apps-gh-runners"
set +e
source $SCDIR/kubeconfig-tmc.sh
echo "Deleting stream-apps-gh-runners"
$SCDIR/delete-k8s-ns.sh actions-runner-system
$SCDIR/delete-k8s-ns.sh cert-manager
kubectl delete rolebinding rolebinding-default-privileged-sa-ns_actions-runner-system
kubectl delete clusterrolebinding actions-runner-privileged-cluster-role-binding
$SCDIR/delete-cluster-tmc.sh
exit 0
