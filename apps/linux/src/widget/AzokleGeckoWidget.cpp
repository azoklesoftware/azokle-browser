#include "AzokleGeckoWidget.hpp"

namespace azokle::linux_app {

AzokleGeckoWidget::AzokleGeckoWidget(std::shared_ptr<engine::IGeckoSession> session)
    : m_session(session) {}

void AzokleGeckoWidget::SetSession(std::shared_ptr<engine::IGeckoSession> session) {
    m_session = session;
    if (m_session) {
        m_session->ResizeSurface(m_width, m_height);
    }
}

void AzokleGeckoWidget::Resize(int width, int height) {
    m_width = width;
    m_height = height;
    if (m_session) {
        m_session->ResizeSurface(width, height);
    }
}

void AzokleGeckoWidget::Render() {
    // Surface rendering pipeline
}

} // namespace azokle::linux_app
