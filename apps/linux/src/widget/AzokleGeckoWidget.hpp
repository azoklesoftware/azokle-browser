#pragma once

#include "azokle/engine/IGeckoSession.hpp"
#include <memory>
#include <functional>

namespace azokle::linux_app {

class AzokleGeckoWidget {
public:
    AzokleGeckoWidget(std::shared_ptr<engine::IGeckoSession> session);
    ~AzokleGeckoWidget() = default;

    void SetSession(std::shared_ptr<engine::IGeckoSession> session);
    std::shared_ptr<engine::IGeckoSession> GetSession() const { return m_session; }

    void Resize(int width, int height);
    void Render();

private:
    std::shared_ptr<engine::IGeckoSession> m_session;
    int m_width{1280};
    int m_height{800};
};

} // namespace azokle::linux_app
