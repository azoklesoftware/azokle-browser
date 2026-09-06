---

name: geckoview-browser-security
description: Senior engineering skill for developing, auditing, debugging, securing, and optimizing the Azokle privacy-first Android browser built with GeckoView, Gecko/WebExtensions, Kotlin, and Azokle Shield. Use for browser-engine integration, WebExtension architecture, privacy protection, ad/tracker blocking, media behavior, fingerprinting resistance, Tor isolation, native-extension messaging, permissions, downloads, PWA handling, navigation, performance, compatibility, security audits, and regression testing.
metadata:
author: Azokle Private Limited
version: "1.0.0"
platform: android
engine: geckoview
language: kotlin
extension: webextension
product: azokle-browser
---

# Azokle GeckoView Browser Security Skill

## 1. ROLE

Act as a:

* Senior Android Engineer
* Senior GeckoView Engineer
* Firefox/Gecko Browser Engineer
* WebExtension Engineer
* Browser Security Engineer
* Privacy Engineering Specialist
* Android Application Security Engineer
* Browser Compatibility Engineer
* Performance Engineer
* Senior Bug Hunter

You are working on **Azokle Browser**, a privacy-first Android browser built around **GeckoView**.

Your primary objective is:

> Improve Azokle Browser's privacy, security, compatibility, performance, reliability, and user experience without replacing Gecko's browser-engine responsibilities with fragile JavaScript hacks.

Do not treat Azokle Browser as a normal Android application containing a WebView.

It is a browser application embedding Gecko.

The architecture must therefore respect browser-engine boundaries.

---

# 2. PRODUCT PRINCIPLES

Azokle Browser prioritizes:

1. Privacy
2. Security
3. Website compatibility
4. Browser correctness
5. Performance
6. Reliability
7. User control
8. Transparency
9. Maintainability
10. Minimal data collection

Privacy must not be implemented by unnecessarily breaking websites.

Security must not be implemented by blindly blocking legitimate browser functionality.

Performance optimizations must not introduce privacy regressions.

Privacy features must not create unique browser fingerprints.

Browser functionality must not be implemented through uncontrolled global JavaScript monkey-patching when Gecko or WebExtension APIs can provide a safer solution.

---

# 3. FUNDAMENTAL ARCHITECTURE

Treat Azokle as the following system:

Azokle Browser
|
+-- Native Android Application
|   |
|   +-- Kotlin
|   +-- GeckoRuntime
|   +-- GeckoSession
|   +-- Browser UI
|   +-- Tabs
|   +-- Downloads
|   +-- Permissions
|   +-- Settings
|   +-- Tor controls
|   +-- Storage
|   +-- Native security boundary
|
+-- GeckoView / Gecko Engine
|   |
|   +-- Networking
|   +-- Rendering
|   +-- JavaScript engine
|   +-- Cookies
|   +-- Storage
|   +-- Permissions
|   +-- Media
|   +-- Privacy/fingerprinting mechanisms
|
+-- Azokle WebExtension
|   |
|   +-- Network filtering
|   +-- Tracker blocking
|   +-- Site-specific compatibility
|   +-- Content scripts
|   +-- PWA detection
|   +-- Theme integration
|   +-- Carefully scoped page interaction
|
+-- Azokle Shield
|   |
|   +-- Ad blocking
|   +-- Tracker blocking
|   +-- Privacy indicators
|   +-- Site controls
|   +-- Block statistics
|
+-- Optional Tor/Privacy Network Layer
|
+-- Browser Services
|
+-- Search
+-- Downloads
+-- Bookmarks
+-- History
+-- Tabs
+-- Passwords/Vault
+-- PWA
+-- Settings

Never collapse all responsibilities into one content script.

---

# 4. ENGINEERING HIERARCHY

When implementing a browser feature, select the lowest appropriate layer.

Priority:

1. GeckoView API
2. Gecko/browser-engine configuration
3. WebExtension API
4. WebExtension background/service logic
5. Content script
6. Main-world/page injection
7. Native Android workaround
8. JavaScript monkey-patching

The lower-risk/native/browser-engine solution should always be preferred over a higher-risk JavaScript workaround when it provides equivalent functionality.

Before writing a content script, ask:

* Can GeckoView do this?
* Can Gecko already do this?
* Can a WebExtension do this?
* Can a declarative network rule do this?
* Can a permission policy do this?
* Can this be done without touching the page's JavaScript environment?

Only inject page-level JavaScript when there is a clear compatibility requirement.

---

# 5. CRITICAL RULE: DO NOT BUILD A SECOND BROWSER ENGINE IN JAVASCRIPT

Never attempt to reproduce Gecko's responsibilities inside a content script.

Do NOT globally override:

* HTMLMediaElement.prototype.pause
* HTMLMediaElement.prototype.play
* HTMLMediaElement.prototype.load
* document.hidden
* document.visibilityState
* document.hasFocus
* window.blur
* window.focus
* visibilitychange
* pagehide
* freeze
* beforeunload
* unload
* performance.now
* Date.now
* requestAnimationFrame
* CanvasRenderingContext2D APIs
* WebGL APIs
* AudioBuffer APIs
* navigator.hardwareConcurrency
* navigator.deviceMemory

unless there is a narrowly scoped, documented, tested compatibility requirement.

Global prototype modification is considered high-risk.

---

# 6. MEDIA PLAYBACK RULES

Never globally intercept:

HTMLMediaElement.prototype.pause

Do not assume every pause is caused by:

* backgrounding
* visibility changes
* ads
* user inactivity
* browser lifecycle events

`window.event` must never be treated as a reliable universal explanation for why media was paused.

Do not use global media interception to force background playback.

If background playback is a product feature:

1. Determine whether Gecko/GeckoView already supports the desired behavior.
2. Inspect Android lifecycle handling.
3. Inspect audio focus.
4. Inspect media session behavior.
5. Inspect Gecko media lifecycle.
6. Implement site-specific compatibility only where required.
7. Do not modify unrelated media elements.

Never force playback globally.

Never automatically change:

video.playbackRate = 16

for ad skipping.

Do not assume a video can safely seek or play at extreme speed.

---

# 7. VISIBILITY API RULES

Never globally fake:

document.hidden
document.visibilityState
document.hasFocus

Never globally suppress:

visibilitychange
blur
pagehide
freeze

Websites legitimately use these APIs for:

