#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

set +e

echo "Updating RELEASE_VERSION: $(cat $SCDIR/version/RELEASE_VERSION) to NEXT_DEV_VERSION: $(cat $SCDIR/version/NEXT_DEV_VERSION)"
cat $SCDIR/version/NEXT_DEV_VERSION > $SCDIR/version/RELEASE_VERSION
echo "Updated RELEASE_VERSION: $(cat $SCDIR/version/RELEASE_VERSION)"
