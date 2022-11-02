#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ "$CLUSTER_NAME" = "" ]; then
  echo "CLUSTER_NAME not defined"
  return 1
fi
mkdir -p $HOME/.kube
echo "Connecting to $CLUSTER_NAME"
set +e
MAX=45
export KUBECONFIG=
for i in $(seq $MAX); do
  tmc cluster auth kubeconfig get $CLUSTER_NAME > $HOME/.kube/config
  RC=$?
  if [ "$RC" = "0" ]; then
    break;
  fi
  sleep 20
done
if [ "$RC" != "0" ]; then
  echo "Error connecting to $CLUSTER_NAME"
  return $RC
fi
export KUBECONFIG=$HOME/.kube/config
echo "Connected to $CLUSTER_NAME"
