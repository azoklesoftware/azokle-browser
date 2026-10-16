#include "window/AzokleBrowserWindow.hpp"
#include "azokle/engine/IGeckoEngine.hpp"
#include "GeckoEngineBridge.hpp"
#include "azokle_core.h"
#include <iostream>
#include <memory>

int main(int argc, char* argv[]) {
    std::cout << "=================================================================\n";
    std::cout << "  Azokle Browser for Linux (C++20 / GTK / GeckoEngine)\n";
    std::cout << "=================================================================\n";

    // 1. Initialize Rust Cross-Platform Core
    azokle_core_init();
    std::cout << "[AzokleCore] Native Rust C-ABI engine initialized.\n";

    // 2. Initialize Gecko Rendering Engine
    auto engine = std::make_shared<azokle::engine::gecko::GeckoEngineBridge>();
    const char* homeEnv = std::getenv("HOME");
    std::string homeDir = homeEnv ? homeEnv : ".";
    azokle::engine::EngineProfileConfig config{
        homeDir + "/.azokle/profile",
        homeDir + "/.azokle/cache",
        false,
        9050,
        false,
        true,
        ""
    };
    engine->Initialize(config);
    std::cout << "[GeckoEngine] Cross-platform Gecko Runtime initialized.\n";

    // 3. Create & Show Main Browser Window (enters GTK event loop)
    auto window = std::make_unique<azokle::linux_app::AzokleBrowserWindow>(engine);
    window->Show();

    std::cout << "[AzokleBrowser] Exiting cleanly.\n";
    return 0;
}
