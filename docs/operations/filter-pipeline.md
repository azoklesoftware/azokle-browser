# Filter List Maintenance & Pipeline

Azokle Browser bundles and dynamically updates high-efficiency adblock and tracking protection filter lists stored in `data/filters/`.

---

## 1. Bundled Filter Lists

| File | Purpose | Source Syntax |
| :--- | :--- | :--- |
| `data/filters/filters.txt` | Core network request blocking rules | uBlock / EasyList / EasyPrivacy |
| `data/filters/cname.txt` | Known CNAME cloaked tracker mapping | Domain alias dictionary |
| `data/filters/cookies.txt` | Cookie consent banner & auto-dismiss rules | EasyList Cookie / Fanboy Annoyances |
| `data/filters/unbreak.txt` | Exception rules ensuring functional web compatibility | uBlock Unbreak rules |
| `data/filters/debounce.json` | Bounce tracking redirect unraveling definitions | Structured JSON rules |
| `data/filters/query-filter.json` | Known tracking URL parameters to strip | Query parameter token list |
| `data/filters/resources.json` | WebExtension surrogate scriptlet registry | Scriptlet stub mapping |

---

## 2. Automated Filter Update Tool

The repository provides an automated Python maintenance script located at:
`tools/filter-update/update_filters.py`

### Verifying Filter Pack Integrity
```bash
python3 tools/filter-update/update_filters.py --verify
```

### Running the Updater
```bash
python3 tools/filter-update/update_filters.py --update --allow-unpinned
```

### Verification & Regression Checks
After updating filter lists, always run the matcher regression test suite:
```bash
cargo test -p azokle-adblock
cargo test -p azokle-tracker-block
```
This ensures that new filter rules do not break critical site logins or degrade matching throughput.
