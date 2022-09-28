#!/usr/bin/env bash
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

# export ARCH=arm64v8 for ARM64 image
if [ "$ARCH" == "" ]; then
    if [ "$HOSTTYPE" == "x86_64" ]; then
        ARCH=amd64
    else
        ARCH=arm64v8
    fi
fi
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
    APP_PATH="$ROOT_DIR/applications/processor/$app/apps/$app-$BROKER/target"
    TARGET_FILE="$APP_PATH/$app-$BROKER-$TAG.jar"
    if [ ! -f "$TARGET_FILE"  ]; then
        echo "Cannot find $TARGET_FILE download using download-apps.sh or build using ./mvnw install"
        exit 1
    fi
    jib jar --from=$ARCH/eclipse-temurin:$v-jdk-jammy $CRED \
        "--target=docker://springcloudstream/$APP_NAME:$TAG" \
        "$TARGET_FILE"
done
for app in ${SINKS[@]}; do
    APP_NAME="$app-$BROKER"
        APP_PATH="$ROOT_DIR/applications/sink/$app/apps/$app-$BROKER/target"
        TARGET_FILE="$APP_PATH/$app-$BROKER-$TAG.jar"
        if [ ! -f "$TARGET_FILE"  ]; then
            echo "Cannot find $TARGET_FILE download using download-apps.sh or build using ./mvnw install"
            exit 1
        fi
        jib jar --from=$ARCH/eclipse-temurin:$v-jdk-jammy $CRED \
            "--target=docker://springcloudstream/$APP_NAME:$TAG" \
            "$TARGET_FILE"
done
for app in ${SOURCES[@]}; do
    APP_NAME="$app-$BROKER"
        APP_PATH="$ROOT_DIR/applications/source/$app/apps/$app-$BROKER/target"
        TARGET_FILE="$APP_PATH/$app-$BROKER-$TAG.jar"
        if [ ! -f "$TARGET_FILE"  ]; then
            echo "Cannot find $TARGET_FILE download using download-apps.sh or build using ./mvnw install"
            exit 1
        fi
        jib jar --from=$ARCH/eclipse-temurin:$v-jdk-jammy $CRED \
            "--target=docker://springcloudstream/$APP_NAME:$TAG" \
            "$TARGET_FILE"
done
