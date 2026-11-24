#!/usr/bin/env python3
"""Fetch and unpack the OSRS gamevals for the revision in game.example.yml."""

import argparse
import os
import re
import subprocess
import tempfile
import tomllib
from pathlib import Path
from zipfile import BadZipFile, ZipFile

ASSET = "osrs-gamevals.zip"
FILES = ("gamevals.dat", "gamevals_generated.dat", "max-ids.toml", "revision.txt")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    config = Path("game.example.yml").read_text(encoding="utf-8")
    match = re.search(r"^revision:[ \t]*(\d+(?:\.\d+)?)[ \t]*$", config, re.MULTILINE)
    if not match:
        raise SystemExit("Expected a numeric revision in game.example.yml.")
    revision = match.group(1)
    tag = f"gamevals-{revision}"

    with tempfile.TemporaryDirectory() as work:
        result = subprocess.run(
            [
                "gh", "release", "download", tag,
                "--repo", os.environ["GITHUB_REPOSITORY"],
                "--pattern", ASSET, "--dir", work,
            ],
            check=False,
        )
        if result.returncode:
            raise SystemExit(
                f"Could not download {tag}. Run Publish Revision Gamevals for {revision}."
            )
        try:
            with ZipFile(Path(work) / ASSET) as archive:
                files = {name: archive.read(name) for name in FILES}
            manifest = tomllib.loads(files["max-ids.toml"].decode())
        except (BadZipFile, KeyError, ValueError) as error:
            raise SystemExit(f"Invalid {tag} snapshot: {error}") from error

    if (
        files["revision.txt"].decode().strip() != revision
        or manifest.get("revision") != int(revision.split(".")[0])
        or not manifest.get("max_ids")
        or not all(files.values())
    ):
        raise SystemExit(f"Incomplete or mismatched snapshot for revision {revision}.")
    args.output.mkdir(parents=True, exist_ok=True)
    for name, data in files.items():
        (args.output / name).write_bytes(data)
    print(f"Loaded OSRS gameval snapshot {tag}.")


if __name__ == "__main__":
    main()
