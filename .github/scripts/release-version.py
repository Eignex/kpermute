#!/usr/bin/env python3
"""Resolve release, snapshot and GitHub-only release-candidate versions."""

import os
import re
import subprocess
from pathlib import Path

CORE = r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)"
STABLE = re.compile(CORE)
RC = re.compile(rf"({CORE})-rc\.(0|[1-9][0-9]*)")
VERSION = re.compile(rf"{CORE}(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?")


def resolve(tags, ref_type, ref_name, dry_run=False, test_release=False, rc_base=""):
    stable = [tuple(map(int, tag[1:].split("."))) for tag in tags if tag.startswith("v") and STABLE.fullmatch(tag[1:])]
    latest = max(stable, default=(0, 0, 0))
    next_patch = f"{latest[0]}.{latest[1]}.{latest[2] + 1}"
    if rc_base and not test_release:
        raise ValueError("rcBaseVersion requires testRelease=true")
    if ref_type == "tag":
        if not ref_name.startswith("v"):
            raise ValueError("Release tags must start with v")
        version = ref_name[1:]
        if rc_base:
            raise ValueError("rcBaseVersion is only used when creating a new RC from a branch")
        if test_release and not RC.fullmatch(version):
            raise ValueError("A test release requires an X.Y.Z-rc.N tag")
    elif test_release:
        base = rc_base or next_patch
        if not STABLE.fullmatch(base):
            raise ValueError("rcBaseVersion must be a stable SemVer version such as 1.4.0")
        if f"v{base}" in tags:
            raise ValueError(f"Cannot create an RC for already released version {base}")
        numbers = [int(match[2]) for tag in tags if tag.startswith("v")
                   and (match := RC.fullmatch(tag[1:])) and match[1] == base]
        version = f"{base}-rc.{max(numbers, default=0) + 1}"
    else:
        version = next_patch if dry_run else f"{next_patch}-SNAPSHOT"
    match = VERSION.fullmatch(version)
    if not match or (match[1] and any(part.isascii() and part.isdigit() and len(part) > 1 and part.startswith("0")
                                    for part in match[1].split("."))):
        raise ValueError(f"Invalid release version: {version}")
    return {
        "version": version,
        "is_snapshot": str(version.endswith("-SNAPSHOT")).lower(),
        "is_prerelease": str(match[1] is not None).lower(),
        "test_release": str(RC.fullmatch(version) is not None).lower(),
        "release_tag": f"v{version}",
        "dry_run": str(dry_run).lower(),
    }


if __name__ == "__main__":
    tags = subprocess.check_output(["git", "tag", "--list"], text=True).splitlines()
    try:
        values = resolve(tags, os.environ["GITHUB_REF_TYPE"], os.environ["GITHUB_REF_NAME"],
                         os.environ.get("DRY_RUN") == "true", os.environ.get("TEST_RELEASE") == "true",
                         os.environ.get("RC_BASE_VERSION", ""))
    except ValueError as error:
        raise SystemExit(str(error)) from error
    values["revision"] = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    with Path(os.environ["GITHUB_OUTPUT"]).open("a") as output:
        output.writelines(f"{key}={value}\n" for key, value in values.items())
    print(f"Resolved {values['version']}; GitHub-only RC: {values['test_release']}; dry run: {values['dry_run']}")
