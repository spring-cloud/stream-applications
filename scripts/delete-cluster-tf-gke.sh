#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

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
if [ "$K8S" == "" ]; then
  export K8S="1.23"
fi
check_env CLUSTER_NAME "$CLUSTER_NAME"
export RGN=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $2}')
if [ "$RGN" == "" ]; then
  if [ "$REGION" == "" ]; then
    echo "Region not provided"
    exit 0
  fi
  echo "Search for other cluster related resources in $REGION"
else
  REGION=$RGN
fi
set +e
if [ -f "$SCDIR/terraform/${CLUSTER_NAME}.tfstate" ] && [ "$REGION" != "" ]; then
  $SCDIR/terraform/tf-destroy.sh $1
fi
rm -f "$SCDIR/terraform/${CLUSTER_NAME}.tfstate*"
$SCDIR/delete-cluster-gke.sh
