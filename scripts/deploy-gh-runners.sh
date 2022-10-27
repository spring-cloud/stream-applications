#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

set +e
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "$1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}

if [ "$1" != "" ]; then
  DEPLOY_TYPE=$1
else
  DEPLOY_TYPE=helm
fi
echo "Deployment Type: $DEPLOY_TYPE"
if [ "$GH_ARC_PAT" == "" ]; then
  check_env GH_ARC_APP_ID
  check_env GH_ARC_INSTALLATION_ID
  check_env GH_ARC_PRIVATE_KEY
fi
echo "Configuring actions-runner-controller"
set +e
echo "Create Certificate Manager"
NS=cert-manager
SVC=cert-manager
$SCDIR/ensure-ns.sh $NS
kubectl apply -f https://github.com/jetstack/cert-manager/releases/latest/download/cert-manager.yaml
set -e
$SCDIR/wait-deployment.sh $SVC $NS
echo "Certificate Manager installed"
echo "Configuring actions-runner-controller"
set +e
export NS=actions-runner-system
export SVC=controller-manager
$SCDIR/ensure-ns.sh $NS
kubectl apply -f $SCDIR/k8s/pod-priorities.yaml
kubectl apply -f $SCDIR/k8s/pod-priorities.yaml --namespace $NS
COUNT=$(kubectl get secrets "$SVC" --namespace $NS)
if ((COUNT > 0)); then
  kubectl delete secret "$SVC" --namespace $NS
fi
if [ "$GH_ARC_PAT" != "" ]; then
  echo "Using GH_ARC_PAT as github_token"
  kubectl create secret generic "$SVC" \
    --namespace $NS \
    --from-literal=github_token="$GH_ARC_PAT"
else
  echo "Using GitHub App $GH_ARC_APP_ID / $GH_ARC_INSTALLATION_ID"
  kubectl create secret generic "$SVC" \
    --namespace $NS \
    --from-literal=github_app_id="${GH_ARC_APP_ID}" \
    --from-literal=github_app_installation_id="${GH_ARC_INSTALLATION_ID}" \
    --from-literal=github_app_private_key="${GH_ARC_PRIVATE_KEY}"
fi
COUNT=$(kubectl get secrets --namespace $NS | grep -c -F "scdf-metadata-default")
if ((COUNT > 0)); then
  kubectl delete secret scdf-metadata-default --namespace $NS
fi
kubectl create secret docker-registry scdf-metadata-default --namespace $NS \
  --docker-server=registry-1.docker.io \
  --docker-username=$DOCKER_HUB_USERNAME \
  --docker-password=$DOCKER_HUB_PASSWORD
COUNT=$(kubectl get secrets --namespace default | grep -c -F "scdf-metadata-default")
if ((COUNT > 0)); then
  kubectl delete secret scdf-metadata-default --namespace default
fi
kubectl create secret docker-registry scdf-metadata-default --namespace default \
  --docker-server=registry-1.docker.io \
  --docker-username=$DOCKER_HUB_USERNAME \
  --docker-password=$DOCKER_HUB_PASSWORD
if [ "$DEPLOY_TYPE" == "helm" ]; then
  HELM_VER=$($SCDIR/determine-default.sh stream-apps-gh-runners "helm_version")
  if [ "$HELM_VER" == "" ] || [ "$HELM_VER" == "null" ]; then
    echo "Cannot determine helm_version"
    exit 1
  fi
  echo "Adding Helm chart https://actions-runner-controller.github.io/actions-runner-controller"
  helm repo add actions-runner-controller https://actions-runner-controller.github.io/actions-runner-controller
  echo "Installing application: actions-runner-controller, Helm chart version:$HELM_VER into $NS"
  helm install --version "$HELM_VER" --namespace $NS -f $SCDIR/arc/values.yml --wait actions-runner-controller actions-runner-controller/actions-runner-controller
else
  ARC_VER=$($SCDIR/determine-default.sh stream-apps-gh-runners "arc_version")
  if [ "$ARC_VER" == "" ] || [ "$ARC_VER" == "null" ]; then
    echo "Cannot determine arc_version"
    exit 1
  fi
  echo "Deploying actions-runner-controller:$ARC_VER using kubectl"
  kubectl create --save-config --namespace $NS -f https://github.com/actions-runner-controller/actions-runner-controller/releases/download/$ARC_VER/actions-runner-controller.yaml
  $SCDIR/wait-deployment.sh $SVC $NS
fi
set -e
echo "Creating runners"
kubectl apply -f "$SCDIR/k8s/runners-stream-ci-large.yaml"
SCALING=$(jq '.stream-apps-gh-runners.runner_scaling' $PARENT/config/defaults.json | sed 's/\"//g')
kubectl apply -f "$SCDIR/k8s/runners-stream-ci-${SCALING}.yaml"
if [ "$SCALING" != "auto" ]; then
  $SCDIR/wait-k8s.sh 1 --for=condition=ready --timeout=1m pod -l runner-deployment-name=runners-stream-ci
fi
$SCDIR/check-runners.sh
