#pragma once

#include <string>
#include <memory>
#include <functional>
#include <cstdint>

namespace azokle::engine {

/**
 * @brief Action to take on a candidate navigation request.
 */
enum class NavigationAction {
    Allow,    ///< Permit the navigation request to proceed.
    Block,    ///< Terminate request immediately.
    Redirect  ///< Rewrite destination URI to a sanitized target.
};

/**
 * @brief Evaluation result produced by security policies for a navigation attempt.
 */
struct NavigationDecision {
    NavigationAction action{NavigationAction::Allow};  ///< Chosen policy enforcement action.
    std::string redirectUri;                          ///< Destination URI if action is Redirect.
    std::string reason;                               ///< Diagnostic rationale for telemetry and UI alerts.
};

/**
 * @brief Security certification and encryption state of the active connection.
 */
enum class SecurityLevel {
    Secure,           ///< Valid TLS certificate with strong modern cipher suite.
    Warning,          ///< Minor TLS discrepancies, mixed content, or weak ciphers.
    Insecure,         ///< Plaintext HTTP, certificate validation failure, or untrusted CA.
    TorCircuitActive, ///< Onion routing active; traffic isolated over Tor network.
    TorCircuit = TorCircuitActive
};

/**
 * @brief Structured cryptographic and security identity metadata.
 */
struct SecurityInfo {
    SecurityLevel level{SecurityLevel::Insecure};
    std::string host;
    std::string issuer;
    std::string cipherSuite;
    bool isTor{false};
};

/**
 * @brief Observer delegate interface for browser session events and lifecycle notifications.
 */
class IEngineDelegate {
public:
    virtual ~IEngineDelegate() = default;

    // Direct session observer callbacks
    virtual void OnLocationChange(const std::string& /*url*/) {}
    virtual void OnTitleChange(const std::string& /*title*/) {}
    virtual void OnProgressChange(int /*progress*/) {}
    virtual void OnLoadingChange(bool /*isLoading*/) {}
    virtual void OnSecurityChange(const SecurityInfo& /*securityInfo*/) {}
    virtual void OnCanGoBackChanged(bool /*canGoBack*/) {}
    virtual void OnCanGoForwardChanged(bool /*canGoForward*/) {}
    virtual void OnNewWindowRequested(const std::string& /*url*/) {}
    virtual void OnWebAuthnPrompt(const std::string& /*origin*/, const std::string& /*rpId*/) {}

    // Multi-session ID observer callbacks (used by mock & multi-session managers)
    virtual NavigationDecision OnBeforeRequest(uint64_t /*sessionId*/, const std::string& /*url*/, const std::string& /*origin*/, const std::string& /*requestType*/) {
        return NavigationDecision{NavigationAction::Allow, "", "Permitted"};
    }
    virtual void OnPageStarted(uint64_t /*sessionId*/, const std::string& /*url*/) {}
    virtual void OnPageFinished(uint64_t /*sessionId*/, const std::string& /*url*/, int /*httpStatusCode*/) {}
    virtual void OnPageError(uint64_t /*sessionId*/, const std::string& /*url*/, int /*errorCode*/, const std::string& /*description*/) {}
    virtual void OnProgressChange(uint64_t /*sessionId*/, double /*progress*/) {}
    virtual void OnTitleChange(uint64_t /*sessionId*/, const std::string& /*title*/) {}
    virtual void OnSecurityChange(uint64_t /*sessionId*/, SecurityLevel /*level*/, const std::string& /*host*/) {}
};

// Legacy backward-compatibility alias
using IGeckoDelegate = IEngineDelegate;

} // namespace azokle::engine
