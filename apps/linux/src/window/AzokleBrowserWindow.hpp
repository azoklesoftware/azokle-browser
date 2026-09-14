#pragma once

#include "azokle/engine/IGeckoEngine.hpp"
#include "azokle/engine/IGeckoDelegate.hpp"
#include "../tabs/AzokleTabManager.hpp"
#include "../widget/AzokleGeckoWidget.hpp"
#include "../security/AzokleShieldController.hpp"
#include <memory>
#include <string>

namespace azokle::linux_app {

class BrowserWindowDelegate : public engine::IGeckoDelegate {
public:
    BrowserWindowDelegate(class AzokleBrowserWindow* window) : m_window(window) {}
    void OnLocationChange(const std::string& url) override;
    void OnTitleChange(const std::string& title) override;
    void OnProgressChange(int progress) override;
    void OnLoadingChange(bool isLoading) override;
    void OnSecurityChange(const engine::SecurityInfo& securityInfo) override;
    void OnCanGoBackChanged(bool canGoBack) override;
    void OnCanGoForwardChanged(bool canGoForward) override;
    void OnNewWindowRequested(const std::string& url) override;
    void OnWebAuthnPrompt(const std::string& origin, const std::string& rpId) override;

private:
    class AzokleBrowserWindow* m_window;
};

class AzokleBrowserWindow {
public:
    AzokleBrowserWindow(std::shared_ptr<engine::IGeckoEngine> engine);
    ~AzokleBrowserWindow() = default;

    void Show();
    void Navigate(const std::string& url);
    void Reload();
    void GoBack();
    void GoForward();
    void ToggleTor();
    void ToggleShield();

    void UpdateUrlBar(const std::string& url);
    void UpdateTitle(const std::string& title);
    void UpdateProgress(int progress);
    void UpdateLoading(bool isLoading);
    void UpdateSecurity(const engine::SecurityInfo& secInfo);
    void HandleNativeIpc(const std::string& json);

private:
    std::shared_ptr<engine::IGeckoEngine> m_engine;
    std::shared_ptr<AzokleTabManager> m_tabManager;
    std::shared_ptr<AzokleGeckoWidget> m_viewWidget;
    std::shared_ptr<BrowserWindowDelegate> m_delegate;
    std::string m_currentUrl;
    std::string m_currentTitle;
    bool m_isTorActive{false};
    bool m_isShieldActive{true};
};

} // namespace azokle::linux_app
