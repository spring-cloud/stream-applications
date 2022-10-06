#!/usr/bin/env bash
WORKFLOWS=("gke-full-ci.yml" "gke-ga-ci.yml" "gke-main-ci.yml" "gke-quick-ga-ci.yml" "gke-quick-main-ci.yml" "gke-smoke-ga-ci.yml" "gke-smoke-main-ci.yml")
echo "Listing last workflows"
echo "[" > last.json
ITEMS=0
for workflow in ${WORKFLOWS[@]}; do
  WF_JSON=$(gh run list -w "$workflow" --limit 1 --json conclusion,event,databaseId,name,status,updatedAt)
  CONCLUSION=$(echo "$WF_JSON" | jq '.[0].conclusion' | sed 's/\"//g')
  if [ "$CONCLUSION" != "null" ]  && [ "$CONCLUSION" != "" ]; then
    STATUS=$(echo "$WF_JSON" | jq '.[0].status' | sed 's/\"//g')
    UPDATED=$(echo "$WF_JSON" | jq '.[0].updatedAt' | sed 's/\"//g')
    EVENT=$(echo "$WF_JSON" | jq '.[0].event' | sed 's/\"//g')
    NAME=$(echo "$WF_JSON" | jq '.[0].name' | sed 's/\"//g')
    ID=$(echo "$WF_JSON" | jq '.[0].databaseId')
    if ((ITEMS > 0)); then
      echo "," >> last.json
    fi
    ITEMS=$((ITEMS+1))
    echo "{\"updated\":\"$UPDATED\", \"status\":\"$STATUS\", \"conclusion\":\"$CONCLUSION\", \"event\":\"$EVENT\", \"id\":\"$ID\", \"name\":\"$NAME\"}" >> last.json
  fi
done
echo "]" >> last.json
jq -s '.[] | sort_by(.updated) | reverse' last.json > sort_last.json
