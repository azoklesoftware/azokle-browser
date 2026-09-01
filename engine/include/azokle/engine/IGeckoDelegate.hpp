#pragma once

#include <string>
#include <memory>
#include <functional>

namespace azokle::engine {

enum class SecurityLevel {
    Secure,
    Warning,
    Insecure,
    TorCircuitActive
};

struct SecurityInfo {
    SecurityLevel level;
    std::string host;
    std::string issuer;
    std::string cipherSuite;
    bool isTor;
};

class IGeckoDelegate {
public:
    virtual ~IGeckoDelegate() = default;
    virtual void OnLocationChange(const std::string& url) = 0;
    virtual void OnTitleChange(const std::string& title) = 0;
    virtual void OnProgressChange(int progress) = 0;
    virtual void OnLoadingChange(bool isLoading) = 0;
    virtual void OnSecurityChange(const SecurityInfo& securityInfo) = 0;
    virtual void OnCanGoBackChanged(bool canGoBack) = 0;
    virtual void OnCanGoForwardChanged(bool canGoForward) = 0;
    virtual void OnNewWindowRequested(const std::string& url) = 0;
    virtual void OnWebAuthnPrompt(const std::string& origin, const std::string& rpId) = 0;
};

} // namespace azokle::engine
