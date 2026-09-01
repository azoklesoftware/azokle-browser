#pragma once

#include "IGeckoSession.hpp"
#include <string>
#include <memory>

namespace azokle::engine {

struct EngineProfileConfig {
    std::string profilePath;
    std::string cachePath;
    bool enableTor;
    int torSocksPort;
    bool privateModeByDefault;
    bool hardwareAcceleration;
    std::string userAgentOverride;
};

class IGeckoEngine {
public:
    virtual ~IGeckoEngine() = default;
    virtual bool Initialize(const EngineProfileConfig& config) = 0;
    virtual void Shutdown() = 0;
    virtual std::shared_ptr<IGeckoSession> CreateSession(bool isPrivate = false, bool isTor = false) = 0;
    virtual void ClearCookies(const std::string& host = "") = 0;
    virtual void ClearCache() = 0;
    virtual bool IsTorActive() const = 0;
};

} // namespace azokle::engine
