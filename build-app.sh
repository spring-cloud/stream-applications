#!/bin/bash
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
if [ "$2" == "" ]; then
  echo "Argument: <project-folder> <application-folder> required"
  if((sourced > 0)); then
    exit 0
  else
    exit 1
  fi
fi
PROJECT_FOLDER=$(realpath "$1")
APP_FOLDER=$2

set -e
check_env ARTIFACTORY_USERNAME
check_env ARTIFACTORY_PASSWORD

pushd "$PROJECT_FOLDER" > /dev/null
  if [ "$VERSION" == "" ]; then
    VERSION=$($SCDIR/mvn-get-version.sh)
  fi
  echo "Project Version:$VERSION"
  if [ "$LOCAL" == "true" ]; then
    MAVEN_GOAL="install verify"
  else
    MAVEN_GOAL="install verify deploy"
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
  rm -rf apps
  echo "Deploying:$APP_FOLDER"
  $SCDIR/build-folder.sh "$APP_FOLDER" $MAVEN_GOAL -Pintegration
#   else
#    echo "Packaging:$APP_FOLDER"
#    $SCDIR/build-folder.sh "$APP_FOLDER" "verify install" -Pintegration
#  fi
  pushd "$APP_FOLDER" > /dev/null
    if [ ! -d apps ]; then
      echo "Cannot find $APP_FOLDER/apps"
      exit 2
    fi
    pushd apps > /dev/null
      APPS=$(find * -maxdepth 0 -type d)
      for app in $APPS; do
        echo "Removing jib-maven-plugin for:$APP_FOLDER/apps/$app"
        $SCDIR/scripts/remove-jib-plugin.sh $app/pom.xml
      done
      for app in $APPS; do
        pushd "$app" > /dev/null
          echo "Building:$APP_FOLDER/apps/$app"
          ./mvnw $MAVEN_OPT -s $SCDIR/.settings.xml $MAVEN_GOAL -Pintegration
          for v in $JDKS; do
            echo "Pack:$app:$VERSION-jdk$v"
            pack build \
              --path "target/$app-$VERSION.jar" \
              --builder gcr.io/paketo-buildpacks/builder:base \
              --env BP_JVM_VERSION=$v \
              --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dfile.encoding=UTF-8 \
              --env BPE_APPEND_JDK_JAVA_OPTIONS=-Dsun.jnu.encoding \
              --env BPE_LC_ALL=en_US.utf8 \
              --env BPE_LANG=en_US.utf8 \
              "springcloudstream/$app:$VERSION-jdk$v"
            echo "Created:springcloudstream/$app:$VERSION-jdk$v"
          done
          if [ "$DEFAULT_JDK" != "" ]; then
            docker tag "springcloudstream/$app:$VERSION-jdk$DEFAULT_JDK" "springcloudstream/$app:$VERSION"
            echo "Tagged:springcloudstream/$app:$VERSION-jdk$DEFAULT_JDK as springcloudstream/$app:$VERSION"
            if [ "$BRANCH" != "" ]; then
              docker tag "springcloudstream/$app:$VERSION-jdk$DEFAULT_JDK" "springcloudstream/$app:$BRANCH"
              echo "Tagged:springcloudstream/$app:$VERSION-jdk$DEFAULT_JDK as springcloudstream/$app:$BRANCH"
            fi
          fi
        popd > /dev/null
      done
    popd > /dev/null
  popd > /dev/null
popd > /dev/null
