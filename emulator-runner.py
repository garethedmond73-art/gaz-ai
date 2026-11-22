import pathlib
import subprocess
import sys
import time

RESULTS = pathlib.Path("test-results")
RESULTS.mkdir(exist_ok=True)

def run(command, check=True, capture=False):
    return subprocess.run(command, check=check, text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None)

def adb(*args, check=True, capture=False):
    return run(["adb", *args], check=check, capture=capture)

# The emulator action invokes its script through sh -c. Keep loops and
# conditional control flow in Python to avoid shell block truncation.
adb("wait-for-device")
booted = False
for attempt in range(90):
    result = adb("shell", "getprop", "sys.boot_completed", check=False, capture=True)
    if result.stdout.strip() == "1":
        booted = True
        break
    time.sleep(2)

if not booted:
    (RESULTS / "emulator-getprop.txt").write_text(
        adb("shell", "getprop", check=False, capture=True).stdout)
    (RESULTS / "emulator-boot-logcat.txt").write_text(
        adb("logcat", "-d", "-t", "5000", check=False, capture=True).stdout)
    sys.exit("Emulator did not finish booting within 180 seconds.")

adb("logcat", "-c", check=False)
apk = "app/build/outputs/apk/debug/app-debug.apk"
installed = False
for attempt in range(1, 4):
    print(f"APK installation attempt {attempt} of 3", flush=True)
    result = adb("install", "--no-streaming", "-r", apk, check=False)
    if result.returncode == 0:
        installed = True
        break
    adb("reconnect", "device", check=False)
    adb("wait-for-device")
    time.sleep(8)

if not installed:
    (RESULTS / "package-manager-diagnostics.txt").write_text(
        adb("shell", "dumpsys", "package", check=False, capture=True).stdout)
    (RESULTS / "install-failure-logcat.txt").write_text(
        adb("logcat", "-d", "-t", "12000", check=False, capture=True).stdout)
    sys.exit("APK installation failed after 3 attempts.")

adb("shell", "am", "force-stop", "uk.co.gazai")
adb("shell", "monkey", "-p", "uk.co.gazai", "-c",
    "android.intent.category.LAUNCHER", "1")
time.sleep(25)

smoke = run(["python3", "emulator-smoke-test.py"], check=False)
if smoke.returncode != 0:
    (RESULTS / "instrumentation-logcat.txt").write_text(
        adb("logcat", "-d", "-t", "12000", check=False, capture=True).stdout)
    sys.exit(smoke.returncode)

adb("logcat", "-c", check=False)
test = run(["gradle", "connectedDebugAndroidTest", "--no-daemon",
            "--max-workers=1", "--stacktrace"], check=False)
(RESULTS / "instrumentation-logcat.txt").write_text(
    adb("logcat", "-d", "-t", "12000", check=False, capture=True).stdout)
(RESULTS / "instrumentation-state.txt").write_text(
    adb("shell", "dumpsys", "activity", "instrumentation",
        check=False, capture=True).stdout)
sys.exit(test.returncode)
