# Local Development Support

The files in this folder are to support local testing and development.

These scripts are useful when you want to build the containers on a different architecture and publish to a private repo.

## `download-apps.sh`
Downloads all applications needed by `create-containers.sh` from Maven repository.

*If the timestamp of snapshots matches the download will be skipped.*

Usage: `download-apps.sh [version] [broker]`
* `version` is the stream applications version like `3.2.1` or default is `3.2.2-SNAPSHOT`
* `broker` is one of rabbitmq, rabbit or kafka

## `create-containers.sh`
Creates all containers and pushes to local docker registry.

This script requires [jib-cli](https://github.com/GoogleContainerTools/jib/tree/master/jib-cli)

Usage: `create-containers.sh [version] [broker] [jre-version]`
* `version` is the skipper version like `3.2.1` or default is `3.2.2-SNAPSHOT`
* `broker` is one of rabbitmq, rabbit or kafka 
* `jre-version` should be one of 11, 17
