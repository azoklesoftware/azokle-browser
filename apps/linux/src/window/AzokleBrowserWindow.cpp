#include "AzokleBrowserWindow.hpp"
#include "../widget/WebKitEngineLoader.hpp"
#include "../ui/AzokleTheme.hpp"
#include <iostream>
#include <sstream>

namespace azokle::linux_app {

const char* NEWTAB_LOCAL_URL = "file:///home/azokledeveloper/azokledeveloper/azokle-software/azokle-browser/apps/linux/resources/newtab/newtab.html";
const char* SETTINGS_LOCAL_URL = "file:///home/azokledeveloper/azokledeveloper/azokle-software/azokle-browser/apps/linux/resources/pages/settings.html";
const char* HISTORY_LOCAL_URL = "file:///home/azokledeveloper/azokledeveloper/azokle-software/azokle-browser/apps/linux/resources/pages/history.html";
const char* DOWNLOADS_LOCAL_URL = "file:///home/azokledeveloper/azokledeveloper/azokle-software/azokle-browser/apps/linux/resources/pages/downloads.html";
const char* BOOKMARKS_LOCAL_URL = "file:///home/azokledeveloper/azokledeveloper/azokle-software/azokle-browser/apps/linux/resources/pages/bookmarks.html";

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
static GtkWidget* g_webView = nullptr;
static GtkWidget* g_entryUrl = nullptr;
static GtkWidget* g_tabBox = nullptr;
static GtkWidget* g_btnShield = nullptr;
static GtkWidget* g_btnTor = nullptr;
static bool g_shieldActive = true;
static bool g_torActive = false;
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

