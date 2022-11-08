#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if (( sourced == 0 )); then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ "$1" != "" ]; then
  export CLUSTER_NAME="$1"
fi
if [ "$CLUSTER_NAME" = "" ]; then
  echo "CLUSTER_NAME not defined"
  return 1
fi
if [ "$REGION" == "" ]; then
  if [ "$REGION" == "" ]; then
    if [ "$2" != "" ]; then
      export REGION="$2"
    else
      export REGION=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $2}')
      if [ "$REGION" == "" ]; then
        echo "Cannot find REGION from $CLUSTER_NAME"
        return 1
      fi
    fi
  fi
fi
export USE_GKE_GCLOUD_AUTH_PLUGIN=True
mkdir -p $HOME/.kube
echo "Connecting to $CLUSTER_NAME"
set +e
MAX=45
export KUBECONFIG=$HOME/.kube/config-gke
for i in $(seq $MAX); do
  gcloud container clusters get-credentials $CLUSTER_NAME --region $REGION
  RC=$?
  if [ "$RC" = "0" ]; then
    break;
  fi
  sleep 20
done
if [ "$RC" != "0" ]; then
  echo "Error connecting to $CLUSTER_NAME"
  exit $RC
fi
echo "Connected to $CLUSTER_NAME"
