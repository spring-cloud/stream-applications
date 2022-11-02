#!/bin/bash
if [ "$1" != "" ]; then
  WORKFLOWS=("$1")
else
  WORKFLOWS=("ci-2021-1-x.yml" "ci-2021-0-x.yml" "ci-main.yml")
fi
echo "" > last.json
for workflow in ${WORKFLOWS[@]}; do
  rm -f wf.json
  gh run list -w "$workflow" --json conclusion,event,databaseId,name,status,updatedAt > wf.json
  jq -c '.[] | {"updated":.updatedAt,"status":.status,"conclusion":.conclusion,"event":.event,"id":.databaseId,"name":.name}' wf.json >> last.json
done
jq -s 'sort_by(.updated) | reverse' last.json > sort_last.json
