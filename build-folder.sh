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

if [ "$VERBOSE" = "true" ]; then
  MAVEN_OPTS="-X"
elif [ "$VERBOSE" = "false" ]; then
  MAVEN_OPTS="-q"
fi
MAVEN_OPTS="$MAVEN_OPTS -s $SCDIR/.settings.xml -B"
if [ "$1" == "" ]; then
  echo -e "Options: ${bold} <comma-separated-folders> [<maven-goals>]*${end}"
fi
FOLDER_NAMES="$1"
shift
MAVEN_GOAL=$*
if [ "$MAVEN_GOAL" == "" ]; then
  echo -e "Using default goal ${bold}install${end}"
  MAVEN_GOAL="install"
fi

# Determine profile based on version type
if [ "$VERSION" == "" ]; then
  echo "Determining version number"
  VERSION=$($SCDIR/mvn-get-version.sh)
fi
echo -e "Determining profile to use for version $VERSION"
set +e
IS_SNAPSHOT=$(echo $VERSION | grep -E "^.*-SNAPSHOT$")
IS_MILESTONE=$(echo $VERSION | grep -E "^.*-(M|RC)[0-9]+$")
IS_GA=$(echo $VERSION | grep -E "^.*\.[0-9]+$")
if [ -n "$IS_MILESTONE" ]; then
  MAVEN_PROFILE="-Pmilestone"
elif [ -n "$IS_SNAPSHOT" ]; then
  MAVEN_PROFILE="-Psnapshot"
elif [ -n "$IS_GA" ]; then
  MAVEN_PROFILE="-Prelease"
else
  echo "Bad version format: $VERSION"
  exit 1
fi
echo "Profile for version $VERSION is '$MAVEN_PROFILE'"

# if goal includes 'deploy' or is GA release then add the profile based on version
IS_DEPLOY=$(echo $MAVEN_GOAL | grep -E "\bdeploy\b")
if [ -n "$IS_DEPLOY" ] || [ -n "$IS_GA" ]; then
  echo "Using profile: $MAVEN_PROFILE"
  MAVEN_GOAL="$MAVEN_GOAL $MAVEN_PROFILE"
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
MAVEN_THREADS_OPT="1"
if [ "$MAVEN_THREADS" != "" ]; then
  case $MAVEN_THREADS in
  "true")
    MAVEN_THREADS_OPT="0.5C"
    ;;
  "false")
    MAVEN_THREADS_OPT="1"

    ;;
  *)
    MAVEN_THREADS_OPT="$MAVEN_THREADS"
    ;;
  esac
fi
MVN_THR="-T $MAVEN_THREADS_OPT"
if [ "$SKIP_RESOLVE" = "" ]; then
  SKIP_RESOLVE=true
fi

if [ "$SKIP_RESOLVE" = "true" ]; then
  RETRIES=0
  RESULT=0
else
  RETRIES=3
fi
while ((RETRIES > 0)); do
  echo -e "Resolving dependencies for ${bold}$FOLDER_FOLDER_NAMES${end}"
  set +e
  $SCDIR/mvnw -U -pl "$FOLDER_NAMES" $MAVEN_OPTS -T 0.5C dependency:resolve
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

if ((RESULT == 0)); then
  for FOLDER in $FOLDERS; do
    # save values that may be modified by set-env.sh
    SAVED_MAVEN_OPTS="$MAVEN_OPTS"
    SAVED_MVN_THR="$MVN_THR"
    SAVED_MAVEN_GOAL="$MAVEN_GOAL"
    if [ -f "$FOLDER/set-env.sh" ]; then
      echo "Sourcing:$FOLDER/set-env.sh"
      source "$FOLDER/set-env.sh"
    fi
    set +e
    echo -e "Maven goals:${bold}-f $FOLDER $MAVEN_OPTS $MVN_THR $MAVEN_GOAL${end}"
    MVNW="./mvnw"
    if [ ! -f $MVNW ]; then
      MVNW="$SCDIR/mvnw"
    fi
    IS_DEPLOY=false
#    if [ "$BUILD_VERSION_TYPE" != "" ]; then
#      if [[ "$MAVEN_GOAL" == *"deploy"* ]]; then
#        MAVEN_GOAL="${MAVEN_GOAL//deploy/}"
#        IS_DEPLOY=true
#      fi
#      MVNW="jfrog mvn"
#    fi
    echo -e "Executing:${bold}$MVNW -f "$FOLDER" $MAVEN_OPTS $MVN_THR $MAVEN_GOAL${end}"
    $MVNW -f "$FOLDER" -Pfull $MAVEN_OPTS $MVN_THR $MAVEN_GOAL
    RESULT=$?
    set -e
    if ((RESULT != 0)); then
      echo -e "Maven goals:${bold}-f $FOLDER $MAVEN_GOAL${end}:FAILED=${RESULT}"
      break
    fi
#    if [ "$IS_DEPLOY" == "true" ]; then
#      echo -e "Executing:${bold}jfrog rt build-publish${end}"
#      jfrog rt build-publish
#    fi
    echo -e "Maven goals:${bold}-f $FOLDER $MAVEN_GOAL${end}:SUCCESS"
    # restore values that may be modified by set-env.sh
    MAVEN_OPTS="$SAVED_MAVEN_OPTS"
    MVN_THR="$SAVED_MVN_THR"
    MAVEN_GOAL="$SAVED_MAVEN_GOAL"
  done
fi
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "Build of $FOLDER_NAMES $MAVEN_GOAL completed with result:$RESULT in $DURATION seconds"
exit $RESULT
