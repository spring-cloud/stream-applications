#!/bin/bash
case $1 in
"ga")
  jfrog mvnc \
    --server-id-resolve=repo.spring.io \
    --server-id-deploy=repo.spring.io \
    --repo-resolve-releases=libs-release-local \
    --repo-resolve-snapshots=libs-snapshot \
    --repo-deploy-releases=libs-release-local \
    --repo-deploy-snapshots=libs-snapshot-local
  ;;
"milestone")
  jfrog mvnc \
    --server-id-resolve=repo.spring.io \
    --server-id-deploy=repo.spring.io \
    --repo-resolve-releases=libs-milestone-local \
    --repo-resolve-snapshots=libs-snapshot \
    --repo-deploy-releases=libs-milestone-local \
    --repo-deploy-snapshots=libs-snapshot-local
  ;;
"snapshot")
  jfrog mvnc \
    --server-id-resolve=repo.spring.io \
    --server-id-deploy=repo.spring.io \
    --repo-resolve-releases=libs-milestone \
    --repo-resolve-snapshots=libs-snapshot \
    --repo-deploy-releases=libs-milestone-local \
    --repo-deploy-snapshots=libs-snapshot-local
  ;;
*)
  echo "Invalid build type $1"
  exit 1
  ;;
esac

