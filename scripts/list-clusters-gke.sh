#!/bin/bash
if [ "$1" = "" ]; then
  echo "cluster name portion to match must be provided"
  exit 1
fi
set +e
COUNT=$(gcloud container clusters list | grep -c "$1")
if [ "$COUNT" != "0" ]; then
  echo "There are $COUNT $1 clusters"
  export CLUSTERS=$(gcloud container clusters list | grep "$1" | awk '{print $1}' | tr "\n" " " | tr "\r" " ")
  for cluster in $CLUSTERS; do
    echo "    $cluster"
  done
fi
