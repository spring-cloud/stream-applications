#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PROJECT_DIR=$(realpath $SCDIR/../..)

if [ "$1" == "" ]; then
  echo "Require folder for terraform files"
  exit 1
fi
(return 0 2>/dev/null) && sourced=1 || sourced=0
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

check_env CLUSTER_NAME "$CLUSTER_NAME"
check_env REGION "$REGION"
check_env GCP_CRED_JSON_FILE "$GCP_CRED_JSON_FILE"

K8S="${K8S//\-/.}"
VERSIONS_JSON=$(cat $PROJECT_DIR/config/k8s-versions.json)
if [ "$K8S" == "" ]; then
  K8S=$(echo "$VERSIONS_JSON" | jq '.default_version' | sed 's/\"//g')
  echo "Choosing default K8S=$K8S"
fi
K8S_VERSION=$(echo "$VERSIONS_JSON" | jq ".versions | map(select(.k8s == \"$K8S\")) | .[].k8s_version" | sed 's/\"//g')
if [ "$K8S_VERSION" == "" ]; then
  echo "Unsupported version: $K8S"
  exit 1
fi
echo "K8S=$K8S => $K8S_VERSION"

TF_DIR="$SCDIR/$1/$K8S"
if [ ! -d "$TF_DIR" ]; then
  TF_DIR="$SCDIR/$1"
fi
if [ ! -d "$TF_DIR" ]; then
  echo "Directory not found $TF_DIR"
  exit 2
fi

pushd $TF_DIR >/dev/null
echo "cluster_name=\"$CLUSTER_NAME\"" >terraform.tfvars
echo "region=\"$REGION\"" >>terraform.tfvars
echo "gcp_creds_json_file=\"$GCP_CRED_JSON_FILE\"" >>terraform.tfvars

if [ "$NODES" != "" ]; then
  echo "node_count=$NODES" >>terraform.tfvars
fi

echo "kubernetes_version=\"$K8S_VERSION\"" >>terraform.tfvars
if [ "$MACHINE_TYPE" != "" ]; then
  echo "machine_type=\"$MACHINE_TYPE\"" >>terraform.tfvars
fi
if [ "$DISK_SIZE" != "" ]; then
  echo "disk_size=$DISK_SIZE" >>terraform.tfvars
fi
if [ "$MAX_MEMORY" != "" ]; then
  echo "max_cluster_memory=$MAX_MEMORY" >>terraform.tfvars
fi
if [ "$MAX_CPU" != "" ]; then
  echo "max_cluster_cpu=$MAX_CPU" >>terraform.tfvars
fi
if [ "$PODS_PER_NODE" != "" ]; then
  echo "pods_per_node=$PODS_PER_NODE" >>terraform.tfvars
fi
echo "" >>terraform.tfvars
if [ ! -f ".terraform.lock.hcl" ] || [ ! -d ".terraform" ]; then
  terraform init
fi
echo "Terraform parameters:"
cat ./terraform.tfvars
echo "Validating Terraform for $CLUSTER_NAME"
set -e
terraform validate -no-color
echo "Applying Terraform for $CLUSTER_NAME"
terraform apply -no-color -auto-approve -refresh=true -input=false -state="${CLUSTER_NAME}.tfstate"
popd >/dev/null
