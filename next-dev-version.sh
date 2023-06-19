#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

set +e
RELEASE_VERSION=$(jq -r '.release_version' ./versions.json)
NEXT_DEV_VERSION=$(jq -r '.next_dev_version' ./versions.json)
if [ "$RELEASE_VERSION" != "$NEXT_DEV_VERSION" ]; then
  echo "Updating RELEASE_VERSION: $RELEASE_VERSION to NEXT_DEV_VERSION: $NEXT_DEV_VERSION"
  cat ./versions.json | jq --arg ndv "$NEXT_DEV_VERSION" '.release_version = $ndv' | tee ./versions.json
  RELEASE_VERSION=$(jq -r '.release_version' ./versions.json)
  echo "Updated RELEASE_VERSION: $RELEASE_VERSION"
else
  echo "No change in $RELEASE_VERSION -> $NEXT_DEV_VERSION"
fi
RELEASE_TRAIN_VERSION=$(jq -r '.release_train.release_version' ./versions.json)
RELEASE_TRAIN_NEXT_DEV_VERSION=$(jq -r '.release_train.next_dev_version' ./versions.json)
if [ "$RELEASE_TRAIN_VERSION" != "$RELEASE_TRAIN_NEXT_DEV_VERSION" ]; then
  echo "Updating RELEASE_TRAIN_VERSION: $RELEASE_TRAIN_VERSION to RELEASE_TRAIN_NEXT_DEV_VERSION: $RELEASE_TRAIN_NEXT_DEV_VERSION"
  cat ./versions.json | jq --arg ndv "$RELEASE_TRAIN_NEXT_DEV_VERSION" '.release_train.release_version = $ndv' | tee ./versions.json
  RELEASE_TRAIN_VERSION=$(jq -r '.release_train.release_version' ./versions.json)
  echo "Updated RELEASE_TRAIN_VERSION: $RELEASE_TRAIN_VERSION"
else
  echo "No change in $RELEASE_TRAIN_VERSION -> $RELEASE_TRAIN_NEXT_DEV_VERSION"
fi
