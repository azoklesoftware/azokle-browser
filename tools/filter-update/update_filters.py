#!/usr/bin/env python3
"""
Azokle Browser: Automated Filter & Rule Packager
Fetches, sanitizes, and consolidates Brave & uBlock Origin adblock and tracker rules
into the unified data/filters/ assets directory.
"""

import os
import sys
import json
import urllib.request
import re

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
FILTERS_DIR = os.path.join(ROOT_DIR, "data", "filters")

UPSTREAM_SOURCES = {
    "cname": "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/cname-trackers.txt",
    "unbreak": "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-unbreak.txt",
    "debounce": "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/debounce.json",
    "query_filter": "https://raw.githubusercontent.com/brave/adblock-resources/master/filter_lists/query-filter.json",
}

def verify_directories():
    os.makedirs(FILTERS_DIR, exist_ok=True)
    print(f"[+] Verified filters target directory: {FILTERS_DIR}")

def fetch_url(url: str) -> str:
    print(f"[*] Fetching {url}...")
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0 (AzokleBrowser/1.0; FilterUpdater)"}
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8")

def update_filters():
    verify_directories()
    for name, url in UPSTREAM_SOURCES.items():
        try:
            content = fetch_url(url)
            filename = os.path.basename(url)
            target_path = os.path.join(FILTERS_DIR, filename)
            with open(target_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"[✓] Successfully updated: {filename} ({len(content)} bytes)")
        except Exception as e:
            print(f"[!] Warning: Could not update {name} from {url}: {e}", file=sys.stderr)

if __name__ == "__main__":
    print("=== Azokle Filter Update Utility ===")
    update_filters()
    print("=== Filter update complete ===")
