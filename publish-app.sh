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

VERSION=$($ROOT_DIR/mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q)
echo "Project Version:$VERSION"

if [[ "$VERSION" == "4."* ]]; then
  JDKS=17
  if [ "$DEFAULT_JDK" == "" ];then
      DEFAULT_JDK=17
    fi
else
  if [ "$DEFAULT_JDK" == "" ];then
    DEFAULT_JDK=11
  fi
  JDKS=8 11 17
fi
pushd $APP_FOLDER > /dev/null
  pushd apps > /dev/null
    echo "Pushing:$APP_FOLDER/apps"
    BROKERS=$(find * -maxdepth 0 -type d)
    for broker in $BROKERS; do
      project="$APP_FOLDER-$broker"
      docker tag "springcloudstream/$project:$VERSION-jdk$DEFAULT_JDK" "springcloudstream/$project:$VERSION"
      docker push "springcloudstream/$project:$VERSION-jdk$DEFAULT_JDK"
      for v in $JDKS; do
        docker push "springcloudstream/$project:$VERSION-jdk$v"
      done
    done
  popd > /dev/null
popd > /dev/null
