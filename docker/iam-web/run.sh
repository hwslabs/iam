#!/bin/bash

set -vx

gradle --stop # Stop running daemons if any

./gradlew run dockerDistJar
