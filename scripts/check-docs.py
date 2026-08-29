#!/usr/bin/env python3
"""Validate documentation governance rules for OMS.

Checks:
  1. Internal Markdown link integrity across all docs + root Markdown files.
  2. Required 5-line metadata block on every file under docs/current/** and docs/ops/**.

Exit code 0 = clean, 1 = violations found. Docs-only tooling, no external deps.
"""
from __future__ import annotations

import os
import re
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

LINK_RE = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
REQUIRED_META = ["**Status:**", "**Audience:**", "**Owner:**", "**Last verified:**", "**Source of truth:**"]
PLACEHOLDER_RE = re.compile(r"\[(?:DATE|YYYY-MM-DD|TBD|GOAL|DESCRIPTION)\]")

ROOT_MD = ["README.md", "ONBOARDING.md", "CONTRIBUTING.md", "CHANGELOG.md"]
DOC_DIRS = ["docs"]
META_REQUIRED_PREFIXES = [
    os.path.join("docs", "current") + os.sep,
    os.path.join("docs", "ops") + os.sep,
]
# Template files intentionally contain placeholder metadata; skip strict checks.
SKIP_META = {os.path.join("docs", "_templates")}


def collect_md_files() -> list[str]:
    files: list[str] = []
    for name in ROOT_MD:
        p = os.path.join(REPO_ROOT, name)
        if os.path.isfile(p):
            files.append(p)
    for d in DOC_DIRS:
        base = os.path.join(REPO_ROOT, d)
        for dp, _, fs in os.walk(base):
            for fn in fs:
                if fn.endswith(".md"):
                    files.append(os.path.join(dp, fn))
    return files


def check_links(files: list[str]) -> list[str]:
    errors: list[str] = []
    for path in files:
        base = os.path.dirname(path)
        with open(path, encoding="utf-8") as f:
            for lineno, line in enumerate(f, 1):
                for m in LINK_RE.finditer(line):
                    target = m.group(1).split("#")[0].strip()
                    if not target or target.startswith(("http://", "https://", "mailto:")):
                        continue
                    resolved = os.path.normpath(os.path.join(base, target))
                    if not os.path.exists(resolved):
                        rel = os.path.relpath(path, REPO_ROOT)
                        errors.append(f"{rel}:{lineno} broken link -> {target}")
    return errors


def check_metadata(files: list[str]) -> list[str]:
    errors: list[str] = []
    for path in files:
        rel = os.path.relpath(path, REPO_ROOT)
        if any(rel.startswith(skip) for skip in SKIP_META):
            continue
        if not any(rel.startswith(prefix) for prefix in META_REQUIRED_PREFIXES):
            continue
        with open(path, encoding="utf-8") as f:
            head = f.read(2000)
        missing = [key for key in REQUIRED_META if key not in head]
        if missing:
            errors.append(f"{rel} missing metadata: {', '.join(missing)}")
    return errors


def check_placeholders(files: list[str]) -> list[str]:
    errors: list[str] = []
    for path in files:
        rel = os.path.relpath(path, REPO_ROOT)
        # Templates legitimately contain placeholder-style tokens.
        if any(rel.startswith(skip) for skip in SKIP_META):
            continue
        with open(path, encoding="utf-8") as f:
            for lineno, line in enumerate(f, 1):
                if PLACEHOLDER_RE.search(line):
                    errors.append(f"{rel}:{lineno} unfilled placeholder -> {line.strip()}")
    return errors


def main() -> int:
    files = collect_md_files()
    link_errors = check_links(files)
    meta_errors = check_metadata(files)
    placeholder_errors = check_placeholders(files)

    if link_errors:
        print("Broken internal links:")
        for e in link_errors:
            print(f"  - {e}")
    else:
        print("Link check: OK (no broken internal links)")

    if meta_errors:
        print("\nMetadata violations:")
        for e in meta_errors:
            print(f"  - {e}")
    else:
        print("Metadata check: OK (all current/ops docs have required metadata)")

    if placeholder_errors:
        print("\nUnfilled placeholders:")
        for e in placeholder_errors:
            print(f"  - {e}")
    else:
        print("Placeholder check: OK (no unfilled [DATE]/[YYYY-MM-DD]/[TBD] tokens)")

    total = len(link_errors) + len(meta_errors) + len(placeholder_errors)
    if total:
        print(
            f"\nFAILED: {len(link_errors)} broken link(s), "
            f"{len(meta_errors)} metadata violation(s), "
            f"{len(placeholder_errors)} placeholder(s)"
        )
        return 1
    print("\nPASSED: documentation governance checks clean")
    return 0


if __name__ == "__main__":
    sys.exit(main())
