#!/usr/bin/env bash
if [ "$1" = "" ]; then
  echo "VERSION required as first argument"
  exit 1
fi
VERSION=$1
./mvnw versions:set \
  -DnewVersion="$VERSION" \
  -s .settings.xml \
  -DprocessAllModules=true \
  -DgenerateBackupPoms=false \
  -Dartifactory.publish.artifacts=false \
  -B $VERBOSE
./mvnw versions:set-property -f stream-applications-release-train \
  -s .settings.xml \
  -DgenerateBackupPoms=false \
  -Dproperty=apps.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE
./mvnw versions:set-property -f applications/stream-applications-core \
  -s .settings.xml \
  -DgenerateBackupPoms=false \
  -Dproperty=stream-apps-core.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE
./mvnw versions:set-property -f applications/stream-applications-core \
  -s .settings.xml \
  -DgenerateBackupPoms=false \
  -Dproperty=java-functions.version \
  -DnewVersion="$VERSION" \
  -B $VERBOSE