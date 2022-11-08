#!/bin/bash
if [ "$1" == "" ]; then
  echo "Provide one or more workflow names like ci.yml that does produce download artefacts"
  exit 1
fi
while [ "$1" != "" ]; do
  echo "Listing workflow runs for $1"
  gh run list -w "$1"
  shift
done
