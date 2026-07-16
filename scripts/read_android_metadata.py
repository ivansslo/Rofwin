#!/usr/bin/env python3
# Baca applicationId/versionCode/versionName dari build file Android.
# Dukung Kotlin DSL (app/build.gradle.kts) maupun Groovy (build.gradle lama).
import argparse
import json
import re
import shlex
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--file", default="app/build.gradle.kts")
parser.add_argument("--shell", action="store_true")
args = parser.parse_args()

content = Path(args.file).read_text(encoding="utf-8")


def extract(pattern: str, default: str = "") -> str:
    match = re.search(pattern, content, re.MULTILINE)
    return match.group(1) if match else default


# Pola fleksibel: `key = "x"` (KTS) ATAU `key 'x'` / `key "x"` (Groovy)
metadata = {
    "applicationId": extract(r"applicationId\s*=?\s*['\"]([^'\"]+)['\"]"),
    "versionCode": extract(r"versionCode\s*=?\s*([0-9]+)"),
    "versionName": extract(r"versionName\s*=?\s*['\"]([^'\"]+)['\"]"),
}

if not metadata["versionCode"] or not metadata["versionName"]:
    raise SystemExit(f"[rofwin] versionCode/versionName tidak ditemukan di {args.file}")

if args.shell:
    for key, value in metadata.items():
        env_key = re.sub(r'([^A-Z])([A-Z])', r'\1_\2', key).upper()
        print(f"export {env_key}={shlex.quote(value)}")
else:
    print(json.dumps(metadata))
