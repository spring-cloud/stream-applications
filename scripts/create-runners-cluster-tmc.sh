#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

export CLUSTER_NAME="stream-apps-gh-runners"
echo "Creating stream-apps-gh-runners"
tmc cluster create -f $PARENT/.github/tmc/gh-runner-template.yaml
