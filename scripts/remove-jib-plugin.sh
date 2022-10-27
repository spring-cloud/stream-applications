#!/usr/bin/env bash
SCDIR=$(realpath $(dirname "$(readlink -f "${BASH_SOURCE[0]}")" ))
if [ "$1" = "" ]; then
  echo "Usage $0 <pom-file>"
  exit 1
fi
FILE=$(realpath $1)
TMP_FILE="$1-pom-mod.xml"
TMP_FILE="${TMP_FILE//\//-/g}"
echo "Removing jib-maven-plugin from $FILE"
xsltproc -o $TMP_FILE $SCDIR/remove-jib-plugin.xsl $FILE
RC=$?
if((RC!=0)); then
  exit 1
fi
mv -f $TMP_FILE $FILE
#rm -f $TMP_FILE
