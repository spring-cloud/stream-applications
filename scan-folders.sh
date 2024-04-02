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
find $SCDIR -type d -name target -exec bash "$SCDIR/scan-jars.sh" '{}' \;
if [ -f "$SCDIR/runs.sarif" ]; then
  echo "{\"version\": \"2.1.0\", \"\$schema\": \"https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json\", \"runs\": [" > $SCDIR/scan.sarif
  cat "$SCDIR/runs.sarif" >> $SCDIR/scan.sarif
  echo "]}" >> $SCDIR/scan.sarif
fi
