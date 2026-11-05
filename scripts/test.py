"""Run all standalone Java policy tests; requires JDK 21 on PATH."""
from pathlib import Path
from tempfile import TemporaryDirectory
import re
import subprocess

root = Path(__file__).resolve().parents[1]
tests = sorted((root / "tests/java").glob("*Test.java"))
src = root / "app/src/main/java"
deps = {src.joinpath(*name.split(".")).with_suffix(".java")
        for test in tests for name in re.findall(r"import (dev\.codex\.[\w.]+);", test.read_text())}
with TemporaryDirectory() as out:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", out, *sorted(deps), *tests], check=True)
    for test in tests:
        subprocess.run(["java", "-cp", out, test.stem], check=True)
