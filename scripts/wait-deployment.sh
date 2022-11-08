#!/bin/bash
set +e
if [ "$1" == "" ]; then
  echo "Deployment name required"
  exit 2
fi
DEPLOYMENT=$1
if [ "$2" != "" ]; then
  NAMESPACE="--namespace $2"
else
  NAMESPACE=
fi
echo "Checking deployment $DEPLOYMENT"
kubectl rollout status deployment "$DEPLOYMENT" $NAMESPACE
REPLICAS=$(kubectl get deployment "$DEPLOYMENT" $NAMESPACE --output json | jq '.status.readyReplicas')
echo "Deployment $DEPLOYMENT: Replicas: $REPLICAS"
RETRIES=40
while [[ "$REPLICAS" == "" ]] || [[ $REPLICAS -eq 0 ]]; do
  STATUS=$(kubectl get deployment "$DEPLOYMENT" $NAMESPACE --output json | jq '.status')
  echo "Waiting for $DEPLOYMENT Status: $STATUS"
  sleep 15
  REPLICAS=$(kubectl get deployment "$DEPLOYMENT" $NAMESPACE --output json | jq '.status.readyReplicas')
  RC=$?
  echo "Deployment $DEPLOYMENT: Replicas: $REPLICAS"
  if [ "$RC" != "0" ]; then
    STATUS=$(kubectl get deployment "$DEPLOYMENT" $NAMESPACE --output json | jq '.status')
    echo "Error checking deployment $DEPLOYMENT $RC: $STATUS"
    exit $RC
  fi
  if (( RETRIES <= 0 )); then
    STATUS=$(kubectl get deployment "$DEPLOYMENT" $NAMESPACE --output json)
    echo "Timeout checking deployment $DEPLOYMENT in $NAMESPACE: $STATUS"
    exit 1
  fi
  RETRIES=$(( RETRIES - 1 ))
done
echo "Deployment $DEPLOYMENT done"
