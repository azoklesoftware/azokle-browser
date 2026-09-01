#include "AzokleTabManager.hpp"
#include <algorithm>
#include <chrono>
#include <sstream>

namespace azokle::linux_app {

AzokleTabManager::AzokleTabManager(std::shared_ptr<engine::IGeckoEngine> engine)
    : m_engine(engine) {}

std::shared_ptr<TabItem> AzokleTabManager::CreateTab(const std::string& url, bool isPrivate, bool isTor) {
    auto session = m_engine ? m_engine->CreateSession(isPrivate, isTor) : nullptr;
    
    auto now = std::chrono::system_clock::now().time_since_epoch().count();
    std::ostringstream ss;
    ss << "tab_" << now;

    auto tab = std::make_shared<TabItem>();
    tab->id = ss.str();
    tab->title = "New Tab";
    tab->url = url;
    tab->isPrivate = isPrivate;
    tab->isTor = isTor;
    tab->session = session;

    if (session) {
        session->LoadUri(url);
    }

    m_tabs.push_back(tab);
    m_activeIndex = (int)m_tabs.size() - 1;
    return tab;
}

void AzokleTabManager::CloseTab(const std::string& tabId) {
    auto it = std::find_if(m_tabs.begin(), m_tabs.end(), [&](const auto& t) { return t->id == tabId; });
    if (it != m_tabs.end()) {
        int idx = (int)(it - m_tabs.begin());
        m_tabs.erase(it);
        if (m_tabs.empty()) {
            m_activeIndex = -1;
        } else if (m_activeIndex >= idx) {
            m_activeIndex = std::max(0, m_activeIndex - 1);
        }
    }
}

void AzokleTabManager::SelectTab(const std::string& tabId) {
    auto it = std::find_if(m_tabs.begin(), m_tabs.end(), [&](const auto& t) { return t->id == tabId; });
    if (it != m_tabs.end()) {
        m_activeIndex = (int)(it - m_tabs.begin());
    }
}

std::shared_ptr<TabItem> AzokleTabManager::GetActiveTab() const {
    if (m_activeIndex >= 0 && (size_t)m_activeIndex < m_tabs.size()) {
        return m_tabs[m_activeIndex];
    }
    return nullptr;
}

} // namespace azokle::linux_app
