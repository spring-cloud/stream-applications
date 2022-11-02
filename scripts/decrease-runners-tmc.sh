#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

function count_runners() {
    kubectl get pods --namespace default | grep Running |  grep -c "runners-stream-ci"
}

if [ "$1" = "" ]; then
  echo "Expected number to decrease"
  exit 1
fi
mkdir -p $HOME/.kube
echo "Connecting to stream-apps-gh-runners"
tmc cluster auth kubeconfig get stream-apps-gh-runners > $HOME/.kube/runners-config
export KUBECONFIG=$HOME/.kube/runners-config
$SCDIR/decrease-runners.sh $*