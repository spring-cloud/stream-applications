#!/usr/bin/env bash
TO_DEL=$(gh run list --limit 100 -w "housekeeping.yml" --json "conclusion,event,databaseId,name,status,updatedAt" --jq 'map(select(.status == "completed" and .conclusion == "cancelled" and .event == "schedule"))')
if [ "$VERBOSE" != "" ] && [ "$VERBOSE" != "false" ]; then
  echo "Deleting:$TO_DEL"
fi
RUNS=$(gh run list --limit 100 -w "housekeeping.yml" --json "conclusion,event,databaseId,name,status,updatedAt"  --jq 'map(select(.status == "completed" and .conclusion == "cancelled" and .event == "schedule")) | .[] | .databaseId' | sed 's/\"//g')

for run in $RUNS; do
  echo "Deleting run $run"
  gh api --method DELETE -H "Accept: application/vnd.github+json" "/repos/pivotal/scdf-pro/actions/runs/$run"
done

RUNS=$(gh run list --limit 100 -w "clean-housekeeping.yml" --json "conclusion,databaseId,status"  --jq 'map(select(.status == "completed" and .conclusion == "success")) | .[] | .databaseId' | sed 's/\"//g')
for run in $RUNS; do
  echo "Deleting run $run"
  gh api --method DELETE -H "Accept: application/vnd.github+json" "/repos/pivotal/scdf-pro/actions/runs/$run"
done
