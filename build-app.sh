#!/usr/bin/env bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "$1 not defined"
    if (( sourced != 0 )); then
      return 1
    else
      exit 1
    fi
  fi
}
if [ "$1" == "" ]; then
  echo "Argument: application-folder required"
  if((sourced > 0)); then
    exit 0
  else
    exit 1
  fi
fi
APP_FOLDER=$1

check_env DOCKER_HUB_USERNAME
check_env DOCKER_HUB_PASSWORD
check_env CI_DEPLOY_USERNAME
check_env CI_DEPLOY_PASSWORD
check_env VERSION

ROOT_DIR=$(realpath $PWD)
if [ "$VERBOSE" == "true" ]; then
  MAVEN_OPT=--debug
else
  MAVEN_OPT=-q
fi

pushd $APP_FOLDER > /dev/null
  rm -rf apps
  if [ -d "src/main/java" ]; then
    echo "Deploying:$APP_FOLDER"
    $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml clean deploy -U -Pintegration
  else
    echo "Packaging:$APP_FOLDER"
    $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml clean package -U -Pintegration
  fi
  if [ ! -d apps ]; then
    echo "Cannot find $APP_FOLDER/apps"
    exit 2
  fi
  pushd apps
    echo "Building:$APP_FOLDER/apps"
    ./mvnw $MAVEN_OPT install deploy -U -Pintegration
    ./mvnw $MAVEN_OPT package jib:build -DskipTests \
                  -Djib.to.tags="$VERSION" \
                  -Djib.httpTimeout=1800000 \
                  -Djib.to.auth.username="$DOCKER_HUB_USERNAME" \
                  -Djib.to.auth.password="$DOCKER_HUB_PASSWORD"
  popd
popd
