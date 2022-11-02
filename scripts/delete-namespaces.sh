#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

(return 0 2>/dev/null) && sourced=1 || sourced=0
NAMES_JSON=$(kubectl get namespaces --output=json | jq '.items | map({name:.metadata.name}) | map(select(.name | startswith("kube") | not)) | map(select(.name != "default"))')
NAMES=$(echo "$NAMES_JSON" | jq '.[] | .name' | sed 's/\"//g')
CARVEL=$(echo "$NAMES_JSON" | jq 'map(select(.name == "kapp-controller")) | .[] | .name')
if [ "$CARVEL" != "" ]; then
  CARVEL=--carvel
fi
while [ "$1" != "" ]; do
  case $1 in
    "--nowait")
      WAIT="--nowait"
      ;;
    "--wait")
      WAIT="--wait"
      ;;
    *)
      FILTER="$1"
      ;;
  esac
  shift
done
while [ "$1" != "" ]; do
  NAMES=$(echo "$NAMES_JSON" | jq --arg value "$1" 'map(select(.name | contains($value)) | .[] | .name' | sed 's/\"//g')
  for name in $NAMES; do
    $SCDIR/delete-k8s-ns.sh $CARVEL $name $WAIT
  done
done
if [ "$FILTER" == "" ]; then
  for name in $NAMES; do
    $SCDIR/delete-k8s-ns.sh $CARVEL $name $WAIT
  done
  $SCDIR/delete-k8s-ns.sh $CARVEL default $WAIT
fi
