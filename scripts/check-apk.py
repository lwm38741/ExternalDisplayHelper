"""Check the merged artifact: both entry points, framework metadata and release manifest."""
import subprocess
import sys
import zipfile
from pathlib import Path

apk, aapt = map(Path, sys.argv[1:])
def require(condition, message):
    if not condition:
        raise SystemExit(message)

with zipfile.ZipFile(apk) as z:
    dex = z.read("classes.dex")
    for symbol in ("input/PhysicalDisplayHook", "probe/ControlActivity", "probe/ProbeActivity"):
        require(("Ldev/codex/externaldisplay/" + symbol + ";").encode() in dex, symbol)
    meta = "META-INF/xposed/"
    require(z.read(meta + "java_init.list").decode().strip() == "dev.codex.externaldisplay.input.PhysicalDisplayHook", "Missing module entry")
    require(set(z.read(meta + "scope.list").decode().split()) == {"system", "android"}, "Unexpected scope")
    require(b"staticScope=true" in z.read(meta + "module.prop"), "Missing static scope")
badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
require("package: name='dev.codex.externaldisplay.input'" in badging, "Wrong upgrade package")
require("launchable-activity: name='dev.codex.externaldisplay.probe.ControlActivity'" in badging, "Missing launcher")
require("application-debuggable" not in badging, "Debuggable")
print("Unified APK: package, launcher, module, scope, DEX and non-debuggable flag verified.")
