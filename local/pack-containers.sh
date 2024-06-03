#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
ROOT_DIR=$(realpath $SCDIR/..)

# set to specific version
if [ "$1" != "" ]; then
  TAG=$1
else
  TAG=2.10.0-SNAPSHOT
fi
case $2 in
"rabbitmq" | "rabbit")
  BROKER=rabbit
  ;;
"kafka")
  BROKER=kafka
  ;;
"")
  echo "Broker default to rabbit"
  BROKER=rabbit
  ;;
*)
  echo "Invalid broker name $1"
  ;;
esac

if [ "$3" != "" ]; then
  v=$3
else
  v=11
fi

FILTER=$4
PROCESSOR=$(uname -p)
if [ "$ARCH" == "" ]; then
    case $PROCESSOR in
    "x86_64")
        ARCH=amd64
        ;;
    *)
        if [[ "$PROCESSOR" == *"arm"* ]]; then
            ARCH=arm64v8
        fi
        ;;
    esac
fi
IMAGE="$ARCH/eclipse-temurin:$v-jdk-jammy"
CRED=
if [ "$DOCKER_USERNAME" != "" ]; then
  CRED="--from-username=$DOCKER_USERNAME --from-password=$DOCKER_PASSWORD"
fi
# set with extra option for buildpacks. BP_OPTIONS=
pushd $ROOT_DIR/applications/processor >/dev/null
PROCESSORS=$(find * -maxdepth 0 -type d)
popd >/dev/null
pushd $ROOT_DIR/applications/sink >/dev/null
SINKS=$(find * -maxdepth 0 -type d)
popd >/dev/null
pushd $ROOT_DIR/applications/source >/dev/null
SOURCES=$(find * -maxdepth 0 -type d)
popd >/dev/null

for app in ${PROCESSORS[@]}; do
  APP_NAME="$app-$BROKER"
  DOWNLOAD=true
  if [ "$FILTER" != "" ]; then
    if [[ "$APP_NAME" == *"$FILTER"* ]]; then
      DOWNLOAD=true
    else
      DOWNLOAD=false
    fi
  fi
  if [ "$DOWNLOAD" == "true" ]; then
    APP_PATH="$ROOT_DIR/applications/processor/$app/apps/$app-$BROKER/target"
    TARGET_FILE="$APP_PATH/$app-$BROKER-$TAG.jar"
    if [ -f "$TARGET_FILE" ]; then
      pack build \
        --path "$TARGET_FILE" \
        --builder paketobuildpacks/builder-jammy-base:latest \
        --env BP_JVM_VERSION=$v \
        --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dfile.encoding=UTF-8 \
        --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dsun.jnu.encoding \
        --env BPE_LC_ALL=en_US.utf8 \
        --env BPE_LANG=en_US.utf8 \
        "springcloudstream/$APP_NAME:$TAG"
      echo "Created springcloudstream/$APP_NAME:$TAG"
    else
      echo "Cannot find $TARGET_FILE won't attempt to create container"
    fi
  fi
done
for app in ${SINKS[@]}; do
  APP_NAME="$app-$BROKER"
  DOWNLOAD=true
  if [ "$FILTER" != "" ]; then
    if [[ "$APP_NAME" == *"$FILTER"* ]]; then
      DOWNLOAD=true
    else
      DOWNLOAD=false
    fi
  fi
  if [ "$DOWNLOAD" == "true" ]; then
    APP_PATH="$ROOT_DIR/applications/sink/$app/apps/$app-$BROKER/target"
    TARGET_FILE="$APP_PATH/$app-$BROKER-$TAG.jar"
    if [ -f "$TARGET_FILE" ]; then
      pack build \
        --path "$TARGET_FILE" \
        --builder paketobuildpacks/builder-jammy-base:latest \
        --env BP_JVM_VERSION=$v \
        --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dfile.encoding=UTF-8 \
        --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dsun.jnu.encoding \
        --env BPE_LC_ALL=en_US.utf8 \
        --env BPE_LANG=en_US.utf8 \
        "springcloudstream/$APP_NAME:$TAG"
      echo "Created springcloudstream/$APP_NAME:$TAG"
    else
      echo "Cannot find $TARGET_FILE won't attempt to create container"
    fi
  fi
done
for app in ${SOURCES[@]}; do
  APP_NAME="$app-$BROKER"
  DOWNLOAD=true
  if [ "$FILTER" != "" ]; then
    if [[ "$APP_NAME" == *"$FILTER"* ]]; then
      DOWNLOAD=true
    else
      DOWNLOAD=false
    fi
  fi
  if [ "$DOWNLOAD" == "true" ]; then
    APP_PATH="$ROOT_DIR/applications/source/$app/apps/$app-$BROKER/target"
    TARGET_FILE="$APP_PATH/$app-$BROKER-$TAG.jar"
    if [ -f "$TARGET_FILE" ]; then
      pack build \
        --path "$TARGET_FILE" \
        --builder paketobuildpacks/builder-jammy-base:latest \
        --env BP_JVM_VERSION=$v \
        --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dfile.encoding=UTF-8 \
        --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dsun.jnu.encoding \
        --env BPE_LC_ALL=en_US.utf8 \
        --env BPE_LANG=en_US.utf8 \
        "springcloudstream/$APP_NAME:$TAG"
      echo "Created springcloudstream/$APP_NAME:$TAG"
    else
      echo "Cannot find $TARGET_FILE won't attempt to create container"
    fi
  fi
done
