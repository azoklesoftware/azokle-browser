# C++20 Engine API Reference

Header Location: `engine/include/azokle/engine/`
Namespace: `azokle::engine`

---

## 1. `IBrowserEngine`

Abstract interface managing browser engine initialization, profile partitions, session factories, and proxy configurations.

### Methods

#### `virtual bool Initialize(const BrowserEngineConfig& config) = 0;`
Initializes engine sub-processes, profile directories, and security policies.
- **Parameters**: `config` - Struct specifying profile paths, Tor configuration, user agent, and hardware acceleration flags.
- **Returns**: `true` on success, `false` on initialization failure.

#### `virtual void Shutdown() = 0;`
Gracefully closes all sessions, terminates background engine threads, flushes profiles, and releases GPU surfaces.

#### `virtual bool IsInitialized() const = 0;`
Returns whether the engine runtime is active and accepting session allocations.

#### `virtual std::shared_ptr<IBrowserSession> CreateSession(bool isPrivate = false, bool isTor = false) = 0;`
Factory method allocating an isolated browser session.
- **`isPrivate`**: When `true`, all cookies and storage partitions operate in volatile RAM only.
- **`isTor`**: When `true`, network traffic routes strictly through Tor onion circuits.
- **Returns**: Shared pointer to the newly allocated `IBrowserSession`.

#### `virtual void DestroySession(uint64_t sessionId) = 0;`
Explicitly tears down a session by its unique ID.

#### `virtual void SetTorProxy(const std::string& host, uint16_t port) = 0;`
Configures engine-wide SOCKS5 proxy routing (e.g. `127.0.0.1:9050`).

#### `virtual void ClearTorProxy() = 0;`
Resets engine proxy settings to direct connection.

---

## 2. `IBrowserSession`

Represents an individual web session (tab or window viewport).

### Methods

#### `virtual void LoadUri(const std::string& uri) = 0;`
Initiates navigation to the specified URI string.

#### `virtual void Reload(bool bypassCache = false) = 0;`
Reloads the current document, optionally bypassing cache.

#### `virtual void Stop() = 0;`
Aborts pending navigation or asset requests.

#### `virtual void GoBack() = 0;` / `virtual void GoForward() = 0;`
Navigates through session history.

#### `virtual void AttachSurface(void* nativeWindowHandle, int width, int height) = 0;`
Binds the platform window handle (Wayland subsurface, X11 Window, HWND, NSView) for WebRender compositing.

#### `virtual void ResizeSurface(int width, int height) = 0;`
Informs the renderer compositor of a window resize event.

#### `virtual void EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) = 0;`
Asynchronously evaluates a JavaScript script within the document realm.

#### `virtual void InjectCss(const std::string& css) = 0;`
Injects custom CSS stylesheets (e.g. adblock cosmetic element hiding rules) into the DOM.

---

## 3. `IEngineDelegate`

Observer interface implemented by presentation controllers to receive session notifications.

```cpp
class IEngineDelegate {
public:
    virtual ~IEngineDelegate() = default;
    virtual void OnLocationChange(const std::string& url) {}
    virtual void OnTitleChange(const std::string& title) {}
    virtual void OnProgressChange(int progress) {}
    virtual void OnLoadingChange(bool isLoading) {}
    virtual void OnSecurityChange(const SecurityInfo& securityInfo) {}
    virtual void OnCanGoBackChanged(bool canGoBack) {}
    virtual void OnCanGoForwardChanged(bool canGoForward) {}
    virtual void OnNewWindowRequested(const std::string& url) {}
    virtual void OnWebAuthnPrompt(const std::string& origin, const std::string& rpId) {}
};
```
