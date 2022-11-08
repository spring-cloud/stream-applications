#!/bin/bash
AGE=90
if [ "$1" != "" ]; then
  AGE=$1
fi
WORKFLOWS=("ci-2021-0-x.yml" "ci-2021-1-x.yml" "ci-main.yml")
CURRENT_SEC=$(date +%s)
for workflow in ${WORKFLOWS[@]}; do
  WF_JSON=$(gh run list -w "$workflow" --limit 1 --json conclusion,event,databaseId,status,updatedAt)
  CONCLUSION=$(echo "$WF_JSON" | jq '.[0].conclusion' | sed 's/\"//g')
  UPDATED=$(echo "$WF_JSON" | jq '.[0].updatedAt' | sed 's/\"//g')
  EVENT=$(echo "$WF_JSON" | jq '.[0].event' | sed 's/\"//g')
  if [ "$CONCLUSION" == "failure" ] && [ "$EVENT" == "schedule" ]; then
    LAST_SEC=$(date --date "$UPDATED" +%s)
    DIFF_MIN=$(((CURRENT_SEC - LAST_SEC) / 60))
    if ((DIFF_MIN < AGE)); then
      ID=$(echo "$WF_JSON" | jq '.[0].databaseId')
      echo "$ID"
    fi
  fi
done
