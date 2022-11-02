#!/bin/bash
RETRIES=$1
ERR_DIR="/tmp/$(dirname $0)"
ERR_FILE="/tmp/$0.err"
mkdir -p "$ERR_DIR"
shift
# shellcheck disable=SC2086
CONDITION=$(kubectl wait $* 2> $ERR_FILE)
COUNT=$(echo "$CONDITION" | grep -c -F "condition met")
if ((COUNT > 0)); then
  echo "Condition met $COUNT times"
  exit 0
fi
RC=$?
while [ "$RC" != "0" ]; do
  # shellcheck disable=SC2086
  if [ -f $ERR_FILE ]; then
      cat "$ERR_FILE"
      rm -f "$ERR_FILE"
  fi
  kubectl wait $* 2> "$ERR_FILE"
  if ((COUNT > 0)); then
    echo "Condition met $COUNT times"
    exit 0
  fi
  RC=$?
  RETRIES=$(( RETRIES - 1 ))
  echo "RC:$RC, RETRIES=$RETRIES"
  if [ $RETRIES -eq 0 ]; then
    break;
  fi
done
if [ "$RC" != "0" ] && [ -f "$ERR_FILE" ]; then
  cat "$ERR_FILE"
  rm -f "$ERR_FILE"
  rm -rf "$ERR_DIR"
fi
exit $RC
