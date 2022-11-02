#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

echo "Authenticating against stream-apps-gh-runners"
source $SCDIR/kubeconfig-gke.sh stream-apps-gh-runners
echo "Configuring actions-runner-controller"
export NS="actions-runner-system"
$SCDIR/ensure-ns.sh $NS
set +e
$SCDIR/add-roles.sh "system:aggregate-to-edit" "system:aggregate-to-admin" "system:aggregate-to-view"
$SCDIR/deploy-gh-runners.sh

