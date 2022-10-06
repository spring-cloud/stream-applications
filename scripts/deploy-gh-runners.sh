#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

set +e
function check_env() {
    eval ev='$'$1
    if [ "$ev" == "" ]; then
      echo "$1 not defined"
      if (( sourced != 0 )); then
        return 1
      else
        exit 1
      fi
    fi
}
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
if [ "$GH_ARC_PAT" != "" ]; then
  echo "Using GH_ARC_PAT as github_token"
  kubectl create secret generic "$SVC-secret" \
          --namespace $NS \
          --from-literal=github_token="$GH_ARC_PAT"
else
  echo "Using GitHub App $GH_ARC_APP_ID / $GH_ARC_INSTALLATION_ID"
  kubectl create secret generic "$SVC-secret" \
      --namespace $NS \
      --from-literal=github_app_id="${GH_ARC_APP_ID}" \
      --from-literal=github_app_installation_id="${GH_ARC_INSTALLATION_ID}" \
      --from-literal=github_app_private_key="${GH_ARC_PRIVATE_KEY}"
fi

kubectl create secret docker-registry scdf-metadata-default --namespace $NS --docker-server=registry-1.docker.io --docker-username=$DOCKER_HUB_USERNAME --docker-password=$DOCKER_HUB_PASSWORD
kubectl create secret docker-registry scdf-metadata-default --namespace default --docker-server=registry-1.docker.io --docker-username=$DOCKER_HUB_USERNAME --docker-password=$DOCKER_HUB_PASSWORD

helm repo add actions-runner-controller https://actions-runner-controller.github.io/actions-runner-controller
helm install --namespace $NS -f $SCDIR/arc/values.yml  --wait actions-runner-controller actions-runner-controller/actions-runner-controller
# kubectl create -f https://github.com/actions-runner-controller/actions-runner-controller/releases/download/v0.25.2/actions-runner-controller.yaml

set -e
#$SCDIR/wait-deployment.sh $SVC $NS
echo "Creating runners"
SCALING=$(jq '.scdf_pro_gh_runners.runner_scaling' $PARENT/config/defaults.json | sed 's/\"//g')
kubectl apply -f "$SCDIR/k8s/runners-ci-${SCALING}.yaml"
kubectl apply -f "$SCDIR/k8s/runners-generic.yaml"
if [ "$SCALING" != "auto" ]; then
  $SCDIR/wait-k8s.sh 1 --for=condition=ready --timeout=1m pod -l runner-deployment-name=runners-ci
fi
$SCDIR/check-runners.sh
