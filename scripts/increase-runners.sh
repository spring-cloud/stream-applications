#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

function max_replicas() {
  kubectl get horizontalrunnerautoscalers --output=json | jq '.items | map(select(.spec.scaleTargetRef.name == "runners-stream-ci")) | .[] | .spec.maxReplicas'
}
function min_replicas() {
  kubectl get horizontalrunnerautoscalers --output=json | jq '.items | map(select(.spec.scaleTargetRef.name == "runners-stream-ci")) | .[] | .spec.minReplicas'
}
function count_runners() {
  kubectl get rdeploy runners-stream-ci --output=json | jq '.spec.replicas'
}
function count_running() {
  kubectl get rdeploy | grep -F "runners-stream-ci" | awk '{print $4}'
}
function count_idle() {
    kubectl get runners -l runner-deployment-name="runners-stream-ci" --output=json | jq -c '.items | map(.status) | .[] | .phase' | grep -c -F "Idle"
}

if [ "$1" = "" ]; then
  echo "Expected number to increase"
  exit 1
fi
echo "Checking stream-apps-gh-runners"
INC=$1

# shellcheck disable=SC2003
MAX_RUNNERS=$2
SCALING=$($SCDIR/determine-default.sh stream-apps-gh-runners runner_scaling)
cp $SCDIR/k8s/runners-stream-ci-${SCALING}-template.yaml runners-stream-ci-change.yaml

if [ "$SCALING" == "auto" ]; then
  if [ "$MAX_RUNNERS" == "" ]; then
    MAX_REPLICAS=$(max_replicas)
    MAX_RUNNERS=$((MAX_REPLICAS + INC))
  else
    if ((MAX_RUNNERS < MAX_REPLICAS)); then
      MAX_RUNNERS=$MAX_REPLICAS
    fi
  fi
  MIN_RUNNERS=$(min_replicas)
  MIN_RUNNERS=$((MIN_RUNNERS + INC))
  if ((MIN_RUNNERS > MAX_RUNNERS)); then
    MIN_RUNNERS=$MAX_RUNNERS
  fi
else
  RUNNING=$(count_runners)
  IDLE=$(count_idle)
  CURRENT=$((RUNNING - IDLE))
  echo "There are now $RUNNING runners and $IDLE idle"
  if ((INC > IDLE)); then
    TARGET=$((CURRENT+INC-IDLE))
    if [ "$MAX_RUNNERS" != "" ]; then
      MAX=$MAX_RUNNERS
      if ((TARGET > MAX)); then
        if ((CURRENT > MAX)); then
          echo "There are $CURRENT which is more than $MAX"
          exit 0
        fi
        echo "There cannot be more than $MAX runners"
        TARGET=$MAX
      fi
    fi
  else
    echo "Sufficient idle"
    $SCDIR/check-runners.sh
    exit 0
  fi
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
sed -i 's/tag-placeholder/'"$IMAGE_SHA"'/g' runners-stream-ci-change.yaml
if [ "$SCALING" == "auto" ]; then
  echo "Runners: changing scaling min: $MIN_RUNNERS, max: $MAX_RUNNERS"
  sed -i 's/max-replicas-placeholder/'"$MAX_RUNNERS"'/g' runners-stream-ci-change.yaml
  sed -i 's/min-replicas-placeholder/'"$MIN_RUNNERS"'/g' runners-stream-ci-change.yaml
else
  echo "Runners:$CURRENT increase by $INC to $TARGET"
  sed -i 's/replicas-placeholder/'"$TARGET"'/g' runners-stream-ci-change.yaml
fi

cmp --silent "$SCDIR/k8s/runners-stream-ci-${SCALING}-template.yaml" runners-stream-ci-change.yaml
RC=$?
if ((RC != 0)); then
  kubectl apply -f runners-stream-ci-change.yaml
  echo "Runners: changes applied"
  if [ "$SCALING" != "auto" ]; then
    $SCDIR/wait-k8s.sh 1 --for=condition=ready --timeout=1m pod -l runner-deployment-name=runners-stream-ci --all-namespaces=true
  fi
else
  echo "Runners: no changes required"
fi
rm -f runners-stream-ci-change.yaml
$SCDIR/check-runners.sh
