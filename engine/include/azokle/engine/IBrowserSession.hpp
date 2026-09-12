#pragma once

#include "IEngineDelegate.hpp"
#include <string>
#include <functional>
#include <memory>
#include <cstdint>

namespace azokle::engine {

/**
 * @brief Unified canonical interface representing an individual browser web session (tab or window).
 *
 * Implemented by rendering backends (Gecko Quantum, GeckoView, MockBrowserEngine).
 */
class IBrowserSession {
public:
    virtual ~IBrowserSession() = default;

    /**
     * @brief Unique integer ID assigned to this session.
     */
    virtual uint64_t GetSessionId() const = 0;

    /**
     * @brief Returns true if this session operates in volatile private mode (zero-persistence RAM isolation).
     */
    virtual bool IsPrivate() const = 0;

    /**
     * @brief Returns true if this session routes its network traffic exclusively through Tor onion circuits.
     */
    virtual bool IsTor() const = 0;

    /**
     * @brief Begins navigating to the specified URI.
     * @param uri Target uniform resource identifier.
     */
    virtual void LoadUri(const std::string& uri) = 0;

    /**
     * @brief Reloads the active page document.
     * @param bypassCache If true, forces fresh network re-fetch bypassing HTTP cache.
     */
    virtual void Reload(bool bypassCache = false) = 0;

    /**
     * @brief Aborts ongoing network requests and page loading.
     */
    virtual void Stop() = 0;

    /**
     * @brief Navigates backward in session history if available.
     */
    virtual void GoBack() = 0;

    /**
     * @brief Navigates forward in session history if available.
     */
    virtual void GoForward() = 0;

    /**
     * @brief Checks if backward history navigation is possible.
     */
    virtual bool CanGoBack() const = 0;

    /**
     * @brief Checks if forward history navigation is possible.
     */
    virtual bool CanGoForward() const = 0;

    /**
     * @brief Checks if document navigation or asset fetching is actively in progress.
     */
    virtual bool IsLoading() const = 0;

    /**
     * @brief Returns the canonical active document URI.
     */
    virtual std::string GetCurrentUri() const = 0;

    /**
     * @brief Returns the document title.
     */
    virtual std::string GetCurrentTitle() const = 0;

    /**
     * @brief Alternative getter for document title matching api conventions.
     */
    virtual std::string GetTitle() const { return GetCurrentTitle(); }

    /**
     * @brief Returns the page load progress from 0.0 to 1.0 (or 0 to 100).
     */
    virtual double GetProgress() const = 0;

    /**
     * @brief Registers the delegate observer for browser navigation and security events.
     * @param delegate Shared pointer to event observer.
     */
    virtual void SetDelegate(std::shared_ptr<IEngineDelegate> delegate) = 0;

    /**
     * @brief Asynchronously evaluates an arbitrary JavaScript string within the page execution realm.
     * @param script JavaScript source text to evaluate.
     * @param callback Optional completion handler receiving JSON-encoded result string.
     */
    virtual void EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) = 0;

    /**
     * @brief Alias for EvaluateJavaScript matching legacy api conventions.
     */
    virtual void ExecuteJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) {
        EvaluateJavaScript(script, callback);
    }

    /**
     * @brief Injects custom CSS stylesheets directly into the DOM of the active page.
     * @param css Stylesheet text (e.g. adblock cosmetic element-hiding selectors).
     */
    virtual void InjectCss(const std::string& css) = 0;

    /**
     * @brief Attaches a platform-native window handle / surface for hardware-accelerated rendering.
     * @param nativeWindowHandle Pointer/handle to native window or display surface.
     * @param width Surface width in pixels.
     * @param height Surface height in pixels.
     */
    virtual void AttachSurface(void* nativeWindowHandle, int width, int height) = 0;

    /**
     * @brief Informs the rendering compositor that the native presentation surface was resized.
     * @param width New width in pixels.
     * @param height New height in pixels.
     */
    virtual void ResizeSurface(int width, int height) = 0;

    /**
     * @brief Explicitly terminates this session and releases rendering resources.
     */
    virtual void Close() = 0;
};

// Legacy backward-compatibility alias
using IGeckoSession = IBrowserSession;

} // namespace azokle::engine
