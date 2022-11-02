#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

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

set -e
if [ "$1" != "" ]; then
  export CLUSTER_NAME="$1"
else
  check_env CLUSTER_NAME "$CLUSTER_NAME"
fi
set +e
COUNT=$(gcloud container clusters list | grep -c -F "$CLUSTER_NAME")
if [ $COUNT -eq 0 ]; then
  echo "Cluster $CLUSTER_NAME does not exist"
else
  echo "Cluster $CLUSTER_NAME found"
fi
if ((COUNT > 1)); then
  CLUSTER=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $1}')
  echo "Multiple clusters matching $CLUSTER_NAME"
  gcloud container clusters list | grep -F "$CLUSTER_NAME"
  if [ "$CLUSTER_NAME" != "$CLUSTER" ]; then
    echo "No exact match for $CLUSTER_NAME"
  fi
fi
export RGN=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $2}')
if [ "$RGN" == "" ]; then
  if [ "$REGION" == "" ]; then
    REGION=$($SCDIR/determine-default.sh $CLUSTER_NAME "region")
    if [ "$REGION" == "" ]; then
      echo "Region not provided"
      exit 0
    fi
    echo "Region not provided using default:$REGION"
  fi
  echo "Search for other cluster related resources in $REGION"
else
  REGION=$RGN
fi
echo "Deleting cluster and resources: $CLUSTER_NAME"
if ((COUNT > 0)); then
  source $SCDIR/kubeconfig-gke.sh $CLUSTER_NAME
  $SCDIR/delete-namespaces.sh --nowait
fi
COUNT=$(gcloud compute firewall-rules list --format="table(name,network)" | grep -c -F "$CLUSTER_NAME")
if [ $COUNT -gt 0 ]; then
  FIREWALLS=$(gcloud compute firewall-rules list --format="table(name,network)" | grep -F "$CLUSTER_NAME" | awk '{print $1}')
  echo "FIREWALLS=$FIREWALLS"
  for firewall in $FIREWALLS; do
    echo "Deleting firewall: $firewall"
    gcloud compute firewall-rules delete "$firewall" --quiet
  done
else
  echo "No firewall rules for $CLUSTER_NAME"
fi
COUNT=$(gcloud container clusters list | grep -c -F "$CLUSTER_NAME")
if [ $COUNT -ne 0 ]; then
  echo "Deleting cluster: $CLUSTER_NAME from $REGION"
  gcloud container clusters delete "${CLUSTER_NAME}" --region "$REGION" --quiet
else
  echo "No cluster named $CLUSTER_NAME"
fi
COUNT=$(gcloud compute routes list | grep -c -F "$CLUSTER_NAME")
if [ $COUNT -gt 0 ]; then
  ROUTES=$(gcloud compute routes list | grep -F "$CLUSTER_NAME" | awk '{print $1}')
  echo "ROUTES=$ROUTES"
  for route in $ROUTES; do
    echo "Deleting route: $route"
    gcloud compute routes delete "$route" --quiet
  done
else
  echo "No routes for $CLUSTER_NAME"
fi
COUNT=$(gcloud compute networks subnets list --regions="$REGION" | grep -c -F "$CLUSTER_NAME")
if [ $COUNT -ne 0 ]; then
  SUBNETS=$(gcloud compute networks subnets list --regions="$REGION" | grep -F "$CLUSTER_NAME" | awk '{print $1}')
  echo "SUBNETS=$SUBNETS"
  for subnet in $SUBNETS; do
    echo "Deleting subnet: $subnet from $REGION"
    gcloud compute networks subnets delete $subnet --region "$REGION" --quiet
  done
else
  echo "No network subnets for $CLUSTER_NAME"
fi
COUNT=$(gcloud compute networks list | grep -c -F "$CLUSTER_NAME")
if [ $COUNT -gt 0 ]; then
  NETWORKS=$(gcloud compute networks list | grep -F "$CLUSTER_NAME" | awk '{print $1}')
  echo "NETWORKS=$NETWORKS"
  for network in $NETWORKS; do
    echo "Deleting network: $network"
    gcloud compute networks delete "$network" --quiet
  done
else
  echo "No networks for $CLUSTER_NAME"
fi
COUNT=$(gcloud compute instances list | grep -c -F "$CLUSTER_NAME")
if [ $COUNT -gt 0 ]; then
  INSTANCES=$(gcloud compute instances list | grep -F "$CLUSTER_NAME" | awk '{print $1}')
  echo "INSTANCES=$INSTANCES"
  for instance in $INSTANCES; do
    ZONE=$(gcloud compute instances list | grep -F "$instance" | awk '{print $2}')
    echo "Deleting instance: $instance from $ZONE"
    gcloud compute instances delete "$instance" --quiet --zone $ZONE
  done
else
  echo "No instances for $CLUSTER_NAME"
fi
