#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

if [ "$1" == "" ]; then
  echo "Require folder for terraform files"
  exit 1
fi
(return 0 2>/dev/null) && sourced=1 || sourced=0
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
K8S="${K8S//\-/.}"
check_env CLUSTER_NAME "$CLUSTER_NAME"
check_env REGION "$REGION"
check_env GCP_CRED_JSON_FILE "$GCP_CRED_JSON_FILE"
TF_DIR="$SCDIR/$1/$K8S"
if [ ! -d "$TF_DIR" ]; then
  TF_DIR="$SCDIR/$1"
fi
if [ ! -d "$TF_DIR" ]; then
  echo "Directory not found $TF_DIR"
fi

pushd $TF_DIR > /dev/null
  echo "cluster_name=\"$CLUSTER_NAME\"" > terraform.tfvars
  echo "region=\"$REGION\"" >> terraform.tfvars
  echo "gcp_creds_json_file=\"$GCP_CRED_JSON_FILE\"" >> terraform.tfvars
  echo "" >> terraform.tfvars
  if [ ! -f ".terraform.lock.hcl" ] || [ ! -d ".terraform" ]; then
    terraform init
  fi
  echo "Terraform parameters:$(cat ./terraform.tfvars)"
  echo "Destroying $CLUSTER_NAME"
  terraform destroy -auto-approve -refresh=true -no-color -input=false -state="${CLUSTER_NAME}.tfstate"
popd > /dev/null
