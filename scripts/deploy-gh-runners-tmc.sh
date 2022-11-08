#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
kubectl create clusterrolebinding actions-runner-privileged-cluster-role-binding \
  --clusterrole=vmware-system-tmc-psp-privileged \
  --group=system:authenticated
$SCDIR/deploy-gh-runners.sh
kubectl create rolebinding rolebinding-default-privileged-sa-ns_actions-runner-system \
  --namespace actions-runner-system \
  --clusterrole=vmware-system-tmc-psp-privileged \
  --user=system:serviceaccount:actions-runner-system:default