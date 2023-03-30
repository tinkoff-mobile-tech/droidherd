#!/usr/bin/env bash

# this helper script to check how emulator is running in docker
# it can be used for debug and testing purposes

EMULATOR_IMAGE="us-docker.pkg.dev/android-emulator-268719/images/29-google-x64-no-metrics"

docker run --rm -it --privileged \
  -v /dev/kvm:/dev/kvm -v /dev/shm:/dev/shm \
  $EMULATOR_IMAGE bash