#pragma once

#include "azokle/engine/IBrowserEngine.hpp"
#include "IBrowserSession.hpp"
#include "IEngineDelegate.hpp"
#include "IWebExtensionHost.hpp"

namespace azokle::engine::api {
    using BrowserEngineConfig = azokle::engine::BrowserEngineConfig;
    using EngineProfileConfig = azokle::engine::EngineProfileConfig;
    using IBrowserEngine = azokle::engine::IBrowserEngine;
    using IGeckoEngine = azokle::engine::IGeckoEngine;
} // namespace azokle::engine::api
