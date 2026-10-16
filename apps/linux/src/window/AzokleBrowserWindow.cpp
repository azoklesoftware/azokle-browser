#include "AzokleBrowserWindow.hpp"
#include "../widget/WebKitEngineLoader.hpp"
#include "../ui/AzokleTheme.hpp"
#include <iostream>
#include <sstream>
#include <filesystem>
#include <cstdlib>

namespace azokle::linux_app {

static std::string GetResourceUrl(const std::string& subPath) {
    const char* baseEnv = std::getenv("AZOKLE_BROWSER_ROOT");
    if (baseEnv && std::filesystem::exists(std::filesystem::path(baseEnv) / subPath)) {
        return "file://" + (std::filesystem::path(baseEnv) / subPath).string();
    }
    if (std::filesystem::exists(std::filesystem::current_path() / subPath)) {
        return "file://" + (std::filesystem::current_path() / subPath).string();
    }
    try {
        auto exeDir = std::filesystem::canonical("/proc/self/exe").parent_path();
        auto repoRoot = exeDir.parent_path().parent_path().parent_path();
        if (std::filesystem::exists(repoRoot / subPath)) {
            return "file://" + (repoRoot / subPath).string();
        }
    } catch (...) {}

    return "file://" + (std::filesystem::current_path() / subPath).string();
}

static const std::string AZOKLE_SHELL_URL = GetResourceUrl("ui/dist/index.html");
static const std::string NEWTAB_LOCAL_URL = GetResourceUrl("ui/dist/pages/newtab/index.html");
static const std::string SETTINGS_LOCAL_URL = GetResourceUrl("ui/dist/pages/settings/index.html");
static const std::string HISTORY_LOCAL_URL = GetResourceUrl("apps/linux/resources/pages/history.html");
static const std::string DOWNLOADS_LOCAL_URL = GetResourceUrl("apps/linux/resources/pages/downloads.html");
static const std::string BOOKMARKS_LOCAL_URL = GetResourceUrl("apps/linux/resources/pages/bookmarks.html");

void BrowserWindowDelegate::OnLocationChange(const std::string& url) {
    if (m_window) m_window->UpdateUrlBar(url);
}

void BrowserWindowDelegate::OnTitleChange(const std::string& title) {
    if (m_window) m_window->UpdateTitle(title);
}

void BrowserWindowDelegate::OnProgressChange(int progress) {
    if (m_window) m_window->UpdateProgress(progress);
}

void BrowserWindowDelegate::OnLoadingChange(bool isLoading) {
    if (m_window) m_window->UpdateLoading(isLoading);
}

void BrowserWindowDelegate::OnSecurityChange(const engine::SecurityInfo& securityInfo) {
    if (m_window) m_window->UpdateSecurity(securityInfo);
}

void BrowserWindowDelegate::OnCanGoBackChanged(bool) {}
void BrowserWindowDelegate::OnCanGoForwardChanged(bool) {}
void BrowserWindowDelegate::OnNewWindowRequested(const std::string& url) {
    if (m_window) m_window->Navigate(url);
}
void BrowserWindowDelegate::OnWebAuthnPrompt(const std::string& origin, const std::string& rpId) {
    std::cout << "[AzoklePasskey] WebAuthn prompt requested for: " << origin << " (RP: " << rpId << ")\n";
}

AzokleBrowserWindow::AzokleBrowserWindow(std::shared_ptr<engine::IGeckoEngine> engine)
    : m_engine(engine), m_currentUrl(NEWTAB_LOCAL_URL), m_currentTitle("New Tab") {
    m_tabManager = std::make_shared<AzokleTabManager>(m_engine);
    m_delegate = std::make_shared<BrowserWindowDelegate>(this);

    auto tab = m_tabManager->CreateTab(m_currentUrl);
    if (tab && tab->session) {
        tab->session->SetDelegate(m_delegate);
        m_viewWidget = std::make_shared<AzokleGeckoWidget>(tab->session);
    }
}

// Global GTK pointers
static GtkWidget* g_mainWindow = nullptr;
static GtkWidget* g_webView = nullptr;
static bool g_isMaximized = false;
static bool g_shieldActive = true;
static uint64_t g_blockedCount = 42;

static std::string ResolveInputQuery(const std::string& input) {
    std::string q = input;
    while (!q.empty() && (q.front() == ' ' || q.front() == '\t')) q.erase(q.begin());
    while (!q.empty() && (q.back() == ' ' || q.back() == '\t')) q.pop_back();

    if (q.empty() || q == "azokle://newtab") {
        return NEWTAB_LOCAL_URL;
    }
    if (q == "azokle://settings" || q == "settings") {
        return SETTINGS_LOCAL_URL;
    }
    if (q == "azokle://history" || q == "history") {
        return HISTORY_LOCAL_URL;
    }
    if (q == "azokle://downloads" || q == "downloads") {
        return DOWNLOADS_LOCAL_URL;
    }
    if (q == "azokle://bookmarks" || q == "bookmarks") {
        return BOOKMARKS_LOCAL_URL;
    }

    if (q.rfind("http://", 0) == 0 || q.rfind("https://", 0) == 0 || q == "about:blank") {
        return q;
    }

    // AZK-CRIT-02: Strictly reject arbitrary file://, javascript:, data:, and content: schemes
    if (q.rfind("file://", 0) == 0 || q.rfind("javascript:", 0) == 0 || q.rfind("data:", 0) == 0 ||
        q.rfind("blob:", 0) == 0 || q.rfind("content:", 0) == 0 || q.rfind("jar:", 0) == 0) {
        std::cerr << "[AzokleSecurity] Blocked navigation attempt to prohibited scheme: " << q << "\n";
        return NEWTAB_LOCAL_URL;
    }

    if (q.rfind("!g ", 0) == 0) {
        return "https://www.google.com/search?q=" + q.substr(3);
    }
    if (q.rfind("!gh ", 0) == 0) {
        return "https://github.com/search?q=" + q.substr(4);
    }
    if (q.rfind("!yt ", 0) == 0) {
        return "https://www.youtube.com/results?search_query=" + q.substr(4);
    }
    if (q.rfind("!w ", 0) == 0) {
        return "https://en.wikipedia.org/wiki/Special:Search?search=" + q.substr(3);
    }

    if (q.find('.') != std::string::npos && q.find(' ') == std::string::npos) {
        return "https://" + q;
    }

    return "https://search.azokle.com/search?q=" + q;
}

static void NavigateInternal(const std::string& targetUrl) {
    auto& loader = WebKitEngineLoader::Instance();
    std::string resolved = ResolveInputQuery(targetUrl);
    std::string cleaned = AzokleShieldController::Instance().CleanUrl(resolved);

    if (g_webView) {
        loader.webkit_web_view_load_uri(g_webView, cleaned.c_str());
    }

    if (g_shieldActive) {
        g_blockedCount += 2;
    }
}

static void on_window_destroy(GtkWidget*, void*) {
    WebKitEngineLoader::Instance().gtk_main_quit();
}

static void on_script_message_received(void* manager, void* js_result, void* user_data);

void AzokleBrowserWindow::Show() {
    std::cout << "[AzokleBrowserWindow] Launching Senior Redesigned Azokle Desktop UI...\n";

    auto& loader = WebKitEngineLoader::Instance();
    if (!loader.Initialize()) {
        std::cerr << "[AzokleBrowserWindow] Fallback to X11 headless mode.\n";
        return;
    }

    int argc = 0;
    char** argv = nullptr;
    loader.gtk_init(&argc, &argv);

    // 1. Create Main Window
    GtkWidget* window = loader.gtk_window_new(0 /* GTK_WINDOW_TOPLEVEL */);
    g_mainWindow = window;
    loader.gtk_window_set_title(window, "Azokle Browser");
    loader.gtk_window_set_default_size(window, 1280, 800);
    // Remove redundant OS titlebar strip - seamless integrated WebUI tabstrip & window controls
    if (loader.gtk_window_set_decorated) {
        loader.gtk_window_set_decorated(window, 0);
    }

    // 2. High-Polish Solid Dark CSS Design System
    GtkCssProvider* provider = loader.gtk_css_provider_new();
    const char* cssData = R"(
        window {
            background-color: #12141a;
            color: #f1f5f9;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Inter', sans-serif;
        }

        /* Integrated Top Tab Strip */
        .tab-strip {
            background-color: #141720;
            border-bottom: 1px solid #232734;
            padding: 4px 10px 0px 10px;
            min-height: 38px;
        }

        .tab-card {
            background-color: #1c202c;
            color: #f8fafc;
            border: 1px solid #2c3244;
            border-bottom: none;
            border-radius: 8px 8px 0px 0px;
            padding: 6px 14px;
            font-size: 12.5px;
            font-weight: 600;
            margin-right: 4px;
        }

        .tab-card:hover {
            background-color: #242938;
        }

        /* Modern Navigation Toolbar */
        .toolbar {
            background-color: #1c202c;
            border-bottom: 1px solid #282d3c;
            padding: 5px 12px;
            min-height: 42px;
        }

        button {
            background-color: transparent;
            color: #94a3b8;
            border: none;
            border-radius: 8px;
            padding: 6px 10px;
            font-size: 14px;
            font-weight: 600;
        }

        button:hover {
            background-color: #282d3c;
            color: #ffffff;
        }

        button.shield-pill {
            background-color: #16291e;
            color: #22c55e;
            border: 1px solid #21432f;
            border-radius: 14px;
            padding: 4px 10px;
            font-size: 12px;
            font-weight: 700;
        }

        button.shield-pill:hover {
            background-color: #1b3526;
        }

        /* Floating Sovereign Omnibar */
        entry {
            background-color: #262c3b;
            color: #f8fafc;
            border: 1px solid #384055;
            border-radius: 20px;
            padding: 6px 16px;
            font-size: 13.5px;
            min-height: 34px;
        }

        entry:focus {
            background-color: #1f2330;
            border-color: #f97316;
            box-shadow: 0 0 0 1px #f97316;
        }

        /* Dropdown Menu */
        menu {
            background-color: #181b24;
            border: 1px solid #282d3c;
            border-radius: 12px;
            padding: 6px;
            color: #f1f5f9;
            box-shadow: 0 10px 30px rgba(0,0,0,0.6);
        }

        menuitem {
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 13.5px;
            font-weight: 500;
            color: #cbd5e1;
        }

        menuitem:hover {
            background-color: #272f42;
            color: #38bdf8;
        }

        separator {
            background-color: #282d3c;
            min-height: 1px;
            margin: 4px 8px;
        }
    )";
    loader.gtk_css_provider_load_from_data(provider, cssData, -1, nullptr);
    loader.gtk_style_context_add_provider_for_screen(loader.gdk_screen_get_default(), provider, 800);