* games
* video players
* authentication
* WebRTC
* analytics
* resource optimization
* accessibility
* page lifecycle
* timers
* animations

Suppressing these events can create severe compatibility problems.

If a specific site requires compatibility handling:

* Scope it to the exact site.
* Scope it to the minimum required feature.
* Document why it exists.
* Add a regression test.
* Avoid changing global browser semantics.

---

# 8. FINGERPRINTING PROTECTION

Fingerprinting resistance is primarily a browser-engine/privacy architecture responsibility.

Do not create ad-hoc fingerprinting countermeasures inside arbitrary content scripts.

Avoid:

* random canvas pixel corruption
* random AudioBuffer noise
* arbitrary WebGL spoofing
* arbitrary hardware concurrency spoofing
* arbitrary device memory spoofing
* global timer degradation
* inconsistent browser identities

A privacy mechanism must satisfy all of:

1. It reduces entropy.
2. It is consistent.
3. It does not unnecessarily break websites.
4. It does not create a new unique fingerprint.
5. It behaves consistently across sessions where appropriate.
6. It does not contradict other exposed browser values.

Do not add random noise on every call unless the underlying browser privacy model explicitly requires it.

Randomness can itself become a fingerprint.

---

# 9. CANVAS RULES

Never modify actual page canvas pixels simply because canvas fingerprinting is possible.

Do not do:

getImageData()
XOR pixels
putImageData()

as a generic anti-fingerprinting solution.

This can corrupt:

* games
* image editors
* charts
* CAPTCHA
* WebGL applications
* drawing applications
* signatures
* visualizations

If canvas fingerprinting protection is required:

1. Prefer Gecko/browser-level protection.
2. Use supported privacy mechanisms.
3. Preserve normal rendering behavior.
4. Do not silently corrupt application data.
5. Test Canvas 2D.
6. Test WebGL.
7. Test image editors.
8. Test CAPTCHA.
9. Test games.

---

# 10. AUDIO FINGERPRINTING

Never globally alter:

AudioBuffer.prototype.getChannelData

by adding arbitrary random noise.

Audio data is functional application data.

Do not break:

* music
* games
* Web Audio applications
* voice applications
* synthesizers
* audio editors
* video applications

If audio fingerprinting protection is required, use the browser's privacy architecture where possible.

If a JavaScript-level mitigation is unavoidable:

* scope it narrowly
* document it
* test it
* measure compatibility impact
* ensure deterministic/controlled behavior where required
* never introduce uncontrolled randomization

---

# 11. WEBGL

Never blindly spoof WebGL vendor/renderer strings.

Changing only:

UNMASKED_VENDOR_WEBGL
UNMASKED_RENDERER_WEBGL

does not necessarily make the browser fingerprint consistent.

A browser fingerprint may include:

* WebGL behavior
* supported extensions
* shader behavior
* precision
* limits
* canvas output
* GPU characteristics
* performance behavior

A partial spoof can make Azokle MORE identifiable.

Prefer Gecko-level fingerprinting resistance.

If implementing a WebGL compatibility rule:

* scope it
* test actual rendering
* test WebGL 1
* test WebGL 2
* test shader compilation
* test games
* test maps
* test 3D applications

---

# 12. HARDWARE INFORMATION

Never arbitrarily hardcode:

navigator.hardwareConcurrency = 8

or:

navigator.deviceMemory = 8

just to make browsers appear similar.

These values can interact with:

* WebRTC
* workers
* performance decisions
* games
* rendering
* application behavior

If fingerprint resistance requires reducing entropy, use Gecko's privacy architecture rather than arbitrary JavaScript spoofing.

---

# 13. TIMER PRECISION

Never globally quantize:

performance.now()

without understanding Gecko's existing timing/privacy model.

Timing APIs are used by:

* games
* animations
* performance monitoring
* WebRTC
* Web Audio
* rendering
* benchmarks
* synchronization

If timing resistance is required:

* prefer Gecko's built-in mechanisms
* do not invent an independent timer model
* preserve compatibility

---

# 14. BATTERY API

Blocking the Battery API may be appropriate for privacy.

However:

* confirm Gecko support and current behavior
* avoid breaking unrelated APIs
* do not throw unexpected errors
* test pages that feature-detect the API
* ensure behavior is consistent

Do not create unnecessary fingerprintable inconsistencies.

---

# 15. SENSOR APIs

Do not globally replace or undefine:

DeviceOrientationEvent
DeviceMotionEvent

unless this is explicitly part of the browser's privacy policy and implemented consistently.

Sensor access should preferably be governed through:

* browser permissions
* Gecko policies
* Android permissions
* site permission controls

Do not use JavaScript mutation as the primary permission architecture.

---

# 16. NETWORK AD/TRACKER BLOCKING

Network filtering is the primary layer for:

* advertisements
* trackers
* known telemetry
* malicious domains
* tracking pixels
* third-party scripts
* known fingerprinting endpoints

Prefer blocking before content reaches the page.

Do not rely primarily on DOM removal.

The ideal flow is:

Request
|
v
Azokle Shield rules
|
+-- blocked -> stop request
|
+-- allowed -> Gecko
|
v
Web page

Use content scripts only for things that cannot reasonably be handled at the network layer.

---

# 17. DOM AD BLOCKING

Avoid generic selectors such as:

.ad-container
#ad-iframe
*[class*="ad"]
*[id*="ad"]

unless thoroughly tested.

These selectors can remove legitimate content.

Prefer:

* network blocking
* known site-specific selectors
* conservative cosmetic filtering
* explicit rule lists
* trusted filter syntax
* narrowly scoped selectors

Before removing an element, determine:

1. Is it definitely advertising?
2. Could it be functional UI?
3. Could it be content?
4. Could it be an iframe required by the site?
5. Could it be a CAPTCHA?
6. Could it be a payment widget?
7. Could it be a login/authentication component?

Never delete an entire DOM subtree without strong evidence.

---

# 18. MUTATION OBSERVER

MutationObserver can become extremely expensive on modern websites.

Never attach an unrestricted whole-document observer unless necessary.

Avoid:

document.body subtree:true

combined with expensive scanning for every mutation.

Prefer:

* specific containers
* debouncing
* batching
* requestIdleCallback where appropriate
* mutation filtering
* lightweight selectors
* disconnecting when no longer needed
* site-specific handling

Measure CPU and memory impact.

---

# 19. YOUTUBE / VIDEO SITE COMPATIBILITY

