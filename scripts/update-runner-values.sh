#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PARENT=$(realpath $SCDIR/..)

helm upgrade actions-runner-controller -f $SCDIR/arc/values.yml --namespace actions-runner-system  actions-runner-controller/actions-runner-controller