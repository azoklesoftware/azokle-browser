#include "MockBrowserEngine.hpp"
#include <iostream>

namespace azokle::engine::mock {

// --- MockBrowserSession ---

MockBrowserSession::MockBrowserSession(uint64_t id, bool isPrivate, bool isTor)
    : m_id(id), m_isPrivate(isPrivate), m_isTor(isTor) {}

void MockBrowserSession::LoadUri(const std::string& uri) {
    std::lock_guard<std::mutex> lock(m_mutex);

    if (m_delegate) {
        auto decision = m_delegate->OnBeforeRequest(m_id, uri, m_currentUri, "main_frame");
        if (decision.action == NavigationAction::Block) {
            m_delegate->OnPageError(m_id, uri, 403, "Blocked by Azokle Shield");
            return;
        } else if (decision.action == NavigationAction::Redirect && !decision.redirectUri.empty()) {
            LoadUri(decision.redirectUri);
            return;
        }
    }

    m_currentUri = uri;
    m_progress = 0.1;
    m_isLoading = true;
    if (m_delegate) {
        m_delegate->OnLocationChange(uri);
        m_delegate->OnLoadingChange(true);
        m_delegate->OnPageStarted(m_id, uri);
        m_delegate->OnProgressChange(m_id, m_progress);
        m_delegate->OnProgressChange(10);
    }

    // Simulate instant mock completion
    m_history.push_back(uri);
    m_historyIndex = static_cast<int>(m_history.size()) - 1;
    m_title = "Mock: " + uri;
    m_progress = 1.0;
    m_isLoading = false;

    if (m_delegate) {
        m_delegate->OnTitleChange(m_title);
        m_delegate->OnTitleChange(m_id, m_title);
        m_delegate->OnProgressChange(100);
        m_delegate->OnProgressChange(m_id, m_progress);
        m_delegate->OnLoadingChange(false);
        SecurityLevel sec = m_isTor ? SecurityLevel::TorCircuitActive : SecurityLevel::Secure;
        m_delegate->OnSecurityChange(SecurityInfo{sec, "mock.local", "Azokle Test CA", "TLS_AES_256_GCM_SHA384", m_isTor});
        m_delegate->OnSecurityChange(m_id, sec, "mock.local");
        m_delegate->OnPageFinished(m_id, uri, 200);
    }
}

void MockBrowserSession::Reload(bool /*bypassCache*/) {
    if (!m_currentUri.empty()) {
        LoadUri(m_currentUri);
    }
}

void MockBrowserSession::Stop() {
    m_progress = 0.0;
    m_isLoading = false;
    if (m_delegate) {
        m_delegate->OnLoadingChange(false);
    }
}

void MockBrowserSession::GoBack() {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (CanGoBack()) {
        m_historyIndex--;
        m_currentUri = m_history[m_historyIndex];
        if (m_delegate) {
            m_delegate->OnLocationChange(m_currentUri);
            m_delegate->OnPageStarted(m_id, m_currentUri);
            m_delegate->OnPageFinished(m_id, m_currentUri, 200);
        }
    }
}

void MockBrowserSession::GoForward() {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (CanGoForward()) {
        m_historyIndex++;
        m_currentUri = m_history[m_historyIndex];
        if (m_delegate) {
            m_delegate->OnLocationChange(m_currentUri);
            m_delegate->OnPageStarted(m_id, m_currentUri);
            m_delegate->OnPageFinished(m_id, m_currentUri, 200);
        }
    }
}

bool MockBrowserSession::CanGoBack() const {
    return m_historyIndex > 0;
}

bool MockBrowserSession::CanGoForward() const {
    return m_historyIndex >= 0 && m_historyIndex < static_cast<int>(m_history.size()) - 1;
}

void MockBrowserSession::EvaluateJavaScript(const std::string& /*script*/, std::function<void(const std::string&)> callback) {
    if (callback) {
        callback("{\"result\": \"mock_success\"}");
    }
}

void MockBrowserSession::InjectCss(const std::string& /*css*/) {}

void MockBrowserSession::AttachSurface(void* nativeWindowHandle, int width, int height) {
    m_windowHandle = nativeWindowHandle;
    m_width = width;
    m_height = height;
}

void MockBrowserSession::ResizeSurface(int width, int height) {
    m_width = width;
    m_height = height;
}

void MockBrowserSession::Close() {
    m_progress = 0.0;
    m_isLoading = false;
}

// --- MockExtensionPort ---

void MockExtensionPort::PostMessage(const std::string& jsonMessage) {
    m_outbox.push_back(jsonMessage);
}

void MockExtensionPort::OnMessage(std::function<void(const std::string&)> callback) {
    m_callback = callback;
}

void MockExtensionPort::Disconnect() {
    m_callback = nullptr;
}

void MockExtensionPort::SimulateMessage(const std::string& message) {
    if (m_callback) {
        m_callback(message);
    }
}

// --- MockWebExtensionHost ---

bool MockWebExtensionHost::InstallExtension(const std::string& extensionId, const std::string& resourcePath) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_installedExtensions[extensionId] = resourcePath;
    return true;
}

bool MockWebExtensionHost::UninstallExtension(const std::string& extensionId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_installedExtensions.erase(extensionId) > 0;
}

bool MockWebExtensionHost::EnableExtension(const std::string& /*extensionId*/, bool /*enable*/) {
    return true;
}

std::vector<std::string> MockWebExtensionHost::GetInstalledExtensions() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    std::vector<std::string> result;
    for (const auto& [id, _] : m_installedExtensions) {
        result.push_back(id);
    }
    return result;
}

void MockWebExtensionHost::RegisterPortHandler(
    const std::string& extensionId,
    std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_portHandlers[extensionId] = handler;
}

// --- MockBrowserEngine ---

bool MockBrowserEngine::Initialize(const BrowserEngineConfig& config) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_config = config;
    m_initialized = true;
    return true;
}

void MockBrowserEngine::Shutdown() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_sessions.clear();
    m_initialized = false;
}

std::shared_ptr<IBrowserSession> MockBrowserEngine::CreateSession(bool isPrivate, bool isTor) {
    std::lock_guard<std::mutex> lock(m_mutex);
    uint64_t id = m_nextSessionId++;
    auto session = std::make_shared<MockBrowserSession>(id, isPrivate, isTor);
    m_sessions[id] = session;
    return session;
}

void MockBrowserEngine::DestroySession(uint64_t sessionId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_sessions.erase(sessionId);
}

std::shared_ptr<IWebExtensionHost> MockBrowserEngine::GetExtensionHost() {
    return m_extensionHost;
}

void MockBrowserEngine::ClearCookies(const std::string& /*host*/) {}
void MockBrowserEngine::ClearCache() {}
void MockBrowserEngine::SetTorProxy(const std::string& /*host*/, uint16_t /*port*/) {
    m_config.enableTorRouting = true;
}
void MockBrowserEngine::ClearTorProxy() {
    m_config.enableTorRouting = false;
}

size_t MockBrowserEngine::GetActiveSessionCount() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_sessions.size();
}

} // namespace azokle::engine::mock