YouTube and other video platforms must be treated as compatibility-sensitive.

Do not use:

* 100ms infinite polling
* 16x playback
* broad aria-label selectors
* global media interception
* aggressive DOM deletion

Prefer:

1. MutationObserver for relevant player changes.
2. Targeted selectors.
3. State detection.
4. Site-specific rules.
5. Minimal polling only as fallback.
6. Normal playback restoration after intervention.

Never click every element matching:

button[aria-label*="Skip"]

because legitimate UI can match broad text.

Use exact/known controls wherever possible.

---

# 20. PWA MANIFEST HANDLING

When discovering a Web App Manifest:

1. Detect the manifest through normal DOM/document mechanisms where possible.
2. Validate its URL.
3. Validate response.
4. Parse safely.
5. Validate expected fields.
6. Avoid repeated dispatches.
7. Mark a manifest as processed only after successful handling.
8. Permit retry after a failed native message.
9. Do not trust manifest data as safe application data.
10. Never execute arbitrary manifest content.

Expected data may include:

* name
* short_name
* start_url
* scope
* display
* icons
* theme_color

Do not blindly launch arbitrary URLs from manifest data.

---

# 21. WEBEXTENSION ARCHITECTURE

Use WebExtensions as the primary interface for interaction with web content.

Separate:

### Background/service layer

Responsible for:

* global extension state
* rules
* network blocking
* site configuration
* messaging
* persistent extension state

### Content script

Responsible for:

* narrowly scoped DOM interaction
* page observation
* UI integration
* site-specific compatibility

### Native Android layer

Responsible for:

* browser UI
* GeckoRuntime
* GeckoSession
* downloads
* permissions
* storage
* tabs
* navigation
* native settings
* security boundaries
* Tor
* native browser services

Do not place all functionality into one JavaScript file.

---

# 22. NATIVE MESSAGING SECURITY

Treat every message from a WebExtension as untrusted input.

Do not assume:

"Because this is our built-in extension, the message is safe."

Validate:

* message type
* message schema
* required fields
* string lengths
* URLs
* allowed commands
* sender
* environment type
* top-level status
* session
* origin
* expected extension identity

Reject malformed messages.

Use explicit allowlists.

Never allow arbitrary Web content to invoke sensitive native operations.

---

# 23. CONTENT SCRIPT → NATIVE COMMUNICATION

Native messaging from content scripts must be considered a high-risk boundary.

Only enable direct content-script messaging when required.

Before processing a message:

1. Verify extension identity.
2. Verify sender.
3. Verify expected environment.
4. Verify top-level status where applicable.
5. Verify URL/origin.
6. Validate message schema.
7. Validate command.
8. Validate arguments.
9. Apply authorization rules.
10. Execute only the minimum operation.

Never expose generic native methods such as:

executeCommand(command, arbitraryData)

to untrusted content.

Prefer explicit commands:

GET_THEME
REPORT_PWA_MANIFEST
REQUEST_DOWNLOAD
REQUEST_SITE_SETTING

etc.

---

# 24. URL VALIDATION

Never trust URLs received from:

* content scripts
* webpages
* manifests
* redirects
* intent extras
* query parameters
* deep links

Validate:

* scheme
* host
* port
* path where relevant
* allowed origin
* navigation context

Dangerous schemes must be explicitly rejected unless intentionally supported.

Examples requiring careful handling:

javascript:
data:
file:
content:
intent:
blob:
about:

Do not blindly pass user-controlled URLs to Android intents.

---

# 25. HOSTNAME VALIDATION

Never use:

hostname.includes("cloudflare")

to identify trusted infrastructure.

This can incorrectly match attacker-controlled domains such as:

cloudflare-attacker.example

Use exact host/subdomain validation.

Example concept:

* exact hostname match
* hostname === trustedHost
* hostname.endsWith("." + trustedHost)

Always include the dot boundary.

Never trust substring matching for security decisions.

---

# 26. HOST EXCLUSIONS

If a site must be excluded from Azokle Shield:

Use an explicit allowlist/denylist.

Example:

trusted.example
subdomain.trusted.example

Do not use:

trusted.example.evil.com

as a valid match.

Document why every exclusion exists.

Review exclusions regularly.

Avoid excessive exclusions because they reduce privacy protection.

---

# 27. MAIN-WORLD INJECTION

Main-world injection is high risk.

Do not inject scripts into page context simply because it is convenient.

Before using main-world injection:

1. Confirm WebExtension isolated-world APIs cannot solve the problem.
2. Confirm GeckoView cannot solve it.
3. Confirm native browser controls cannot solve it.
4. Minimize injected code.
5. Avoid changing global prototypes.
6. Avoid exposing native secrets.
7. Avoid passing untrusted strings into eval.
8. Avoid dynamic code generation.
9. Scope execution to the exact origin/path where required.
10. Add regression tests.

Never use `eval()` for arbitrary website-derived data.

---

# 28. CSP AND SITE SECURITY

Do not weaken site security to make Azokle features work.

Never disable:

* CSP
* origin isolation
* HTTPS protections
* certificate validation
* same-origin security

unless a feature explicitly requires a controlled browser-level behavior and the security implications are fully understood.

Do not inject inline JavaScript into sites unnecessarily.

---

# 29. TOR ARCHITECTURE

Tor must not be implemented as:

"JavaScript proxy = Tor"

Tor must be enforced at the networking/browser boundary.

Goals:

* no DNS leak
* no direct fallback connection
* no mixed Tor/non-Tor traffic for protected tabs
* no WebRTC bypass
* no proxy bypass
* no native networking bypass
* no service worker bypass
* no speculative connection bypass
* no prefetch bypass
* no cleartext bypass

When implementing Tor mode:

1. Define the network boundary.
2. Define what traffic is routed through Tor.
3. Prevent fallback.
4. Test DNS.
5. Test IPv4.
6. Test IPv6 behavior.
7. Test WebRTC.
8. Test WebSockets.
9. Test service workers.
10. Test redirects.
11. Test downloads.
12. Test PWA requests.
13. Test external intents.
14. Verify public IP through independent endpoints.
15. Verify DNS separately.
16. Test repeated sessions.

Do not claim "Tor protected" based only on the visible page IP.

---

# 30. TOR ISOLATION

Private/Tor windows should have explicit isolation boundaries.

Do not share unnecessarily:

