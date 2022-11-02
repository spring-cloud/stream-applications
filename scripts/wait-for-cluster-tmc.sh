#!/bin/bash
set +e

if [ "$1" != "" ]; then
  export CLUSTER_NAME="$1"
fi

if [ "$CLUSTER_NAME" = "" ]; then
  echo "CLUSTER_NAME not defined"
  exit 1
fi

echo "Waiting for $CLUSTER_NAME"

STATUS=$(tmc cluster get "$CLUSTER_NAME" --output json | jq '.status.phase' | sed 's/"//g')
while [ "$STATUS" != "READY" ]; do
  echo "Waiting for $CLUSTER_NAME. Status: $STATUS"
  sleep 30
  STATUS=$(tmc cluster get "$CLUSTER_NAME" --output json | jq '.status.phase' | sed 's/"//g')
  RC=$?
  if [ "$RC" != "0" ]; then
    MSG=$(tmc cluster get "$CLUSTER_NAME" --output json | jq '.status')
    echo "Error reading cluster $CLUSTER_NAME: $RC: $MSG"
    exit $RC
  fi
done
echo "Checking $CLUSTER_NAME health"
AGENT_READY=$(tmc cluster get "$CLUSTER_NAME" --output json | jq '.status.conditions."Agent-READY"')
READY=$(echo "$AGENT_READY" | jq '.status' | sed 's/"//g')
while [ "$READY" != "TRUE" ]; do
  echo "$CLUSTER_NAME Ready=$READY. $AGENT_READY"
  sleep 30
  AGENT_READY=$(tmc cluster get "$CLUSTER_NAME" --output json | jq '.status.conditions."Agent-READY"')
  READY=$(echo "$AGENT_READY" | jq '.status' | sed 's/"//g')
  RC=$?
  if [ "$RC" != "0" ]; then
    echo "$CLUSTER_NAME not ready. $RC $AGENT_READY"
    exit $RC
  fi
done
echo "Connected to $CLUSTER_NAME: Ready=$READY"
