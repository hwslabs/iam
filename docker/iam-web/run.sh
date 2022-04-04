#!/bin/bash

set -vx

gradle --stop # Stop running daemons if any
gradle wrapper
./gradlew run dockerDistJar
