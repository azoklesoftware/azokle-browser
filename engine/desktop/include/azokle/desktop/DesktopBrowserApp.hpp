#pragma once

#include "azokle/engine/IBrowserEngine.hpp"
#include "azokle/engine/IBrowserSession.hpp"
#include "azokle/engine/IEngineDelegate.hpp"
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <functional>

namespace azokle::desktop {

/**
 * @brief Represents a single browser tab on desktop platforms.
 */
struct DesktopTab {
    uint64_t id;
    std::string title{"New Tab"};
    std::string url{"about:blank"};
    bool isPrivate{false};
    bool isTor{false};
    std::shared_ptr<engine::IBrowserSession> session;
};

/**
 * @brief UI callbacks observer interface implemented by native desktop presentation shells.
 */
class IDesktopUiObserver {
public:
    virtual ~IDesktopUiObserver() = default;

    virtual void OnTabCreated(uint64_t tabId, const std::string& title, const std::string& url) = 0;
    virtual void OnTabClosed(uint64_t tabId) = 0;
    virtual void OnTabActivated(uint64_t tabId) = 0;
    virtual void OnUrlChanged(uint64_t tabId, const std::string& url) = 0;
    virtual void OnTitleChanged(uint64_t tabId, const std::string& title) = 0;
    virtual void OnProgressChanged(uint64_t tabId, double progress) = 0;
    virtual void OnLoadingStateChanged(uint64_t tabId, bool isLoading) = 0;
    virtual void OnSecurityStateChanged(uint64_t tabId, engine::SecurityLevel level, const std::string& host) = 0;
    virtual void OnShieldMetricsUpdated(uint64_t totalBlocked) = 0;
};

/**
 * @brief Universal desktop application controller shared across Linux, Windows, and macOS.
 */
class DesktopBrowserApp : public engine::IEngineDelegate, public std::enable_shared_from_this<DesktopBrowserApp> {
public:
    explicit DesktopBrowserApp(std::shared_ptr<engine::IBrowserEngine> engine);
    ~DesktopBrowserApp() override = default;

    /**
     * @brief Registers the native presentation window observer.
     */
    void SetUiObserver(std::shared_ptr<IDesktopUiObserver> observer);

    /**
     * @brief Creates a new browser tab with given privacy attributes.
     */
    std::shared_ptr<DesktopTab> OpenTab(const std::string& url = "https://search.azokle.com", bool isPrivate = false, bool isTor = false);

    /**
     * @brief Closes an existing tab by identifier.
     */
    void CloseTab(uint64_t tabId);

    /**
     * @brief Activates a specific tab.
     */
    void SelectTab(uint64_t tabId);

    /**
     * @brief Returns the currently active tab.
     */
    std::shared_ptr<DesktopTab> GetActiveTab() const;

    /**
     * @brief Returns all currently open tabs.
     */
    std::vector<std::shared_ptr<DesktopTab>> GetTabs() const;

    /**
     * @brief Navigates the active tab to a URL after evaluating Azokle Shield policies.
     */
    void Navigate(const std::string& url);

    /**
     * @brief Reloads the active tab.
     */
    void Reload();

    /**
     * @brief Navigates back in active tab history.
     */
    void GoBack();

    /**
     * @brief Navigates forward in active tab history.
     */
    void GoForward();

    /**
     * @brief Toggles Tor circuit routing globally.
     */
    void ToggleTor();

    /**
     * @brief Checks if Tor is active.
     */
    bool IsTorActive() const { return m_isTorActive; }

    /**
     * @brief Toggles Azokle Shield protection on or off.
     */
    void ToggleShield();

    /**
     * @brief Checks if Azokle Shield is currently active.
     */
    bool IsShieldActive() const { return m_isShieldActive; }

    // --- IEngineDelegate Overrides ---
    void OnLocationChange(const std::string& url) override;
    void OnTitleChange(const std::string& title) override;
    void OnProgressChange(int progress) override;
    void OnLoadingChange(bool isLoading) override;
    void OnSecurityChange(const engine::SecurityInfo& securityInfo) override;
    void OnCanGoBackChanged(bool canGoBack) override;
    void OnCanGoForwardChanged(bool canGoForward) override;

private:
    std::shared_ptr<engine::IBrowserEngine> m_engine;
    std::weak_ptr<IDesktopUiObserver> m_observer;
    std::vector<std::shared_ptr<DesktopTab>> m_tabs;
    int m_activeIndex{-1};
    bool m_isTorActive{false};
    bool m_isShieldActive{true};
    uint64_t m_shieldBlockedCount{0};
    mutable std::mutex m_mutex;
};

} // namespace azokle::desktop
