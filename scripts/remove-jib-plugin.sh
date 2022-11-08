#!/bin/bash
SCDIR=$(realpath $(dirname "$(readlink -f "${BASH_SOURCE[0]}")" ))
if [ "$1" = "" ]; then
  echo "Usage $0 <pom-file>"
  exit 1
fi
FILE=$(realpath $1)
TMP_FILE=pom-mod.xml
echo "Removing jib-maven-plugin from $FILE"
xsltproc -o $TMP_FILE $SCDIR/remove-jib-plugin.xsl $FILE
RC=$?
if((RC!=0)); then
  exit 1
fi
mv -f $TMP_FILE $FILE
rm -rf $TMP_FILE
