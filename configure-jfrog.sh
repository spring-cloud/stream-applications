#!/bin/bash
MVNC=
case $1 in
"ga")
  MVNC="--server-id-resolve=repo.spring.io --server-id-deploy=repo.spring.io --repo-resolve-releases=release --repo-resolve-snapshots=libs-snapshot --repo-deploy-releases=libs-staging-local --repo-deploy-snapshots=libs-snapshot-local"
  ;;
"milestone")
  MVNC="--server-id-resolve=repo.spring.io --server-id-deploy=repo.spring.io --repo-resolve-releases=milestone --repo-resolve-snapshots=snapshot --repo-deploy-releases=libs-milestone-local --repo-deploy-snapshots=libs-snapshot-local"
  ;;
"snapshot")
  MVNC="--server-id-resolve=repo.spring.io --server-id-deploy=repo.spring.io --repo-resolve-releases=snapshot --repo-resolve-snapshots=snapshot --repo-deploy-releases=libs-milestone-local --repo-deploy-snapshots=libs-snapshot-local"
  ;;
*)
  echo "Invalid build type $1"
  exit 1
  ;;
esac
echo "MVNC=$MVNC"
jfrog mvnc $MVNC
