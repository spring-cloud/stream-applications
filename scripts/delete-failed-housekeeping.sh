#!/bin/bash
TO_DEL=$(gh run list --limit 100 -w "housekeeping.yml" --json "conclusion,databaseId,name,status" --jq 'map(select(.status == "completed" and .conclusion == "failure"))')
if [ "$VERBOSE" != "" ] && [ "$VERBOSE" != "false" ]; then
  echo "Deleting:$TO_DEL"
fi
RUNS=$(echo "$TO_DEL" | jq '.[] | .databaseId' | sed 's/\"//g')
for run in $RUNS; do
  echo "Deleting run $run"
  gh api --method DELETE -H "Accept: application/vnd.github+json" "/repos/spring-cloud/stream-applications/actions/runs/$run"
done

RUNS=$(gh run list --limit 100 -w "clean-housekeeping.yml" --json "conclusion,databaseId,status"  --jq 'map(select(.status == "completed" and .conclusion == "failure")) | .[] | .databaseId' | sed 's/\"//g')
for run in $RUNS; do
  echo "Deleting run $run"
  gh api --method DELETE -H "Accept: application/vnd.github+json" "/repos/spring-cloud/stream-applications/actions/runs/$run"
done
