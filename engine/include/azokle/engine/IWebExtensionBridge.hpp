#pragma once

#include <string>
#include <memory>
#include <functional>
#include <vector>

namespace azokle::engine {

/**
 * @brief Bidirectional messaging port between native C++ browser core and WebExtension background/content scripts.
 */
class IWebExtensionPort {
public:
    virtual ~IWebExtensionPort() = default;

    /**
     * @brief Transmits a JSON-encoded string payload into the WebExtension message channel.
     * @param jsonMessage Valid JSON string payload.
     */
    virtual void PostMessage(const std::string& jsonMessage) = 0;

    /**
     * @brief Registers an asynchronous incoming message handler callback.
     * @param callback Function invoked when the WebExtension pushes a message to native code.
     */
    virtual void OnMessage(std::function<void(const std::string&)> callback) = 0;

    /**
     * @brief Closes the native communication port.
     */
    virtual void Disconnect() = 0;
};

// Legacy backward-compatibility alias
using IExtensionPort = IWebExtensionPort;

/**
 * @brief Host manager interface for WebExtensions lifecycle and port dispatch.
 */
class IWebExtensionHost {
public:
    virtual ~IWebExtensionHost() = default;

    /**
     * @brief Unpacks and installs a WebExtension from local disk.
     */
    virtual bool InstallExtension(const std::string& extensionId, const std::string& resourcePath) = 0;

    /**
     * @brief Uninstalls a WebExtension.
     */
    virtual bool UninstallExtension(const std::string& extensionId) = 0;

    /**
     * @brief Toggles runtime activation state of an extension.
     */
    virtual bool EnableExtension(const std::string& extensionId, bool enable) = 0;

    /**
     * @brief Retrieves all installed WebExtension identifiers.
     */
    virtual std::vector<std::string> GetInstalledExtensions() const = 0;

    /**
     * @brief Registers a native port connection factory for an extension ID.
     */
    virtual void RegisterPortHandler(
        const std::string& extensionId,
        std::function<void(std::shared_ptr<IWebExtensionPort>)> handler
    ) = 0;
};

using IWebExtensionBridge = IWebExtensionHost;

} // namespace azokle::engine
