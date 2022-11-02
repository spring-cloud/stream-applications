#!/bin/bash
if [ "$1" != "" ]; then
  NS="$1"
fi
if [ "$NS" == "" ]; then
  NS=default
fi
PODS=$(kubectl get pods --namespace $NS --output=json | jq '.items[] | .metadata.name' | sed 's/\"//g')
for pod in $PODS; do
  POD_JSON_FILE="${pod}.json"
  POD_JSON=$(kubectl get pod $pod --namespace $NS --output=json)
  if [ -f $POD_JSON_FILE ]; then
    mv $POD_JSON_FILE "${POD_JSON_FILE}.prev"
    echo "$POD_JSON" >$POD_JSON_FILE
    echo "Saved: $POD_JSON_FILE"
    diff --ignore-all-space $POD_JSON_FILE "${POD_JSON_FILE}.prev"
  else
    echo "$POD_JSON" >$POD_JSON_FILE
    echo "Saved: $POD_JSON_FILE"
  fi
  POD_FILE="${pod}.txt"
  POD_DESC=$(kubectl describe pod $pod --namespace $NS)
  if [ -f $POD_FILE ]; then
    mv $POD_FILE "${POD_FILE}.prev"
    echo "$POD_DESC" >$POD_FILE
    echo "Saved: $POD_FILE"
    diff --ignore-all-space $POD_FILE "${POD_FILE}.prev"
  else
    echo "$POD_DESC" >$POD_FILE
    echo "Saved: $POD_FILE"
  fi
done
