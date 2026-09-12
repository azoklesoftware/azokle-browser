#pragma once

#include "azokle/engine/IBrowserEngine.hpp"
#include "azokle/engine/IBrowserSession.hpp"
#include <vector>
#include <mutex>
#include <atomic>
#include <unordered_map>

namespace azokle::engine::gecko {

/**
 * @brief Concrete session bridge connecting Gecko / GeckoView runtime windows with Azokle browser sessions.
 */
class GeckoSessionBridge : public IBrowserSession, public std::enable_shared_from_this<GeckoSessionBridge> {
public:
    /**
     * @brief Constructs a Gecko session bridge with specified privacy and Tor routing attributes.
     */
    GeckoSessionBridge(uint64_t sessionId, bool isPrivate, bool isTor);
    ~GeckoSessionBridge() override = default;

    uint64_t GetSessionId() const override { return m_sessionId; }
    bool IsPrivate() const override { return m_isPrivate; }
    bool IsTor() const override { return m_isTor; }

    void LoadUri(const std::string& uri) override;
    void Reload(bool bypassCache = false) override;
    void GoBack() override;
    void GoForward() override;
    void Stop() override;
    std::string GetCurrentUri() const override { return m_currentUri; }
    std::string GetCurrentTitle() const override { return m_currentTitle; }
    double GetProgress() const override { return m_progress; }
    bool CanGoBack() const override { return m_canGoBack; }
    bool CanGoForward() const override { return m_canGoForward; }
    bool IsLoading() const override { return m_isLoading; }

    void SetDelegate(std::shared_ptr<IEngineDelegate> delegate) override { m_delegate = delegate; }
    void EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) override;
    void InjectCss(const std::string& css) override;
    void AttachSurface(void* nativeWindowHandle, int width, int height) override;
    void ResizeSurface(int width, int height) override;
    void Close() override;

    // Internal event dispatching
    void DispatchLocationChange(const std::string& url);
    void DispatchTitleChange(const std::string& title);
    void DispatchProgress(int progress);
    void DispatchSecurity(SecurityLevel level, const std::string& host);

private:
    uint64_t m_sessionId{0};
    bool m_isPrivate;
    bool m_isTor;
    std::string m_currentUri;
    std::string m_currentTitle;
    double m_progress{0.0};
    std::atomic<bool> m_canGoBack{false};
    std::atomic<bool> m_canGoForward{false};
    std::atomic<bool> m_isLoading{false};
    void* m_windowHandle{nullptr};
    int m_width{1280};
    int m_height{800};
    std::shared_ptr<IEngineDelegate> m_delegate{nullptr};
    std::vector<std::string> m_history;
    int m_historyIndex{-1};
};

/**
 * @brief Concrete implementation of IBrowserEngine managing Gecko runtime lifecycle and WebExtension bindings.
 */
class GeckoEngineBridge : public IBrowserEngine {
public:
    GeckoEngineBridge();
    ~GeckoEngineBridge() override;

    bool Initialize(const BrowserEngineConfig& config) override;
    bool Initialize(const EngineProfileConfig& config) override;
    void Shutdown() override;
    bool IsInitialized() const override { return m_isInitialized; }

    std::shared_ptr<IBrowserSession> CreateSession(bool isPrivate = false, bool isTor = false) override;
    void DestroySession(uint64_t sessionId) override;
    std::shared_ptr<IWebExtensionHost> GetExtensionHost() override;

    void ClearCookies(const std::string& host = "") override;
    void ClearCache() override;
    bool IsTorActive() const override { return m_config.enableTorRouting; }
    void SetTorProxy(const std::string& host, uint16_t port) override;
    void ClearTorProxy() override;

    bool RegisterWebExtension(const std::string& extensionId, const std::string& resourcePath) override;
    void SetWebExtensionPortHandler(
        const std::string& extensionId,
        std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
    ) override;

private:
    BrowserEngineConfig m_config;
    bool m_isInitialized{false};
    std::atomic<uint64_t> m_nextSessionId{1};
    std::mutex m_sessionMutex;
    std::unordered_map<uint64_t, std::shared_ptr<GeckoSessionBridge>> m_sessions;
};

} // namespace azokle::engine::gecko