* cookies
* localStorage
* IndexedDB
* service workers
* cache
* credentials
* permissions
* browsing history
* identifiers

between protected and normal browsing contexts.

Document the exact isolation guarantees.

Never claim stronger isolation than the implementation actually provides.

---

# 31. DOWNLOAD SECURITY

Downloads must be treated as untrusted.

Validate:

* source URL
* redirect chain
* filename
* MIME type
* destination
* permissions
* file existence
* path traversal
* external app handling

Never allow a webpage to choose an arbitrary filesystem path.

Never construct filesystem paths directly from untrusted filenames.

Sanitize:

../
absolute paths
URI encoded traversal
special characters

Downloads requiring sensitive access should go through native Android controls.

---

# 32. PASSWORD / VAULT INTEGRATION

Never expose:

* passwords
* encryption keys
* vault secrets
* authentication tokens

to arbitrary content scripts.

Only inject credentials after:

1. User authorization.
2. Correct origin verification.
3. Correct tab/session verification.
4. Correct form identification.
5. Explicit user policy.

Never log secrets.

Never include secrets in crash reports.

Never send passwords through generic WebExtension messages.

---

# 33. COOKIES AND STORAGE

Treat cookies as site-origin data.

Do not copy cookies into:

* JavaScript logs
* analytics
* native logs
* debugging telemetry
* shared preferences

Do not expose cookies to WebExtension code unless required.

Avoid creating unnecessary native mirrors of browser storage.

Prefer Gecko-managed storage.

---

# 34. PERMISSIONS

Use least privilege.

Do not request permissions merely because they may be useful later.

Every permission must have:

* purpose
* owner
* security implication
* privacy implication
* user-visible explanation where applicable

Review:

* Android permissions
* WebExtension permissions
* native messaging permissions
* Gecko permissions
* notification permissions
* storage access
* camera/microphone
* location
* sensor access

---

# 35. JAVASCRIPT ERROR HANDLING

Do not use:

try {
...
} catch {}

throughout the code.

Silent catches hide browser-breaking bugs.

Use:

try {
...
} catch (error) {
safeDebugLog(...)
}

Debug logging must never contain:

* URLs containing sensitive query data
* cookies
* tokens
* passwords
* page contents
* private browsing data

Production builds should minimize logging.

---

# 36. GLOBAL STATE

Avoid global flags such as:

window.__azokleShieldExtLoaded

as a security mechanism.

A global flag may prevent duplicate initialization, but it is not:

* authentication
* authorization
* security
* origin verification
* process isolation

Use module-level state or extension lifecycle mechanisms where possible.

If a global marker is required:

* namespace it
* make initialization idempotent
* do not treat it as a trust boundary

---

# 37. CONTENT SCRIPT INITIALIZATION

Every content script should be:

* idempotent
* lightweight
* origin-aware
* lifecycle-aware
* failure-tolerant
* memory-safe

Avoid repeated initialization caused by:

* SPA navigation
* history.pushState
* history.replaceState
* bfcache
* iframe creation
* page reloads

Use explicit state management.

---

# 38. SINGLE PAGE APPLICATIONS

Modern websites often do not reload when navigating.

Support:

* pushState
* replaceState
* popstate
* hash navigation
* route transitions

Do not assume:

DOMContentLoaded = entire site lifecycle

For site-specific features, design around SPA navigation carefully.

Do not globally monkey-patch history APIs unless absolutely necessary.

---

# 39. IFRAMES

Never assume every iframe belongs to the top-level site.

Consider:

* cross-origin iframe
* third-party iframe
* sandboxed iframe
* embedded payment
* authentication
* video
* ads
* CAPTCHA

Native messaging should distinguish:

* top-level frame
* subframe
* extension context

Sensitive native operations should generally require the expected top-level context.

---

# 40. CSP / TRUST BOUNDARY

Treat all webpage JavaScript as hostile.

The website is not part of Azokle's trusted computing base.

Do not allow a website to:

* execute native commands
* change browser settings
* enable Tor
* disable Shield
* access browser storage
* read extension secrets
* invoke privileged APIs
* change download directories
* change proxy configuration

unless explicitly authorized by browser UI and security controls.

---

# 41. SITE-SPECIFIC EXCEPTIONS

Every exception must have:

```text
site:
reason:
problem:
solution:
scope:
security-impact:
privacy-impact:
compatibility-tests:
created:
owner:
```

Do not add:

if (hostname === "...") {
disableEverything();
}

Instead, disable only the exact incompatible behavior.

---

# 42. AD BLOCKER DESIGN

Use layered blocking:

Layer 1:
Network rules

Layer 2:
Request/response classification

Layer 3:
Cosmetic filtering

Layer 4:
Site-specific compatibility

Layer 5:
Optional user controls

Do not make DOM removal the primary blocker.

The blocker must support:

* allowlist
* blocklist
* cosmetic rules
* per-site controls
* temporary disable
* statistics
* updateable rule sets

Privacy statistics should be calculated locally whenever possible.

---

# 43. SHIELD UI

Azokle Shield should communicate:

* ads blocked
* trackers blocked
* fingerprinting protection status
* secure connection status
* site permissions
* protection exceptions

Do not expose misleading numbers.

For example:

"127 trackers blocked"

must represent a clearly defined measurement.

Do not count:

* every DOM element
* every script
* every blocked subresource

as a "tracker" unless it actually matches the defined classification.

---

# 44. PRIVACY BY DEFAULT

Default behavior should minimize:

* tracking
* telemetry
* third-party data sharing
* unnecessary permissions
* persistent identifiers
* cross-site state
* unnecessary storage

However:

Do not claim "zero tracking" if Azokle itself sends telemetry.

If telemetry exists:

* document it
* minimize it
* make it privacy-preserving
* avoid URL/page-content collection
* avoid persistent identifiers where possible
* provide controls

---

# 45. NO HIDDEN TELEMETRY

Never add analytics libraries without explicit authorization.

Never send:

* complete browsing history
* full URLs
* page contents
* search queries
* passwords
* cookies
* form contents

to an Azokle server unless explicitly required and clearly disclosed.

Do not add third-party analytics automatically.

---

# 46. SECURITY LOGGING

Logs must be safe.

Never log:

* Authorization headers
* cookies
* passwords
* access tokens
* refresh tokens
* session identifiers
* full private URLs
* private page content
* Tor identity information

When logging a URL, prefer:

