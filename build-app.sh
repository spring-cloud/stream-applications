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
  echo "Application folder required"
  if((sourced > 0)); then
    exit 0
  else
    exit 1
  fi
fi
APP_FOLDER=$1

check_env DOCKER_HUB_USERNAME
check_env DOCKER_HUB_PASSWORD
check_env VERSION

ROOT_DIR=$(realpath $PWD)
if [ "$VERBOSE" == "true" ]; then
  MAVEN_OPT=--debug
fi

pushd $APP_FOLDER > /dev/null
  rm -rf apps
  $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml clean
  if [ -d "src/main/java" ]; then
    $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml install deploy -U -Pintegration
  else
    $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml install -U -Pintegration
  fi
  pushd apps
    $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml package jib:build -DskipTests \
                  -Djib.to.tags="$VERSION" \
                  -Djib.httpTimeout=1800000 \
                  -Djib.to.auth.username="$DOCKER_HUB_USERNAME" \
                  -Djib.to.auth.password="$DOCKER_HUB_PASSWORD"
  popd
popd
