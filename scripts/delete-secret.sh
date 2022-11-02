#!/bin/bash
NS=default
if [ "$1" == "" ]; then
  echo "Arguments: <secret-name> [namespace]"
  exit 1
fi
SECRET=$1
if [ "$2" != "" ]; then
  NS=$2
fi
COUNT=$(kubectl get secrets --namespace $NS | grep -c -F "$SECRET")
if ((COUNT > 0)); then
  kubectl delete secret $SECRET --namespace $NS
fi
