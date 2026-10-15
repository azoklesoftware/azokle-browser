#!/usr/bin/env python3
"""
Azokle Browser: Automated Filter & Rule Packager & Integrity Verifier
Remediates AZK-MED-13: Verifies SHA-256 digests, enforces minimum size thresholds,
validates syntax/structure, and prevents hostile or corrupted filter packs from
being written to the data/filters/ directory.
"""

import os
import sys
import json
import hashlib
import argparse
import urllib.request
import re

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
FILTERS_DIR = os.path.join(ROOT_DIR, "data", "filters")
CHECKSUM_FILE = os.path.join(FILTERS_DIR, "checksums.sha256")

# Pinned SHA-256 digests for bundled filter releases (AZK-MED-13)
PINNED_DIGESTS = {
    "cname.txt": "62686ce875274a636b8e8b40fb05d64ce6f49e9b1ab77099877ba7d140aebac4",
    "cookies.txt": "4f7d8da9bed01c53728aa3a1c3170c021fc043ff3d10955e248c9b21817ce60f",
    "debounce.json": "fb2396985a78983c8f88ee701100c5cb4681731239ac6624e86da1212645dbf8",
    "filters.txt": "de1a1e65df145ec9a619e28e527217f76a32396b459907cbb0afc3b4197ed615",
    "query-filter.json": "c7b7d0292dd5c1e0f00f7ff27d52621ae07f8dc6e76fc2fe8cebb4b92d614a0d",
    "resources.json": "d085eb0754b7a54d9e1a18a0314b1946b066f61a15457de834d98beb53b4e40f",
    "unbreak.txt": "f76e83a6a4cd71197f34ecb53af78c0614143fc3ed422d856150725516e10a91",
}

# Minimum byte size boundaries to reject truncated or corrupted downloads
MINIMUM_SIZES = {
    "cname.txt": 100_000,
    "cookies.txt": 10_000,
    "debounce.json": 5_000,
    "filters.txt": 500_000,
    "query-filter.json": 1_000,
    "resources.json": 100_000,
    "unbreak.txt": 50_000,
}

UPSTREAM_SOURCES = {
    "cname.txt": "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/cname-trackers.txt",
    "unbreak.txt": "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-unbreak.txt",
    "debounce.json": "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/debounce.json",
    "query-filter.json": "https://raw.githubusercontent.com/brave/adblock-resources/master/filter_lists/query-filter.json",
}

def compute_sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def validate_structure(filename: str, data: bytes) -> bool:
    """Validates that content is syntactically sound before writing to disk."""
    if len(data) < MINIMUM_SIZES.get(filename, 500):
        print(f"[!] Validation failed: {filename} size {len(data)} bytes is below minimum {MINIMUM_SIZES.get(filename, 500)}", file=sys.stderr)
        return False

    if filename.endswith(".json"):
        try:
            parsed = json.loads(data.decode("utf-8"))
            if not isinstance(parsed, (dict, list)) or len(parsed) == 0:
                print(f"[!] Validation failed: {filename} parsed as empty JSON structure", file=sys.stderr)
                return False
        except Exception as e:
            print(f"[!] Validation failed: {filename} invalid JSON: {e}", file=sys.stderr)
            return False
    elif filename.endswith(".txt"):
        try:
            text = data.decode("utf-8")
            lines = [line.strip() for line in text.splitlines() if line.strip() and not line.startswith("!")]
            if len(lines) < 10:
                print(f"[!] Validation failed: {filename} contains fewer than 10 valid rule lines", file=sys.stderr)
                return False
        except UnicodeDecodeError as e:
            print(f"[!] Validation failed: {filename} is not valid UTF-8: {e}", file=sys.stderr)
            return False

    return True

def verify_filters(fail_fast: bool = True) -> bool:
    """Verifies all filter lists in data/filters against pinned digests."""
    print(f"[*] Verifying filter integrity in: {FILTERS_DIR}")
    all_ok = True

    for filename, expected_hash in PINNED_DIGESTS.items():
        file_path = os.path.join(FILTERS_DIR, filename)
        if not os.path.isfile(file_path):
            print(f"[✗] Missing filter pack: {filename}", file=sys.stderr)
            all_ok = False
            continue

        try:
            with open(file_path, "rb") as f:
                content = f.read()

            if not validate_structure(filename, content):
                all_ok = False
                continue

            actual_hash = compute_sha256(content)
            if actual_hash != expected_hash:
                print(f"[✗] SHA-256 MISMATCH for {filename}!", file=sys.stderr)
                print(f"    Expected: {expected_hash}", file=sys.stderr)
                print(f"    Actual:   {actual_hash}", file=sys.stderr)
                all_ok = False
            else:
                print(f"[✓] {filename}: OK ({len(content)} bytes, {actual_hash[:16]}...)")
        except Exception as e:
            print(f"[✗] Error reading {filename}: {e}", file=sys.stderr)
            all_ok = False

    if not all_ok and fail_fast:
        sys.exit(1)
    return all_ok

def fetch_url(url: str) -> bytes:
    print(f"[*] Fetching {url}...")
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0 (AzokleBrowser/2.0; FilterUpdater/SecurityEnforced)"}
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read()

def update_filters(allow_unpinned: bool = False):
    """Fetches upstream filter updates, enforces integrity/size, and atomically writes."""
    os.makedirs(FILTERS_DIR, exist_ok=True)
    updated_hashes = {}

    for target_name, url in UPSTREAM_SOURCES.items():
        try:
            content = fetch_url(url)
            if not validate_structure(target_name, content):
                print(f"[!] Aborting update for {target_name} due to validation failure", file=sys.stderr)
                continue

            digest = compute_sha256(content)
            expected = PINNED_DIGESTS.get(target_name)

            if expected and digest != expected and not allow_unpinned:
                print(f"[!] Upstream content hash changed for {target_name}:", file=sys.stderr)
                print(f"    Old pinned: {expected}", file=sys.stderr)
                print(f"    New digest: {digest}", file=sys.stderr)
                print(f"    Pass --allow-unpinned to accept new upstream revisions.", file=sys.stderr)
                continue

            # Atomic write
            target_path = os.path.join(FILTERS_DIR, target_name)
            tmp_path = target_path + ".tmp"
            with open(tmp_path, "wb") as f:
                f.write(content)
            os.replace(tmp_path, target_path)

            updated_hashes[target_name] = digest
            print(f"[✓] Successfully updated: {target_name} ({len(content)} bytes, sha256={digest[:16]}...)")
        except Exception as e:
            print(f"[!] Warning: Could not update {target_name} from {url}: {e}", file=sys.stderr)

    # Regenerate checksums file
    if updated_hashes:
        print("[*] Re-verifying all filter packs...")
        verify_filters(fail_fast=False)

def main():
    parser = argparse.ArgumentParser(description="Azokle Filter Pipeline & Integrity Verifier (AZK-MED-13)")
    parser.add_argument("--verify", action="store_true", default=False, help="Verify integrity of all local filter packs against pinned digests")
    parser.add_argument("--update", action="store_true", default=False, help="Fetch and apply upstream updates with size & structure validation")
    parser.add_argument("--allow-unpinned", action="store_true", default=False, help="Permit new upstream digests during update")

    args = parser.parse_args()

    print("=== Azokle Filter Pipeline & Integrity Verifier ===")
    if args.update:
        update_filters(allow_unpinned=args.allow_unpinned)
    else:
        # Default behavior: verify integrity
        if verify_filters(fail_fast=True):
            print("=== All filter lists cryptographically verified and intact ===")

if __name__ == "__main__":
    main()
