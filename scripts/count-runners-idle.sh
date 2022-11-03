#!/bin/bash
if [ "$1" = "" ]; then
  echo "RunnerDeployment name required"
  exit 1
fi
DEPLOYMENT_NAME="$1"
kubectl get runners -l runner-deployment-name="$DEPLOYMENT_NAME" --output=json | jq '.items | map(.status) | .[] | .phase' | grep -c -F "Idle"