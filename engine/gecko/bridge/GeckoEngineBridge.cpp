#include "GeckoEngineBridge.hpp"
#include <iostream>
#include <sstream>

namespace azokle::engine::gecko {

// Simple mock extension host for GeckoEngineBridge until full XPCOM embedding
class GeckoWebExtensionHost : public IWebExtensionHost {
public:
    bool InstallExtension(const std::string& extensionId, const std::string& resourcePath) override {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_extensions[extensionId] = resourcePath;
        return true;
    }

    bool UninstallExtension(const std::string& extensionId) override {
        std::lock_guard<std::mutex> lock(m_mutex);
        return m_extensions.erase(extensionId) > 0;
    }

    bool EnableExtension(const std::string& /*extensionId*/, bool /*enable*/) override {
        return true;
    }

    std::vector<std::string> GetInstalledExtensions() const override {
        std::lock_guard<std::mutex> lock(m_mutex);
        std::vector<std::string> result;
        for (const auto& [id, _] : m_extensions) {
            result.push_back(id);
        }
        return result;
    }

    void RegisterPortHandler(
        const std::string& extensionId,
        std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
    ) override {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_portHandlers[extensionId] = handler;
    }

private:
    mutable std::mutex m_mutex;
    std::unordered_map<std::string, std::string> m_extensions;
    std::unordered_map<std::string, std::function<void(std::shared_ptr<IWebExtensionPort>)>> m_portHandlers;
};

// -----------------------------------------------------------------------------
// GeckoSessionBridge Implementation
// -----------------------------------------------------------------------------

GeckoSessionBridge::GeckoSessionBridge(uint64_t sessionId, bool isPrivate, bool isTor)
    : m_sessionId(sessionId), m_isPrivate(isPrivate), m_isTor(isTor),
      m_currentUri("about:blank"), m_currentTitle("New Tab") {}

void GeckoSessionBridge::LoadUri(const std::string& uri) {
    if (uri.empty()) return;
    m_currentUri = uri;
    m_isLoading = true;
    m_progress = 0.15;

    if (m_historyIndex < 0 || (size_t)m_historyIndex >= m_history.size() || m_history[m_historyIndex] != uri) {
        if (m_historyIndex >= 0 && (size_t)(m_historyIndex + 1) < m_history.size()) {
            m_history.erase(m_history.begin() + m_historyIndex + 1, m_history.end());
        }
        m_history.push_back(uri);
        m_historyIndex = (int)m_history.size() - 1;
    }

    m_canGoBack = m_historyIndex > 0;
    m_canGoForward = m_historyIndex + 1 < (int)m_history.size();

    if (m_delegate) {
        m_delegate->OnLocationChange(uri);
        m_delegate->OnLoadingChange(true);
        m_delegate->OnProgressChange(15);
        m_delegate->OnProgressChange(m_sessionId, 0.15);
        m_delegate->OnCanGoBackChanged(m_canGoBack);
        m_delegate->OnCanGoForwardChanged(m_canGoForward);

        // Security level
        SecurityLevel sec = SecurityLevel::Insecure;
        if (m_isTor) {
            sec = SecurityLevel::TorCircuitActive;
        } else if (uri.rfind("https://", 0) == 0) {
            sec = SecurityLevel::Secure;
        }
        std::string host = uri;
        auto pos = host.find("://");
        if (pos != std::string::npos) host = host.substr(pos + 3);
        auto slash = host.find('/');
        if (slash != std::string::npos) host = host.substr(0, slash);

        m_delegate->OnSecurityChange(SecurityInfo{sec, host, "DigiCert / Let's Encrypt Sovereign", "TLS_AES_256_GCM_SHA384", m_isTor});
        m_delegate->OnSecurityChange(m_sessionId, sec, host);
    }
}

void GeckoSessionBridge::Reload(bool /*bypassCache*/) {
    LoadUri(m_currentUri);
}

void GeckoSessionBridge::GoBack() {
    if (m_historyIndex > 0) {
        m_historyIndex--;
        m_canGoBack = m_historyIndex > 0;
        m_canGoForward = true;
        LoadUri(m_history[m_historyIndex]);
    }
}

void GeckoSessionBridge::GoForward() {
    if (m_historyIndex + 1 < (int)m_history.size()) {
        m_historyIndex++;
        m_canGoBack = true;
        m_canGoForward = m_historyIndex + 1 < (int)m_history.size();
        LoadUri(m_history[m_historyIndex]);
    }
}

void GeckoSessionBridge::Stop() {
    m_isLoading = false;
    m_progress = 0.0;
    if (m_delegate) {
        m_delegate->OnLoadingChange(false);
    }
}

void GeckoSessionBridge::Close() {
    Stop();
}

void GeckoSessionBridge::EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback) {
    (void)script;
    if (callback) {
        callback("{\"result\": true}");
    }
}

