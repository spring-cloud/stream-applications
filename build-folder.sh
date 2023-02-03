#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

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
  MAVEN_OPTS="--debug"
elif [ "$VERBOSE" == "false" ]; then
  MAVEN_OPTS="-q"
fi
if [ "$MAVEN_OPTS" == "" ]; then
  MAVEN_OPTS="-s $SCDIR/.settings.xml -B"
else
  MAVEN_OPTS="$MAVEN_OPTS -s $SCDIR/.settings.xml -B"
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
START_TIME=$(date +%s)
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
  check_env ARTIFACTORY_USERNAME
  check_env ARTIFACTORY_PASSWORD
fi

RETRIES=3
for FOLDER in $FOLDERS; do
  while ((RETRIES >= 0)); do
    echo -e "Resolving dependencies for ${bold}$FOLDER${end}"
    set +e
    $SCDIR/mvnw -U -f "$FOLDER" $MAVEN_OPTS -T 0.75C dependency:resolve
    RESULT=$?
    set -e
    if ((RESULT == 0)); then
      break
    fi
    RETRIES=$((RETRIES - 1))
    if ((RETRIES >= 0)); then
      echo -e "RETRY:Resolving dependencies for ${bold}$FOLDER${end}"
    fi
  done
done

if ((RESULT == 0)); then
  for FOLDER in $FOLDERS; do
    set +e
    if [ -f "$FOLDER/set-env.sh" ]; then
      echo "Sourcing:$FOLDER/set-env.sh"
      source "$FOLDER/set-env.sh"
    fi
    if [ "$MAVEN_THREADS" = "true" ]; then
      MVN_THR="-T 0.3C"
    else
      MVN_THR=
    fi
    echo -e "Maven goals:${bold}-f $FOLDER $MAVEN_OPTS $MVN_THR $MAVEN_GOAL${end}"
    MVNW="./mvnw"
    if [ ! -f $MVNW ]; then
      MVNW="$SCDIR/mvnw"
    fi
    $MVNW -f "$FOLDER" $MAVEN_OPTS $MVN_THR $MAVEN_GOAL
    RESULT=$?
    set -e
    if ((RESULT != 0)); then
      echo -e "Maven goals:${bold}-f $FOLDER $MAVEN_GOAL${end}:FAILED"
      break
    fi
    echo -e "Maven goals:${bold}-f $FOLDER $MAVEN_GOAL${end}:SUCCESS"
  done
fi
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "Build of $FOLDER_NAMES $MAVEN_GOAL completed with result:$RESULT in $DURATION seconds"
exit $RESULT
