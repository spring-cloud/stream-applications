#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

function max_replicas() {
  kubectl get horizontalrunnerautoscalers --output=json | jq '.items | map(select(.spec.scaleTargetRef.name == "runners-stream-ci-large")) | .[] | .spec.maxReplicas'
}

function count_runners() {
  kubectl get rdeploy | grep -F "runners-stream-ci-large" | awk '{print $2}'
}
function count_running() {
  kubectl get rdeploy | grep -F "runners-stream-ci-large" | awk '{print $4}'
}
TARGET=$1
if [ "$1" = "" ]; then
  TARGET=1
fi
CURRENT=$(count_runners)
echo "Connecting to stream-apps-gh-runners. $CURRENT runners"
MAX_RUNNERS=$2
if [ "$MAX_RUNNERS" != "" ]; then
  if ((MAX_RUNNERS < TARGET)); then
    TARGET=$MAX_RUNNERS
    echo "Runners. Target reduced to $TARGET"
  fi
fi
if ((TARGET < 0)); then
  TARGET=1
fi
OS=$(uname)
OS="${OS//./L&}"
ARCH=$(uname -i)
case $ARCH in
"x86_64")
  ARCH=amd64
  ;;
*)
  echo "Architecture:$ARCH may not be supported by summerwind/actions-runner"
  ;;
esac

SCALING=$($SCDIR/determine-default.sh stream_apps_gh_runners runner_scaling)
cp $SCDIR/k8s/runners-stream-ci-large-template.yaml runners-stream-ci.yaml
ARC_RUNNER_VER=$($SCDIR/determine-default.sh stream-apps-gh-runners "actions_runner_version")
if [ "$ARC_RUNNER_VER" == "" ]; then
  ARC_RUNNER_VER=latest
fi
sed -i 's/tag-placeholder/'"$ARC_RUNNER_VER"'/g' runners-stream-ci.yaml
if [ "$MAX_RUNNERS" == "" ] || [ "$MAX_RUNNERS" == "null" ]; then
  MAX_RUNNERS=$(max_replicas)
fi
if [ "$MAX_RUNNERS" == "" ] || [ "$MAX_RUNNERS" == "null" ]; then
  MAX_RUNNERS=2
fi
echo "Runners: Max runners to $MAX_RUNNERS"
echo "Runners: changing scaling min: $TARGET, max: $MAX_RUNNERS"
sed -i 's/max-replicas-placeholder/'"$MAX_RUNNERS"'/g' runners-stream-ci.yaml
sed -i 's/min-replicas-placeholder/'"$TARGET"'/g' runners-stream-ci.yaml
kubectl apply -f runners-stream-ci.yaml
rm -f runners-stream-ci.yaml
$SCDIR/check-runners.sh
