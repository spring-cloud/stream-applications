#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$1" = "" ]; then
  echo "Workflow name required"
  exit 2
fi
$SCDIR/list-last-workflows.sh "$1" success
if [ "$2" != "" ]; then
  jq --arg conclusion "$2" '.[] | select(.conclusion == $conclusion)' sort_last.json | jq -s '.' > sort_last2.json
  mv sort_last2.json sort_last.json
fi
LAST_TS=$(jq '.[0] | .updated' sort_last.json | sed 's/\"//g')
if [ "$LAST_TS" = "" ] || [ "$LAST_TS" = "null" ]; then
  echo "0"
else
  LAST_SEC=$(date --date "$LAST_TS" '+%s')
  NOW_SEC=$(date '+%s')
  echo "$(((NOW_SEC - LAST_SEC) / 60))"
fi
