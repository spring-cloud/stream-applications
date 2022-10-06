#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

function max_replicas() {
  kubectl get horizontalrunnerautoscalers --output=json | jq '.items | map(select(.spec.scaleTargetRef.name == "runners-ci")) | .[] | .spec.maxReplicas'
}
function min_replicas() {
  kubectl get horizontalrunnerautoscalers --output=json | jq '.items | map(select(.spec.scaleTargetRef.name == "runners-ci")) | .[] | .spec.minReplicas'
}
function count_runners() {
  kubectl get rdeploy runners-ci --output=json | jq '.spec.replicas'
}
function count_running() {
  kubectl get rdeploy | grep -F "runners-ci" | awk '{print $4}'
}

if [ "$1" = "" ]; then
  echo "Expected number to decrease"
  exit 1
fi
echo "Checking stream-apps-gh-runners"
DEC=$1
MIN_RUNNERS=$2

SCALING=$(jq '.scdf_pro_gh_runners.runner_scaling' $PARENT/config/defaults.json | sed 's/\"//g')
cp $SCDIR/k8s/runners-ci-${SCALING}-template.yaml runners-ci-change.yaml
if [ "$SCALING" == "auto" ]; then
  MAX_RUNNERS=$(max_replicas)
  MAX_RUNNERS=$((MAX_RUNNERS - DEC))
  CURRENT=$(min_replicas)
  if [ "$MIN_RUNNERS" == "" ]; then
    MIN_RUNNERS=$((CURRENT - DEC))
  fi
  if ((MAX_RUNNERS < 1)); then
    MAX_RUNNERS=1
  fi
  if ((MIN_RUNNERS < 1)); then
    MIN_RUNNERS=1
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
if ((TARGET < 1)); then
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
sed -i 's/tag-placeholder/'"$IMAGE_SHA"'/g' runners-ci-change.yaml
if [ "$SCALING" == "auto" ]; then
  MAX_RUNNERS=$((MAX_RUNNERS - DEC))
  echo "Runners: changing scaling min: $MIN_RUNNERS, max: $MAX_RUNNERS"
  sed -i 's/max-replicas-placeholder/'"$MAX_RUNNERS"'/g' runners-ci-change.yaml
  sed -i 's/min-replicas-placeholder/'"$MIN_RUNNERS"'/g' runners-ci-change.yaml
else
  echo "Runners:$CURRENT decrease by $DEC to $TARGET"
  sed -i 's/replicas-placeholder/'"$TARGET"'/g' runners-ci-change.yaml
fi
cmp --silent "$SCDIR/k8s/runners-ci-${SCALING}-template.yaml" runners-ci-change.yaml
RC=$?
if ((RC != 0)); then
  kubectl apply -f runners-ci-change.yaml
  echo "Runners: changed to $RUNNERS"
  if [ "$SCALING" != "auto" ]; then
    $SCDIR/wait-k8s.sh 1 --for=condition=ready --timeout=1m pod -l runner-deployment-name=runners-ci --all-namespaces=true
  fi
else
  echo "Runners: unchanged"
fi
rm -f runners-ci-change.yaml
$SCDIR/check-runners.sh
