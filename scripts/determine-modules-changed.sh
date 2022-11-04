#!/bin/bash
function itemInModules() {
  local e
  for e in ${!MODULES[@]}; do
    if [[ "$e" == "$ITEM" ]]; then
      echo "1"
      return 0
    fi
  done
  echo "0"
}
MODIFIED="$1"
FUNCTIONS=$(jq -c '.functions | .[]' matrix.json | sed 's/\"//g')
CONSUMERS=$(jq -c '.consumers | .[]' matrix.json | sed 's/\"//g')
SUPPLIERS=$(jq -c '.suppliers | .[]' matrix.json | sed 's/\"//g')
PROCESSORS=$(jq -c '.processors | .[]' matrix.json | sed 's/\"//g')
SINKS=$(jq -c '.sinks | .[]' matrix.json | sed 's/\"//g')
SOURCES=$(jq -c '.sources | .[]' matrix.json | sed 's/\"//g')
declare -A MODULES
for file in $MODIFIED; do
  FILE=$(realpath $file)
  echo "$file was changed"
  for app in $FUNCTIONS; do
    ITEM="functions/function/$app"
    echo "Checking:$ITEM"
    if [[ "$file" == *"$ITEM"* ]]; then
      echo "Matched $ITEM"
      HAS_ITEM=$(itemInModules)
      if ((HAS_ITEM == 0)); then
        echo "Add:$ITEM"
        MODULES+=("$ITEM")
      fi
    fi
  done
  for app in $CONSUMERS; do
    ITEM="functions/consumer/$app"
    echo "Checking:$ITEM"
    if [[ "$file" == *"$ITEM"* ]]; then
      echo "Matched $ITEM"
      HAS_ITEM=$(itemInModules)
      if ((HAS_ITEM == 0)); then
        echo "Add:$ITEM"
        MODULES+=("$ITEM")
      fi
    fi
  done
  for app in $SUPPLIERS; do
    ITEM="functions/supplier/$app"
    echo "Checking:$ITEM"
    if [[ "$file" == *"$ITEM"* ]]; then
      echo "Matched $ITEM"
      HAS_ITEM=$(itemInModules)
      if ((HAS_ITEM == 0)); then
        echo "Add:$ITEM"
        MODULES+=("$ITEM")
      fi
    fi
  done
  for app in $PROCESSORS; do
    ITEM="application/processor/$app"
    echo "Checking:$ITEM"
    if [[ "$file" == *"$ITEM"* ]]; then
      echo "Matched $ITEM"
      HAS_ITEM=$(itemInModules)
      if ((HAS_ITEM == 0)); then
        echo "Add:$ITEM"
        MODULES+=("$ITEM")
      fi
    fi
  done
  for app in $SINKS; do
    ITEM="application/sink/$app"
    echo "Checking:$ITEM"
    if [[ "$file" == *"$ITEM"* ]]; then
      echo "Matched $ITEM"
      HAS_ITEM=$(itemInModules)
      if ((HAS_ITEM == 0)); then
        echo "Add:$ITEM"
        MODULES+=("$ITEM")
      fi
    fi
  done
  for app in $SOURCES; do
    ITEM="application/source/$app"
    echo "Checking:$ITEM"
    if [[ "$file" == *"$ITEM"* ]]; then
      echo "Matched $ITEM"
      HAS_ITEM=$(itemInModules)
      if ((HAS_ITEM == 0)); then
        echo "Add:$ITEM"
        MODULES+=("$ITEM")
      fi
    fi
  done
done
echo "[" >modules.json
COUNT=0
for module in ${!MODULES[@]}; do
  if ((COUNT > 0)); then
    echo "," >>modules.json
  fi
  COUNT=$((COUNT + 1))
  echo "\"$module\"" >>modules.json
done
echo "]" >>modules.json

