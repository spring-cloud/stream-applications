#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
ROOT_DIR=$(realpath $SCDIR/..)

if [ "$1" != "" ]; then
  VER=$1
else
  VER=3.2.2-SNAPSHOT
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

FILTER=$3

function download_deps() {
  DEP=$1
  TARGET=$2
  echo "Downloading $DEP"
  set +e
  SNAPSHOT=$(echo "$DEP" | grep -c "\-SNAPSHOT")
  MILESTONE=$(echo "$DEP" | grep -c "\-M")
  if ((SNAPSHOT > 0)); then
    INC_VER=true
    URL="https://repo.spring.io/snapshot"
  elif ((MILESTONE > 0)); then
    INC_VER=false
    URL="https://repo.spring.io/milestone"
  else
    INC_VER=false
    URL="https://repo.maven.apache.org/maven2"
  fi
  GROUP_ID=$(echo "$DEP" | awk -F":" '{split($0,a); print a[1]}')
  ARTIFACT_ID=$(echo "$DEP" | awk -F":" '{split($0,a); print a[2]}')
  VERSION=$(echo "$DEP" | awk -F":" '{split($0,a); print a[3]}')
  echo "Dependency: groupId: $GROUP_ID, artifactId: $ARTIFACT_ID, version: $VERSION"
  TS=
  if [ "$INC_VER" == "true" ]; then
    DEP_PATH="${DEP//\:/\/}"
    META_DATA="$URL/${GROUP_ID//\./\/}/$ARTIFACT_ID/$VERSION/maven-metadata.xml"
    echo "Reading $META_DATA"
    rm -f ./maven-metadata.xml
    wget --show-progress -O maven-metadata.xml "$META_DATA"
    RC=$?
    if ((RC > 0)); then
      echo "Error downloading $META_DATA. Exit code $RC"
      exit $RC
    fi
    TS=$(xmllint --xpath "/metadata/versioning/snapshot/timestamp/text()" maven-metadata.xml)
    RC=$?
    if ((RC > 0)); then
      echo "Error extracting timestamp. Exit code $RC"
      exit $RC
    fi
    DS="${TS:0:4}-${TS:4:2}-${TS:6:2} ${TS:9:2}:${TS:11:2}:${TS:13:2}"
    VAL=$(xmllint --xpath "/metadata/versioning/snapshotVersions/snapshotVersion[1]/value/text()" maven-metadata.xml)
    RC=$?
    if ((RC > 0)); then
      echo "Error extracting build number. Exit code $RC"
      exit $RC
    fi
    EXT=$(xmllint --xpath "/metadata/versioning/snapshotVersions/snapshotVersion[1]/extension/text()" maven-metadata.xml)
    RC=$?
    if ((RC > 0)); then
      echo "Error extracting extension. Exit code $RC"
      exit $RC
    fi
    SOURCE="$URL/${GROUP_ID//\./\/}/$ARTIFACT_ID/$VERSION/${ARTIFACT_ID}-${VAL}.${EXT}"
  else
    EXT="jar"
    SOURCE="$URL/${GROUP_ID//\./\/}/$ARTIFACT_ID/$VERSION/${ARTIFACT_ID}-${VERSION}.${EXT}"
  fi
  mkdir -p $TARGET
  TARGET_FILE="${TARGET}/${ARTIFACT_ID}-${VERSION}.${EXT}"
  if [ -f "$TARGET_FILE" ]; then
    if [ "$TS" != "" ] && [ "$DS" != "" ]; then
      FD=$(date -r "$TARGET_FILE" +"%Y-%m-%d %H:%M:%S")
      if [ "$FD" == "$DS" ]; then
        echo "$(realpath --relative-to $PWD $TARGET_FILE) has same timestamp ($FD) as $SOURCE."
        echo "Skipping download"
        return 0
      fi
    else
      echo "$(realpath --relative-to $PWD $TARGET_FILE) exists. Skipping download"
      return 0
    fi
  fi
  echo "Downloading to $(realpath --relative-to $PWD $TARGET_FILE) from $SOURCE"
  TARGET_DIR=$(dirname "$TARGET_FILE")
  mkdir -p "$TARGET_DIR"
  wget --show-progress -O "$TARGET_FILE" "$SOURCE"
  RC=$?
  if ((RC > 0)); then
    echo "Error downloading $SOURCE. Exit code $RC"
    rm -f "$TARGET_FILE"
    exit $RC
  fi
  if [ "$TS" != "" ] && [ "$DS" != "" ]; then
    touch -d "$DS" "$TARGET_FILE"
  fi
  set -e
}

set -e
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
    APP_PATH="applications/processor/$app/apps/$app-$BROKER/target"
    download_deps "org.springframework.cloud.stream.app:$app-$BROKER:$VER" "$ROOT_DIR/$APP_PATH"
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
    APP_PATH="applications/sink/$app/apps/$app-$BROKER/target"
    download_deps "org.springframework.cloud.stream.app:$app-$BROKER:$VER" "$ROOT_DIR/$APP_PATH"
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
    APP_PATH="applications/source/$app/apps/$app-$BROKER/target"
    download_deps "org.springframework.cloud.stream.app:$app-$BROKER:$VER" "$ROOT_DIR/$APP_PATH"
  fi
done