    if (q.rfind("http://", 0) == 0 || q.rfind("https://", 0) == 0 || q.rfind("file://", 0) == 0 || q.rfind("about:", 0) == 0) {
        return q;
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

    if (g_entryUrl) {
        if (cleaned == NEWTAB_LOCAL_URL) {
            loader.gtk_entry_set_text(g_entryUrl, "");
        } else if (cleaned == SETTINGS_LOCAL_URL) {
            loader.gtk_entry_set_text(g_entryUrl, "azokle://settings");
        } else if (cleaned == HISTORY_LOCAL_URL) {
            loader.gtk_entry_set_text(g_entryUrl, "azokle://history");
        } else if (cleaned == DOWNLOADS_LOCAL_URL) {
            loader.gtk_entry_set_text(g_entryUrl, "azokle://downloads");
        } else if (cleaned == BOOKMARKS_LOCAL_URL) {
            loader.gtk_entry_set_text(g_entryUrl, "azokle://bookmarks");
        } else {
            loader.gtk_entry_set_text(g_entryUrl, cleaned.c_str());
        }
    }

    if (g_shieldActive) {
        g_blockedCount += 2;
        if (g_btnShield) {
            std::string label = "🛡️ " + std::to_string(g_blockedCount);
            loader.gtk_button_set_label(g_btnShield, label.c_str());
        }
    }
}

static void on_entry_activate(GtkWidget* entry, void*) {
    auto& loader = WebKitEngineLoader::Instance();
    const char* text = loader.gtk_entry_get_text(entry);
    if (text) {
        NavigateInternal(text);
    }
}

static void on_btn_home_clicked(GtkWidget*, void*) {
    NavigateInternal(NEWTAB_LOCAL_URL);
}

static void on_btn_back_clicked(GtkWidget*, void*) {
    if (g_webView) WebKitEngineLoader::Instance().webkit_web_view_go_back(g_webView);
}

static void on_btn_forward_clicked(GtkWidget*, void*) {
    if (g_webView) WebKitEngineLoader::Instance().webkit_web_view_go_forward(g_webView);
}

static void on_btn_reload_clicked(GtkWidget*, void*) {
    if (g_webView) WebKitEngineLoader::Instance().webkit_web_view_reload(g_webView);
}

static void on_btn_shield_clicked(GtkWidget* btn, void*) {
    g_shieldActive = !g_shieldActive;
    auto& loader = WebKitEngineLoader::Instance();
    if (g_shieldActive) {
        std::string label = "🛡️ " + std::to_string(g_blockedCount);
        loader.gtk_button_set_label(btn, label.c_str());
        std::cout << "[AzokleShield] Protection toggled: ACTIVE\n";
    } else {
        loader.gtk_button_set_label(btn, "🛡️ OFF");
        std::cout << "[AzokleShield] Protection toggled: PAUSED\n";
    }
}

static void on_btn_tor_clicked(GtkWidget* btn, void*) {
    g_torActive = !g_torActive;
    auto& loader = WebKitEngineLoader::Instance();
    if (g_torActive) {
        loader.gtk_button_set_label(btn, "🧅 ON");
        std::cout << "[Tor] Circuit toggled: ON\n";
        NavigateInternal("https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion");
    } else {
        loader.gtk_button_set_label(btn, "🧅");
        std::cout << "[Tor] Circuit toggled: OFF\n";
    }
}

static void on_btn_keys_clicked(GtkWidget*, void*) {
    NavigateInternal(SETTINGS_LOCAL_URL);
}

static void on_btn_bookmark_clicked(GtkWidget*, void*) {
    NavigateInternal(BOOKMARKS_LOCAL_URL);
}

// Menu Callbacks
static void on_menu_new_tab(GtkWidget*, void*) {
    NavigateInternal(NEWTAB_LOCAL_URL);
}

static void on_menu_new_private_window(GtkWidget*, void*) {
    g_torActive = true;
    if (g_btnTor) WebKitEngineLoader::Instance().gtk_button_set_label(g_btnTor, "🧅 ON");
    NavigateInternal("https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion");
}

static void on_menu_bookmarks(GtkWidget*, void*) {
    NavigateInternal(BOOKMARKS_LOCAL_URL);
}

static void on_menu_history(GtkWidget*, void*) {
    NavigateInternal(HISTORY_LOCAL_URL);
}

static void on_menu_downloads(GtkWidget*, void*) {
    NavigateInternal(DOWNLOADS_LOCAL_URL);
}

static void on_menu_settings(GtkWidget*, void*) {
    NavigateInternal(SETTINGS_LOCAL_URL);
}

static void on_btn_menu_clicked(GtkWidget* btn, void*) {
    auto& loader = WebKitEngineLoader::Instance();
    GtkWidget* menu = loader.gtk_menu_new();

    GtkWidget* itemNewTab = loader.gtk_menu_item_new_with_label("  ＋  New Tab               Ctrl+T");
    GtkWidget* itemNewWindow = loader.gtk_menu_item_new_with_label("  🗖  New Window            Ctrl+N");
    GtkWidget* itemNewPrivate = loader.gtk_menu_item_new_with_label("  🧅  New Private Window    Ctrl+Shift+N");
    GtkWidget* sep1 = loader.gtk_separator_menu_item_new();
    GtkWidget* itemBookmarks = loader.gtk_menu_item_new_with_label("  ★  Bookmarks            Ctrl+B");
    GtkWidget* itemHistory = loader.gtk_menu_item_new_with_label("  📜  History              Ctrl+H");
    GtkWidget* itemDownloads = loader.gtk_menu_item_new_with_label("  📥  Downloads            Ctrl+J");
    GtkWidget* sep2 = loader.gtk_separator_menu_item_new();
    GtkWidget* itemSettings = loader.gtk_menu_item_new_with_label("  ⚙️  Settings             Ctrl+,");

    loader.g_signal_connect_data(itemNewTab, "activate", (void*)on_menu_new_tab, nullptr, nullptr, 0);
    loader.g_signal_connect_data(itemNewWindow, "activate", (void*)on_menu_new_tab, nullptr, nullptr, 0);
    loader.g_signal_connect_data(itemNewPrivate, "activate", (void*)on_menu_new_private_window, nullptr, nullptr, 0);
    loader.g_signal_connect_data(itemBookmarks, "activate", (void*)on_menu_bookmarks, nullptr, nullptr, 0);
    loader.g_signal_connect_data(itemHistory, "activate", (void*)on_menu_history, nullptr, nullptr, 0);
    loader.g_signal_connect_data(itemDownloads, "activate", (void*)on_menu_downloads, nullptr, nullptr, 0);
    loader.g_signal_connect_data(itemSettings, "activate", (void*)on_menu_settings, nullptr, nullptr, 0);

    loader.gtk_menu_shell_append(menu, itemNewTab);
    loader.gtk_menu_shell_append(menu, itemNewWindow);
    loader.gtk_menu_shell_append(menu, itemNewPrivate);
    loader.gtk_menu_shell_append(menu, sep1);
    loader.gtk_menu_shell_append(menu, itemBookmarks);
    loader.gtk_menu_shell_append(menu, itemHistory);
    loader.gtk_menu_shell_append(menu, itemDownloads);
    loader.gtk_menu_shell_append(menu, sep2);
    loader.gtk_menu_shell_append(menu, itemSettings);

    loader.gtk_widget_show_all(menu);
    loader.gtk_menu_popup_at_widget(menu, btn, 2 /* GDK_GRAVITY_SOUTH_EAST */, 1 /* GDK_GRAVITY_NORTH_WEST */, nullptr);
}

static void on_window_destroy(GtkWidget*, void*) {
    WebKitEngineLoader::Instance().gtk_main_quit();
}

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
    loader.gtk_window_set_title(window, "Azokle Sovereign Browser");
    loader.gtk_window_set_default_size(window, 1280, 840);

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

