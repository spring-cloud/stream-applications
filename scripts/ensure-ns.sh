#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$1" == "" ]; then
  echo "Usage: ensure-ns.sh <namespace>"
  if ((sourced != 0)); then
    return 1
  else
    exit 1
  fi
fi
NS=$1
set +e
FOUND=$(kubectl get namespaces --output=json | jq --arg namespace $NS '.items | map(select(.metadata.name == $namespace)) | .[] | .metadata.name')
if [ "$FOUND" == "" ]; then
  echo "Creating namespace $NS"
  kubectl create namespace "$NS"
  set +e
  COUNT=$(kubectl get namespaces 2>/dev/null | grep -c -F "$NS")
  while ((COUNT == 0)); do
    echo "Waiting for Namespace:$NS"
    sleep 3
    COUNT=$(kubectl get namespaces 2>/dev/null | grep -c -F "$NS")
  done
else
  echo "Namespace $NS exists"
fi
