#!/bin/bash
MVNC=
readonly JF_SERVER_ID=${JF_SERVER_ID:-spring-cloud}
case $1 in
"ga" | "release")
  MVNC="--server-id-resolve=${JF_SERVER_ID} --server-id-deploy=${JF_SERVER_ID} --repo-resolve-releases=libs-release --repo-resolve-snapshots=libs-snapshot --repo-deploy-releases=libs-staging-local --repo-deploy-snapshots=libs-snapshot-local"
  ;;
"milestone")
  MVNC="--server-id-resolve=${JF_SERVER_ID} --server-id-deploy=${JF_SERVER_ID} --repo-resolve-releases=libs-milestone --repo-resolve-snapshots=libs-snapshot --repo-deploy-releases=libs-milestone-local --repo-deploy-snapshots=libs-snapshot-local"
  ;;
"snapshot")
  MVNC="--server-id-resolve=${JF_SERVER_ID} --server-id-deploy=${JF_SERVER_ID} --repo-resolve-releases=libs-snapshot --repo-resolve-snapshots=libs-snapshot --repo-deploy-releases=libs-milestone-local --repo-deploy-snapshots=libs-snapshot-local"
  ;;
*)
  echo "Invalid build type $1"
  exit 1
  ;;
esac
echo "MVNC=$MVNC"
jfrog mvnc $MVNC
RC=$?
if ((RC !=0)); then
  echo "jfrog mvnc $MVNC"
  echo "error: $RC"
  exit $RC
fi
jfrog rt ping
RC=$?
if ((RC !=0)); then
  echo "jfrog rt ping error: $RC"
  exit $RC
fi