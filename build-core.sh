#!/usr/bin/env bash
if [ "$VERBOSE" == "true" ]; then
  MAVEN_OPT=--debug
else
  MAVEN_OPT=-q
fi
./mvnw $MAVEN_OPT -s ./.settings.xml install deploy -f stream-applications-build -U
./mvnw $MAVEN_OPT -s ./.settings.xml install deploy -f functions -N -U
./mvnw $MAVEN_OPT -s ./.settings.xml install deploy -f functions/function-dependencies -N -U
./mvnw $MAVEN_OPT -s ./.settings.xml install deploy -f applications/stream-applications-core -N -U
