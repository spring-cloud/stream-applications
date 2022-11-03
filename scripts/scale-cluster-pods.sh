#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

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

if [ "$1" == "" ]; then
  echo "Cluster name required"
  exit 1
fi
CLUSTER_NAME=$1
if [ "$2" == "" ]; then
  echo "POD count required"
  exit 1
fi
PODS=$2

RAM_PER_POD=$($SCDIR/determine-default.sh $CLUSTER_NAME "ram-per-pod")
CPU_PER_POD=$($SCDIR/determine-default.sh $CLUSTER_NAME "cpu-per-pod")
PODS_PER_JOB=$($SCDIR/determine-default.sh $CLUSTER_NAME "pods_per_job")
if [ "$RAM_PER_POD" = "" ] || [ "$RAM_PER_POD" = "null" ]; then
  RAM_PER_POD=1
fi
if [ "$CPU_PER_POD" = "" ] || [ "$CPU_PER_POD" = "null" ]; then
  CPU_PER_POD=1
fi
if [ "$PODS_PER_JOB" = "" ] || [ "$PODS_PER_JOB" = "null" ]; then
  PODS_PER_JOB=1
fi
# provide for fractions
echo "Scaling $CLUSTER_NAME with $PODS pods at ${RAM_PER_POD}Gi and $CPU_PER_POD vCPUs per pod using $PODS_PER_JOB per job"
ARGS=
while [ "$3" != "" ]; do
  if [ "$ARGS" != "" ]; then
    ARGS="$ARGS $3"
  else
    ARGS="$3"
  fi
  shift
done
$SCDIR/scale-cluster-nodes.sh $CLUSTER_NAME --ram-per-pod $RAM_PER_POD --cpu-per-pod $CPU_PER_POD --pods-per-job $PODS_PER_JOB --pods $PODS $ARGS
