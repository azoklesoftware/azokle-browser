# Network Defense & DNS-over-HTTPS

The Azokle networking subsystem (`crates/networking`) enforces comprehensive transport-level security, leak mitigation, and DNS security.

---

## 1. DNS-over-HTTPS (DoH) Client (`crates/networking/src/dns.rs`)

Traditional UDP port 53 plaintext DNS queries are vulnerable to ISP eavesdropping, spoofing, and man-in-the-middle tampering. Azokle enforces DoH by default:
- **Zero Plaintext Fallback**: When enabled, the browser rejects plaintext port 53 queries entirely.
- **Trusted DoH Resolvers**: Pre-configured with trusted privacy-first resolvers:
  - Quad9 (`https://dns.quad9.net/dns-query`)
  - Cloudflare Privacy (`https://cloudflare-dns.com/dns-query`)
  - Mullvad DoH (`https://doh.mullvad.net/dns-query`)
- **DNSSEC Validation**: Verifies cryptographic signatures on DNS resource records to block cache-poisoning attacks.

---

## 2. Server-Side Request Forgery (SSRF) Firewall (`crates/networking/src/security.rs`)

To protect local network infrastructure and IoT devices from malicious web content executing unauthorized fetch commands, Azokle enforces an SSRF firewall:
- **Private & Loopback IP Blacklisting**: Blocks web content from initiating requests to:
  - `127.0.0.0/8` (IPv4 Loopback)
  - `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` (Private RFC 1918)
  - `169.254.0.0/16` (Link-Local / Cloud Metadata e.g. AWS/GCP instance metadata endpoints)
  - `::1` and `fc00::/7` (IPv6 Loopback and Unique Local)
- **CORS & Origin Hardening**: Strips `Origin` headers from non-cross-origin asset requests to prevent credential harvesting.

---

## 3. Scheme Downgrade & Mixed Content Defenses
- **HTTPS-Only Mode**: Automatically upgrades `http://` navigation attempts to `https://`. If a secure endpoint is unavailable, the user receives a high-contrast security warning before unencrypted communication is allowed.
- **Strict Transport Security (HSTS)**: Preloads HSTS directives and honors `includeSubDomains` and `preload` headers across all sessions.
