#include "window/AzokleWin32Window.hpp"
#include "azokle/engine/IBrowserEngine.hpp"
#include "GeckoEngineBridge.hpp"
#include "azokle_core.h"
#include <iostream>
#include <memory>

int main(int argc, char* argv[]) {
    std::cout << "=================================================================\n";
    std::cout << "  Azokle Sovereign Browser for Windows (C++20 / Win32 / Gecko)    \n";
    std::cout << "=================================================================\n";

    // 1. Initialize Rust Cross-Platform Core
    azokle_core_init();
    std::cout << "[AzokleCore] Native Rust C-ABI engine initialized.\n";

    // 2. Initialize Gecko Rendering Engine
    auto engine = std::make_shared<azokle::engine::gecko::GeckoEngineBridge>();
    azokle::engine::BrowserEngineConfig config{
        "C:\\Users\\Default\\AppData\\Local\\Azokle\\profile",
        "C:\\Users\\Default\\AppData\\Local\\Azokle\\cache",
        "C:\\Users\\Default\\AppData\\Local\\Azokle\\data",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
        false,
        9050,
        true,
        true,
        false
    };
    engine->Initialize(config);
    std::cout << "[GeckoEngine] Mozilla Quantum Gecko backend initialized.\n";

    // 3. Create Shared Desktop Application Controller
    auto app = std::make_shared<azokle::desktop::DesktopBrowserApp>(engine);

    // 4. Create Win32 Presentation Window
    auto window = std::make_shared<azokle::windows_app::AzokleWin32Window>(app);
    app->SetUiObserver(window);
    window->Initialize(nullptr, 1);

    // 5. Open Default Sovereign Tab
    app->OpenTab("https://search.azokle.com");

    std::cout << "[AzokleWindows] Ready. Presentation shell operational.\n";
    return 0;
}
