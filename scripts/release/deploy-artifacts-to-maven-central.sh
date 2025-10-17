#!/bin/bash

if [ $# -ne 4 ]; then
  echo "usage: $(basename "$0") <buildName> <buildNumber> <appNamesPattern> <targetDir>"
  exit 1
fi

buildName="$1"
buildNumber="$2"
appNamesPattern="$3"
targetDir="$4"

OIFS="$IFS"
IFS=','
read -r -a appNames <<< "$appNamesPattern"
IFS="$OIFS"

for appName in "${appNames[@]}"; do
  echo "Downloading $appName ..."
  jfrog rt download \
    --spec "./.github/stream-apps-release-files-spec.json" \
    --spec-vars "buildname=$buildName;buildnumber=$buildNumber;appDirMatch=org/springframework/cloud/stream/app/$appName;targetDir=$targetDir/"
done
