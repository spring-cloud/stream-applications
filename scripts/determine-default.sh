#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

(return 0 2>/dev/null) && sourced=1 || sourced=0

function determine_provider() {
  CHECK_NAME=$1
  CHECK_NAME=${CHECK_NAME//\-/_}
  CHECK_VALUE=$2
  CHECK_VALUE=${CHECK_VALUE//\-/_}
  QUERY=".${CHECK_NAME}.${CHECK_VALUE}"
  VALUE=$(jq ''"$QUERY"'' $PARENT/config/defaults.json | sed 's/\"//g')
  if [ "$VALUE" == "" ] || [ "$VALUE" == "null" ]; then
    KEYS=$(jq 'to_entries[] | .key' $PARENT/config/defaults.json | sed 's/\"//g')
    CHECK_NAME=
    PREFIX=$1
    PREFIX=${PREFIX//\-/_}
    for key in $KEYS; do
      if [[ "$PREFIX" == *"$key"* ]]; then
        CHECK_NAME=$key
      fi
    done
    if [ "$CHECK_NAME" != "" ]; then
      QUERY=".${CHECK_NAME}.${CHECK_VALUE}"
      VALUE=$(jq ''"$QUERY"'' $PARENT/config/defaults.json | sed 's/\"//g')
    fi
    if [ "$VALUE" == "" ] || [ "$VALUE" == "null" ]; then
      CHECK_NAME=default
      QUERY=".${CHECK_NAME}.${CHECK_VALUE}"
      VALUE=$(jq ''"$QUERY"'' $PARENT/config/defaults.json | sed 's/\"//g')
    fi
  fi
  echo "$VALUE"
}

if [ "$2" == "" ]; then
  echo "Usage: $0 <name> <field>"
  if ((sourced != 0)); then
    return 1
  else
    exit 1
  fi
fi
BLOCK="$1"
FIELD="$2"

VALUE=$(determine_provider "$BLOCK" "$FIELD")
export VALUE
echo "$VALUE"
if [ "$VALUE" == "" ] || [ "$VALUE" == "null" ]; then
  exit 1
fi