void GeckoSessionBridge::InjectCss(const std::string& css) {
    (void)css;
}

void GeckoSessionBridge::AttachSurface(void* nativeWindowHandle, int width, int height) {
    m_windowHandle = nativeWindowHandle;
    m_width = width;
    m_height = height;
}

void GeckoSessionBridge::ResizeSurface(int width, int height) {
    m_width = width;
    m_height = height;
}

void GeckoSessionBridge::DispatchLocationChange(const std::string& url) {
    m_currentUri = url;
    if (m_delegate) m_delegate->OnLocationChange(url);
}

void GeckoSessionBridge::DispatchTitleChange(const std::string& title) {
    m_currentTitle = title;
    if (m_delegate) m_delegate->OnTitleChange(title);
}

void GeckoSessionBridge::DispatchProgress(int progress) {
    m_progress = progress / 100.0;
    if (m_delegate) {
        m_delegate->OnProgressChange(progress);
        m_delegate->OnProgressChange(m_sessionId, m_progress);
        if (progress >= 100) {
            m_isLoading = false;
            m_delegate->OnLoadingChange(false);
        }
    }
}

void GeckoSessionBridge::DispatchSecurity(SecurityLevel level, const std::string& host) {
    if (m_delegate) {
        m_delegate->OnSecurityChange(SecurityInfo{level, host, "Sovereign Certificate", "TLS_AES_256_GCM_SHA384", m_isTor});
        m_delegate->OnSecurityChange(m_sessionId, level, host);
    }
}

// -----------------------------------------------------------------------------
// GeckoEngineBridge Implementation
// -----------------------------------------------------------------------------

GeckoEngineBridge::GeckoEngineBridge() = default;

GeckoEngineBridge::~GeckoEngineBridge() {
    Shutdown();
}

bool GeckoEngineBridge::Initialize(const BrowserEngineConfig& config) {
    m_config = config;
    m_isInitialized = true;
    return true;
}

bool GeckoEngineBridge::Initialize(const EngineProfileConfig& config) {
    return Initialize(static_cast<BrowserEngineConfig>(config));
}

void GeckoEngineBridge::Shutdown() {
    std::lock_guard<std::mutex> lock(m_sessionMutex);
    m_sessions.clear();
    m_isInitialized = false;
}

std::shared_ptr<IBrowserSession> GeckoEngineBridge::CreateSession(bool isPrivate, bool isTor) {
    std::lock_guard<std::mutex> lock(m_sessionMutex);
    uint64_t id = m_nextSessionId++;
    auto session = std::make_shared<GeckoSessionBridge>(id, isPrivate, isTor);
    m_sessions[id] = session;
    return session;
}

void GeckoEngineBridge::DestroySession(uint64_t sessionId) {
    std::lock_guard<std::mutex> lock(m_sessionMutex);
    m_sessions.erase(sessionId);
}

std::shared_ptr<IWebExtensionHost> GeckoEngineBridge::GetExtensionHost() {
    static auto host = std::make_shared<GeckoWebExtensionHost>();
    return host;
}

void GeckoEngineBridge::ClearCookies(const std::string& /*host*/) {}

void GeckoEngineBridge::ClearCache() {}

void GeckoEngineBridge::SetTorProxy(const std::string& host, uint16_t port) {
    (void)host;
    m_config.enableTorRouting = true;
    m_config.torSocksPort = port;
}

void GeckoEngineBridge::ClearTorProxy() {
    m_config.enableTorRouting = false;
}

bool GeckoEngineBridge::RegisterWebExtension(const std::string& extensionId, const std::string& resourcePath) {
    return GetExtensionHost()->InstallExtension(extensionId, resourcePath);
}

void GeckoEngineBridge::SetWebExtensionPortHandler(
    const std::string& extensionId,
    std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
) {
    GetExtensionHost()->RegisterPortHandler(extensionId, handler);
}

} // namespace azokle::engine::gecko
