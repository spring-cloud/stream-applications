#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [[ "$1" != *"-sources.jar" ]] && [[ "$1" != *"-javadoc.jar" ]]; then
    if [ "$TRIVY_UPLOAD" == "true" ]; then
      echo "Scanning:$1"
      echo "trivy rootfs --format sarif -o \"$1.sarif\" \"$1\""
      trivy rootfs --format sarif -o "$1.sarif" "$1"
      if [ -f "$1.sarif" ]; then
        if [ -f $SCDIR/runs.sarif ]; then
          echo "," >> "$SCDIR/runs.sarif"
        fi
        jq -c '.runs | .[]' "$1.sarif" >> "$SCDIR/runs.sarif"
      else
        echo "Could not find:$1.sarif"
      fi
    else
      trivy rootfs -q "$1"
    fi
else
  if [ "$TRIVY_UPLOAD" == "true" ]; then
    echo "Skipping $1"
  fi
fi
