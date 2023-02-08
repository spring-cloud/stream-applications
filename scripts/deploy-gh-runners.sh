#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

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

set -e
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
#kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.8.2/cert-manager.yaml
set -e
$SCDIR/wait-deployment.sh $SVC $NS
echo "Certificate Manager installed"

echo "Configuring actions-runner-controller"
export NS=actions-runner-system
export SVC=actions-runner-controller
$SCDIR/ensure-ns.sh $NS
kubectl apply -f $SCDIR/k8s/pod-priorities.yaml
kubectl apply -f $SCDIR/k8s/pod-priorities.yaml --namespace $NS
echo "Create secret controller-manager in $NS"
$SCDIR/delete-secret.sh controller-manager
$SCDIR/delete-secret.sh controller-manager $NS
if [ "$GH_ARC_PAT" != "" ]; then
  echo "Using GH_ARC_PAT as github_token"
  kubectl create secret generic controller-manager \
    --namespace $NS \
    --from-literal=github_token="$GH_ARC_PAT"
    
    kubectl create secret generic controller-manager \
    --from-literal=github_token="$GH_ARC_PAT"
else
  echo "Using GitHub App $GH_ARC_APP_ID / $GH_ARC_INSTALLATION_ID"
  kubectl create secret generic controller-manager \
    --namespace $NS \
    --from-literal=github_app_id="${GH_ARC_APP_ID}" \
    --from-literal=github_app_installation_id="${GH_ARC_INSTALLATION_ID}" \
    --from-literal=github_app_private_key="${GH_ARC_PRIVATE_KEY}"
    
    kubectl create secret generic controller-manager \
    --from-literal=github_app_id="${GH_ARC_APP_ID}" \
    --from-literal=github_app_installation_id="${GH_ARC_INSTALLATION_ID}" \
    --from-literal=github_app_private_key="${GH_ARC_PRIVATE_KEY}"
fi
echo "Create secret scdf-metadata-default in $NS"
$SCDIR/delete-secret.sh scdf-metadata-default $NS
kubectl create secret docker-registry scdf-metadata-default --namespace $NS \
  --docker-server=registry-1.docker.io \
  --docker-username=$DOCKERHUB_USERNAME \
  --docker-password=$DOCKERHUB_TOKEN
echo "Create secret scdf-metadata-default in default"
$SCDIR/delete-secret.sh scdf-metadata-default default
kubectl create secret docker-registry scdf-metadata-default --namespace default \
  --docker-server=registry-1.docker.io \
  --docker-username=$DOCKERHUB_USERNAME \
  --docker-password=$DOCKERHUB_TOKEN

if [ "$DEPLOY_TYPE" == "helm" ]; then
  HELM_VER=$($SCDIR/determine-default.sh stream-apps-gh-runners "helm_version")
  if [ "$HELM_VER" == "" ] || [ "$HELM_VER" == "null" ]; then
    echo "Cannot determine helm_version"
    exit 1
  fi
  echo "Adding Helm chart https://actions-runner-controller.github.io/actions-runner-controller"
  helm repo add actions-runner-controller https://actions-runner-controller.github.io/actions-runner-controller
  echo "Installing application: actions-runner-controller, Helm chart version:$HELM_VER into $NS"
  helm upgrade --install --version "$HELM_VER" --namespace $NS -f $SCDIR/arc/values.yml --wait --timeout 15m actions-runner-controller actions-runner-controller/actions-runner-controller
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
$SCDIR/large-runners.sh 1
SCALE_MIN=$($SCDIR/determine-default.sh $CLUSTER_NAME "scale_down")
$SCDIR/limit-runners.sh $SCALE_MIN
