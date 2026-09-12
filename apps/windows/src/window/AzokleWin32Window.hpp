#pragma once

#include "azokle/desktop/DesktopBrowserApp.hpp"
#include <memory>
#include <string>

#ifdef _WIN32
#include <windows.h>
#else
// Cross-compilation stubs for non-Windows host analysis
using HWND = void*;
using HINSTANCE = void*;
using LRESULT = long long;
using UINT = unsigned int;
using WPARAM = unsigned long long;
using LPARAM = long long;
#endif

namespace azokle::windows_app {

/**
 * @brief Native Win32 desktop presentation shell implementing IDesktopUiObserver.
 */
class AzokleWin32Window : public desktop::IDesktopUiObserver, public std::enable_shared_from_this<AzokleWin32Window> {
public:
    explicit AzokleWin32Window(std::shared_ptr<desktop::DesktopBrowserApp> appController);
    ~AzokleWin32Window() override = default;

    bool Initialize(HINSTANCE hInstance, int nCmdShow);
    void RunMessageLoop();

    // IDesktopUiObserver callbacks
    void OnTabCreated(uint64_t tabId, const std::string& title, const std::string& url) override;
    void OnTabClosed(uint64_t tabId) override;
    void OnTabActivated(uint64_t tabId) override;
    void OnUrlChanged(uint64_t tabId, const std::string& url) override;
    void OnTitleChanged(uint64_t tabId, const std::string& title) override;
    void OnProgressChanged(uint64_t tabId, double progress) override;
    void OnLoadingStateChanged(uint64_t tabId, bool isLoading) override;
    void OnSecurityStateChanged(uint64_t tabId, engine::SecurityLevel level, const std::string& host) override;
    void OnShieldMetricsUpdated(uint64_t totalBlocked) override;

private:
    std::shared_ptr<desktop::DesktopBrowserApp> m_app;
    HWND m_hWnd{nullptr};
    HWND m_hUrlBar{nullptr};
    HWND m_hStatusBar{nullptr};
    HWND m_hRenderSurface{nullptr};
};

} // namespace azokle::windows_app
