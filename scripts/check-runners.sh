#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)
SCALING=$($SCDIR/determine-default.sh stream-apps-gh-runners runner_scaling)
if [ "$SCALING" == "auto" ]; then
  echo "Auto scaling:"
  kubectl get horizontalrunnerautoscalers
fi
echo ""
echo "RunnerDeployments:"
DEPLOYMENTS=$(kubectl get rdeploy)
echo "$DEPLOYMENTS"
echo ""
echo "Runners:"
RUNNER_DEPLOYMENTS=$(echo "$DEPLOYMENTS" | grep -F "runner" | awk '{print $1}')
for deployment in $RUNNER_DEPLOYMENTS; do
  RUNNERS=$(kubectl get runners -l runner-deployment-name=$deployment --output=json | jq -c '.')
  PHASES=$(echo "$RUNNERS" | jq '.items | map(.status.phase) | unique | .[]' | sed 's/\"//g')
  for phase in $PHASES; do
    # kubectl get runners -l runner-deployment-name=$deployment --output=json | jq --arg deployment $deployment '.items | map(.status) | group_by(.phase,.ready) | map({ "deployment": $deployment, "count": length, "phase": .[0].phase})'
    echo "$RUNNERS"| jq --arg phase $phase --arg deployment $deployment '.items | map(.status) | .[] | select(.phase == $phase)' | jq --slurp --arg phase $phase --arg deployment $deployment 'group_by(.phase) | map({ "deployment": $deployment, "count": length, "phase": $phase})'
  done
done
