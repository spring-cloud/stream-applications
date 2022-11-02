#!/bin/bash
set +e
WAIT_NODES=false
while [ "$1" != "" ]; do
  case $1 in
  "--nodes")
    WAIT_NODES=true
    ;;
  *)
    CLUSTER_NAME="$1"
    ;;
  esac
  shift
done

if [ "$CLUSTER_NAME" = "" ]; then
  echo "CLUSTER_NAME not defined"
  exit 1
fi
REGION=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $2}')
if [ "$REGION" == "" ]; then
  echo "Cannot find $CLUSTER_NAME"
  exit 2
fi

echo "Waiting for $CLUSTER_NAME"

STATUS=$(gcloud container clusters describe "$CLUSTER_NAME" --region $REGION --format json | jq '.status' | sed 's/"//g')
while [ "$STATUS" != "RUNNING" ]; do
  echo "Waiting for $CLUSTER_NAME. Status: $STATUS"
  sleep 30
  STATUS=$(gcloud container clusters describe "$CLUSTER_NAME" --region $REGION --format json | jq '.status' | sed 's/"//g')
  RC=$?
  if [ "$RC" != "0" ]; then
    echo "Error reading cluster $CLUSTER_NAME: $RC: $STATUS"
    exit $RC
  fi
done
echo "Connected to $CLUSTER_NAME: Status=$STATUS"
if [ "$WAIT_NODES" == "true" ]; then
  NODE_POOL=$(gcloud container node-pools list --region "$REGION" --cluster "$CLUSTER_NAME" "--format=table(name,status,config.tags)" | grep -F "$CLUSTER_NAME")
  STATUS=$(echo "$NODE_POOL" | awk '{print $2}')
  POOL=$(echo "$NODE_POOL" | awk '{print $1}')
  while [ "$STATUS" != "RUNNING" ]; do
    echo "Waiting for node pool $CLUSTER_NAME/$POOL. Status: $STATUS"
      sleep 30
      NODE_POOL=$(gcloud container node-pools list --region "$REGION" --cluster "$CLUSTER_NAME" "--format=table(name,status,config.tags)" | grep -F "$CLUSTER_NAME")
      RC=$?
      POOL=$(echo "$NODE_POOL" | awk '{print $1}')
      STATUS=$(echo "$NODE_POOL" | awk '{print $2}')
      if [ "$RC" != "0" ]; then
        echo "Error reading node pool $CLUSTER_NAME/$POOL: $RC: $STATUS"
        exit $RC
      fi
  done
  echo "Node pool $POOL - Status: $STATUS"
fi
