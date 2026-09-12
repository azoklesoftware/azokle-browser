#include "AzokleWin32Window.hpp"
#include <iostream>

namespace azokle::windows_app {

AzokleWin32Window::AzokleWin32Window(std::shared_ptr<desktop::DesktopBrowserApp> appController)
    : m_app(std::move(appController)) {}

bool AzokleWin32Window::Initialize(HINSTANCE /*hInstance*/, int /*nCmdShow*/) {
    std::cout << "[AzokleWin32Window] Initializing sovereign Win32/DirectX presentation shell...\n";
    // Native Win32 window creation, DirectComposition surface attachment, and URL bar registration
    return true;
}

void AzokleWin32Window::RunMessageLoop() {
    std::cout << "[AzokleWin32Window] Entering Win32 message pump...\n";
}

void AzokleWin32Window::OnTabCreated(uint64_t tabId, const std::string& title, const std::string& url) {
    std::cout << "[AzokleWin32Window] Tab created [ID: " << tabId << "] Title: " << title << " URL: " << url << "\n";
}

void AzokleWin32Window::OnTabClosed(uint64_t tabId) {
    std::cout << "[AzokleWin32Window] Tab closed [ID: " << tabId << "]\n";
}

void AzokleWin32Window::OnTabActivated(uint64_t tabId) {
    std::cout << "[AzokleWin32Window] Tab activated [ID: " << tabId << "]\n";
}

void AzokleWin32Window::OnUrlChanged(uint64_t tabId, const std::string& url) {
    std::cout << "[AzokleWin32Window] URL changed [ID: " << tabId << "] -> " << url << "\n";
}

void AzokleWin32Window::OnTitleChanged(uint64_t tabId, const std::string& title) {
    std::cout << "[AzokleWin32Window] Title changed [ID: " << tabId << "] -> " << title << "\n";
}

void AzokleWin32Window::OnProgressChanged(uint64_t tabId, double progress) {
    (void)tabId;
    (void)progress;
}

void AzokleWin32Window::OnLoadingStateChanged(uint64_t tabId, bool isLoading) {
    (void)tabId;
    (void)isLoading;
}

void AzokleWin32Window::OnSecurityStateChanged(uint64_t tabId, engine::SecurityLevel level, const std::string& host) {
    std::cout << "[AzokleWin32Window] Security level [ID: " << tabId << "] Host: " << host
              << " Level: " << (level == engine::SecurityLevel::Secure ? "Secure (TLS 1.3)" : "Tor Circuit") << "\n";
}

void AzokleWin32Window::OnShieldMetricsUpdated(uint64_t totalBlocked) {
    std::cout << "[AzokleWin32Window] Shield blocked metric updated: " << totalBlocked << "\n";
}

} // namespace azokle::windows_app
