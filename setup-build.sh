#!/usr/bin/env bash
./mvnw clean install -f stream-applications-build
./mvnw clean install -f functions -N
./mvnw clean install -f functions/function-dependencies -N
./mvnw clean install -f applications/stream-applications-core -N