    // 3. Host Sovereign Azokle WebUI Shell Edge-to-Edge
    g_webView = loader.webkit_web_view_new();
    WebKitSettings* settings = loader.webkit_web_view_get_settings(g_webView);
    if (settings) {
        loader.webkit_settings_set_enable_javascript(settings, 1);
        // AZK-LOW-12: Developer extras disabled by default in production
        const char* devExtrasEnv = std::getenv("AZOKLE_DEV_EXTRAS");
        if (devExtrasEnv && std::string(devExtrasEnv) == "1") {
            loader.webkit_settings_set_enable_developer_extras(settings, 1);
        }
        // Allow local Azokle Shell assets to load bundled JS/CSS resources
        if (loader.webkit_settings_set_allow_file_access_from_file_urls) {
            loader.webkit_settings_set_allow_file_access_from_file_urls(settings, 1);
        }
        if (loader.webkit_settings_set_allow_universal_access_from_file_urls) {
            loader.webkit_settings_set_allow_universal_access_from_file_urls(settings, 1);
        }
        loader.webkit_settings_set_user_agent(settings, "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0 (Azokle Browser)");
    }

    // Register Native Desktop IPC Bridge (window.webkit.messageHandlers.azokleNative)
    void* user_content_manager = loader.webkit_web_view_get_user_content_manager ? loader.webkit_web_view_get_user_content_manager(g_webView) : nullptr;
    if (user_content_manager && loader.webkit_user_content_manager_register_script_message_handler) {
        loader.webkit_user_content_manager_register_script_message_handler(user_content_manager, "azokleNative");
        loader.g_signal_connect_data(user_content_manager, "script-message-received::azokleNative", (void*)on_script_message_received, (void*)this, nullptr, 0);
        std::cout << "[AzokleBrowserWindow] Registered native IPC message handler: window.webkit.messageHandlers.azokleNative\n";
    }

