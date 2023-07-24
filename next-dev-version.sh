#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
function update_field() {
  FILE=$1
  FIELD=$2
  VALUE=$3
  cat $FILE | jq --arg ndv "$VALUE" "$FIELD = \$ndv" > "$FILE.tmp"
  CHECK=$(jq -r "$FIELD" "$FILE.tmp")
  if [ "$CHECK" != "$VALUE" ]; then
    echo "Expected $FIELD to be $VALUE not $CHECK"
    exit 1
  fi
  cp -f "$FILE.tmp" "$FILE"
  rm -f "$FILE.tmp"
}
set +e
RELEASE_VERSION=$(jq -r '.release_version' ./versions.json)
NEXT_DEV_VERSION=$(jq -r '.next_dev_version' ./versions.json)
if [ "$RELEASE_VERSION" != "$NEXT_DEV_VERSION" ]; then
  echo "Updating RELEASE_VERSION: $RELEASE_VERSION to NEXT_DEV_VERSION: $NEXT_DEV_VERSION"
  update_field ./versions.json ".release_version" $NEXT_DEV_VERSION
  RELEASE_VERSION=$(jq -r '.release_version' ./versions.json)
  echo "Updated RELEASE_VERSION: $RELEASE_VERSION"
else
  echo "No change in $RELEASE_VERSION -> $NEXT_DEV_VERSION"
fi
RELEASE_TRAIN_VERSION=$(jq -r '.release_train.release_version' ./versions.json)
RELEASE_TRAIN_NEXT_DEV_VERSION=$(jq -r '.release_train.next_dev_version' ./versions.json)
if [ "$RELEASE_TRAIN_VERSION" != "$RELEASE_TRAIN_NEXT_DEV_VERSION" ]; then
  echo "Updating RELEASE_TRAIN_VERSION: $RELEASE_TRAIN_VERSION to RELEASE_TRAIN_NEXT_DEV_VERSION: $RELEASE_TRAIN_NEXT_DEV_VERSION"
  update_field ./versions.json ".release_train.release_version" "$RELEASE_TRAIN_NEXT_DEV_VERSION"
  RELEASE_TRAIN_VERSION=$(jq -r '.release_train.release_version' ./versions.json)
  echo "Updated RELEASE_TRAIN_VERSION: $RELEASE_TRAIN_VERSION"
else
  echo "No change in $RELEASE_TRAIN_VERSION -> $RELEASE_TRAIN_NEXT_DEV_VERSION"
fi
