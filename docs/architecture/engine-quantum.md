# Mozilla Quantum Engine Integration

Azokle Browser employs **Mozilla Quantum** (Gecko / GeckoView / WebRender / SpiderMonkey) as its primary rendering backend. The engine layer is encapsulated in C++20 under `engine/` to cleanly separate browser rendering mechanics from the Rust privacy core and platform UI shells.

---

## Canonical C++20 Engine Interfaces

All rendering engines in Azokle must implement the unified canonical interface suite located in `engine/include/azokle/engine/`:

```
engine/include/azokle/engine/
├── IBrowserEngine.hpp        # Engine lifecycle, profile configs, proxy routing
├── IBrowserSession.hpp       # Tab/window navigation, JS execution, surface attaching
├── IEngineDelegate.hpp       # Navigation, loading, progress, and security events
└── IWebExtensionBridge.hpp   # Bi-directional WebExtension messaging ports
```

### 1. `IBrowserEngine`
Defines initialization, session allocation, cookie/cache isolation, and proxy routing:
```cpp
class IBrowserEngine {
public:
    virtual ~IBrowserEngine() = default;
    virtual bool Initialize(const BrowserEngineConfig& config) = 0;
    virtual void Shutdown() = 0;
    virtual bool IsInitialized() const = 0;
    virtual std::shared_ptr<IBrowserSession> CreateSession(bool isPrivate = false, bool isTor = false) = 0;
    virtual void DestroySession(uint64_t sessionId) = 0;
    virtual std::shared_ptr<IWebExtensionHost> GetExtensionHost() = 0;
    virtual void ClearCookies(const std::string& host = "") = 0;
    virtual void ClearCache() = 0;
    virtual bool IsTorActive() const = 0;
    virtual void SetTorProxy(const std::string& host, uint16_t port) = 0;
    virtual void ClearTorProxy() = 0;
};
```

### 2. `IBrowserSession`
Represents an individual tab or rendering viewport:
```cpp
class IBrowserSession {
public:
    virtual ~IBrowserSession() = default;
    virtual uint64_t GetSessionId() const = 0;
    virtual bool IsPrivate() const = 0;
    virtual bool IsTor() const = 0;
    virtual void LoadUri(const std::string& uri) = 0;
    virtual void Reload(bool bypassCache = false) = 0;
    virtual void Stop() = 0;
    virtual void GoBack() = 0;
    virtual void GoForward() = 0;
    virtual bool CanGoBack() const = 0;
    virtual bool CanGoForward() const = 0;
    virtual bool IsLoading() const = 0;
    virtual std::string GetCurrentUri() const = 0;
    virtual std::string GetCurrentTitle() const = 0;
    virtual double GetProgress() const = 0;
    virtual void SetDelegate(std::shared_ptr<IEngineDelegate> delegate) = 0;
    virtual void EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) = 0;
    virtual void InjectCss(const std::string& css) = 0;
    virtual void AttachSurface(void* nativeWindowHandle, int width, int height) = 0;
    virtual void ResizeSurface(int width, int height) = 0;
    virtual void Close() = 0;
};
```

---

## Gecko / GeckoView Embedding Strategy

1. **Desktop Linux / Windows / macOS (`GeckoEngineBridge`)**:
   - Embeds Gecko through native window handle attachment (`AttachSurface`).
   - WebRender paints GPU-accelerated frames directly onto the native window surface (Wayland subsurfaces / X11 Window on Linux, HWND/DXGI on Windows, NSView/Metal on macOS).
   - Network requests are intercepted at the Necko socket layer and passed through Azokle Shield before transmission.

2. **Android (`GeckoView`)**:
   - Integrated via Mozilla's official `org.mozilla.geckoview.GeckoRuntime` and `GeckoSession`.
   - Native Kotlin delegate proxies (`AzokleNavigationDelegate`, `AzokleProgressDelegate`) bind GeckoView runtime callbacks to Android UI state and JNI shield filters.

3. **Headless & Automated CI (`MockBrowserEngine`)**:
   - Fully implements `IBrowserEngine` without requiring GPU or X11/Wayland dependencies.
   - Used for continuous integration, deterministic unit testing, and navigation verification.
