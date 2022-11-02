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
# provide for fractions
set +e
bc -v > /dev/null
RC=$?
if ((RC > 0)); then
  bc -v
  echo "Error finding bc"
  exit 1
fi
cp $SCDIR/ceil.bc calc-ram.bc
echo "ceil($RAM_PER_POD * $PODS * $PODS_PER_JOB)" >> calc-ram.bc
echo "quit" >> calc-ram.bc
TOTAL_RAM=$(bc calc-ram.bc)
cp $SCDIR/ceil.bc calc-cpu.bc
echo "ceil($CPU_PER_POD * $PODS * $PODS_PER_JOB)" >> calc-cpu.bc
echo "quit" >> calc-cpu.bc
TOTAL_CPU=$(bc calc-cpu.bc)
echo "Scaling $CLUSTER_NAME with $PODS pods at ${RAM_PER_POD}Gi and $CPU_PER_POD vCPUs per pod using $PODS_PER_JOB per job. Total RAM:$TOTAL_RAM and CPU:$TOTAL_CPU"
ARGS=
while [ "$3" != "" ]; do
  if [ "$ARGS" != "" ]; then
    ARGS="$ARGS $3"
  else
    ARGS="$3"
  fi
  shift
done
$SCDIR/scale-cluster-nodes.sh $CLUSTER_NAME --ram $TOTAL_RAM --cpu $TOTAL_CPU $ARGS
