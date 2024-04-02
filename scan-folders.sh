#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ -f $SCDIR/runs.sarif ]; then
  rm $SCDIR/runs.sarif
fi
export TRIVY_UPLOAD=true
while [ "$1" != "" ]; do
  if [ "$1" == "table" ]; then
    export TRIVY_UPLOAD=false
  fi
  shift
done
REAL_PATH=$(realpath $PWD)
echo "Scanning in $REAL_PATH"
find . -type d -name target -exec bash "$SCDIR/scan-jars.sh" '{}' \;
echo "{\"version\": \"2.1.0\", \"\$schema\": \"https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json\", \"runs\": [" > "$SCDIR/scan.sarif"
if [ -f "$SCDIR/runs.sarif" ]; then
  cat "$SCDIR/runs.sarif" >> "$SCDIR/scan.sarif"
fi
echo "]}" >> "$SCDIR/scan.sarif"
echo "Created $SCDIR/scan.sarif"
