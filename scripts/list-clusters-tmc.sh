#!/bin/bash
if [ "$1" = "" ]; then
  echo "cluster name portion to match must be provided"
  exit 1
fi
set +e
COUNT=$(tmc cluster list | grep -c "$1")
if [ "$COUNT" != "0" ]; then
  echo "There are $COUNT $1 clusters remaining"
  CLUSTERS=$(tmc cluster list | grep "$1" | awk '{print $1}' | tr "\n" " " | tr "\r" " ")
  for cluster in $CLUSTERS; do
    echo "   $cluster"
  done
fi
