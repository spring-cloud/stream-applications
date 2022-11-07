#!/bin/bash
bold="\033[1m"
red="\033[31m"
dim="\033[2m"
end="\033[0m"

(return 0 2>/dev/null) && sourced=1 || sourced=0
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "$1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}

if [ "$VERBOSE" == "true" ]; then
  MAVEN_OPTS="-s ./.settings.xml --debug -B -T 1C"
else
  MAVEN_OPTS="-s ./.settings.xml -B -T 1C"
fi
if [ "$1" == "" ]; then
  echo -e "Options: ${bold} <folder> [<maven-goals>]*${end}"
fi
FOLDER_NAMES="$1"
shift
MAVEN_GOAL=$*
if [ "$MAVEN_GOAL" == "" ]; then
  echo -e "Using default goal ${bold}install${end}"
  MAVEN_GOAL="install"
fi
SAVED_IFS=$IFS
IFS=,
FOLDERS=
for FOLDER in $FOLDER_NAMES; do
  if [ ! -d "$FOLDER" ]; then
    echo -e "${bold}Folder not found ${red}$FOLDER${end}"
    exit 2
  fi
  if [ "$FOLDERS" == "" ]; then
    FOLDERS="$FOLDER"
  else
    FOLDERS="$FOLDERS $FOLDER"
  fi
done
# IFS will affect mvnw ability to split arguments.
IFS=$SAVED_IFS
if [[ "$MAVEN_GOAL" == *"deploy"* ]]; then
  check_env CI_DEPLOY_USERNAME
  check_env CI_DEPLOY_PASSWORD
fi
for FOLDER in $FOLDERS; do
  RETRIES=3
  while ((RETRIES > 0)); do
    set +e
    ./mvnw -U -f "$FOLDER" $MAVEN_OPTS dependency:tree
    set -e
    RESULT=$?
    if ((RESULT == 0)); then
      break
    fi
    RETRIES=$((RETRIES - 1))
  done
done

MODULE_ARGS=""
for FOLDER in $FOLDERS; do
  if [ "$MAVEN_GOAL" != "install" ]; then
    echo -e "Maven goals:${bold}-f $FOLDER -DskipTests install${end}"
    ./mvnw -f "$FOLDER" -DskipTests $MAVEN_OPTS install
  fi
  if [ "$MODULE_ARGS" == "" ]; then
    MODULE_ARGS="$FOLDER"
  else
    MODULE_ARGS="$MODULE_ARGS,$FOLDER"
  fi
done
echo -e "Maven goals:${bold}-pl $MODULE_ARGS $MAVEN_GOAL${end}"
./mvnw -pl "$MODULE_ARGS" $MAVEN_OPTS $MAVEN_GOAL
