#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)
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
function div_round_up() {
  DIVIDEND=$1
  DIVISOR=$2
  QUOTIENT=$((DIVIDEND / DIVISOR))
  if ((QUOTIENT * DIVISOR < DIVIDEND)); then
    echo "$((QUOTIENT + 1))"
  else
    echo "$QUOTIENT"
  fi
}

function set_nodecount_machine_type() {
  if [ "$PROVIDER" == "gke" ]; then
    REGION=$(gcloud container clusters list 2>/dev/null | grep -F "$CLUSTER_NAME" | awk '{print $2}')
    if [ "$REGION" == "" ]; then
      echo "Cannot find $CLUSTER_NAME"
      exit 2
    fi
    export REGION
    NODE_JSON=$(gcloud container node-pools list --region "$REGION" --cluster "$CLUSTER_NAME" --format=json)
    NODE_POOL=$(echo "$NODE_JSON" | jq '.[0].name' | sed 's/\"//g')
    if [ "$NODE_POOL" == "" ] || [ "$NODE_POOL" == "null" ]; then
      echo "Node pool not found in $NODE_JSON"
      exit 2
    fi
    NODE_COUNT=$(gcloud container node-pools describe $NODE_POOL --region "$REGION" --cluster "$CLUSTER_NAME" "--format=table(name,initialNodeCount)" | grep -F "$NODE_POOL")
    NODE_POOLS=$(gcloud container node-pools list --region "$REGION" --cluster "$CLUSTER_NAME" "--format=table(name,locations)" | grep -F "$NODE_POOL" | sed 's/,\ /,/g')
    ZONES=$(echo "$NODE_POOLS" | awk '{print $2}' | sed 's/,/\n/g' | grep -c -F "$REGION")
    INITIAL_NODE_COUNT=$(echo "$NODE_COUNT" | awk '{print $2}')
    if [ "$INITIAL_NODE_COUNT" == "" ]; then
      INITIAL_NODE_COUNT=0
    fi
    if ((ZONES > 0)); then
      CURRENT_NODES=$((INITIAL_NODE_COUNT * ZONES))
    else
      ZONES=1
      CURRENT_NODES=$INITIAL_NODE_COUNT
    fi
    if [ "$DETERMINE_TYPE" == "true" ]; then
      MACHINE_TYPE=$(gcloud container clusters describe $CLUSTER_NAME --region $REGION "--format=table(nodeConfig.machineType)" | grep -v "MACHINE_TYPE")
    fi
  elif [ "$PROVIDER" == "tmc" ]; then
    PRESENT=$(tmc cluster list | grep -c -F $CLUSTER_NAME)
    if ((PRESENT == 0)); then
      echo "Cluster not found $CLUSTER_NAME"
      exit 2
    fi
    NODE_POOL=$(tmc cluster nodepool list --cluster-name $CLUSTER_NAME | grep -F "$CLUSTER_NAME" | awk '{print $1}')
    if [ "$NODE_POOL" == "" ]; then
      echo "TMC cluster $CLUSTER_NAME nodepool not found"
      exit 2
    fi
    NODE_POOL_JSON=$(tmc cluster nodepool get $NODE_POOL --cluster-name $CLUSTER_NAME --output json)
    INITIAL_NODE_COUNT=$(echo "$NODE_POOL_JSON" | jq '.spec.workerNodeCount' | sed 's/\"//g')
    CURRENT_NODES=$INITIAL_NODE_COUNT
    if [ "$DETERMINE_TYPE" == "true" ]; then
      MACHINE_TYPE=$(echo "$NODE_POOL_JSON" | jq '.spec.tkgAws.instanceType' | sed 's/\"//g')
    fi
  else
    echo "Unsupported provider: $PROVIDER"
  fi
  echo "Node pool $NODE_POOL, current node count=$CURRENT_NODES in $ZONES zones. Machine type: $MACHINE_TYPE"
  export CURRENT_NODES
  export MACHINE_TYPE
  export NODE_POOL
  export ZONES
}

function usage() {
  echo "Usage $0 <provider> <cluster> [--ram <min-ram>] | [--nodes <min-nodes>] [--cpu <min-cpu>] [--max fixed-nodes] [--add <additional>] [--shrink]"
  echo "  If nodes and fixed-nodes is the same the node count will be set to that value"
  echo "  If nodes is negative the number will be reduced by that many nodes"
  echo "  If --ram then the machine type will be used to determine the number of nodes"
  echo "  --cpu is optional and will be used to scale cluster to minimum required"
  echo "  Negative values will trigger a shrink or scale down of the cluster."
  echo "  Positive values that can be satisfied by current usage will only result in scale down when --shrink is provided."
}
if [ "$1" == "" ]; then
  echo "Cluster name required"
  exit 1
