# Shared Desktop Controller Architecture

To prevent architectural fragmentation and UI state drift across desktop operating systems (Linux, Windows, and macOS), Azokle Browser introduces a centralized C++20 desktop controller: `DesktopBrowserApp`.

Located at:
- Header: `engine/desktop/include/azokle/desktop/DesktopBrowserApp.hpp`
- Source: `engine/desktop/src/DesktopBrowserApp.cpp`

---

## Design Pattern: Observer / Controller Decoupling

`DesktopBrowserApp` acts as the single orchestrator for all desktop browser window sessions, maintaining tab collections, active tab indices, navigation history, and shield evaluation. Native presentation shells (GTK, Win32, Cocoa) simply register an implementation of `IDesktopUiObserver`:

```cpp
class IDesktopUiObserver {
public:
    virtual ~IDesktopUiObserver() = default;

    virtual void OnTabCreated(uint64_t tabId, const std::string& title, const std::string& url) = 0;
    virtual void OnTabClosed(uint64_t tabId) = 0;
    virtual void OnTabActivated(uint64_t tabId) = 0;
    virtual void OnUrlChanged(uint64_t tabId, const std::string& url) = 0;
    virtual void OnTitleChanged(uint64_t tabId, const std::string& title) = 0;
    virtual void OnProgressChanged(uint64_t tabId, double progress) = 0;
    virtual void OnLoadingStateChanged(uint64_t tabId, bool isLoading) = 0;
    virtual void OnSecurityStateChanged(uint64_t tabId, engine::SecurityLevel level, const std::string& host) = 0;
    virtual void OnShieldMetricsUpdated(uint64_t totalBlocked) = 0;
};
```

---

## Core Capabilities

### 1. Unified Tab Lifecycle
- `OpenTab(url, isPrivate, isTor)`: Spawns an isolated `IBrowserSession` from the active engine, binds it to a `DesktopTab` structure, attaches `DesktopBrowserApp` as its `IEngineDelegate`, and notifies the UI observer.
- `CloseTab(tabId)`: Gracefully terminates the session, updates the active index, and prompts the UI to switch focus.
- `SelectTab(tabId)`: Switches the active presentation surface without reloading the page.

### 2. Pre-Navigation Shield Interception
Before issuing a navigation command to the underlying rendering engine, `DesktopBrowserApp::Navigate` directly queries the Rust core C-ABI:
```cpp
void DesktopBrowserApp::Navigate(const std::string& url) {
    auto tab = GetActiveTab();
    if (!tab) return;

    if (m_isShieldActive) {
        auto decision = azokle_core_check_url(url.c_str(), tab->url.c_str(), false);
        if (decision.should_block) {
            m_shieldBlockedCount++;
            if (auto obs = m_observer.lock()) {
                obs->OnShieldMetricsUpdated(m_shieldBlockedCount);
            }
            return; // Terminate blocked request immediately
        }
    }

    tab->session->LoadUri(url);
}
```

### 3. Global Tor Circuit Orchestration
- Toggling Tor dynamically updates engine-wide SOCKS5 routing parameters (`engine->SetTorProxy("127.0.0.1", 9050)`).
- Session delegates update security indicators (`SecurityLevel::TorCircuitActive`), alerting presentation chrome to update address bar badges.
