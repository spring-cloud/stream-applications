#!/bin/bash
function add_app() {
  if ((COUNT > 0)); then
    echo "," >> matrix.json
  fi
  echo "\"$1\"" >> matrix.json
  COUNT=$((COUNT+1))
  TOTAL=$((TOTAL+1))
}

pushd applications/processor > /dev/null
PROCESSORS=$(find * -maxdepth 0 -type d)
popd  > /dev/null
pushd applications/sink  > /dev/null
SINKS=$(find * -maxdepth 0 -type d)
popd  > /dev/null
pushd applications/source  > /dev/null
SOURCES=$(find * -maxdepth 0 -type d)
popd  > /dev/null

TOTAL=0
echo "{" > matrix.json
echo "\"processors\":[" >> matrix.json
COUNT=0
for app in $PROCESSORS; do
  add_app $app
done
echo "],\"sinks\":[" >> matrix.json
COUNT=0
for app in $SINKS; do
  add_app $app
done
echo "],\"sources\":[" >> matrix.json
COUNT=0
for app in $SOURCES; do
  add_app $app
done
echo "]" >> matrix.json
echo ",\"count\": $TOTAL" >> matrix.json
echo "}" >> matrix.json
MATRIX=$(jq -c . matrix.json)
echo "$MATRIX" > matrix.json
