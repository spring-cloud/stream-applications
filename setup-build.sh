#!/usr/bin/env bash
./mvnw clean install -f stream-applications-build -U
./mvnw clean install -f functions -N -U
./mvnw clean install -f functions/function-dependencies -N -U
./mvnw clean install -f applications/stream-applications-core -N -U

