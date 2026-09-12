# Multi-Platform Native Shells Architecture

Azokle Browser provides native user experiences across five major operating systems. Client applications are located under `apps/` and act strictly as presentation shells.

---

## Supported Platform Matrix

| Platform | Directory | Framework & Language | Engine Backend | State Controller |
| :--- | :--- | :--- | :--- | :--- |
| **Android** | `apps/android` | Kotlin / Jetpack Compose / Coroutines | Mozilla GeckoView | `GeckoEngineManager` |
| **Linux** | `apps/linux` | C++20 / Wayland / X11 / GTK | Gecko Quantum (`GeckoEngineBridge`) | `DesktopBrowserApp` |
| **Windows** | `apps/windows` | C++20 / Win32 / DirectComposition / DXGI | Gecko Quantum (`GeckoEngineBridge`) | `DesktopBrowserApp` |
| **macOS** | `apps/macos` | Swift 5.9 / SwiftUI / AppKit Cocoa | Gecko Quantum + Metal / UniFFI | `DesktopBrowserApp` / `BrowserViewModel` |
| **iOS** | `apps/ios` | Swift 5.9 / SwiftUI mobile | Gecko-iOS mobile / UniFFI | `IosBrowserState` |

---

## Platform Details

### 1. Android (`apps/android`)
- Modern Jetpack Compose UI architecture following MVI (Model-View-Intent).
- Deep integration with Android system services: DownloadManager, biometric authentication via `BiometricPrompt`, and dynamic app shortcut badges.
- Tab management with memory suspension: background tabs automatically surrender heavy texture buffers under OS low-memory signals (`onTrimMemory`).

### 2. Linux Desktop (`apps/linux`)
- Sovereign C++20 application with dual Wayland and X11 surface attachment.
- Uses `azokle_engine` and links against `libazokle_c.a` for instantaneous local adblocking.
- Integrates FreeDesktop standards: desktop entry files, MIME type associations, dark mode theme detection via D-Bus portal (`org.freedesktop.portal.Settings`).

### 3. Windows Desktop (`apps/windows`)
- Native Win32 desktop application implementing DPI-aware presentation window (`AzokleWin32Window`).
- DirectComposition and DXGI swapchain integration for rendering Gecko WebRender surfaces with hardware acceleration.
- Consumes `azokle_engine` and `azokle_c.dll`.

### 4. macOS Desktop (`apps/macos`)
- Native macOS Cocoa / SwiftUI application packaged via SwiftPM (`Package.swift`).
- Borderless sovereign chrome toolbar with native Mac keyboard shortcuts (`Cmd+T`, `Cmd+W`, `Cmd+R`, `Cmd+L`).
- Integrates `bindings/swift` for memory-safe Rust shield telemetry and adblock filtering.

### 5. iOS Mobile (`apps/ios`)
- Native iOS 16+ SwiftUI application with mobile bottom URL bar ergonomics.
- Integrated privacy dashboard sheet displaying real-time blocked tracker metrics and instant Tor circuit toggles.
- Supports ephemeral private browsing tabs with guaranteed zero-persistence RAM disk storage.
