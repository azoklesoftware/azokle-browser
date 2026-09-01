#pragma once

#include "azokle/engine/IGeckoEngine.hpp"
#include "azokle/engine/IGeckoSession.hpp"
#include <vector>
#include <memory>
#include <string>

namespace azokle::linux_app {

struct TabItem {
    std::string id;
    std::string title;
    std::string url;
    bool isPrivate;
    bool isTor;
    std::shared_ptr<engine::IGeckoSession> session;
};

class AzokleTabManager {
public:
    AzokleTabManager(std::shared_ptr<engine::IGeckoEngine> engine);
    ~AzokleTabManager() = default;

    std::shared_ptr<TabItem> CreateTab(const std::string& url = "https://search.azokle.com", bool isPrivate = false, bool isTor = false);
    void CloseTab(const std::string& tabId);
    void SelectTab(const std::string& tabId);
    std::shared_ptr<TabItem> GetActiveTab() const;
    const std::vector<std::shared_ptr<TabItem>>& GetTabs() const { return m_tabs; }

private:
    std::shared_ptr<engine::IGeckoEngine> m_engine;
    std::vector<std::shared_ptr<TabItem>> m_tabs;
    int m_activeIndex{-1};
};

} // namespace azokle::linux_app
