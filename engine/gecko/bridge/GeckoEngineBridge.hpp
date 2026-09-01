#pragma once

#include "azokle/engine/IGeckoEngine.hpp"
#include "azokle/engine/IGeckoSession.hpp"
#include <vector>
#include <mutex>
#include <atomic>

namespace azokle::engine::gecko {

class GeckoSessionBridge : public IGeckoSession, public std::enable_shared_from_this<GeckoSessionBridge> {
public:
    GeckoSessionBridge(bool isPrivate, bool isTor);
    ~GeckoSessionBridge() override = default;

    void LoadUri(const std::string& uri) override;
    void Reload() override;
    void GoBack() override;
    void GoForward() override;
    void Stop() override;
    std::string GetCurrentUri() const override { return m_currentUri; }
    std::string GetCurrentTitle() const override { return m_currentTitle; }
    bool CanGoBack() const override { return m_canGoBack; }
    bool CanGoForward() const override { return m_canGoForward; }
    bool IsLoading() const override { return m_isLoading; }
    bool IsPrivate() const override { return m_isPrivate; }
    bool IsTor() const override { return m_isTor; }
    void SetDelegate(std::shared_ptr<IGeckoDelegate> delegate) override { m_delegate = delegate; }
    void EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) override;
    void InjectCss(const std::string& css) override;
    void AttachSurface(void* nativeWindowHandle, int width, int height) override;
    void ResizeSurface(int width, int height) override;

    // Internal event dispatching
    void DispatchLocationChange(const std::string& url);
    void DispatchTitleChange(const std::string& title);
    void DispatchProgress(int progress);
    void DispatchSecurity(SecurityLevel level, const std::string& host);

private:
    bool m_isPrivate;
    bool m_isTor;
    std::string m_currentUri;
    std::string m_currentTitle;
    std::atomic<bool> m_canGoBack{false};
    std::atomic<bool> m_canGoForward{false};
    std::atomic<bool> m_isLoading{false};
    void* m_windowHandle{nullptr};
    int m_width{1280};
    int m_height{800};
    std::shared_ptr<IGeckoDelegate> m_delegate{nullptr};
    std::vector<std::string> m_history;
    int m_historyIndex{-1};
};

class GeckoEngineBridge : public IGeckoEngine {
public:
    GeckoEngineBridge();
    ~GeckoEngineBridge() override;

    bool Initialize(const EngineProfileConfig& config) override;
    void Shutdown() override;
    std::shared_ptr<IGeckoSession> CreateSession(bool isPrivate = false, bool isTor = false) override;
    void ClearCookies(const std::string& host = "") override;
    void ClearCache() override;
    bool IsTorActive() const override { return m_config.enableTor; }

private:
    EngineProfileConfig m_config;
    bool m_isInitialized{false};
    std::mutex m_sessionMutex;
    std::vector<std::weak_ptr<GeckoSessionBridge>> m_sessions;
};

} // namespace azokle::engine::gecko