scheme + hostname

instead of full URL when possible.

---

# 47. PERFORMANCE

Every browser feature must be evaluated for:

* CPU
* memory
* battery
* network
* startup time
* page load
* scrolling
* animation
* media playback

Avoid:

* tight intervals
* infinite polling
* repeated DOM queries
* repeated full-document scans
* unnecessary MutationObservers
* synchronous heavy computation
* large object retention
* unnecessary native↔JS communication

Use event-driven behavior whenever possible.

---

# 48. POLLING POLICY

Polling should be the fallback, not the default.

Bad:

setInterval(..., 50)

Better:

MutationObserver
event listener
browser API
WebExtension event
GeckoView callback

If polling is unavoidable:

* use the largest safe interval
* stop when unnecessary
* limit retries
* avoid work when state hasn't changed

---

# 49. MEMORY MANAGEMENT

Watch for:

* MutationObserver leaks
* event listener leaks
* WebExtension port leaks
* GeckoSession references
* Activity references
* Context leaks
* cached page data
* tab references
* large DOM snapshots

When a tab/session is destroyed:

release associated resources.

Never keep a reference to an Activity in a long-lived singleton unless lifecycle-safe.

---

# 50. ANDROID LIFECYCLE

GeckoView code must respect:

* Activity lifecycle
* Fragment lifecycle
* process death
* configuration changes
* background/foreground transitions

Do not assume the Activity remains alive.

Persist only what must survive.

Restore:

* tabs
* sessions
* settings

using appropriate lifecycle-safe mechanisms.

---

# 51. GECKORUNTIME / GECKOSESSION

Keep responsibilities clear.

GeckoRuntime:

* browser-engine runtime

GeckoSession:

* individual browser session/tab context

Do not recreate runtime unnecessarily.

Do not create excessive sessions.

Ensure sessions are correctly opened/closed.

---

# 52. TAB ARCHITECTURE

Tabs should have explicit lifecycle states:

CREATED
OPEN
BACKGROUND
DISCARDING
DISCARDED
CLOSED

Do not assume an Android UI object equals a GeckoSession.

Maintain clear ownership.

When a tab closes:

* remove UI reference
* remove session reference
* unregister listeners
* close session when appropriate
* clear tab-specific extension state

---

# 53. NAVIGATION SECURITY

Navigation should distinguish:

* user-entered URL
* search query
* page link
* redirect
* external intent
* deep link
* download
* PWA launch

Do not automatically treat every string as a URL.

Search input should be classified before navigation.

Validate dangerous schemes.

---

# 54. SEARCH ENGINE INTEGRATION

Azokle Search should not be allowed to silently override browser security.

Search results may contain:

* malicious links
* redirects
* tracking URLs
* downloads
* deep links

The browser security layer remains authoritative.

Do not trust search results simply because they originated from Azokle.

---

# 55. EXTERNAL INTENTS

External app launching must be explicit.

A website must not silently launch arbitrary applications.

Before launching an external intent:

* validate URI
* identify target package if applicable
* verify user action
* consider private/Tor context
* consider data leakage

Never leak a private browsing URL unnecessarily into another application.

---

# 56. CLIPBOARD

Do not automatically read clipboard contents.

Only access clipboard when required.

Never send clipboard contents to servers.

Avoid logging clipboard contents.

---

# 57. CAMERA / MICROPHONE

Permission prompts must correspond to real site requests.

Do not auto-grant.

Do not expose microphone/camera to a page without Gecko/Android permission flow.

Private browsing/Tor modes should be evaluated separately for media permissions.

---

# 58. WEBRTC

WebRTC requires special attention in privacy/Tor modes.

Test:

* local IP exposure
* public IP exposure
* ICE candidates
* STUN
* TURN
* microphone
* camera
* data channels

Do not assume HTTP proxying automatically protects WebRTC.

---

# 59. SERVICE WORKERS

Service workers can continue activity independently from normal page lifecycle.

Test privacy controls against:

* fetch
* cache
* background operations
* push
* notification
* IndexedDB

Tor/private-mode isolation must consider service worker storage and lifecycle.

---

# 60. CACHE

Do not assume clearing page UI state means clearing all browser state.

Understand:

* HTTP cache
* memory cache
* disk cache
* service worker cache
* cookies
* localStorage
* IndexedDB

Private browsing requirements must be tested against each storage mechanism.

---

# 61. PWA SECURITY

A PWA installation must validate:

* manifest
* origin
* HTTPS
* scope
* start_url
* icons
* display
* navigation boundary

Never allow a malicious manifest to impersonate another origin.

Do not allow PWA scope to escape its intended origin.

---

# 62. WEBSITE COMPATIBILITY

Every privacy feature must be tested against representative categories:

### Normal websites

* news
* shopping
* banking
* documentation

### Media

* YouTube
* streaming sites
* audio sites

### Web applications

* Gmail-like applications
* editors
* dashboards
* cloud applications

### Games

* Canvas games
* WebGL games
* WebAssembly

### Security

* CAPTCHA
* WebAuthn
* passkeys
* authentication

### Communication

* video calls
* WebRTC
* chat applications

### PWA

* installable web apps
* offline apps
* service workers

---

# 63. WEB AUTHENTICATION

Never break:

* WebAuthn
* passkeys
* FIDO2
* platform authenticators

Privacy features must not interfere with:

* origin validation
* challenge/response
* credential storage
* secure contexts

Test login flows after changes to:

* cookies
* storage
* visibility
* user agent
* iframe handling
* permissions

---

# 64. CAPTCHA

Never block CAPTCHA blindly.

Do not:

* remove CAPTCHA iframes
* modify CAPTCHA canvas
* alter CAPTCHA timers
* block CAPTCHA resources indiscriminately

A legitimate CAPTCHA should continue working.

---

# 65. ACCESSIBILITY

Privacy and blocking features must not unnecessarily remove:

* labels
* buttons
* controls
* navigation
* keyboard functionality
* screen-reader semantics

Avoid DOM mutations that destroy accessibility.

---

# 66. SECURITY REVIEW PROCESS

When asked to fix a browser issue:

### Step 1 — Reproduce

Determine:

* exact URL
* Android version
* GeckoView version
* device
* browser mode
* tab state
* private/Tor state
* Shield state

### Step 2 — Identify layer

Determine whether the issue belongs to:

* Gecko
* GeckoView
* Android
* WebExtension
* content script
* network blocker
* website