fi
export CLUSTER_NAME=$1
DETERMINE_TYPE=false
ZONES=1
VERBOSE=false
ARGS=
while [ "$2" != "" ]; do
  case $2 in
  "--cpu" | "--ram" | "--pods-per-job" | "--pods" | "--cpu-per-pod" | "--ram-per-pod")
    DETERMINE_TYPE=true
    ;;
  "--verbose")
    VERBOSE=true
    ;;
  esac
  if [ "$ARGS" != "" ]; then
    ARGS="$ARGS $2"
  else
    ARGS="$2"
  fi
  shift
  export ARGS
done
if [ "$VERBOSE" == "true" ]; then
  echo "Determine provide for :$CLUSTER_NAME"
fi
export PROVIDER=$($SCDIR/determine-provider.sh $CLUSTER_NAME)
if [ "$VERBOSE" == "true" ]; then
  echo "Connect to $CLUSTER_NAME on $PROVIDER"
fi
source $SCDIR/use-${PROVIDER}.sh $CLUSTER_NAME
source $SCDIR/kubeconfig-${PROVIDER}.sh $CLUSTER_NAME

set_nodecount_machine_type
if [ "$DETERMINE_TYPE" == "true" ]; then
  kubectl get pods --all-namespaces=true -o json >pods.json
  PODFILE=$(realpath pods.json)
  export ARGS="--machine $MACHINE_TYPE --podfile $PODFILE $ARGS"
fi
export ARGS="--current $CURRENT_NODES $ARGS"
if [ "$VERBOSE" == "true" ]; then
  echo "groovy $SCDIR/calculate-nodes.groovy --output nodes.json $ARGS"
fi
groovy $SCDIR/calculate-nodes.groovy --output nodes.json $ARGS
RC=$?
if ((RC != 0)); then
  exit $RC
fi
NODES=$(jq '.nodes' nodes.json)
SHRINK=$(jq '.shrink' nodes.json)
echo "NODES=$NODES, ZONES=$ZONES, SHRINK=$SHRINK"
TARGET=$(div_round_up $NODES $ZONES)
SCALE_DOWN=$($SCDIR/determine-default.sh $CLUSTER_NAME "scale_down")
if ((TARGET < SCALE_DOWN)); then
  TARGET=$SCALE_DOWN
fi
if ((TARGET < INITIAL_NODE_COUNT)); then
  if [ "$SHRINK" == "true" ]; then
    echo "Scaling down $CLUSTER_NAME on $REGION and $ZONES zones from $INITIAL_NODE_COUNT to $TARGET per zone"
  else
    echo "Not scaling down. Leaving $CLUSTER_NAME on $INITIAL_NODE_COUNT nodes per zone"
    TARGET=
  fi
elif ((TARGET > INITIAL_NODE_COUNT)); then
  echo "Scaling $CLUSTER_NAME on $REGION and $ZONES zones from $INITIAL_NODE_COUNT to $TARGET per zone"
else
  echo "Leaving $CLUSTER_NAME on $INITIAL_NODE_COUNT nodes per zone"
  TARGET=
fi
if [ "$TARGET" != "" ]; then
  if [ "$PROVIDER" == "gke" ]; then
    if [ "$DRY_RUN" == "true" ]; then
      echo "gcloud container clusters resize $CLUSTER_NAME --region $REGION --num-nodes=$TARGET --node-pool=$NODE_POOL --quiet"
    else
      gcloud container clusters resize "$CLUSTER_NAME" --region "$REGION" "--num-nodes=$TARGET" --node-pool=$NODE_POOL --quiet
    fi
  elif [ "$PROVIDER" == "tmc" ]; then
    if [ "$DRY_RUN" == "true" ]; then
      echo "tmc cluster nodepool update $NODE_POOL --cluster-name $CLUSTER_NAME --worker-node-count $TARGET"
    else
      tmc cluster nodepool update $NODE_POOL --cluster-name $CLUSTER_NAME --worker-node-count $TARGET
      STATUS=$(tmc cluster nodepool get $NODE_POOL --cluster-name $CLUSTER_NAME --output json | jq '.status.phase' | sed 's/\"//g')
      while [ "$STATUS" != "READY" ]; do
        echo "TMC Node pool $NODE_POOL for $CLUSTER_NAME status:$STATUS"
        sleep 10
        STATUS=$(tmc cluster nodepool get $NODE_POOL --cluster-name $CLUSTER_NAME --output json | jq '.status.phase' | sed 's/\"//g')
      done
      echo "TMC Node pool $NODE_POOL for $CLUSTER_NAME status:$STATUS"
      COUNT=$(tmc cluster nodepool get $NODE_POOL --cluster-name $CLUSTER_NAME --output json | jq '.spec.workerNodeCount' | sed 's/\"//g')
      echo "TMC cluster $CLUSTER_NAME has $COUNT nodes"
    fi
  fi
fi
