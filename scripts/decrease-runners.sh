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

if [ "$1" = "" ]; then
  echo "Expected number to decrease"
  exit 1
fi
CLUSTER_NAME=stream-apps-gh-runners
echo "Checking stream-apps-gh-runners"
DEC=$1
MIN_RUNNERS=$2


SCALE_MIN=$($SCDIR/determine-default.sh $CLUSTER_NAME "scale_down")
SCALING=$($SCDIR/determine-default.sh $CLUSTER_NAME "runner_scaling")

if [ "$SCALING" == "auto" ]; then
  MAX_RUNNERS=$(max_replicas)
  MAX_RUNNERS=$((MAX_RUNNERS - DEC))
  CURRENT=$(min_replicas)
  if [ "$MIN_RUNNERS" == "" ]; then
    MIN_RUNNERS=$((CURRENT - DEC))
  fi
  if ((MAX_RUNNERS < SCALE_MIN)); then
    MAX_RUNNERS=SCALE_MIN
  fi
  if ((MIN_RUNNERS < SCALE_MIN)); then
    MIN_RUNNERS=SCALE_MIN
  fi
else
  CURRENT=$(count_runners)
  echo "There are now $CURRENT runners"
  TARGET=$((CURRENT - DEC))
fi

if [ "$MIN_RUNNERS" != "" ]; then
  if ((TARGET < MIN_RUNNERS)); then
    TARGET=$MIN_RUNNERS
  fi
fi
if ((TARGET < SCALE_MIN)); then
  TARGET=SCALE_MIN
fi
if [ "$OS" = "" ]; then
  OS=$(uname)
fi
OS="${OS//./L&}" #lowercase
if [ "$ARCH" = "" ]; then
  ARCH=$(uname -i)
fi
ARCH="${ARCH//./L&}" #lowercase
case $ARCH in
"x86_64")
  ARCH=amd64
  ;;
"arm" | "arm64" | "armv8" | "aarch64")
  ARCH=arm64
  ;;
*)
  echo "Architecture:$ARCH may not be supported by summerwind/actions-runner"
  ;;
esac
# the specific template
cp $SCDIR/k8s/runners-stream-ci-${SCALING}-template.yaml runners-stream-ci.yaml

ARC_RUNNER_VER=$($SCDIR/determine-default.sh stream-apps-gh-runners "actions_runner_version")
if [ "$ARC_RUNNER_VER" == "" ]; then
  ARC_RUNNER_VER=latest
fi
sed -i 's/tag-placeholder/'"$ARC_RUNNER_VER"'/g' runners-stream-ci.yaml
if [ "$SCALING" == "auto" ]; then
  MAX_RUNNERS=$((MAX_RUNNERS - DEC))
  echo "Runners: changing scaling min: $MIN_RUNNERS, max: $MAX_RUNNERS"
  sed -i 's/max-replicas-placeholder/'"$MAX_RUNNERS"'/g' runners-stream-ci.yaml
  sed -i 's/min-replicas-placeholder/'"$MIN_RUNNERS"'/g' runners-stream-ci.yaml
else
  echo "Runners:$CURRENT decrease by $DEC to $TARGET"
  sed -i 's/replicas-placeholder/'"$TARGET"'/g' runners-stream-ci.yaml
fi
cmp --silent "$SCDIR/k8s/runners-stream-ci-${SCALING}-template.yaml" runners-stream-ci.yaml
RC=$?
if ((RC != 0)); then
  kubectl apply -f runners-stream-ci.yaml
  echo "Runners: changed to $RUNNERS"
  if [ "$SCALING" != "auto" ]; then
    $SCDIR/wait-k8s.sh 1 --for=condition=ready --timeout=1m pod -l runner-deployment-name=runners-stream-ci --all-namespaces=true
  fi
else
  echo "Runners: unchanged"
fi
rm -f runners-stream-ci.yaml
$SCDIR/check-runners.sh
