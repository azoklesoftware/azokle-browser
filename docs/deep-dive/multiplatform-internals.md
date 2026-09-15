# Deep Dive: Multi-Platform Native Architecture & Presentation

This document details the platform-specific threading models, memory constraints, and windowing mechanics across Linux, Windows, macOS, Android, and iOS.

---

## 1. Linux Presentation Shell (Wayland / X11)

### Wayland Protocol Integration
- **Surface Architecture**: Utilizes `wl_compositor` and `xdg_shell` protocol extensions.
- **Subsurfaces**: Gecko WebRender outputs frames to a dedicated `wl_subsurface` positioned beneath the native UI chrome.
- **Event Loop**: Runs on a GTK4 / GLib main loop. Native Wayland input events (`wl_pointer`, `wl_keyboard`, `wl_touch`) are marshaled into Gecko DOM input events.

### X11 Fallback Mode
- Under pure X11 displays, `apps/linux` binds via an `XVisualInfo` matching the 32-bit TrueColor depth required by WebRender's OpenGL/Vulkan context.
- Double-buffering prevents tearing during fast tab switching.

---

## 2. Windows Presentation Shell (Win32 / DirectComposition)

### Window Architecture
- **DPI-Awareness**: Registers as `Per-Monitor V2` DPI-aware (`SetProcessDpiAwarenessContext`), ensuring crisp text on 4K multi-monitor mixed-scaling setups.
- **DirectComposition Tree**:
  - Root Visual: Contains the top browser chrome, address bar, and tab strip.
  - Content Visual: Hosts the Direct3D 11/12 swapchain created by Gecko.
- **Message Loop**: Traditional high-performance `PeekMessage` pump processing `WM_PAINT`, `WM_SIZE`, and `WM_KEYDOWN` without message queue latency.

---

## 3. macOS Presentation Shell (Cocoa / Metal)

### Window & Rendering Hierarchy
- **Window**: Borderless `NSWindow` with `fullSizeContentView` style mask, hiding the default macOS titlebar to accommodate custom tab controls.
- **Metal Layer**: Houses a `CAMetalLayer` with `wantsLayer = true`. Gecko WebRender compiles shaders into Metal Shading Language (MSL) bytecodes, rendering 120Hz ProMotion frames on Apple Silicon.
- **AppKit Menus & Shortcuts**: Native `NSMenu` binding system commands directly to `DesktopBrowserApp` methods via Objective-C++ bridges.

---

## 4. Android Mobile Client (GeckoView / Compose)

### Lifecycle & Process Architecture
- **Multi-Process Architecture**:
  - Main Process: Runs the Jetpack Compose UI, ViewModel state, and Room databases.
  - Gecko Content Process: Runs out-of-process (OOP) tab renderers in isolated Linux UID sandboxes.
- **Low-Memory Management (`onTrimMemory`)**:
  - When the system issues `TRIM_MEMORY_RUNNING_CRITICAL`, `GeckoEngineManager` systematically purges texture caches and suspends background tabs, reducing RAM footprint by up to 70%.

---

## 5. iOS Mobile Client (SwiftUI / Hardened Engine)

### Dual Engine Strategy
- **Primary**: Experimental Mozilla Gecko for iOS embedding using out-of-process WebRender mobile compositor.
- **App Store Fallback**: Hardened `WKWebView` sandbox:
  - Strict declarative rulesets compiled via `WKContentRuleList`.
  - Custom `WKURLSchemeHandler` routing `.onion` schemes through `UniversalBrowserEngine`.
  - Content security policies blocking third-party tracking pixels before HTML parsing.
