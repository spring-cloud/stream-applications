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
set -e
check_env CLUSTER_NAME "$CLUSTER_NAME"
export REGION=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $2}')
if [ "$REGION" == "" ]; then
  echo "Cannot find $CLUSTER_NAME"
  exit 2
fi
check_env GCP_CRED_JSON_FILE "$GCP_CRED_JSON_FILE"

# Machine types
# e2-standard-2	  2	    8	128	257	No	4
# e2-standard-4	  4	   16	128	257	No	8
# e2-standard-8 	8	   32	128	257	No	16
# e2-standard-16	16	 64	128	257	No	16
# e2-standard-32	32	128	128	257	No	16

$SCDIR/terraform/tf-apply.sh $1
