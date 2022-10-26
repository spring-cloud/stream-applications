#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
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

check_env CI_DEPLOY_USERNAME
check_env CI_DEPLOY_PASSWORD


if [ "$VERBOSE" == "true" ]; then
  MAVEN_OPT=--debug
else
  MAVEN_OPT=-q
fi
if [ "$VERSION" == "" ]; then
  VERSIONS=$($SCDIR/mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q)
  for v in $VERSIONS; do
    VERSION=$v
  done
fi
echo "Project Version:$VERSION"
if [ "$LOCAL" == "true" ]; then
  MAVEN_GOAL="install"
else
  MAVEN_GOAL="install deploy"
fi

if [[ "$VERSION" == "4."* ]]; then
  JDKS="17"
  if [ "$DEFAULT_JDK" == "" ]; then
    DEFAULT_JDK=17
  fi
else
  JDKS="8 11 17"
  if [ "$DEFAULT_JDK" == "" ]; then
      DEFAULT_JDK=11
    fi
fi

pushd $APP_FOLDER > /dev/null
  rm -rf apps
  if [ -d "src/main/java" ]; then
    echo "Deploying:$APP_FOLDER"
    set -e
    $SCDIR/mvnw $MAVEN_OPT -s $SCDIR/.settings.xml clean $MAVEN_GOAL -U -Pintegration
    set +e
  else
    echo "Packaging:$APP_FOLDER"
    set -e
    $SCDIR/mvnw $MAVEN_OPT -s $SCDIR/.settings.xml clean install -U -Pintegration
    set +e
  fi
  if [ ! -d apps ]; then
    echo "Cannot find $APP_FOLDER/apps"
    exit 2
  fi
  pushd apps > /dev/null
    echo "Building:$APP_FOLDER/apps"
    set -e
    ./mvnw $MAVEN_OPT -s $SCDIR/.settings.xml $MAVEN_GOAL -U -Pintegration
    set +e
    APPS=$(find * -maxdepth 0 -type d)
    for app in $APPS; do
      for v in $JDKS; do
        echo "Pack:springcloudstream/$app:$VERSION-jdk$v"
        set -e
        pack build \
          --path "springcloudstream/$app-$VERSION.jar" \
          --builder gcr.io/paketo-buildpacks/builder:base \
          --env BP_JVM_VERSION=$v \
          --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dfile.encoding=UTF-8 \
          --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dsun.jnu.encoding \
          --env BPE_LC_ALL=en_US.utf8 \
          --env BPE_LANG=en_US.utf8 \
          "springcloudstream/$app:$VERSION-jdk$v"
        set +e
        echo "Created:springcloudstream/$app:$VERSION-jdk$v"
      done
      if [ "$DEFAULT_JDK" == "" ]; then
        set -e
        docker tag "springcloudstream/$app:$VERSION-jdk$DEFAULT_JDK" "springcloudstream/$app:$VERSION"
        echo "Tagged:springcloudstream/$app:$VERSION-jdk$DEFAULT_JDK as springcloudstream/$app:$VERSION"
        set +e
      fi
    done
  popd > /dev/null
popd > /dev/null
