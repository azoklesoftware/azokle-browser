# Deep Dive: Mozilla Quantum Engine Internals

This document provides a low-level architectural analysis of the **Mozilla Quantum** rendering engine (Gecko, WebRender, Necko, SpiderMonkey) and its integration within Azokle Browser.

---

## 1. Engine Subsystems Architecture

```
+-----------------------------------------------------------------------------------+
|                            MOZILLA QUANTUM SUBSYSTEMS                             |
+-----------------------------------------------------------------------------------+
|  1. Necko (Network Layer)        -> SOCKS5 proxying, HTTP/3, TLS 1.3, Cache       |
|  2. Stylo (CSS Engine)           -> Parallelized CSS selector matching in Rust    |
|  3. WebRender (GPU Compositor)   -> Hardware-accelerated retained-mode renderer   |
|  4. SpiderMonkey (JS/Wasm)       -> IonMonkey JIT, Baseline JIT, GC heap          |
|  5. Gecko DOM & Layout           -> Reflow, frame tree construction, text shaping  |
|  6. XPCOM & Native Messaging     -> Cross-language component object model & IPC   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Necko Network Pipeline & Azokle Interception

**Necko** is Gecko's multi-threaded network library. It manages HTTP transactions, socket pooling, protocol handlers, and disk/memory cache.

### Interception at Necko Channels
Azokle hooks into the Necko channel lifecycle before any TCP handshake or socket transmission occurs:
1. **Channel Creation**: A document or subresource requests a URI. Gecko allocates an `nsIChannel` or `nsIHttpChannel`.
2. **OnBeforeRequest Hook**: The channel URI is evaluated against the `AzokleShield` fast-path bitmask matcher.
   - If blocked: Necko halts with `NS_ERROR_CONTENT_BLOCKED` or `NS_ERROR_ABORT`.
   - If redirected: Channel destination is rewritten (`nsIChannel::SetURI`).
   - If allowed: Necko dispatches the request over the configured socket provider.
3. **Tor Proxy Delegation**: When Tor is active, Necko is configured with `network.proxy.socks` pointing to `127.0.0.1:9050` with SOCKS version 5. Hostname resolution is strictly disabled in Necko (`network.proxy.socks_remote_dns = true`), delegating all DNS to Tor.

---

## 3. WebRender Hardware-Accelerated Compositing

**WebRender** is a GPU-based renderer written in Rust, utilizing retained-mode display lists to draw web content like a 3D game engine.

### Presentation Surface Lifecycle
1. **Window Handle Attachment**: The presentation shell invokes `IBrowserSession::AttachSurface(nativeHandle, width, height)`.
2. **Swapchain / Visual Tree Creation**:
   - **Linux**: Binds `wl_subsurface` on Wayland or an `XWindow` visual on X11.
   - **Windows**: Initializes a Direct3D 11/12 device, creating a `IDXGISwapChain` attached to a DirectComposition visual.
   - **macOS**: Configures a `CAMetalLayer` backed by the Metal rendering pipeline.
   - **Android**: GeckoView connects to an Android `SurfaceView` backed by `ANativeWindow`.
3. **Frame Pipeline**:
   - Layout generates a display list of draw primitives.
   - WebRender batches primitives into GPU vertex and index buffers.
   - Shaders rasterize text glyphs, box shadows, and textures directly on the GPU.

---

## 4. SpiderMonkey JavaScript & WebAssembly Runtime

**SpiderMonkey** executes all page scripts and WebExtension background tasks:
- **JIT Compilation Tiers**:
  - Interpreter -> Warm-up counter.
  - Baseline Compiler -> Fast single-pass compilation.
  - IonMonkey -> Optimizing compiler using Static Single Assignment (SSA) form.
- **Partitioning & Realms**:
  - Each browser session tab operates in an isolated `JS::Realm`.
  - Private tabs run within a volatile memory pool where the garbage collector (GC) operates with zero swapping to disk.
  - Upon tab closure, `JS_GC()` triggers full heap reclamation and zeroization of sensitive typed arrays.

---

## 5. WebExtension Native Messaging & Scriptlet Defusers

Azokle communicates bidirectionally with WebExtensions via `IWebExtensionBridge` and `IWebExtensionPort`:
1. **Native Port Initialization**: The extension registers a port listener (`browser.runtime.connectNative("azokle_core")`).
2. **Message Packaging**: JSON-encoded commands are exchanged over shared-memory queues without inter-process file serialization.
3. **Content Script Injection**: Cosmetic element-hiding CSS selectors and JavaScript defusers are injected at document start (`document_start`), running before website scripts can probe the environment.
