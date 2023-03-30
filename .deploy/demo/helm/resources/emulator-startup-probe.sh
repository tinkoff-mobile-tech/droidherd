#!/usr/bin/env bash

while [ "$(${ANDROID_SDK_ROOT}/platform-tools/adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
  echo "Still waiting for boot.."
  exit 1
done

exit 0