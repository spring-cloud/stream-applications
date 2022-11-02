#!/bin/bash
if [ "$1" == "" ]; then
  echo "Expect name of workflow to ignore"
fi
JOB="$1"
if [ "$2" != "" ]; then
  RETRIES=$2
else
  RETRIES=10
fi
JOBS=$(gh run list --json status,name --jq "map(select(.status == \"in_progress\" or .status == \"queued\")) | map(select(.name != \"$JOB\")) | length")
while ((JOBS != 0)); do
  RETRIES=$((RETRIES - 1))
  if ((RETRIES <= 0)); then
    echo "THERE CAN BE ONLY ONE!"
    exit 1
  fi
  echo "Jobs In progress or queued:"
  gh run list --json status,name --jq "map(select(.status == \"in_progress\" or .status == \"queued\")) | map(select(.name != \"$JOB\")) | .[] | .name"
  sleep 10
  JOBS=$(gh run list --json status,name --jq "map(select(.status == \"in_progress\" or .status == \"queued\")) | map(select(.name != \"$JOB\")) | length")
done
echo "There is only one."
