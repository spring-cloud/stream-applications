#!/bin/bash
function itemInModules() {
  local e
  for e in ${MODULES}; do
    if [[ "$e" == "$ITEM" ]]; then
      echo "1"
      return 0
    fi
  done
  echo "0"
}
function addItem() {
    if [ "$MODULES" == "" ]; then
      echo "$1"
    else
      echo "$MODULES $1"
    fi
}
MODIFIED="$*"
ALL_MODULES=$(find . -name "pom.xml" -type f -exec dirname '{}' \; | sed 's/\.\///' | sort -r)
echo "[" >modules.json
COUNT=0
for module in $ALL_MODULES; do
  if ((COUNT > 0)); then
    echo "," >> modules.json
  fi
  COUNT=$((COUNT + 1))
  echo "\"$module\"" >> modules.json
done
echo "]" >>modules.json
