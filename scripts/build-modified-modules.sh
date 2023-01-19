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
MODULES=
for file in $MODIFIED; do
  FILE=$(realpath $file)
  echo "$file was changed"
  for ITEM in $ALL_MODULES; do
    if [[ "$ITEM" != "." ]] && [[ "$file" == *"$ITEM"* ]]; then
      echo "Matched $ITEM"
      HAS_ITEM=$(itemInModules)
      if ((HAS_ITEM == 0)); then
        echo "Add:$ITEM"
        MODULES=$(addItem "$ITEM")
      fi
      break
    fi
  done
done

MVN_MODULES=

for module in $MODULES; do
  if [[ "$module" == *"/"* ]]; then
    if [ "$MVN_MODULES" == "" ]; then
      MVN_MODULES=$module
    else
      MVN_MODULES="$MVN_MODULES,$module"
    fi
  else
    ./mvnw install -pl $module
  fi
done
if [ "$MVN_MODULES" != "" ]; then
  echo "Building $MVN_MODULES"
  ./mvnw install -pl "$MVN_MODULES" -amd
fi
