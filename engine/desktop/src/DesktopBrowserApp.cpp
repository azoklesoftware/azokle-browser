#include "azokle/desktop/DesktopBrowserApp.hpp"
#include "azokle_core.h"
#include <iostream>

namespace azokle::desktop {

DesktopBrowserApp::DesktopBrowserApp(std::shared_ptr<engine::IBrowserEngine> engine)
    : m_engine(std::move(engine)) {}

void DesktopBrowserApp::SetUiObserver(std::shared_ptr<IDesktopUiObserver> observer) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_observer = observer;
}

std::shared_ptr<DesktopTab> DesktopBrowserApp::OpenTab(const std::string& url, bool isPrivate, bool isTor) {
    std::lock_guard<std::mutex> lock(m_mutex);
    auto session = m_engine->CreateSession(isPrivate, isTor);
    auto tab = std::make_shared<DesktopTab>();
    tab->id = session->GetSessionId();
    tab->title = "New Tab";
    tab->url = url;
    tab->isPrivate = isPrivate;
    tab->isTor = isTor;
    tab->session = session;

    // Attach ourself as delegate for events
    session->SetDelegate(shared_from_this());

    m_tabs.push_back(tab);
    m_activeIndex = static_cast<int>(m_tabs.size()) - 1;

    if (auto obs = m_observer.lock()) {
        obs->OnTabCreated(tab->id, tab->title, tab->url);
        obs->OnTabActivated(tab->id);
    }

    session->LoadUri(url);
    return tab;
}

void DesktopBrowserApp::CloseTab(uint64_t tabId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    for (auto it = m_tabs.begin(); it != m_tabs.end(); ++it) {
        if ((*it)->id == tabId) {
            (*it)->session->Close();
            m_tabs.erase(it);
            break;
        }
    }

    if (m_activeIndex >= static_cast<int>(m_tabs.size())) {
        m_activeIndex = static_cast<int>(m_tabs.size()) - 1;
    }

    if (auto obs = m_observer.lock()) {
        obs->OnTabClosed(tabId);
        if (m_activeIndex >= 0) {
            obs->OnTabActivated(m_tabs[m_activeIndex]->id);
        }
    }
}

void DesktopBrowserApp::SelectTab(uint64_t tabId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i]->id == tabId) {
            m_activeIndex = static_cast<int>(i);
            if (auto obs = m_observer.lock()) {
                obs->OnTabActivated(tabId);
            }
            break;
        }
    }
}

std::shared_ptr<DesktopTab> DesktopBrowserApp::GetActiveTab() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_activeIndex >= 0 && m_activeIndex < static_cast<int>(m_tabs.size())) {
        return m_tabs[m_activeIndex];
    }
    return nullptr;
}

std::vector<std::shared_ptr<DesktopTab>> DesktopBrowserApp::GetTabs() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_tabs;
}

void DesktopBrowserApp::Navigate(const std::string& url) {
    auto tab = GetActiveTab();
    if (!tab) return;

    // AZK-LOW-12: Enforce scheme allowlist (http, https, and internal safe schemes)
    if (url.rfind("http://", 0) != 0 && url.rfind("https://", 0) != 0 &&
        url.rfind("azokle://", 0) != 0 && url != "about:blank") {
        std::cerr << "[DesktopBrowserApp] Blocked navigation attempt to prohibited scheme: " << url << "\n";
        return;
    }

    if (m_isShieldActive) {
        auto decision = azokle_core_check_url(url.c_str(), tab->url.c_str(), false);
        if (decision.should_block) {
            m_shieldBlockedCount++;
            std::cout << "[AzokleShield] Intercepted blocked request: " << url
                      << " (Category: " << static_cast<int>(decision.category) << ")\n";
            if (auto obs = m_observer.lock()) {
                obs->OnShieldMetricsUpdated(m_shieldBlockedCount);
            }
            return;
        }
    }

    tab->session->LoadUri(url);
}

void DesktopBrowserApp::Reload() {
    auto tab = GetActiveTab();
    if (tab) {
        tab->session->Reload();
    }
}

void DesktopBrowserApp::GoBack() {
    auto tab = GetActiveTab();
    if (tab && tab->session->CanGoBack()) {
        tab->session->GoBack();
    }
}

void DesktopBrowserApp::GoForward() {
    auto tab = GetActiveTab();
    if (tab && tab->session->CanGoForward()) {
        tab->session->GoForward();
    }
}

void DesktopBrowserApp::ToggleTor() {
    m_isTorActive = !m_isTorActive;
    if (m_isTorActive) {
        m_engine->SetTorProxy("127.0.0.1", 9050);
    } else {
        m_engine->ClearTorProxy();
    }
}

void DesktopBrowserApp::ToggleShield() {
    m_isShieldActive = !m_isShieldActive;
}

// --- IEngineDelegate Overrides ---

void DesktopBrowserApp::OnLocationChange(const std::string& url) {
    auto tab = GetActiveTab();
    if (tab) {
        tab->url = url;
        if (auto obs = m_observer.lock()) {
            obs->OnUrlChanged(tab->id, url);
        }
    }
}

void DesktopBrowserApp::OnTitleChange(const std::string& title) {
    auto tab = GetActiveTab();
    if (tab) {
        tab->title = title;
        if (auto obs = m_observer.lock()) {
            obs->OnTitleChanged(tab->id, title);
        }
    }
}

void DesktopBrowserApp::OnProgressChange(int progress) {
    auto tab = GetActiveTab();
    if (tab) {
        if (auto obs = m_observer.lock()) {
            obs->OnProgressChanged(tab->id, progress / 100.0);
        }
    }
}

void DesktopBrowserApp::OnLoadingChange(bool isLoading) {
    auto tab = GetActiveTab();
    if (tab) {
        if (auto obs = m_observer.lock()) {
            obs->OnLoadingStateChanged(tab->id, isLoading);
        }
    }
}

void DesktopBrowserApp::OnSecurityChange(const engine::SecurityInfo& securityInfo) {
    auto tab = GetActiveTab();
    if (tab) {
        if (auto obs = m_observer.lock()) {
            obs->OnSecurityStateChanged(tab->id, securityInfo.level, securityInfo.host);
        }
    }
}

void DesktopBrowserApp::OnCanGoBackChanged(bool /*canGoBack*/) {}
void DesktopBrowserApp::OnCanGoForwardChanged(bool /*canGoForward*/) {}

} // namespace azokle::desktop