### Step 3 — Find existing API

Before writing a workaround, inspect current GeckoView/Gecko APIs.

### Step 4 — Minimize change

Fix only the affected behavior.

### Step 5 — Test compatibility

Run representative websites.

### Step 6 — Test privacy

Verify no new data leak.

### Step 7 — Test performance

Measure CPU/memory/battery impact.

### Step 8 — Regression test

Add a test for the original bug.

---

# 67. BUG CLASSIFICATION

Classify every bug as one of:

SECURITY
PRIVACY
COMPATIBILITY
PERFORMANCE
RELIABILITY
UI
NETWORKING
MEDIA
STORAGE
LIFECYCLE
WEBEXTENSION
GECKOVIEW
ANDROID
TOR

A bug may have multiple categories.

Do not fix a compatibility bug by introducing a security bug.

Do not fix a privacy bug by introducing a reliability bug without explicit justification.

---

# 68. PRIORITY

Security priority:

P0:
Remote code execution
Native bridge compromise
credential theft
arbitrary file access
Tor bypass
critical origin bypass

P1:
privacy leak
WebRTC IP leak
cookie isolation failure
permission bypass
navigation security bypass

P2:
major website compatibility issue
browser crash
major performance regression

P3:
minor UI/UX issue
cosmetic issue

---

# 69. SECURITY PATCH RULE

When a security vulnerability is discovered:

1. Reproduce.
2. Identify trust boundary.
3. Determine exploitability.
4. Minimize attack surface.
5. Patch.
6. Add regression test.
7. Audit similar code.
8. Check logs.
9. Check release configuration.
10. Document security impact.

Do not merely add a hostname check if the underlying trust boundary remains unsafe.

---

# 70. CODE REVIEW CHECKLIST

Before accepting a change, check:

[ ] Does it use GeckoView correctly?
[ ] Does it use WebExtensions where appropriate?
[ ] Does it avoid unnecessary main-world injection?
[ ] Does it avoid global prototype mutation?
[ ] Does it avoid arbitrary fingerprint spoofing?
[ ] Does it avoid broad DOM deletion?
[ ] Does it avoid unnecessary polling?
[ ] Does it validate native messages?
[ ] Does it validate URLs?
[ ] Does it validate sender/origin?
[ ] Does it respect private/Tor mode?
[ ] Does it avoid logging sensitive information?
[ ] Does it preserve WebAuthn?
[ ] Does it preserve CAPTCHA?
[ ] Does it preserve WebRTC where allowed?
[ ] Does it preserve media?
[ ] Does it preserve accessibility?
[ ] Does it avoid memory leaks?
[ ] Does it avoid CPU-heavy observers?
[ ] Does it have a regression test?
[ ] Does it have a rollback path?

---

# 71. AZOKLE SHIELD CONTENT-SCRIPT RULES

The Shield content script must remain small.

Prefer:

```text
Shield
|
+-- network rules
|
+-- cosmetic rules
|
+-- site adapters
|
+-- privacy UI
|
+-- PWA detection
|
+-- minimal compatibility code
```

Do not create a 400+ line "everything privacy" script unless there is a strong architectural reason.

A content script should not attempt to implement:

* a fingerprinting engine
* a media engine
* a network stack
* a browser lifecycle manager
* a permission system
* a Tor proxy
* an authentication system

---

# 72. SITE ADAPTER PATTERN

For complicated websites use:

```text
site-adapters/
    youtube.js
    google.js
    instagram.js
    reddit.js
    example.js
```

Each adapter must expose narrowly scoped behavior.

Conceptually:

```javascript
const SiteAdapter = {
    matches(url) {},
    init() {},
    destroy() {},
    onNavigation() {},
    onMutation() {}
};
```

Do not execute every site adapter on every domain.

---

# 73. RULE ENGINE

Blocking rules should be data-driven.

Prefer:

```text
rules/
    network/
    cosmetic/
    exceptions/
    sites/
```

over hundreds of hardcoded conditions.

Rule updates should be versioned.

Do not silently replace security-critical rules with remote data without integrity validation.

---

# 74. REMOTE RULE UPDATES

If Azokle downloads filter lists:

* use HTTPS
* validate response
* validate format
* verify integrity where applicable
* reject malformed rules
* use version numbers
* support rollback
* avoid executing downloaded JavaScript
* never treat filter lists as executable code

A rule update must never become arbitrary code execution.

---

# 75. DEPENDENCY SECURITY

Before adding a dependency:

* determine maintainer
* inspect license
* inspect update frequency
* inspect known vulnerabilities
* inspect transitive dependencies
* determine why it is necessary

Do not add a large library for a simple browser operation.

---

# 76. R8 / RELEASE BUILD

Do not blindly enable aggressive R8 rules.

Verify:

* GeckoView functionality
* WebExtension loading
* reflection
* JNI
* serialization
* native messaging
* release-only behavior

Run release builds in CI.

Debug success does not prove release correctness.

---

# 77. TEST MATRIX

At minimum test:

Android:

* current supported Android release
* previous supported release
* low-memory device
* modern device

GeckoView:

* current pinned version
* upgrade candidate

Modes:

* normal
* private
* Tor
* Shield on
* Shield off

Sites:

* normal
* SPA
* video
* WebGL
* WebRTC
* PWA
* authentication
* CAPTCHA
* downloads

---

# 78. AUTOMATED TESTS

Where practical create tests for:

* URL validation
* host matching
* sender validation
* message schema
* rule parsing
* blocker matching
* PWA parsing
* navigation classification
* Tor state
* private state
* download validation

Browser integration tests should cover:

* page load
* navigation
* tab lifecycle
* downloads
* media
* permissions
* WebExtensions
* native messaging

---

# 79. REGRESSION TEST RULE

Every bug fix must produce a regression test when practical.

Bug:

"swiping down refreshes page unexpectedly"

must become a test covering the relevant touch/scroll/navigation behavior.

Bug:

"omnibar does not update"

must become a navigation test.

Bug:

"video stops in background"

must become a media/lifecycle test.

Bug:

"native message can be triggered from iframe"

must become a security test.

---

# 80. NO ASSUMPTIONS ABOUT GECKOVIEW APIs

Never invent a GeckoView API.

Before using an API:

1. Check current GeckoView documentation.
2. Check the version used by the project.
3. Confirm availability.
4. Confirm deprecation status.
5. Confirm threading requirements.
6. Confirm lifecycle requirements.

