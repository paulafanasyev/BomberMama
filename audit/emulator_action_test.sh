#!/usr/bin/env bash
set -euo pipefail
mkdir -p audit/screens

curl -fL --retry 3 -o audit/release.apk \
  https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/BomberMama.apk
echo "f6f099ab09954e9e550a13712d0b42ca2242857e0847c373ae8ae9e2fcc41  audit/release.apk" | sha256sum -c -

adb wait-for-device
adb shell getprop sys.boot_completed | grep -q 1

adb uninstall com.bombermama >/dev/null 2>&1 || true
adb install audit/release.apk
adb shell pm clear com.bombermama >/dev/null
adb logcat -c
adb shell monkey -p com.bombermama 1 >/dev/null
sleep 3
test -n "$(adb shell pidof com.bombermama)"

screenshot() {
  adb exec-out screencap -p > "audit/screens/$1.png"
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
xml = open("audit/window.xml", encoding="utf-8").read()
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
xml = open("audit/window.xml", encoding="utf-8").read()
m = re.search(r'<node[^>]*resource-id="' + re.escape(rid) + r'"[^>]*checked="([^"]*)"', xml)
if not m:
    raise SystemExit(f"checked attribute not found: {rid}")
actual = m.group(1)
if actual != expected:
    raise SystemExit(f"{rid}: expected checked={expected}, got {actual}")
PY
}

screenshot "01_main"
click_id "com.bombermama:id/btnLevels"
screenshot "02_levels"
adb shell input keyevent 4
sleep 1

click_id "com.bombermama:id/btnSettings"
screenshot "03_settings"
assert_checked "com.bombermama:id/swSound" "true"
assert_checked "com.bombermama:id/swMusic" "true"

click_id "com.bombermama:id/swSound"
click_id "com.bombermama:id/swMusic"
assert_checked "com.bombermama:id/swSound" "false"
assert_checked "com.bombermama:id/swMusic" "false"
screenshot "04_settings_off"

adb shell input keyevent 4
sleep 1
click_id "com.bombermama:id/btnNewGame"
sleep 3
test -n "$(adb shell pidof com.bombermama)"
screenshot "05_game_initial"

adb shell input swipe 160 900 340 900 500
sleep 0.5
adb shell input tap 1700 900
sleep 0.5
screenshot "06_bomb_active"
sleep 1.5
screenshot "07_explosion_window"
sleep 0.8
screenshot "08_post_explosion"

click_id "com.bombermama:id/btnPause"
screenshot "09_pause"
click_id "com.bombermama:id/btnResume"
sleep 1
screenshot "10_resume"

adb shell input keyevent 4
sleep 1
screenshot "11_back_to_pause"
click_id "com.bombermama:id/btnMenu"
screenshot "12_main_after_menu"

adb shell am force-stop com.bombermama
adb shell monkey -p com.bombermama 1 >/dev/null
sleep 2
click_id "com.bombermama:id/btnSettings"
assert_checked "com.bombermama:id/swSound" "false"
assert_checked "com.bombermama:id/swMusic" "false"
screenshot "13_settings_persisted"

adb shell input keyevent 4
sleep 1
screenshot "14_relaunch"

adb logcat -d -v threadtime > audit/logcat.txt
adb shell dumpsys package com.bombermama > audit/dumpsys-package.txt
adb shell dumpsys meminfo com.bombermama > audit/meminfo.txt
adb shell uiautomator dump /sdcard/final.xml >/dev/null || true
adb shell cat /sdcard/final.xml > audit/final-ui.xml || true

if grep -E "FATAL EXCEPTION|AndroidRuntime.*FATAL|ANR in com.bombermama|Process com.bombermama.*has died" audit/logcat.txt; then
  exit 1
fi

echo "BOMBERMAMA_EMULATOR_RUNTIME_OK"
