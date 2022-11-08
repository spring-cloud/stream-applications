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
check_env CLUSTER_NAME
check_env REGION
check_env GCP_CRED_JSON_FILE

if [ "$K8S" == "" ]; then
  export K8S="1.23"
fi
if [ "$NODES" == "" ]; then
  export NODES=1
fi

# Machine types
# e2-standard-2	  2	    8	128	257	No	4
# e2-standard-4	  4	   16	128	257	No	8
# e2-standard-8 	8	   32	128	257	No	16
# e2-standard-16	16	 64	128	257	No	16
# e2-standard-32	32	128	128	257	No	16

$SCDIR/terraform/tf-apply.sh $1

echo "Google Cloud console for cluster at https://console.cloud.google.com/kubernetes/clusters/details/$REGION/$CLUSTER_NAME/details?project=spring-cloud-dataflow-148214"

