#include "GeckoEngineBridge.hpp"
#include <iostream>
#include <sstream>

namespace azokle::engine::gecko {

GeckoSessionBridge::GeckoSessionBridge(bool isPrivate, bool isTor)
    : m_isPrivate(isPrivate), m_isTor(isTor), m_currentUri("about:blank"), m_currentTitle("New Tab") {}

void GeckoSessionBridge::LoadUri(const std::string& uri) {
    if (uri.empty()) return;
    m_currentUri = uri;
    m_isLoading = true;

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
    }
}

void GeckoSessionBridge::Reload() {
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
    if (m_delegate) {
        m_delegate->OnLoadingChange(false);
    }
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
    if (m_delegate) {
        m_delegate->OnProgressChange(progress);
        if (progress >= 100) {
            m_isLoading = false;
            m_delegate->OnLoadingChange(false);
        }
    }
}

void GeckoSessionBridge::DispatchSecurity(SecurityLevel level, const std::string& host) {
    if (m_delegate) {
        m_delegate->OnSecurityChange(SecurityInfo{level, host, "Sovereign Certificate", "TLS_AES_256_GCM_SHA384", m_isTor});
    }
}

// -----------------------------------------------------------------------------
// Engine Factory
// -----------------------------------------------------------------------------

GeckoEngineBridge::GeckoEngineBridge() = default;
GeckoEngineBridge::~GeckoEngineBridge() {
    Shutdown();
}

bool GeckoEngineBridge::Initialize(const EngineProfileConfig& config) {
    m_config = config;
    m_isInitialized = true;
    return true;
}

void GeckoEngineBridge::Shutdown() {
    m_isInitialized = false;
}

std::shared_ptr<IGeckoSession> GeckoEngineBridge::CreateSession(bool isPrivate, bool isTor) {
    std::lock_guard<std::mutex> lock(m_sessionMutex);
    auto session = std::make_shared<GeckoSessionBridge>(isPrivate, isTor);
    m_sessions.push_back(session);
    return session;
}

void GeckoEngineBridge::ClearCookies(const std::string& host) {
    (void)host;
}

void GeckoEngineBridge::ClearCache() {}

} // namespace azokle::engine::gecko
