#!/usr/bin/env bash
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
version="3.2.1-SNAPSHOT"
rootdir="$(pwd)"
processors="aggregator bridge filter groovy header-enricher http-request image-recognition object-detection script semantic-segmentation splitter transform twitter-trend"
echo "Build processors:$processor"
pushd applications/processor
  for app in $processors
  do
    pushd "${app}-processor"
      echo "$Building $(pwd)"
      rm -rf apps
      $rootdir/mvnw clean package -Pintegration
      pushd apps
        $rootdir/mvnw package jib:dockerBuild -DskipTests -Djib.to.tags=${version}
      popd
    popd
  done
popd
sinks="analytics cassandra elasticsearch file ftp geode jdbc log mongodb mqtt pgcopy rabbit redis router rsocket s3 sftp tcp throughput twitter-message twitter-update wavefront websocket zeromq"
echo "Build sinks:$sinks"
pushd applications/sink
  for app in $sinks
  do
      pushd "${app}-sink"
        echo "$Building $(pwd)"
        rm -rf apps
        $rootdir/mvnw clean package -Pintegration
        pushd apps
          $rootdir/mvnw package jib:dockerBuild -DskipTests -Djib.to.tags=${version}
        popd
      popd
  done
popd
sources="cdc-debezium file ftp geode http jdbc jms load-generator mail mongodb mqtt rabbit s3 sftp syslog tcp time twitter-message twitter-search twitter-stream websocket zeromq"
echo "Build sources:sources"
pushd applications/source
  for app in $sources
  do
      pushd "${app}-source"
        echo "$Building $(pwd)"
        rm -rf apps
        $rootdir/mvnw clean package -Pintegration
        pushd apps
          $rootdir/mvnw package jib:dockerBuild -DskipTests -Djib.to.tags=${version}
        popd
      popd
  done
popd