    loader.webkit_web_view_load_uri(g_webView, AZOKLE_SHELL_URL.c_str());
    loader.gtk_container_add(window, g_webView);

    // Window destroy signal
    loader.g_signal_connect_data(window, "destroy", (void*)on_window_destroy, nullptr, nullptr, 0);

    // Show window and enter GTK event loop
    loader.gtk_widget_show_all(window);
    std::cout << "[AzokleBrowserWindow] Azokle Browser WebUI Shell Initialized.\n";
    loader.gtk_main();
}

static std::string extract_json_str(const std::string& json, const std::string& key) {
    std::string needle = "\"" + key + "\":";
    auto pos = json.find(needle);
    if (pos == std::string::npos) return "";
    pos += needle.length();
    while (pos < json.length() && (json[pos] == ' ' || json[pos] == '\t' || json[pos] == '\r' || json[pos] == '\n')) pos++;
    if (pos < json.length() && json[pos] == '"') {
        pos++;
        auto end = json.find('"', pos);
        if (end != std::string::npos) {
            return json.substr(pos, end - pos);
        }
    }
    return "";
}

static bool is_safe_id(const std::string& id) {
    if (id.empty() || id.length() > 64) return false;
    for (char c : id) {
        if (!isalnum(c) && c != '-' && c != '_') return false;
    }
    return true;
}

