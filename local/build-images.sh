#!/bin/bash
java --version
if [ $? -eq 0 ]
then
  java_version=$(java --version)
  echo "$java_version"
  grep -e "1.8" "$java_version"
  if [ $? -ne 0 ]
  then
    echo "Java 1.8 required not $java_version"
    exit 2
  fi
fi
rootdir="$(realpath $PWD)"
if [ "$VERSION" == "" ]; then
  export VERSION=$($ROOT_DIR/mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q)
fi
pushd applications/processor > /dev/null
processors=$(find * -maxdepth 0 -type d)
popd  > /dev/null
for app in $processors; do
  $rootdir/build-app.sh "applications/processor/$app"
done
pushd applications/sink > /dev/null
sinks=$(find * -maxdepth 0 -type d)
popd  > /dev/null
for app in $sinks; do
  $rootdir/build-app.sh "applications/sink/$app"
done
pushd applications/source > /dev/null
sources=$(find * -maxdepth 0 -type d)
popd  > /dev/null
for app in $sinks; do
  $rootdir/build-app.sh "applications/source/$app"
done
