#!/usr/bin/env python3
"""Verify the complete staged KMP publication before independent consumer resolution."""

import hashlib
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile


def verify(repository, version):
    base = Path(repository).resolve() / "com/eignex"
    modules = ["kpermute"] + ["kpermute-" + target for target in (
        "jvm", "js", "wasm-js", "wasm-wasi", "linuxx64", "linuxarm64", "macosarm64",
        "mingwx64", "iosx64", "iosarm64", "iossimulatorarm64",
    )]
    for module in modules:
        directory = base / module / version
        stem = f"{module}-{version}"
        metadata = directory / f"{stem}.module"
        pom = directory / f"{stem}.pom"
        data = json.loads(metadata.read_text())
        ET.parse(pom)
        if "kbuild-platform" in metadata.read_text() or "kbuild-platform" in pom.read_text():
            raise ValueError(f"Build platform leaked into {module}")
        for variant in data["variants"]:
            redirect = variant.get("available-at")
            if redirect and not (directory / redirect["url"]).is_file():
                raise ValueError(f"Missing target metadata: {redirect['url']}")
            for artifact in variant.get("files", []):
                path = directory / artifact["url"]
                if not path.is_file() or path.stat().st_size != artifact["size"]:
                    raise ValueError(f"Missing or truncated artifact: {path}")
                if hashlib.sha256(path.read_bytes()).hexdigest() != artifact["sha256"]:
                    raise ValueError(f"Artifact checksum mismatch: {path}")
            for dependency in variant.get("dependencies", []):
                if not any(dependency.get("version", {}).get(key) for key in ("requires", "strictly", "prefers")):
                    raise ValueError(f"Versionless dependency in {module}: {dependency}")
        for classifier in ("sources", "javadoc"):
            with ZipFile(directory / f"{stem}-{classifier}.jar") as archive:
                if not any(name != "META-INF/MANIFEST.MF" and not name.endswith("/") for name in archive.namelist()):
                    raise ValueError(f"Empty {classifier} artifact for {module}")
        if module == "kpermute-jvm":
            for variant in data["variants"]:
                if (variant.get("attributes", {}).get("org.gradle.category") == "library"
                        and variant["attributes"].get("org.gradle.usage") in ("java-api", "java-runtime")):
                    if variant["attributes"].get("org.gradle.jvm.version") != 17:
                        raise ValueError("Published JVM requirement must remain Java 17")
            with ZipFile(directory / f"{stem}.jar") as archive:
                for name in archive.namelist():
                    if name.endswith(".class") and int.from_bytes(archive.read(name)[6:8], "big") > 61:
                        raise ValueError(f"Bytecode above Java 17: {name}")
    print(f"Verified {len(modules)} complete publications at {version}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: verify-publication.py REPOSITORY VERSION")
    verify(*sys.argv[1:])
