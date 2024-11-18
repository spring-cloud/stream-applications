#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$TRIVY_UPLOAD" == "true" ]; then
  echo "Scanning $1"
fi
find $1 -type f -name "*.jar" -exec bash "$SCDIR/scan-jar.sh" '{}' \;
