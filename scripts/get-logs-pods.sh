#!/bin/bash
USE_NS=$1
if [ "$USE_NS" == "" ]; then
  if [ "$NS" == "" ]; then
    echo "NS not defined"
    exit 1
  else
    USE_NS=$NS
  fi
fi
set +e
PODS=$(kubectl get pods --namespace "$USE_NS" -o=jsonpath='{.items[*].metadata.name}' 2>/dev/null)
for pod in $PODS; do
  echo "=================================="
  echo "Config for $pod"
  POD_JSON=$(kubectl get pod $pod --namespace "$USE_NS" --output=json)
  echo "$POD_JSON"
  echo "----------------------------------"
  CN=$(echo "$POD_JSON" | jq '.spec.containers[0].name' | sed 's/\"//g')
  echo "Logs for $pod/$CN"
  kubectl logs $pod --container="$CN" --namespace "$USE_NS" --since=30m --timestamps=true
done