If documentation is unavailable, clearly state uncertainty.

Do not fabricate methods, classes, permissions, flags, or callbacks.

---

# 81. VERSION AWARENESS

Always inspect the project's actual:

* GeckoView version
* Android Gradle Plugin
* Kotlin version
* compileSdk
* targetSdk
* minSdk

Do not assume the latest version is being used.

Do not automatically upgrade GeckoView while fixing an unrelated bug.

If a bug appears engine-specific, determine whether upgrading GeckoView is safer than maintaining a workaround.

---

# 82. OFFICIAL DOCUMENTATION PRIORITY

When technical behavior is uncertain, prioritize:

1. GeckoView official documentation
2. GeckoView Javadocs
3. Mozilla Firefox Source Documentation
4. Mozilla Bugzilla
5. Android Developers documentation
6. Android security guidance
7. WebExtensions documentation
8. MDN
9. High-quality technical sources

Do not treat random blog posts as authoritative for browser security.

---

# 83. WEB RESEARCH RULE

For current GeckoView behavior, API changes, security issues, or version-specific behavior:

Research before modifying architecture.

Record:

* source
* version
* relevant API
* limitation
* implementation decision

Do not rely solely on model memory for current GeckoView APIs.

---

# 84. MOZILLA BUG REPORTING

If behavior appears to be an engine bug:

Do not create increasingly complex JavaScript hacks.

First determine:

* reproducibility
* minimal reproduction
* GeckoView version
* Android version
* expected Gecko behavior
* whether Firefox Android reproduces it
* whether it is a GeckoView embedder issue

If appropriate, prepare a minimal Mozilla Bugzilla-quality reproduction.

---

# 85. ANDROID-SPECIFIC SECURITY

Use Android best practices for:

* Intent handling
* exported components
* deep links
* permissions
* file providers
* secure storage
* network security
* backup behavior
* clipboard
* notifications
* WebView only when WebView is actually used
* inter-process communication

Do not apply WebView-specific solutions to GeckoView blindly.

---

# 86. WEBVIEW WARNING

Azokle uses GeckoView.

Do not suggest:

"Enable WebView JavaScript"

"WebViewClient"

"WebChromeClient"

"addJavascriptInterface"

as solutions unless the project actually contains a separate WebView component.

GeckoView is not Android WebView.

---

# 87. PRIVACY THREAT MODEL

When auditing Azokle, consider:

### Network attacker

Can they observe traffic?

### Malicious website

Can it access native APIs?

### Malicious iframe

Can it invoke privileged functionality?

### Malicious extension/page

Can it communicate with native code?

### Malicious downloaded file

Can it execute or escape sandboxing?

### Fingerprinter

Can it distinguish Azokle users?

### Tracker

Can it correlate sessions?

### Compromised third-party dependency

Can it access sensitive browser data?

### Malicious redirect

Can it escape Tor/private mode?

---

# 88. PRIVACY BUDGET MINDSET

Every exposed browser property contributes to fingerprinting.

Consider:

* user agent
* platform
* screen dimensions
* timezone
* language
* hardwareConcurrency
* deviceMemory
* WebGL
* Canvas
* Audio
* fonts
* APIs
* media capabilities

Do not independently randomize each property.

Consistency is important.

---

# 89. BROWSER IDENTITY

Do not unnecessarily spoof the user agent.

User-agent changes can break:

* website compatibility
* authentication
* device detection
* downloads
* media

If user-agent reduction is required, implement it consistently with Gecko's browser identity model.

---

# 90. PERFORMANCE AUDIT

When a feature causes lag:

Measure before changing it.

Look for:

* JS execution frequency
* DOM mutation count
* observer callbacks
* layout thrashing
* repeated selectors
* memory growth
* Gecko main-thread work
* native message frequency

Do not solve performance problems by disabling privacy protections blindly.

---

# 91. ENERGY EFFICIENCY

Android browsers are battery-sensitive.

Avoid background:

* timers
* polling
* page scans
* native messages
* network requests

especially when:

* tab is inactive
* browser is backgrounded
* screen is off

---

# 92. CRASH INVESTIGATION

When GeckoView crashes:

Collect:

* Android version
* device
* GeckoView version
* crash stack
* reproducible URL
* private/Tor state
* Shield state
* extension version

Do not "fix" crashes by hiding exceptions.

Determine whether the crash is:

* Kotlin
* JNI
* Gecko
* WebExtension
* renderer/content process
* GPU
* Android lifecycle

---

# 93. CHANGE MANAGEMENT

For large changes:

1. Create a plan.
2. Identify affected layers.
3. Modify the smallest layer possible.
4. Build.
5. Run tests.
6. Test real websites.
7. Inspect logs.
8. Compare performance.
9. Review security.
10. Document behavior.

Do not combine unrelated browser-engine changes into one patch.

---

# 94. REQUIRED OUTPUT WHEN FIXING CODE

When asked to fix an issue, report:

### Root cause

What actually caused it.

### Layer

Where the bug belongs:

GeckoView / Gecko / WebExtension / Content Script / Kotlin / Android / Network.

### Fix

What changed.

### Why

Why this solution is safer than the previous implementation.

### Compatibility impact

What websites/features could be affected.

### Security impact

Whether attack surface changed.

### Privacy impact

Whether fingerprinting/tracking behavior changed.

### Performance impact

CPU/memory/network implications.

### Tests

What was tested.

### Remaining limitations

Anything not fully solved.

---

# 95. DO NOT MASK BUGS

Never "fix" a bug by:

* swallowing exceptions
* hiding errors
* disabling Shield
* disabling JavaScript
* disabling entire websites
* disabling Gecko features
* adding massive delays
* forcing reloads
* restarting GeckoSession
* recreating GeckoRuntime
* injecting arbitrary scripts

unless explicitly justified.

---

# 96. USER EXPERIENCE

Privacy should be understandable.

When a feature blocks something:

Provide useful feedback when appropriate.

Examples:

Shield:
"3 trackers blocked"

Tor:
"Tor connection active"

Permission:
"Camera access blocked"

Download:
"Download blocked because the file source is unsafe"

Avoid technical messages such as:

"GeckoResult failed"

for ordinary users.

---

# 97. SAFE DEFAULTS

When uncertain:

Prefer:

* deny
* isolate
* validate
* minimize
* fail closed

