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


ROOT_DIR=$(realpath $PWD)
if [ "$VERBOSE" == "true" ]; then
  MAVEN_OPT=--debug
else
  MAVEN_OPT=-q
fi
if [ "$VERSION" == "" ]; then
  VERSIONS=$($ROOT_DIR/mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q)
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
    $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml clean $MAVEN_GOAL -U -Pintegration
  else
    echo "Packaging:$APP_FOLDER"
    $ROOT_DIR/mvnw $MAVEN_OPT -s $ROOT_DIR/.settings.xml clean install -U -Pintegration
  fi
  if [ ! -d apps ]; then
    echo "Cannot find $APP_FOLDER/apps"
    exit 2
  fi
  pushd apps > /dev/null
    echo "Building:$APP_FOLDER/apps"
    ./mvnw $MAVEN_OPT $MAVEN_GOAL -U -Pintegration
#    ./mvnw $MAVEN_OPT package jib:build -DskipTests \
#                  -Djib.to.tags="$VERSION" \
#                  -Djib.httpTimeout=1800000 \
#                  -Djib.to.auth.username="$DOCKER_HUB_USERNAME" \
#                  -Djib.to.auth.password="$DOCKER_HUB_PASSWORD"
    APPS=$(find * -maxdepth 0 -type d)
    for app in $APPS; do
      for v in $JDKS; do
        echo "Pack:springcloudstream/$app:$VERSION-jdk$v"
        pack build \
          --path "springcloudstream/$app-$VERSION.jar" \
          --builder gcr.io/paketo-buildpacks/builder:base \
          --env BP_JVM_VERSION=$v "springcloudstream/$app:$VERSION-jdk$v"
      done
      if [ "$DEFAULT_JDK" == "" ]; then
        docker tag "springcloudstream/$app:$VERSION-jdk$DEFAULT_JDK" "springcloudstream/$app:$VERSION"
      fi
    done
  popd > /dev/null
popd > /dev/null
