#!/usr/bin/env python3
"""Detect and rewrite custom gamevals that collide with OSRS reserved max IDs.

Reads `.data/gamevals-binary/max-ids.toml` (written by freshCache).
IDs in 0..=max per table are reserved; custom entries must be > max.

Exit codes:
  0 - no changes needed
  2 - files were rewritten
  1 - hard failure
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from pathlib import Path

SECTION_RE = re.compile(r"^\s*\[gamevals\.([^.\]]+)\]\s*$")
KV_RE = re.compile(r"^(\s*)([^=]+?)(\s*=\s*)(-?\d+)(\s*)$")
RSCM_KV_RE = re.compile(r"^([^=]+?)=(-?\d+)\s*$")
MAX_ID = 65535
# Packed from table/column defs — not authored as custom gamevals.
SKIP_TABLES = frozenset({"dbcol"})


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def load_max_ids(root: Path) -> dict[str, int]:
    path = root / ".data" / "gamevals-binary" / "max-ids.toml"
    if not path.is_file():
        raise SystemExit(
            f"Missing {path.as_posix()}. Run :or-cache:freshCache to generate it."
        )
    max_ids: dict[str, int] = {}
    in_section = False
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line == "[max_ids]":
            in_section = True
            continue
        if line.startswith("["):
            in_section = False
            continue
        if in_section and "=" in line:
            key, value = line.split("=", 1)
            table = key.strip()
            if table in SKIP_TABLES:
                continue
            max_ids[table] = int(value.strip())
    if not max_ids:
        raise SystemExit(f"No [max_ids] entries in {path.as_posix()}")
    return max_ids


def is_generated(path: Path) -> bool:
    parts = {p.lower() for p in path.parts}
    return bool(parts & {"build", "out", "target"})


def iter_gameval_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for base in (root / "content", root / "api"):
        if not base.is_dir():
            continue
        for path in base.rglob("gamevals.toml"):
            if not is_generated(path):
                files.append(path)
    gamevals_dir = root / ".data" / "gamevals"
    if gamevals_dir.is_dir():
        files.extend(
            sorted(
                path
                for path in gamevals_dir.glob("*.rscm")
                if path.stem not in SKIP_TABLES
            )
        )
    return files


def parse_toml_entries(path: Path) -> list[tuple[int, str, str, int, re.Match[str]]]:
    """Return (line_index, table, key, value, match) for toml gamevals."""
    entries: list[tuple[int, str, str, int, re.Match[str]]] = []
    table: str | None = None
    lines = path.read_text(encoding="utf-8").splitlines()
    for idx, line in enumerate(lines):
        section = SECTION_RE.match(line)
        if section:
            table = section.group(1)
            if table in SKIP_TABLES:
                table = None
            continue
        if table is None:
            continue
        match = KV_RE.match(line)
        if not match:
            continue
        key = match.group(2).strip()
        value = int(match.group(4))
        entries.append((idx, table, key, value, match))
    return entries


def parse_rscm_entries(path: Path) -> list[tuple[int, str, str, int, re.Match[str]]]:
    table = path.stem
    entries: list[tuple[int, str, str, int, re.Match[str]]] = []
    lines = path.read_text(encoding="utf-8").splitlines()
    for idx, line in enumerate(lines):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        match = RSCM_KV_RE.match(line.strip())
        if not match:
            continue
        key = match.group(1).strip()
        value = int(match.group(2))
        entries.append((idx, table, key, value, match))
    return entries


def collect_entries(root: Path) -> dict[tuple[str, str], list[tuple[Path, int, int, str]]]:
    """Map (table, key) -> list of (file, line_index, value, original_line)."""
    by_key: dict[tuple[str, str], list[tuple[Path, int, int, str]]] = defaultdict(list)
    for path in iter_gameval_files(root):
        text_lines = path.read_text(encoding="utf-8").splitlines()
        parsed = (
            parse_rscm_entries(path)
            if path.suffix.lower() == ".rscm"
            else parse_toml_entries(path)
        )
        for idx, table, key, value, _match in parsed:
            by_key[(table, key)].append((path, idx, value, text_lines[idx]))
    return by_key


def preferred_value(locations: list[tuple[Path, int, int, str]]) -> int:
    def sort_key(item: tuple[Path, int, int, str]) -> tuple[int, str, int]:
        path, idx, _value, _line = item
        toml_first = 0 if path.name == "gamevals.toml" else 1
        return (toml_first, path.as_posix().lower(), idx)

    return min(locations, key=sort_key)[2]


def plan_reassignments(
    by_key: dict[tuple[str, str], list[tuple[Path, int, int, str]]],
    max_ids: dict[str, int],
) -> dict[tuple[str, str], tuple[int, int, str]]:
    """Return (table, key) -> (old_id, new_id, reason)."""
    used: dict[str, set[int]] = defaultdict(set)
    claimed: dict[str, set[int]] = defaultdict(set)
    decisions: list[tuple[str, str, int | None, bool, str | None]] = []

    for table, key in sorted(by_key.keys(), key=lambda tk: (tk[0], tk[1])):
        locations = by_key[(table, key)]
        value = preferred_value(locations)
        floor = max_ids.get(table, -1) + 1

        if value == -1:
            decisions.append((table, key, None, True, "unassigned"))
        elif value < floor:
            decisions.append(
                (table, key, None, True, f"reserved (id {value} <= max {floor - 1})")
            )
        elif value in claimed[table]:
            decisions.append((table, key, None, True, f"duplicate id {value}"))
        else:
            claimed[table].add(value)
            used[table].add(value)
            decisions.append((table, key, value, False, None))

    plan: dict[tuple[str, str], tuple[int, int, str]] = {}
    targets: dict[tuple[str, str], int] = {}

    for table, key, keep_id, needs_assign, reason in decisions:
        table_key = (table, key)
        if not needs_assign:
            if keep_id is not None:
                targets[table_key] = keep_id
            continue

        floor = max_ids.get(table, -1) + 1
        new_id = next((i for i in range(MAX_ID, floor - 1, -1) if i not in used[table]), None)
        if new_id is None:
            raise SystemExit(f"No free IDs for table '{table}' in range [{floor}..{MAX_ID}]")
        used[table].add(new_id)
        claimed[table].add(new_id)
        targets[table_key] = new_id
        old_id = preferred_value(by_key[table_key])
        plan[table_key] = (old_id, new_id, reason or "unassigned")

    # Sync divergent copies of the same key to the chosen target.
    for table_key, locations in by_key.items():
        target = targets.get(table_key)
        if target is None:
            continue
        if any(value != target for _p, _i, value, _l in locations) and table_key not in plan:
            plan[table_key] = (locations[0][2], target, "sync divergent id")

    return plan


def rewrite_line(line: str, new_id: int, rscm: bool) -> str:
    if rscm:
        match = RSCM_KV_RE.match(line.strip())
        if not match:
            return line
        prefix = line[: len(line) - len(line.lstrip())]
        return f"{prefix}{match.group(1).rstrip()}={new_id}"
    match = KV_RE.match(line)
    if not match:
        return line
    return f"{match.group(1)}{match.group(2)}{match.group(3)}{new_id}{match.group(5)}"


def apply_plan(
    root: Path,
    by_key: dict[tuple[str, str], list[tuple[Path, int, int, str]]],
    plan: dict[tuple[str, str], tuple[int, int, str]],
    write: bool,
) -> list[Path]:
    file_lines: dict[Path, list[str]] = {}
    changed: set[Path] = set()

    for table_key, (_old, new_id, _reason) in plan.items():
        for path, idx, value, _line in by_key[table_key]:
            if value == new_id:
                continue
            if path not in file_lines:
                text = path.read_text(encoding="utf-8")
                file_lines[path] = text.splitlines()
                # Preserve final newline decision later via original text
            lines = file_lines[path]
            updated = rewrite_line(lines[idx], new_id, path.suffix.lower() == ".rscm")
            if updated != lines[idx]:
                lines[idx] = updated
                changed.add(path)

    if write:
        for path, lines in file_lines.items():
            if path not in changed:
                continue
            original = path.read_text(encoding="utf-8")
            ending = "\n" if original.endswith("\n") else ""
            path.write_text("\n".join(lines) + ending, encoding="utf-8")

    return sorted(changed, key=lambda p: p.as_posix().lower())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=None)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Report conflicts without writing files",
    )
    args = parser.parse_args()
    root = (args.root or repo_root()).resolve()

    max_ids = load_max_ids(root)
    by_key = collect_entries(root)
    plan = plan_reassignments(by_key, max_ids)

    if not plan:
        print("No gameval conflicts or unassigned IDs found.")
        return 0

    changed = apply_plan(root, by_key, plan, write=not args.check)
    print(f"Gameval reassignments ({len(plan)}):")
    for (table, key), (old_id, new_id, reason) in sorted(plan.items()):
        print(f"  {table}.{key}: {old_id} -> {new_id} ({reason})")
    print(f"{'Would update' if args.check else 'Updated'} {len(changed)} file(s):")
    for path in changed:
        print(f"  {path.relative_to(root).as_posix()}")
    return 2


if __name__ == "__main__":
    sys.exit(main())
