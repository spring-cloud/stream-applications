#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

function max_replicas() {
  kubectl get horizontalrunnerautoscalers --output=json | jq '.items | map(select(.spec.scaleTargetRef.name == "runners-stream-ci")) | .[] | .spec.maxReplicas'
}

function count_runners() {
  kubectl get rdeploy | grep -F "runners-stream-ci" | awk '{print $2}'
}
function count_running() {
  kubectl get rdeploy | grep -F "runners-stream-ci" | awk '{print $4}'
}
if [ "$1" = "" ]; then
  echo "Expected number to runners"
  exit 1
fi
CURRENT=$(count_runners)
echo "Connecting to stream-apps-gh-runners. $CURRENT runners"
TARGET=$1
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
IMAGE_SHA=$(wget -q -O- https://hub.docker.com/v2/repositories/summerwind/actions-runner/tags | jq --arg os $OS --arg arch $ARCH '.results | map(select(.name="latest")) | .[0].images | map(select(.os==$os and .architecture==$arch)) | .[0].digest' | sed 's/\"//g')
if [ "$IMAGE_SHA" == "" ]; then
  IMAGE_SHA="latest"
fi
SCALING=$(jq '.stream-apps-gh-runners.runner_scaling' $PARENT/config/defaults.json | sed 's/\"//g')
cp $SCDIR/k8s/runners-stream-ci-${SCALING}-template.yaml runners-stream-ci-change.yaml
sed -i 's/tag-placeholder/'"$IMAGE_SHA"'/g' runners-stream-ci-change.yaml
if [ "$SCALING" == "auto" ]; then
  if [ "$MAX_RUNNERS" == "" ]; then
    MAX_RUNNERS=$(max_replicas)
  fi
  echo "Runners: Max runners to $MAX_RUNNERS"
  echo "Runners: changing scaling min: $TARGET, max: $MAX_RUNNERS"
  sed -i 's/max-replicas-placeholder/'"$MAX_RUNNERS"'/g' runners-stream-ci-change.yaml
  sed -i 's/min-replicas-placeholder/'"$TARGET"'/g' runners-stream-ci-change.yaml
else
  echo "Runners:$CURRENT change to $TARGET"
  sed -i 's/replicas-placeholder/'"$TARGET"'/g' runners-stream-ci-change.yaml
fi
cmp --silent "$SCDIR/k8s/runners-stream-ci-${SCALING}-template.yaml" runners-stream-ci-change.yaml
RC=$?
if ((RC != 0)); then
  kubectl apply -f runners-stream-ci-change.yaml
  echo "Runners: applying changes"
  if [ "$SCALING" != "auto" ]; then
    $SCDIR/wait-k8s.sh 1 --for=condition=ready --timeout=1m pod -l runner-deployment-name=runners-stream-ci --all-namespaces=true
  fi
else
  echo "Runners:no changes required"
fi
rm -f runners-stream-ci-change.yaml
$SCDIR/check-runners.sh