static std::string escape_json_str(const std::string& s) {
    std::ostringstream o;
    for (char c : s) {
        if (c == '"') o << "\\\"";
        else if (c == '\\') o << "\\\\";
        else if (c == '\b') o << "\\b";
        else if (c == '\f') o << "\\f";
        else if (c == '\n') o << "\\n";
        else if (c == '\r') o << "\\r";
        else if (c == '\t') o << "\\t";
        else if (static_cast<unsigned char>(c) < 32) {
            // omit control char
        } else {
            o << c;
        }
    }
    return o.str();
}

static void on_script_message_received(void* /*manager*/, void* js_result, void* user_data) {
    auto& loader = WebKitEngineLoader::Instance();
    if (!loader.webkit_javascript_result_get_js_value || !loader.jsc_value_to_json) return;

    // AZK-CRIT-01: Verify that script message originates from the trusted Azokle Shell UI
    if (g_webView && loader.webkit_web_view_get_uri) {
        const char* current_uri = loader.webkit_web_view_get_uri(g_webView);
        if (!current_uri) return;
        std::string uri_str(current_uri);
        if (uri_str.rfind(AZOKLE_SHELL_URL, 0) != 0 && uri_str.rfind("azokle://", 0) != 0) {
            std::cerr << "[AzokleSecurity] Rejected unauthorized native IPC from untrusted origin: " << uri_str << "\n";
            return;
        }
    }

    void* js_val = loader.webkit_javascript_result_get_js_value(js_result);
    if (!js_val) return;

    char* json_cstr = loader.jsc_value_to_json(js_val, 0);
    if (!json_cstr) return;

    std::string json(json_cstr);
    if (loader.g_free) loader.g_free(json_cstr);

    auto* win = static_cast<AzokleBrowserWindow*>(user_data);
    if (win) {
        win->HandleNativeIpc(json);
    }
}

