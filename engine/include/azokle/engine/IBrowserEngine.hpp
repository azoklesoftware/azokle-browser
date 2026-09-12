#pragma once

#include "IBrowserSession.hpp"
#include "IEngineDelegate.hpp"
#include "IWebExtensionBridge.hpp"
#include <string>
#include <memory>
#include <functional>
#include <cstdint>

namespace azokle::engine {

/**
 * @brief Configuration parameters for initializing a browser rendering engine instance.
 */
struct BrowserEngineConfig {
    std::string profileDirectory;      ///< Persistent profile directory for cookies and web storage.
    std::string cacheDirectory;        ///< Temporary cache partition on disk.
    std::string dataDirectory;         ///< Application-specific auxiliary data directory.
    std::string userAgent;             ///< Custom HTTP User-Agent string.
    bool enableTorRouting{false};      ///< If true, route all network traffic through local Tor SOCKS5 proxy.
    uint16_t torSocksPort{9050};       ///< Local Tor SOCKS5 listener port.
    bool totalCookieProtection{true};  ///< Dynamic First-Party isolation partitioning state per eTLD+1.
    bool hardwareAcceleration{true};   ///< Enable GPU-accelerated compositing and WebGL.
    bool privateModeByDefault{false};  ///< Force zero-disk-footprint private mode across all sessions.
};

// Legacy backward-compatibility alias
struct EngineProfileConfig {
    std::string profilePath;
    std::string cachePath;
    bool enableTor{false};
    int torSocksPort{9050};
    bool privateModeByDefault{false};
    bool hardwareAcceleration{true};
    std::string userAgentOverride;

    EngineProfileConfig() = default;

    EngineProfileConfig(
        std::string p,
        std::string c,
        bool tor,
        int port,
        bool priv,
        bool hw,
        std::string ua = ""
    ) : profilePath(std::move(p)),
        cachePath(std::move(c)),
        enableTor(tor),
        torSocksPort(port),
        privateModeByDefault(priv),
        hardwareAcceleration(hw),
        userAgentOverride(std::move(ua)) {}

    // Implicit conversion constructor from BrowserEngineConfig
    EngineProfileConfig(const BrowserEngineConfig& cfg)
        : profilePath(cfg.profileDirectory),
          cachePath(cfg.cacheDirectory),
          enableTor(cfg.enableTorRouting),
          torSocksPort(cfg.torSocksPort),
          privateModeByDefault(cfg.privateModeByDefault),
          hardwareAcceleration(cfg.hardwareAcceleration),
          userAgentOverride(cfg.userAgent) {}

    // Conversion operator to BrowserEngineConfig
    operator BrowserEngineConfig() const {
        BrowserEngineConfig cfg;
        cfg.profileDirectory = profilePath;
        cfg.cacheDirectory = cachePath;
        cfg.enableTorRouting = enableTor;
        cfg.torSocksPort = static_cast<uint16_t>(torSocksPort);
        cfg.privateModeByDefault = privateModeByDefault;
        cfg.hardwareAcceleration = hardwareAcceleration;
        cfg.userAgent = userAgentOverride;
        return cfg;
    }
};

/**
 * @brief Abstract unified interface defining the lifecycle and session factory of a web rendering engine.
 *
 * Implemented by concrete rendering backends (Gecko Quantum, GeckoView, MockBrowserEngine).
 */
class IBrowserEngine {
public:
    virtual ~IBrowserEngine() = default;

    /**
     * @brief Initializes engine sub-processes, profile directories, and security policies.
     * @param config Configuration parameters.
     * @return True if initialized successfully, false otherwise.
     */
    virtual bool Initialize(const BrowserEngineConfig& config) = 0;

    /**
     * @brief Overload accepting legacy EngineProfileConfig.
     */
    virtual bool Initialize(const EngineProfileConfig& config) {
        return Initialize(static_cast<BrowserEngineConfig>(config));
    }

    /**
     * @brief Gracefully terminates background threads, saves profiles, and destroys sessions.
     */
    virtual void Shutdown() = 0;

    /**
     * @brief Checks whether the engine has been successfully initialized and is ready for sessions.
     */
    virtual bool IsInitialized() const = 0;

    /**
     * @brief Spawns an isolated browser window/tab session.
     * @param isPrivate If true, operates entirely in volatile RAM with zero persistent disk storage.
     * @param isTor If true, isolates network routing strictly to the Tor SOCKS5 circuit.
     * @return Shared pointer to the newly allocated session.
     */
    virtual std::shared_ptr<IBrowserSession> CreateSession(bool isPrivate = false, bool isTor = false) = 0;

    /**
     * @brief Explicitly tears down and unregisters a session by identifier.
     */
    virtual void DestroySession(uint64_t sessionId) = 0;

    /**
     * @brief Retrieves the WebExtension host subsystem for extension management.
     */
    virtual std::shared_ptr<IWebExtensionHost> GetExtensionHost() = 0;

    /**
     * @brief Clears HTTP cookies, optionally filtered by host domain.
     */
    virtual void ClearCookies(const std::string& host = "") = 0;

    /**
     * @brief Purges in-memory and on-disk HTTP/network caches.
     */
    virtual void ClearCache() = 0;

    /**
     * @brief Returns true if Tor routing is currently active across the engine.
     */
    virtual bool IsTorActive() const = 0;

    /**
     * @brief Dynamically configures the engine SOCKS5 proxy to route through Tor.
     */
    virtual void SetTorProxy(const std::string& host, uint16_t port) = 0;

    /**
     * @brief Resets engine proxy settings to direct connection.
     */
    virtual void ClearTorProxy() = 0;

    /**
     * @brief Registers a WebExtension with the engine.
     */
    virtual bool RegisterWebExtension(const std::string& extensionId, const std::string& resourcePath) = 0;

    /**
     * @brief Registers a native port handler for a WebExtension.
     */
    virtual void SetWebExtensionPortHandler(
        const std::string& extensionId,
        std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
    ) = 0;
};

// Legacy backward-compatibility alias
using IGeckoEngine = IBrowserEngine;

} // namespace azokle::engine
