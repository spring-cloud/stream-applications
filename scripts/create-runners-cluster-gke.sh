#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

export CLUSTER_NAME="stream-apps-gh-runners"
if [ "$REGION" == "" ]; then
  export REGION="us-central1"
fi

# MACHINE_TYPE="e2-standard-8"
export K8S="1.23"
export MACHINE_TYPE=$($SCDIR/determine-default.sh $CLUSTER_NAME "machine_type")
export DISK_SIZE=$($SCDIR/determine-default.sh $CLUSTER_NAME "disk_size")
export NODES=1
export PODS_PER_NODE=30


echo "Creating stream-apps-gh-runners"
$SCDIR/create-cluster-tf-gke.sh stream-apps-gh-runners
