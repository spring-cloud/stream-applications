#!/bin/bash
if [ "$1" == "" ]; then
  echo "Expect name of workflow to ignore"
  exit 1
fi
JOB="$1"
if [ "$2" != "" ]; then
  RETRIES=$2
else
  RETRIES=1
fi
JOBS=$(gh run list --json status,name --jq "map(select(.status == \"in_progress\" or .status == \"queued\")) | map(select(.name != \"$JOB\")) | length")
while ((JOBS != 0)); do
  RETRIES=$((RETRIES - 1))
  if ((RETRIES <= 0)); then
    echo "false"
    exit 0
  fi
  sleep 15
  JOBS=$(gh run list --json status,name --jq "map(select(.status == \"in_progress\" or .status == \"queued\")) | map(select(.name != \"$JOB\")) | length")
done
if ((JOBS == 0)); then
  echo "true"
else
  echo "false"
fi
