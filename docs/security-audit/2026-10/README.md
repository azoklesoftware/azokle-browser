# Security Audit — October 2026 (commit cffca87)

Independent line-by-line security audit of the `development` branch at
commit `cffca87` ("resolve all 36 independent security audit findings").

## Deliverables

| File | Description |
|---|---|
| `Azokle-Browser-Security-Audit-Report.pdf` | Full 79-page typeset report (TOC, figures, tables) |
| `Azokle-Browser-Security-Audit-Report.md` | Markdown edition, identical content and finding registry |
| `azokle-audit-charts/` | Figures referenced by the Markdown edition (relative links) |

## Summary

- **68 findings**: 2 HIGH, 25 MEDIUM, 37 LOW, 4 INFO (CVSS v3.1 + CWE mapped)
- Scope: ~51,000 first-party lines — Rust core crates, vendored adblock engine,
  iOS Swift app, TypeScript UI + WebExtension, C++ engine shell, bindings,
  protocols, build & CI
- Headline items:
  - `AZK-F-001` (HIGH 7.6): filter-list CSS injection — `css-validation` feature
    disabled in the browser build of the vendored adblock engine
  - `AZK-F-055` (HIGH 7.0): GitHub Actions not SHA-pinned, including
    secrets-bearing release/fdroid workflows
  - 6 of the 36 previously "fixed" findings are incomplete or regressed
    (chapter 13 of the report)
- Verified clean: no known-vulnerable dependencies, no hardcoded secrets,
  vault crypto (Argon2id + ChaCha20-Poly1305) sound, Tor fail-closed behavior

## Remediation priority

- **P0 (release blockers)**: AZK-F-001, AZK-F-055 + related CI pinning
- **P1**: crafted `.dat` cache memory-safety cluster, privacy leaks
- **P2**: TLS in the network core, IPC envelope validation, duplicate-tree reconciliation
- **P3**: governance, documentation integrity, regression harness

Full detail, code evidence, exploit scenarios and per-finding remediations are
in the report; the roadmap is chapter 14.
