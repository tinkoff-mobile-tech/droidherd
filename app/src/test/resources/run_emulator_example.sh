#!/usr/bin/env bash

# This is example script to run android emulator.
# It is provide abilities to some customization and workarounds
# for some pitfalls.
# You case use as base for your own.

(
set -ex
set -m

adb_reconnect_counter=0

adb_with_timeout() {
  local adb_exit_code=0
  timeout 3 adb $@ || adb_exit_code=$?
  [[ "$adb_exit_code" == "124" ]] && {
      [[ "$adb_reconnect_counter" == "3" ]] && {
        echo "ADB is not responding - kill emulator";
        exit 4;
      }
      adb reconnect
      adb_reconnect_counter=$((adb_reconnect_counter+1))
  }
}

wait_for_boot() {
  attempt_counter=0
  max_attempts=41
  until [ `adb_with_timeout shell "getprop sys.boot_completed"` != *"1"* ]; do
      if [ ${attempt_counter} -eq ${max_attempts} ];then
        echo "Max attempts reached"
        exit 1
      fi

      echo "waiting for next attempt"
      attempt_counter=$(($attempt_counter+1))
      sleep 5
  done
}

if [[ -z "${VERSION}" ]]; then
    echo_error "You must specify VERSION environment variable"
    exit 1
fi

if [[ -z "${EMULATOR_ARGS}" ]]; then
    export EMULATOR_ARGS=""
fi

if [[ -z "${EMULATOR_LOCALE}" ]]; then
    export EMULATOR_LOCALE="ru-RU"
fi

console_port=$CONSOLE_PORT
adb_port=$ADB_PORT
adb_server_port=$ADB_SERVER_PORT
emulator_opts=$EMULATOR_OPTS

export QTWEBENGINE_DISABLE_SANDBOX=1

if [ -z "$console_port" ]
then
  console_port="5554"
fi
if [ -z "$adb_port" ]
then
  adb_port="5555"
fi
if [ -z "$adb_server_port" ]
then
  adb_server_port="5037"
fi
if [ -z "$emulator_opts" ]
then
  emulator_opts="-screen multi-touch -no-boot-anim -noaudio -nojni -netfast -verbose -camera-back emulated -camera-front none -skip-adb-auth -snapshot default -no-snapshot-save -writable-system"
fi

CONFIG="/root/.android/avd/${ANDROID_ARCH}.avd/config.ini"

# Detect ip and forward ADB ports outside to outside interface
ip=$(ip addr list eth0|grep "inet "|cut -d' ' -f6|cut -d/ -f1)
redir --laddr=$ip --lport=$adb_server_port --caddr=127.0.0.1 --cport=$adb_server_port &
redir --laddr=$ip --lport=$console_port --caddr=127.0.0.1 --cport=$console_port &
redir --laddr=$ip --lport=$adb_port --caddr=127.0.0.1 --cport=$adb_port &

export DISPLAY=:1
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/android-sdk-linux/emulator/lib64/qt/lib:/opt/android-sdk-linux/emulator/lib64/libstdc++:/opt/android-sdk-linux/emulator/lib64:/opt/android-sdk-linux/emulator/lib64/gles_swiftshader

Xvfb :1 +extension GLX +extension RANDR +extension RENDER +extension XFIXES -screen 0 1920x1080x24 &
XVFB_PID=$!
sleep 1 && fluxbox -display ":1.0" &
FLUXBOX_PID=$!
if [[ "$EMULATOR_ENABLE_VNC" == "true" ]]; then
  sleep 2 && x11vnc -display :1 -nopw -forever &
  VNC_PID=$!
else
  echo "VNC disabled"
fi

# Set up and run emulator
# qemu references bios by relative path
cd /opt/android-sdk-linux/emulator

if [ -n "$ANDROID_CONFIG" ];
then
  IFS=';' read -ra OPTS <<< "$ANDROID_CONFIG"
  for OPT in "${OPTS[@]}"; do
    IFS='=' read -ra KV <<< "$OPT"
    KEY=${KV[0]}
    VALUE=${KV[1]}
    mv ${CONFIG} ${CONFIGTMP}
    cat ${CONFIGTMP} | grep -v ${KEY}= > ${CONFIG}
    echo ${OPT} >> ${CONFIG}
  done
fi

echo "emulator_opts: $emulator_opts"
adb start-server

LIBGL_DEBUG=verbose ./qemu/linux-x86_64/qemu-system-x86_64 -avd ${ANDROID_ARCH} -no-window -ports $console_port,$adb_port $EMULATOR_ARGS  $emulator_opts -qemu $QEMU_OPTS &

echo "Waiting for emulator booting..."

sleep 30

echo "Checking boot_completed status"

wait_for_boot

echo "Applying settings..."

adb logcat -G 16M

adb shell settings put global window_animation_scale 0.0
adb shell settings put global transition_animation_scale 0.0
adb shell settings put global animator_duration_scale 0.0
adb shell settings put secure show_ime_with_hard_keyboard 1
adb shell 'echo "chrome --no-sandbox --disable-fre --no-default-browser-check --no-first-run" > /data/local/tmp/chrome-command-line'
adb shell "su 0 setprop persist.sys.locale $EMULATOR_LOCALE"

{
  # send startup metric to the droidherd server
  attempt_counter=0
  max_attempts=10
  while true; do
      if [ ${attempt_counter} -eq ${max_attempts} ]; then
        echo "Impossible to parse setup time"
        break
      fi
      startup_time=$(grep --text "boot time" /emulator.log | awk '{ print $5 }')
      if [[ $startup_time ]]; then
        break
      fi
      echo "waiting for next attempt"
      attempt_counter=$(($attempt_counter+1))
      sleep 5
  done

  curl -XPOST -H "Content-type: application/json" -d "{\"image\": \"$ANDROID_VERSION\", \"value\": \"$startup_time\"}" "http://$DROIDHERD_HOST/api/internal/startup-metric" || true
} &

adb shell "su 0 setprop ctl.restart zygote"
echo "Checking boot_completed status after restart"
wait_for_boot
adb shell "echo \"ready\""
[[ "$?" != "0" ]] && { echo "adb shell echo failed on emulator" ; exit 1; }

echo 1 >> /emulator.ready
fg %LIBGL_DEBUG=verbose ./qemu/linux-x86_64/qemu-system-x86_64

) | tee /emulator.log

