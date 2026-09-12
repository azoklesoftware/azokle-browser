#pragma once

#include "azokle/engine/IBrowserEngine.hpp"
#include <unordered_map>
#include <atomic>
#include <vector>
#include <mutex>

namespace azokle::engine::mock {

class MockBrowserSession : public IBrowserSession {
public:
    MockBrowserSession(uint64_t id, bool isPrivate, bool isTor);
    ~MockBrowserSession() override = default;

    uint64_t GetSessionId() const override { return m_id; }
    bool IsPrivate() const override { return m_isPrivate; }
    bool IsTor() const override { return m_isTor; }

    void LoadUri(const std::string& uri) override;
    void Reload(bool bypassCache = false) override;
    void Stop() override;
    void GoBack() override;
    void GoForward() override;
    bool CanGoBack() const override;
    bool CanGoForward() const override;
    bool IsLoading() const override { return m_isLoading; }

    std::string GetCurrentUri() const override { return m_currentUri; }
    std::string GetCurrentTitle() const override { return m_title; }
    std::string GetTitle() const override { return m_title; }
    double GetProgress() const override { return m_progress; }

    void SetDelegate(std::shared_ptr<IEngineDelegate> delegate) override { m_delegate = delegate; }
    void EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) override;
    void ExecuteJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) override {
        EvaluateJavaScript(script, callback);
    }
    void InjectCss(const std::string& css) override;
    void AttachSurface(void* nativeWindowHandle, int width, int height) override;
    void ResizeSurface(int width, int height) override;
    void Close() override;

    // Test Inspection Hooks
    const std::vector<std::string>& GetNavigationHistory() const { return m_history; }

private:
    uint64_t m_id;
    bool m_isPrivate;
    bool m_isTor;
    std::string m_currentUri;
    std::string m_title;
    double m_progress{0.0};
    bool m_isLoading{false};
    void* m_windowHandle{nullptr};
    int m_width{1280};
    int m_height{800};
    std::shared_ptr<IEngineDelegate> m_delegate;
    std::vector<std::string> m_history;
    int m_historyIndex{-1};
    mutable std::mutex m_mutex;
};

class MockExtensionPort : public IWebExtensionPort {
public:
    void PostMessage(const std::string& jsonMessage) override;
    void OnMessage(std::function<void(const std::string&)> callback) override;
    void Disconnect() override;

    // Test Hook: Simulate extension reply
    void SimulateMessage(const std::string& message);

private:
    std::function<void(const std::string&)> m_callback;
    std::vector<std::string> m_outbox;
};

class MockWebExtensionHost : public IWebExtensionHost {
public:
    bool InstallExtension(const std::string& extensionId, const std::string& resourcePath) override;
    bool UninstallExtension(const std::string& extensionId) override;
    bool EnableExtension(const std::string& extensionId, bool enable) override;
    std::vector<std::string> GetInstalledExtensions() const override;

    void RegisterPortHandler(
        const std::string& extensionId,
        std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
    ) override;

private:
    std::unordered_map<std::string, std::string> m_installedExtensions;
    std::unordered_map<std::string, std::function<void(std::shared_ptr<IWebExtensionPort>)>> m_portHandlers;
    mutable std::mutex m_mutex;
};

class MockBrowserEngine : public IBrowserEngine {
public:
    MockBrowserEngine() = default;
    ~MockBrowserEngine() override = default;

    bool Initialize(const BrowserEngineConfig& config) override;
    void Shutdown() override;
    bool IsInitialized() const override { return m_initialized; }

    std::shared_ptr<IBrowserSession> CreateSession(bool isPrivate = false, bool isTor = false) override;
    void DestroySession(uint64_t sessionId) override;

    std::shared_ptr<IWebExtensionHost> GetExtensionHost() override;

    void ClearCookies(const std::string& host = "") override;
    void ClearCache() override;
    void SetTorProxy(const std::string& host, uint16_t port) override;
    void ClearTorProxy() override;
    bool IsTorActive() const override { return m_config.enableTorRouting; }

    bool RegisterWebExtension(const std::string& extensionId, const std::string& resourcePath) override {
        return m_extensionHost->InstallExtension(extensionId, resourcePath);
    }
    void SetWebExtensionPortHandler(
        const std::string& extensionId,
        std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
    ) override {
        m_extensionHost->RegisterPortHandler(extensionId, handler);
    }

    // Test Hooks
    size_t GetActiveSessionCount() const;

private:
    bool m_initialized{false};
    BrowserEngineConfig m_config;
    std::atomic<uint64_t> m_nextSessionId{1};
    std::unordered_map<uint64_t, std::shared_ptr<MockBrowserSession>> m_sessions;
    std::shared_ptr<MockWebExtensionHost> m_extensionHost{std::make_shared<MockWebExtensionHost>()};
    mutable std::mutex m_mutex;
};

} // namespace azokle::engine::mock
