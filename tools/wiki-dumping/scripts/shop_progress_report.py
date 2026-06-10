#!/usr/bin/env python3
"""Build markdown for the shop-mapping progress GitHub issue."""

from __future__ import annotations

import csv
import os
import sys
from dataclasses import dataclass
from pathlib import Path

MARKER = "<!-- shop-mapping-progress -->"
REPO_ROOT = Path(os.environ.get("REPO_ROOT", ".")).resolve()
MAPPINGS = REPO_ROOT / "tools/wiki-dumping/src/main/resources/shopmappings.csv"
SHOPS_DIR = REPO_ROOT / ".data/raw-cache/server/shops"


@dataclass
class ShopRow:
    inv: str
    slug: str
    wiki_article: str
    wiki_store: str = ""

    @property
    def has_inv(self) -> bool:
        return bool(self.inv) and self.inv != "-"

    @property
    def has_wiki(self) -> bool:
        return bool(strip_wiki(self.wiki_article))

    @property
    def wiki_title(self) -> str:
        return strip_wiki(self.wiki_article)


def strip_wiki(value: str) -> str:
    text = (value or "").strip()
    if text.startswith("[[") and text.endswith("]]"):
        text = text[2:-2]
    return text.strip()


def load_rows() -> list[ShopRow]:
    if not MAPPINGS.is_file():
        raise FileNotFoundError(f"missing mappings csv: {MAPPINGS}")

    rows: list[ShopRow] = []
    with MAPPINGS.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for raw in reader:
            rows.append(
                ShopRow(
                    inv=(raw.get("inv") or "").strip(),
                    slug=(raw.get("slug") or "").strip(),
                    wiki_article=(raw.get("wiki_article") or "").strip(),
                    wiki_store=(raw.get("wiki_store") or "").strip(),
                )
            )
    return rows


def load_dumped_invs() -> set[str]:
    if not SHOPS_DIR.is_dir():
        return set()
    return {path.stem for path in SHOPS_DIR.glob("*.toml")}


def wiki_table(rows: list[ShopRow], *, limit: int | None = 200) -> str:
    if not rows:
        return "_None._\n"

    lines = ["| Wiki article | Slug / notes |", "| --- | --- |"]
    shown = rows if limit is None else rows[:limit]
    for row in shown:
        wiki = row.wiki_title or "—"
        if row.has_inv:
            notes = f"`inv.{row.inv}` · `{row.slug}`"
        else:
            notes = f"`{row.slug}` · needs `inv.*` gameval"
        lines.append(f"| {wiki} | {notes} |")

    if limit is not None and len(rows) > limit:
        lines.append(f"\n_…and {len(rows) - limit} more._")
    return "\n".join(lines) + "\n"


def build_report() -> str:
    rows = load_rows()
    dumped_invs = load_dumped_invs()

    needs_inv = [row for row in rows if not row.has_inv]
    needs_wiki = [row for row in rows if row.has_inv and not row.has_wiki]
    mappable = [row for row in rows if row.has_inv and row.has_wiki]

    dumpable_total = len(mappable)
    dump_done = sum(1 for row in mappable if row.inv in dumped_invs)
    needs_dump = [row for row in mappable if row.inv not in dumped_invs]
    dump_remaining = len(needs_dump)

    mapping_done = len(mappable)
    mapping_total = len(rows)

    lines = [
        MARKER,
        "",
        "_Auto-updated by CI from `shopmappings.csv` and `.data/raw-cache/server/shops/`._",
        "",
        "## Summary",
        "",
        "| Metric | Count |",
        "| --- | ---: |",
        f"| Wiki shop rows tracked | {mapping_total} |",
        f"| Mapping complete (`inv` + wiki article) | {mapping_done} |",
        f"| **Needs matching `inv.*` gameval** | **{len(needs_inv)}** |",
        f"| Needs wiki article on mapped inv | {len(needs_wiki)} |",
        f"| Stock TOML dumped | {dump_done} |",
        f"| Stock dump still to do | {dump_remaining} |",
        "",
        f"**Mapping progress:** {mapping_done} / {mapping_total} rows have both an inv gameval and a wiki article.",
        "",
        f"**Dump progress:** {dump_done} / {dumpable_total} dumpable shops have stock TOML under `.data/raw-cache/server/shops/`.",
        "",
        "## What still needs doing",
        "",
        "### 1. Find the matching `inv.*` gameval",
        "",
        "These shops are documented on the OSRS wiki but our cache/gamevals do not have a matching inventory key yet. "
        "For each row below:",
        "",
        "1. Find the correct `inv.*` name in RS3/OSRS cache data (or add it to gamevals if missing).",
        "2. Add a row to `tools/wiki-dumping/src/main/resources/shopmappings.csv` with `inv`, `slug`, and `wiki_article`.",
        "3. Re-run `./gradlew :tools:wiki-dumping:mapShopNames` and `./gradlew :tools:wiki-dumping:dumpShops`.",
        "",
        "Until an inv exists, the dumper cannot write server shop TOML for that shop.",
        "",
        f"### Shops needing `inv.*` gameval ({len(needs_inv)})",
        "",
        wiki_table(needs_inv),
        "",
    ]

    if needs_wiki:
        lines += [
            f"### Mapped invs still missing a wiki article ({len(needs_wiki)})",
            "",
            "These have a gameval inv but no wiki link in `shopmappings.csv` yet. "
            "Add the `[[Wiki page title]]` manually or improve `mapShopNames` matching.",
            "",
            "| Inv | Slug |",
            "| --- | --- |",
        ]
        for row in sorted(needs_wiki, key=lambda r: r.inv):
            lines.append(f"| `{row.inv}` | `{row.slug}` |")
        lines.append("")

    if needs_dump:
        lines += [
            f"### Stock dump still pending ({len(needs_dump)})",
            "",
            "These rows have inv + wiki mapping but do not have a dumped TOML under `.data/raw-cache/server/shops/` yet.",
            "",
            "| Inv | Wiki article |",
            "| --- | --- |",
        ]
        for row in sorted(needs_dump, key=lambda r: r.inv):
            lines.append(f"| `{row.inv}` | {row.wiki_title} |")
        lines.append("")

    lines += [
        "## Commands",
        "",
        "```bash",
        "./gradlew :tools:wiki-dumping:mapShopNames",
        "./gradlew :tools:wiki-dumping:dumpShops",
        "```",
        "",
    ]

    return "\n".join(lines)


def main() -> int:
    output = Path(os.environ.get("OUTPUT", "")).resolve() if os.environ.get("OUTPUT") else None
    report = build_report()

    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(report, encoding="utf-8")
        print(f"wrote {output}")
    else:
        print(report)

    return 0


if __name__ == "__main__":
    sys.exit(main())
