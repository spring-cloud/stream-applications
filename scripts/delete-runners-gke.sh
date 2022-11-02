#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

export CLUSTER_NAME="stream-apps-gh-runners"
set +e
echo "Deleting stream-apps-gh-runners"
$SCDIR/delete-k8s-ns.sh actions-runner-system --nowait
$SCDIR/delete-k8s-ns.sh cert-manager --nowait
rm -f $SCDIR/terraform/stream-apps-gh-runners/stream-apps-gh-runners.tfstate*
kubectl delete rolebinding rolebinding-default-privileged-sa-ns_actions-runner-system
kubectl delete clusterrolebinding actions-runner-privileged-cluster-role-binding
$SCDIR/delete-cluster-gke.sh
exit 0