But do not break basic browsing unnecessarily.

For security-sensitive native operations:

fail closed.

For ordinary website features:

fail gracefully.

---

# 98. FINAL DECISION RULE

Before implementing any browser privacy workaround, ask:

> "Am I fixing this at the correct layer?"

If the answer is no, stop and redesign.

Use:

GeckoView/Gecko
>
WebExtension
>
Content script
>
Main-world injection

Only move downward when the upper layer cannot solve the problem.

---

# 99. AZOKLE 446-LINE SHIELD SCRIPT AUDIT RULES

If an existing Azokle Shield content script contains functionality similar to:

* media pause interception
* visibility spoofing
* canvas pixel mutation
* audio noise
* WebGL spoofing
* hardware spoofing
* timer quantization
* global DOM ad removal
* 100ms polling
* aggressive YouTube playback
* broad skip selectors
* unrestricted MutationObserver
* main-world eval

do not preserve it merely because it already works.

Audit each feature independently.

For each:

1. Identify original purpose.
2. Identify current implementation.
3. Determine whether Gecko already handles it.
4. Determine whether WebExtension can handle it.
5. Determine compatibility impact.
6. Determine privacy benefit.
7. Determine fingerprinting risk.
8. Determine performance cost.
9. Replace unsafe implementation where possible.
10. Remove dead code.

Do not perform a blind line-by-line rewrite.

Perform an architectural audit first.

---

# 100. MIGRATION STRATEGY FOR LEGACY SHIELD CODE

When replacing an old Shield script:

Phase 1:
Inventory functionality.

Phase 2:
Classify each feature.

Phase 3:
Remove dangerous global monkey patches.

Phase 4:
Move network blocking to network rules.

Phase 5:
Move fingerprint protection to Gecko/privacy controls.

Phase 6:
Move native communication to WebExtension messaging.

Phase 7:
Keep only required site adapters.

Phase 8:
Add tests.

Phase 9:
Measure performance.

Phase 10:
Perform security review.

Do not rewrite everything simultaneously without tests.

---

# 101. DEFINITION OF DONE

A browser feature is complete only when:

[ ] Implementation works
[ ] Correct layer selected
[ ] No unnecessary browser-engine emulation
[ ] Security reviewed
[ ] Privacy reviewed
[ ] Performance reviewed
[ ] Website compatibility tested
[ ] Private mode tested
[ ] Tor mode tested where relevant
[ ] WebExtension lifecycle tested
[ ] Android lifecycle tested
[ ] Error handling implemented
[ ] Logs contain no sensitive data
[ ] Regression test added
[ ] Release build tested
[ ] Documentation updated

---

# 102. ABSOLUTE RULES

Never:

* invent GeckoView APIs
* invent Android APIs
* use WebView APIs in a GeckoView project
* trust webpage JavaScript
* trust content-script messages
* trust iframe messages
* trust URLs from webpages
* use substring hostname security checks
* globally fake browser lifecycle APIs
* globally intercept media APIs
* corrupt canvas pixels
* randomly corrupt audio
* arbitrarily spoof hardware
* globally quantize browser timing
* use broad DOM deletion
* use aggressive infinite polling
* expose native secrets
* log credentials
* silently swallow security errors
* disable browser security to solve compatibility
* claim Tor protection without testing network behavior
* claim privacy without defining what is protected

---

# 103. AGENT OPERATING MODE

When this skill is active, do not immediately start coding.

First:

1. Inspect repository structure.
2. Identify GeckoView version.
3. Identify Android/Kotlin versions.
4. Identify WebExtension structure.
5. Identify Shield architecture.
6. Identify native messaging.
7. Identify networking/proxy/Tor architecture.
8. Identify tests.
9. Identify existing privacy controls.
10. Identify the exact bug.

Then produce an implementation plan.

If the requested change violates the architecture above, explain why and propose the correct layer.

If multiple solutions exist, prefer the solution with:

* lower attack surface
* lower compatibility risk
* lower maintenance cost
* less JavaScript injection
* more Gecko-native behavior
* better testability

---

# 104. AGENT RESPONSE STYLE

Be technically direct.

Do not say:

"this should probably work."

Instead state:

* confirmed
* likely
* unknown
* requires testing

Do not fabricate certainty.

When an API/version is unknown, verify current documentation before coding.

When a browser behavior is engine-dependent, distinguish:

* Gecko behavior
* GeckoView behavior
* Android behavior
* website behavior

---

# 105. PRIMARY GOAL

The final Azokle Browser architecture should resemble:

Native Android
|
v
GeckoView
|
v
Gecko Engine
|
+---- Privacy/Fingerprinting
|
+---- Networking
|
+---- Permissions
|
+---- Media
|
+---- Storage
|
v
WebExtension
|
+---- Network blocking
+---- Tracker blocking
+---- Cosmetic filtering
+---- Site adapters
+---- PWA detection
+---- Minimal content scripts
|
v
Azokle Shield UI

NOT:

Android
|
v
Huge JavaScript file
|
+-- fake browser lifecycle
+-- fake media engine
+-- fake fingerprint engine
+-- fake network blocker
+-- fake privacy engine
+-- DOM scanner
+-- random spoofing
+-- main-world eval
+-- native bridge

The first architecture is the target.

---

# 106. OFFICIAL REFERENCE PRIORITY

Use official documentation whenever possible.

Primary references:

* Android Developers — Android Skills
* Android Developers — Android Security
* Mozilla GeckoView documentation
* Mozilla GeckoView Javadocs
* Firefox Source Documentation
* Mozilla WebExtensions documentation
* MDN WebExtensions documentation

When documentation conflicts with model knowledge, documentation wins.

When documentation conflicts with assumptions in existing Azokle code, investigate the discrepancy before changing code.

---

# 107. FINAL PRINCIPLE

Azokle should not try to be a website that behaves like a browser.

Azokle is a browser.

Therefore:

Let Gecko handle browser behavior.

Let GeckoView expose browser capabilities.

Let WebExtensions handle web-content interaction.

Let Android handle application/security boundaries.

Let Azokle Shield handle blocking policy.

Let site adapters handle exceptional compatibility cases.

Keep JavaScript small.

Keep trust boundaries explicit.

Keep privacy measurable.

Keep security testable.

Keep compatibility first-class.

## Build the browser as an engine-integrated privacy product, not as a collection of JavaScript patches.

## End of Skill
