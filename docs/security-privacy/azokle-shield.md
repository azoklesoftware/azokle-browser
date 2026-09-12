# Azokle Shield Architecture

**Azokle Shield** is the native privacy engine powering ad blocking, tracker defusal, and anti-fingerprinting across all platforms. Implemented in `crates/privacy/adblock` and `crates/privacy/tracker-block`, it processes network requests in microseconds without degrading page render performance.

---

## Shield Capabilities Overview

```
+---------------------------------------------------------------------------------+
|                                 AZOKLE SHIELD                                   |
|                                                                                 |
|  1. Network Request Blocker      -> uBlock / Brave syntax filter lists          |
|  2. Cosmetic Element Hiding      -> Injected CSS rules matching ad containers   |
|  3. Scriptlet Injection          -> Neutralizing defusers (defuser.js, etc.)    |
|  4. CNAME Uncloaking             -> Defeating third-party alias redirection     |
|  5. Bounce Tracking Unwrapper    -> Stripping intermediate tracking hops        |
|  6. URL Query Stripper           -> Removing tracking parameters (utm_*, etc.)  |
|  7. Fingerprint Defenses         -> Randomizing canvas, audio, and font hashes  |
+---------------------------------------------------------------------------------+
```

---

## 1. Network Filter Engine (`crates/privacy/adblock`)
- **Compatibility**: Parses standard EasyList, EasyPrivacy, and uBlock Origin rule syntax (e.g. `||tracker.com^$third-party,script`).
- **Rule Categorization**:
  - `BlockCategory::Ad`: Commercial banner, video, and interstitial ad scripts.
  - `BlockCategory::Tracker`: Analytics beacons, audience tracking, and profiling scripts.
  - `BlockCategory::Telemetry`: Product telemetry and user session recording scripts.
  - `BlockCategory::Fingerprinting`: Canvas, WebGL, and battery status fingerprint probes.
  - `BlockCategory::Cryptomining`: In-browser cryptocurrency mining WebAssembly blobs.
  - `BlockCategory::Annoyance`: Cookie consent banners, paywalls, and social widgets.

---

## 2. Cosmetic Element Hiding & Scriptlet Injection
- **Cosmetic CSS**: Injects stylesheet rules containing element selectors (e.g. `div[class*="-ad-"]`) directly into the DOM tree, suppressing visible placeholder elements left behind by blocked network assets.
- **Defuser Scriptlets**: Located in `extensions/azokle-shield/scriptlets/`:
  - `autoconsent.js`: Automatically dismisses cookie and GDPR consent modals with minimum tracking choices.
  - `defuser.js`: Overwrites tracking APIs (`window.ga`, `window._fbq`) with dummy no-op proxies, preventing page crash bugs when tracking libraries fail to load.
  - `fingerprint_shield.js`: Adds non-deterministic micro-noise to canvas context reads and audio frequency tables.

---

## 3. Bounce Tracking & Query Stripping (`crates/privacy/tracker-block`)
- **Bounce Unwrapping**: Intercepts redirect hops (e.g. `https://tracker.com/link?dest=https%3A%2F%2Ftarget.com`) and navigates directly to the target destination without touching the tracking server.
- **Query Parameter Stripping**: Strips over 80 known tracking parameters from destination URLs before dispatching the request:
  - Google: `gclid`, `gclsrc`, `dclid`, `gad_source`
  - Facebook/Meta: `fbclid`
  - Microsoft: `msclkid`
  - Marketing & Analytics: `utm_source`, `utm_medium`, `utm_campaign`, `utm_term`, `utm_content`, `mc_eid`, `igshid`, `yclid`
