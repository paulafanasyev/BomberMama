#!/usr/bin/env bash
set -euo pipefail

mkdir -p audit/screens
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

echo "=== JAVA ==="
java -version

echo "=== GRADLE ==="
curl -fL --retry 3 -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.13-bin.zip
unzip -q /tmp/gradle.zip -d /tmp
export PATH="/tmp/gradle-8.13/bin:$PATH"
gradle --version

echo "=== JVM + ROBOLECTRIC TESTS ==="
gradle test --no-daemon --stacktrace

echo "=== DEBUG BUILD ==="
gradle :app:assembleDebug --no-daemon --stacktrace

echo "=== EXACT RELEASE APK ==="
curl -fL --retry 3 -o audit/release.apk \
  https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/BomberMama.apk
echo "f6f099ab09954e9e550a13712d0b42ca2242857e0847c373ae8ae62e9e2fcc41  audit/release.apk" | sha256sum -c -
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;35.0.0" "platform-tools" "emulator" "platforms;android-35" "system-images;android-35;google_apis;x86_64" >/tmp/sdk-install.log
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging audit/release.apk | tee audit/aapt-badging.txt
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose audit/release.apk | tee audit/apksigner.txt
sha256sum audit/release.apk | tee audit/sha256.txt

echo "=== AVD ==="
echo "no" | avdmanager create avd \
  -n bombermama-audit \
  -k "system-images;android-35;google_apis;x86_64" \
  --device "pixel_2" \
  --force

emulator @bombermama-audit \
  -no-window -no-audio -gpu swiftshader_indirect -no-snapshot -no-boot-anim \
  >/tmp/emulator.log 2>&1 &
EMULATOR_PID=$!

cleanup() {
  adb emu kill >/dev/null 2>&1 || true
  kill "$EMULATOR_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

adb wait-for-device
for i in $(seq 1 120); do
  BOOTED=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
  if [ "$BOOTED" = "1" ]; then break; fi
  if ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
    cat /tmp/emulator.log || true
    exit 1
  fi
  sleep 2
done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

echo "=== INSTALL ==="
adb uninstall com.bombermama >/dev/null 2>&1 || true
adb install audit/release.apk
adb shell pm clear com.bombermama >/dev/null
adb logcat -c
adb shell monkey -p com.bombermama 1 >/dev/null
sleep 3
test -n "$(adb shell pidof com.bombermama)"
adb shell dumpsys activity activities | grep -m1 "com.bombermama" | tee audit/launch-activity.txt || true

screenshot() {
  local name="$1"
  adb exec-out screencap -p > "audit/screens/$name.png"
}

dump_ui() {
  adb shell uiautomator dump /sdcard/window.xml >/dev/null
  adb shell cat /sdcard/window.xml | tr -d '\n' > audit/window.xml
}

click_id() {
  local rid="$1"
  dump_ui
  local xy
  xy=$(python3 - "$rid" <<'PY'
import re, sys
rid = sys.argv[1]
xml = open("audit/window.xml", "r", encoding="utf-8").read()
m = re.search(r'<node[^>]*resource-id="' + re.escape(rid) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if not m:
    raise SystemExit(f"resource-id not found: {rid}")
x1,y1,x2,y2 = map(int, m.groups())
print((x1+x2)//2, (y1+y2)//2)
PY
)
  adb shell input tap $xy
  sleep 1
}

assert_checked() {
  local rid="$1"
  local expected="$2"
  dump_ui
  python3 - "$rid" "$expected" <<'PY'
import re, sys
rid, expected = sys.argv[1], sys.argv[2]
xml = open("audit/window.xml", "r", encoding="utf-8").read()
m = re.search(r'<node[^>]*resource-id="' + re.escape(rid) + r'"[^>]*checked="([^"]*)"', xml)
if not m:
    raise SystemExit(f"checked attribute not found: {rid}")
actual = m.group(1)
if actual != expected:
    raise SystemExit(f"{rid}: expected checked={expected}, got {actual}")
print(f"{rid}: checked={actual}")
PY
}

screenshot "01_main"

click_id "com.bombermama:id/btnLevels"
screenshot "02_levels"
adb shell input keyevent 4
sleep 1

click_id "com.bombermama:id/btnSettings"
screenshot "03_settings_initial"
assert_checked "com.bombermama:id/swSound" "true"
assert_checked "com.bombermama:id/swMusic" "true"

click_id "com.bombermama:id/swSound"
click_id "com.bombermama:id/swMusic"
screenshot "04_settings_off"
assert_checked "com.bombermama:id/swSound" "false"
assert_checked "com.bombermama:id/swMusic" "false"

adb shell input keyevent 4
sleep 1
click_id "com.bombermama:id/btnNewGame"
sleep 3
screenshot "05_game_initial"
test -n "$(adb shell pidof com.bombermama)"

# Exercise real touch input: move via D-pad, then place a bomb.
adb shell input swipe 160 900 340 900 500
sleep 0.5
adb shell input tap 1700 900
sleep 0.4
screenshot "06_bomb_active"

# Fuse is 2.2s. This capture is taken inside the expected explosion window.
sleep 1.9
screenshot "07_explosion_window"
test -n "$(adb shell pidof com.bombermama)"

sleep 0.8
screenshot "08_post_explosion"

click_id "com.bombermama:id/btnPause"
screenshot "09_pause"

click_id "com.bombermama:id/btnResume"
sleep 0.8
screenshot "10_resume"

adb shell input keyevent 4
sleep 0.8
screenshot "11_back_to_pause"

click_id "com.bombermama:id/btnMenu"
screenshot "12_main_after_menu"

adb shell am force-stop com.bombermama
adb shell monkey -p com.bombermama 1 >/dev/null
sleep 2
click_id "com.bombermama:id/btnSettings"
screenshot "13_settings_persisted"
assert_checked "com.bombermama:id/swSound" "false"
assert_checked "com.bombermama:id/swMusic" "false"

adb shell input keyevent 4
sleep 1
screenshot "14_main_relaunch"

echo "=== RUNTIME EVIDENCE ==="
adb shell dumpsys package com.bombermama > audit/dumpsys-package.txt
adb shell dumpsys meminfo com.bombermama > audit/meminfo.txt
adb logcat -d -v threadtime > audit/logcat.txt
cat /tmp/emulator.log > audit/emulator.log || true
adb shell uiautomator dump /sdcard/final.xml >/dev/null || true
adb shell cat /sdcard/final.xml > audit/final-ui.xml || true

if grep -E "FATAL EXCEPTION|AndroidRuntime.*FATAL|ANR in com.bombermama|Process com.bombermama.*has died" audit/logcat.txt; then
  exit 1
fi

echo "EMULATOR_AUDIT_RUNTIME_OK"