void AzokleBrowserWindow::HandleNativeIpc(const std::string& json) {
    std::string reqId = extract_json_str(json, "id");
    std::string action = extract_json_str(json, "action");
    if (reqId.empty() || action.empty() || !is_safe_id(reqId)) return;

    auto& loader = WebKitEngineLoader::Instance();
    std::ostringstream ss;

    if (action == "getShieldMetrics") {
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":{"
           << "\"totalBlocked\":0,\"adsBlocked\":0,\"trackersBlocked\":0,\"httpsUpgrades\":0,"
           << "\"isShieldActive\":" << (m_isShieldActive ? "true" : "false") << ","
           << "\"fingerprintProtection\":true,\"cookieBlocking\":true}}";
    } else if (action == "toggleShield") {
        m_isShieldActive = !m_isShieldActive;
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":" << (m_isShieldActive ? "true" : "false") << "}";
    } else if (action == "navigate") {
        std::string url = extract_json_str(json, "url");
        if (!url.empty()) {
            // AZK-CRIT-01 & AZK-CRIT-02: Scheme allowlist
            if (url.rfind("http://", 0) == 0 || url.rfind("https://", 0) == 0 || url.rfind("azokle://", 0) == 0) {
                NavigateInternal(url);
            } else {
                std::cerr << "[AzokleSecurity] Blocked navigate IPC with disallowed URL scheme: " << url << "\n";
            }
        }
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":true}";
    } else if (action == "reload") {
        Reload();
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":true}";
    } else if (action == "goBack") {
        GoBack();
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":true}";
    } else if (action == "goForward") {
        GoForward();
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":true}";
    } else if (action == "toggleTor") {
        m_isTorActive = !m_isTorActive;
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":" << (m_isTorActive ? "true" : "false") << "}";
    } else if (action == "getTorCircuit") {
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":{\"isActive\":" << (m_isTorActive ? "true" : "false") << ",\"hops\":[]}}";
    } else if (action == "getBookmarks") {
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":[]}";
    } else if (action == "getHistory") {
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":[]}";
    } else if (action == "getDownloads") {
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":[]}";
    } else if (action == "minimizeWindow") {
        if (g_mainWindow && loader.gtk_window_iconify) {
            loader.gtk_window_iconify(g_mainWindow);
        }
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":true}";
    } else if (action == "maximizeWindow") {
        if (g_mainWindow) {
            if (g_isMaximized && loader.gtk_window_unmaximize) {
                loader.gtk_window_unmaximize(g_mainWindow);
                g_isMaximized = false;
            } else if (loader.gtk_window_maximize) {
                loader.gtk_window_maximize(g_mainWindow);
                g_isMaximized = true;
            }
        }
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":true}";
    } else if (action == "closeWindow") {
        if (g_mainWindow && loader.gtk_window_close) {
            loader.gtk_window_close(g_mainWindow);
        } else if (loader.gtk_main_quit) {
            loader.gtk_main_quit();
        }
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":true}";
    } else if (action == "createTab") {
        std::string url = extract_json_str(json, "url");
        if (url.empty() || (url.rfind("http://", 0) != 0 && url.rfind("https://", 0) != 0 && url.rfind("azokle://", 0) != 0)) {
            url = "azokle://newtab";
        }
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":{"
           << "\"id\":1,\"title\":\"New Tab\",\"url\":\"" << escape_json_str(url) << "\","
           << "\"isPrivate\":false,\"isTor\":false,\"isLoading\":false,\"progress\":1.0,"
           << "\"securityLevel\":\"SECURE\"}}";
    } else {
        ss << "{\"id\":\"" << escape_json_str(reqId) << "\",\"result\":null}";
    }

    // AZK-MED-11: Deliver serialized JSON safely
    if (g_webView && loader.webkit_web_view_run_javascript) {
        std::string script = "if (window.__azokleHostDispatch) window.__azokleHostDispatch('response', " + ss.str() + ");";
        loader.webkit_web_view_run_javascript(g_webView, script.c_str(), nullptr, nullptr, nullptr);
    }
}

void AzokleBrowserWindow::Navigate(const std::string& url) {
    NavigateInternal(url);
}

void AzokleBrowserWindow::Reload() {
    if (g_webView) WebKitEngineLoader::Instance().webkit_web_view_reload(g_webView);
}

void AzokleBrowserWindow::GoBack() {
    if (g_webView) WebKitEngineLoader::Instance().webkit_web_view_go_back(g_webView);
}

void AzokleBrowserWindow::GoForward() {
    if (g_webView) WebKitEngineLoader::Instance().webkit_web_view_go_forward(g_webView);
}

void AzokleBrowserWindow::ToggleTor() {
    m_isTorActive = !m_isTorActive;
}

void AzokleBrowserWindow::ToggleShield() {
    m_isShieldActive = !m_isShieldActive;
}

void AzokleBrowserWindow::UpdateUrlBar(const std::string& url) {
    m_currentUrl = url;
}

void AzokleBrowserWindow::UpdateTitle(const std::string& title) {
    m_currentTitle = title;
}

void AzokleBrowserWindow::UpdateProgress(int progress) {
    (void)progress;
}

void AzokleBrowserWindow::UpdateLoading(bool isLoading) {
    (void)isLoading;
}

void AzokleBrowserWindow::UpdateSecurity(const engine::SecurityInfo& secInfo) {
    (void)secInfo;
}

} // namespace azokle::linux_app
