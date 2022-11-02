#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

export CLUSTER_NAME="stream-apps-gh-runners"
if [ "$REGION" == "" ]; then
  export REGION="us-central1"
fi

export K8S=$($SCDIR/determine-default.sh $CLUSTER_NAME "k8s_version")
export MACHINE_TYPE=$($SCDIR/determine-default.sh $CLUSTER_NAME "machine_type")
if [ "$MACHINE_TYPE" == "" ] || [ "$MACHINE_TYPE" == "null" ]; then
  echo "Could not determine machine_type for $CLUSTER_NAME"
fi
export DISK_SIZE=$($SCDIR/determine-default.sh $CLUSTER_NAME "disk_size")
if [ "$DISK_SIZE" == "" ] || [ "$DISK_SIZE" == "null" ]; then
  echo "Could not determine disk_size for $CLUSTER_NAME"
fi
export NODES=1
export PODS_PER_NODE=30


echo "Creating stream-apps-gh-runners"
$SCDIR/create-cluster-tf-gke.sh $CLUSTER_NAME
