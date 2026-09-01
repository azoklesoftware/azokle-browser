#pragma once

#include "IGeckoDelegate.hpp"
#include <string>
#include <memory>

namespace azokle::engine {

class IGeckoSession {
public:
    virtual ~IGeckoSession() = default;
    virtual void LoadUri(const std::string& uri) = 0;
    virtual void Reload() = 0;
    virtual void GoBack() = 0;
    virtual void GoForward() = 0;
    virtual void Stop() = 0;
    virtual std::string GetCurrentUri() const = 0;
    virtual std::string GetCurrentTitle() const = 0;
    virtual bool CanGoBack() const = 0;
    virtual bool CanGoForward() const = 0;
    virtual bool IsLoading() const = 0;
    virtual bool IsPrivate() const = 0;
    virtual bool IsTor() const = 0;
    virtual void SetDelegate(std::shared_ptr<IGeckoDelegate> delegate) = 0;
    virtual void EvaluateJavaScript(const std::string& script, std::function<void(const std::string&)> callback = nullptr) = 0;
    virtual void InjectCss(const std::string& css) = 0;
    virtual void AttachSurface(void* nativeWindowHandle, int width, int height) = 0;
    virtual void ResizeSurface(int width, int height) = 0;
};

} // namespace azokle::engine
