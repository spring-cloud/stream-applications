#!/bin/bash
if [ "$1" == "" ]; then
  echo "Input required"
  exit 2
fi
export GCP_CRED_JSON="$1"
if [[ "$GCP_CRED_JSON" == *"=" ]]; then
  export GCP_CRED_JSON="$(echo $1 | base64 -d)"
fi
GCP_CRED_JSON_FILE="$HOME/.gcloud_cred.json"
echo "$GCP_CRED_JSON" > $GCP_CRED_JSON_FILE
CLIENT_EMAIL=$(jq '.client_email' $GCP_CRED_JSON_FILE | sed 's/\"//g')
if [ "$CLIENT_EMAIL" == "" ]; then
  echo "Cannot parse credentials"
  exit 1
fi
echo "Using $CLIENT_EMAIL"
export GCP_CRED_JSON_FILE=$GCP_CRED_JSON_FILE
