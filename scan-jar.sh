#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [[ "$1" != *"-sources.jar" ]] && [[ "$1" != *"-javadoc.jar" ]]; then
  trivy rootfs \
    --scanners vuln \
    --severity HIGH,CRITICAL \
    --exit-code 1 \
    --quiet \
    --ignorefile .trivyignore \
    --show-suppressed \
    --format table "$1"
fi
