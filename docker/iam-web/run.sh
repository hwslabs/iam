#!/bin/bash

set -vx

gradle wrapper
./gradlew build
java -cp ./build/libs/hypto-iam-server-1.0.0-all.jar com.hypto.iam.server.ApplicationKt