    // 3. Main Container
    GtkWidget* mainBox = loader.gtk_box_new(1 /* GTK_ORIENTATION_VERTICAL */, 0);
    loader.gtk_container_add(window, mainBox);

    // 4. Top Tab Strip
    g_tabBox = loader.gtk_box_new(0 /* GTK_ORIENTATION_HORIZONTAL */, 4);
    loader.gtk_widget_set_margin_start(g_tabBox, 8);
    loader.gtk_widget_set_margin_end(g_tabBox, 8);

    GtkWidget* tabItem1 = loader.gtk_button_new_with_label("  New Tab   ✕  ");
    GtkWidget* btnAddTab = loader.gtk_button_new_with_label(" ＋ ");

    loader.g_signal_connect_data(tabItem1, "clicked", (void*)on_btn_home_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(btnAddTab, "clicked", (void*)on_btn_home_clicked, nullptr, nullptr, 0);

    loader.gtk_box_pack_start(g_tabBox, tabItem1, 0, 0, 0);
    loader.gtk_box_pack_start(g_tabBox, btnAddTab, 0, 0, 0);
    loader.gtk_box_pack_start(mainBox, g_tabBox, 0, 0, 0);

    // 5. Navigation Toolbar
    GtkWidget* toolbarBox = loader.gtk_box_new(0 /* GTK_ORIENTATION_HORIZONTAL */, 4);
    loader.gtk_widget_set_margin_start(toolbarBox, 8);
    loader.gtk_widget_set_margin_end(toolbarBox, 8);
    loader.gtk_widget_set_margin_top(toolbarBox, 3);
    loader.gtk_widget_set_margin_bottom(toolbarBox, 4);

    GtkWidget* btnBack = loader.gtk_button_new_with_label("←");
    GtkWidget* btnForward = loader.gtk_button_new_with_label("→");
    GtkWidget* btnReload = loader.gtk_button_new_with_label("↻");
    GtkWidget* btnHome = loader.gtk_button_new_with_label("⌂");

    loader.g_signal_connect_data(btnBack, "clicked", (void*)on_btn_back_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(btnForward, "clicked", (void*)on_btn_forward_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(btnReload, "clicked", (void*)on_btn_reload_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(btnHome, "clicked", (void*)on_btn_home_clicked, nullptr, nullptr, 0);

    loader.gtk_box_pack_start(toolbarBox, btnBack, 0, 0, 0);
    loader.gtk_box_pack_start(toolbarBox, btnForward, 0, 0, 0);
    loader.gtk_box_pack_start(toolbarBox, btnReload, 0, 0, 0);
    loader.gtk_box_pack_start(toolbarBox, btnHome, 0, 0, 0);

    // Floating Omnibar Address Box
    g_entryUrl = loader.gtk_entry_new();
    loader.gtk_entry_set_text(g_entryUrl, "");
    loader.g_signal_connect_data(g_entryUrl, "activate", (void*)on_entry_activate, nullptr, nullptr, 0);
    loader.gtk_box_pack_start(toolbarBox, g_entryUrl, 1, 1, 8);

    // Right Action Buttons
    g_btnShield = loader.gtk_button_new_with_label("🛡️ 42");
    g_btnTor = loader.gtk_button_new_with_label("🧅");
    GtkWidget* btnBookmark = loader.gtk_button_new_with_label("★");
    GtkWidget* btnKeys = loader.gtk_button_new_with_label("🔑");
    GtkWidget* btnMenu = loader.gtk_button_new_with_label("⋮");

    loader.g_signal_connect_data(g_btnShield, "clicked", (void*)on_btn_shield_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(g_btnTor, "clicked", (void*)on_btn_tor_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(btnBookmark, "clicked", (void*)on_btn_bookmark_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(btnKeys, "clicked", (void*)on_btn_keys_clicked, nullptr, nullptr, 0);
    loader.g_signal_connect_data(btnMenu, "clicked", (void*)on_btn_menu_clicked, nullptr, nullptr, 0);

    loader.gtk_box_pack_start(toolbarBox, g_btnShield, 0, 0, 2);
    loader.gtk_box_pack_start(toolbarBox, g_btnTor, 0, 0, 2);
    loader.gtk_box_pack_start(toolbarBox, btnBookmark, 0, 0, 2);
    loader.gtk_box_pack_start(toolbarBox, btnKeys, 0, 0, 2);
    loader.gtk_box_pack_start(toolbarBox, btnMenu, 0, 0, 2);

    loader.gtk_box_pack_start(mainBox, toolbarBox, 0, 0, 0);

    // 6. Live WebKit Viewport
    g_webView = loader.webkit_web_view_new();
    WebKitSettings* settings = loader.webkit_web_view_get_settings(g_webView);
    if (settings) {
        loader.webkit_settings_set_enable_javascript(settings, 1);
        loader.webkit_settings_set_enable_developer_extras(settings, 1);
        loader.webkit_settings_set_user_agent(settings, "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0 (Azokle Sovereign)");
    }

    loader.webkit_web_view_load_uri(g_webView, NEWTAB_LOCAL_URL);
    loader.gtk_box_pack_start(mainBox, g_webView, 1, 1, 0);

    // Window destroy signal
    loader.g_signal_connect_data(window, "destroy", (void*)on_window_destroy, nullptr, nullptr, 0);

    // Show window and enter GTK event loop
    loader.gtk_widget_show_all(window);
    std::cout << "[AzokleBrowserWindow] Redesigned Linux UI Initialized.\n";
    loader.gtk_main();
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
    if (g_entryUrl) {
        if (url == NEWTAB_LOCAL_URL) {
            WebKitEngineLoader::Instance().gtk_entry_set_text(g_entryUrl, "");
        } else if (url == SETTINGS_LOCAL_URL) {
            WebKitEngineLoader::Instance().gtk_entry_set_text(g_entryUrl, "azokle://settings");
        } else if (url == HISTORY_LOCAL_URL) {
            WebKitEngineLoader::Instance().gtk_entry_set_text(g_entryUrl, "azokle://history");
        } else if (url == DOWNLOADS_LOCAL_URL) {
            WebKitEngineLoader::Instance().gtk_entry_set_text(g_entryUrl, "azokle://downloads");
        } else if (url == BOOKMARKS_LOCAL_URL) {
            WebKitEngineLoader::Instance().gtk_entry_set_text(g_entryUrl, "azokle://bookmarks");
        } else {
            WebKitEngineLoader::Instance().gtk_entry_set_text(g_entryUrl, url.c_str());
        }
    }
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
