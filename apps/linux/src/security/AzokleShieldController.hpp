#pragma once

#include "azokle_core.h"
#include <string>
#include <memory>

namespace azokle::linux_app {

class AzokleShieldController {
public:
    static AzokleShieldController& Instance() {
        static AzokleShieldController instance;
        return instance;
    }

    AzokleShieldController() {
        azokle_core_init();
    }

    bool CheckUrl(const std::string& url, const std::string& origin = "", bool isThirdParty = false) {
        AzokleFilterDecision dec = azokle_core_check_url(url.c_str(), origin.empty() ? nullptr : origin.c_str(), isThirdParty);
        bool shouldBlock = dec.should_block;
        azokle_decision_free(&dec);
        return shouldBlock;
    }

    std::string CleanUrl(const std::string& url) {
        char* cleaned = azokle_core_clean_url(url.c_str());
        if (!cleaned) return url;
        std::string res(cleaned);
        azokle_string_free(cleaned);
        return res;
    }

    std::string GetCosmeticCss(const std::string& domain) {
        char* css = azokle_core_get_cosmetic_css(domain.c_str());
        if (!css) return "";
        std::string res(css);
        azokle_string_free(css);
        return res;
    }

    bool ToggleShield(const std::string& host, bool enabled) {
        return azokle_core_toggle_host_shield(host.c_str(), enabled);
    }

    bool IsShieldEnabled(const std::string& host) {
        return azokle_core_is_host_shield_enabled(host.c_str());
    }

    std::string GetStatsJson() {
        char* json = azokle_core_get_stats_json();
        if (!json) return "{}";
        std::string res(json);
        azokle_string_free(json);
        return res;
    }
};

} // namespace azokle::linux_app
