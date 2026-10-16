#pragma once

#include <dlfcn.h>
#include <iostream>
#include <functional>

namespace azokle::linux_app {

struct GtkWidget;
struct GtkWindow;
struct GtkBox;
struct GtkEntry;
struct GtkButton;
struct GtkHeaderBar;
struct GtkCssProvider;
struct GdkScreen;
struct WebKitWebView;
struct WebKitSettings;
struct WebKitUserContentManager;

class WebKitEngineLoader {
public:
    static WebKitEngineLoader& Instance() {
        static WebKitEngineLoader instance;
        return instance;
    }

    bool Initialize() {
        if (m_initialized) return true;

        m_gtkLib = dlopen("libgtk-3.so.0", RTLD_LAZY);
        if (!m_gtkLib) {
            m_gtkLib = dlopen("/usr/lib/x86_64-linux-gnu/libgtk-3.so.0", RTLD_LAZY);
        }

        m_webkitLib = dlopen("libwebkit2gtk-4.1.so.0", RTLD_LAZY);
        if (!m_webkitLib) {
            m_webkitLib = dlopen("/usr/lib/x86_64-linux-gnu/libwebkit2gtk-4.1.so.0", RTLD_LAZY);
        }

        if (!m_gtkLib || !m_webkitLib) {
            std::cerr << "[WebKitLoader] Failed to open GTK3 or WebKit2GTK library: " << dlerror() << "\n";
            return false;
        }

        // Load GTK symbols
        gtk_init = (void(*)(int*, char***))dlsym(m_gtkLib, "gtk_init");
        gtk_window_new = (GtkWidget*(*)(int))dlsym(m_gtkLib, "gtk_window_new");
        gtk_window_set_title = (void(*)(GtkWidget*, const char*))dlsym(m_gtkLib, "gtk_window_set_title");
        gtk_window_set_default_size = (void(*)(GtkWidget*, int, int))dlsym(m_gtkLib, "gtk_window_set_default_size");
        gtk_window_set_decorated = (void(*)(GtkWidget*, int))dlsym(m_gtkLib, "gtk_window_set_decorated");
        gtk_window_iconify = (void(*)(GtkWidget*))dlsym(m_gtkLib, "gtk_window_iconify");
        gtk_window_maximize = (void(*)(GtkWidget*))dlsym(m_gtkLib, "gtk_window_maximize");
        gtk_window_unmaximize = (void(*)(GtkWidget*))dlsym(m_gtkLib, "gtk_window_unmaximize");
        gtk_window_close = (void(*)(GtkWidget*))dlsym(m_gtkLib, "gtk_window_close");
        gtk_box_new = (GtkWidget*(*)(int, int))dlsym(m_gtkLib, "gtk_box_new");
        gtk_box_pack_start = (void(*)(GtkWidget*, GtkWidget*, int, int, unsigned int))dlsym(m_gtkLib, "gtk_box_pack_start");
        gtk_container_add = (void(*)(GtkWidget*, GtkWidget*))dlsym(m_gtkLib, "gtk_container_add");
        gtk_widget_show = (void(*)(GtkWidget*))dlsym(m_gtkLib, "gtk_widget_show");
        gtk_widget_show_all = (void(*)(GtkWidget*))dlsym(m_gtkLib, "gtk_widget_show_all");
        gtk_main = (void(*)())dlsym(m_gtkLib, "gtk_main");
        gtk_main_quit = (void(*)())dlsym(m_gtkLib, "gtk_main_quit");
        gtk_entry_new = (GtkWidget*(*)(void))dlsym(m_gtkLib, "gtk_entry_new");
        gtk_entry_set_text = (void(*)(GtkWidget*, const char*))dlsym(m_gtkLib, "gtk_entry_set_text");
        gtk_entry_get_text = (const char*(*)(GtkWidget*))dlsym(m_gtkLib, "gtk_entry_get_text");
        gtk_button_new_with_label = (GtkWidget*(*)(const char*))dlsym(m_gtkLib, "gtk_button_new_with_label");
        gtk_button_set_label = (void(*)(GtkWidget*, const char*))dlsym(m_gtkLib, "gtk_button_set_label");
        gtk_widget_set_size_request = (void(*)(GtkWidget*, int, int))dlsym(m_gtkLib, "gtk_widget_set_size_request");
        gtk_widget_set_margin_start = (void(*)(GtkWidget*, int))dlsym(m_gtkLib, "gtk_widget_set_margin_start");
        gtk_widget_set_margin_end = (void(*)(GtkWidget*, int))dlsym(m_gtkLib, "gtk_widget_set_margin_end");
        gtk_widget_set_margin_top = (void(*)(GtkWidget*, int))dlsym(m_gtkLib, "gtk_widget_set_margin_top");
        gtk_widget_set_margin_bottom = (void(*)(GtkWidget*, int))dlsym(m_gtkLib, "gtk_widget_set_margin_bottom");

        g_signal_connect_data = (unsigned long(*)(void*, const char*, void*, void*, void*, int))dlsym(m_gtkLib, "g_signal_connect_data");

        // Menu symbols
        gtk_menu_new = (GtkWidget*(*)(void))dlsym(m_gtkLib, "gtk_menu_new");
        gtk_menu_item_new_with_label = (GtkWidget*(*)(const char*))dlsym(m_gtkLib, "gtk_menu_item_new_with_label");
        gtk_menu_shell_append = (void(*)(GtkWidget*, GtkWidget*))dlsym(m_gtkLib, "gtk_menu_shell_append");
        gtk_separator_menu_item_new = (GtkWidget*(*)(void))dlsym(m_gtkLib, "gtk_separator_menu_item_new");
        gtk_menu_popup_at_widget = (void(*)(GtkWidget*, GtkWidget*, int, int, void*))dlsym(m_gtkLib, "gtk_menu_popup_at_widget");

        // CSS Styling symbols
        gtk_css_provider_new = (GtkCssProvider*(*)(void))dlsym(m_gtkLib, "gtk_css_provider_new");
        gtk_css_provider_load_from_data = (int(*)(GtkCssProvider*, const char*, long, void*))dlsym(m_gtkLib, "gtk_css_provider_load_from_data");
        gdk_screen_get_default = (GdkScreen*(*)(void))dlsym(m_gtkLib, "gdk_screen_get_default");
        gtk_style_context_add_provider_for_screen = (void(*)(GdkScreen*, void*, unsigned int))dlsym(m_gtkLib, "gtk_style_context_add_provider_for_screen");

        // Load WebKit symbols
        webkit_web_view_new = (GtkWidget*(*)(void))dlsym(m_webkitLib, "webkit_web_view_new");
        webkit_web_view_load_uri = (void(*)(GtkWidget*, const char*))dlsym(m_webkitLib, "webkit_web_view_load_uri");
        webkit_web_view_get_uri = (const char*(*)(GtkWidget*))dlsym(m_webkitLib, "webkit_web_view_get_uri");
        webkit_web_view_get_title = (const char*(*)(GtkWidget*))dlsym(m_webkitLib, "webkit_web_view_get_title");
        webkit_web_view_reload = (void(*)(GtkWidget*))dlsym(m_webkitLib, "webkit_web_view_reload");
        webkit_web_view_go_back = (void(*)(GtkWidget*))dlsym(m_webkitLib, "webkit_web_view_go_back");
        webkit_web_view_go_forward = (void(*)(GtkWidget*))dlsym(m_webkitLib, "webkit_web_view_go_forward");
        webkit_web_view_get_settings = (WebKitSettings*(*)(GtkWidget*))dlsym(m_webkitLib, "webkit_web_view_get_settings");
        webkit_settings_set_enable_developer_extras = (void(*)(WebKitSettings*, int))dlsym(m_webkitLib, "webkit_settings_set_enable_developer_extras");
        webkit_settings_set_enable_javascript = (void(*)(WebKitSettings*, int))dlsym(m_webkitLib, "webkit_settings_set_enable_javascript");
        webkit_settings_set_user_agent = (void(*)(WebKitSettings*, const char*))dlsym(m_webkitLib, "webkit_settings_set_user_agent");
        webkit_settings_set_allow_file_access_from_file_urls = (void(*)(WebKitSettings*, int))dlsym(m_webkitLib, "webkit_settings_set_allow_file_access_from_file_urls");
        webkit_settings_set_allow_universal_access_from_file_urls = (void(*)(WebKitSettings*, int))dlsym(m_webkitLib, "webkit_settings_set_allow_universal_access_from_file_urls");

        // Load WebKitGTK IPC & Script Message Handlers
        g_free = (void(*)(void*))dlsym(m_gtkLib, "g_free");
        webkit_web_view_get_user_content_manager = (void*(*)(GtkWidget*))dlsym(m_webkitLib, "webkit_web_view_get_user_content_manager");
        webkit_user_content_manager_register_script_message_handler = (void(*)(void*, const char*))dlsym(m_webkitLib, "webkit_user_content_manager_register_script_message_handler");
        webkit_web_view_run_javascript = (void(*)(GtkWidget*, const char*, void*, void*, void*))dlsym(m_webkitLib, "webkit_web_view_run_javascript");
        webkit_javascript_result_get_js_value = (void*(*)(void*))dlsym(m_webkitLib, "webkit_javascript_result_get_js_value");

        m_jscLib = dlopen("libjavascriptcoregtk-4.1.so.0", RTLD_LAZY);
        if (!m_jscLib) {
            m_jscLib = dlopen("/usr/lib/x86_64-linux-gnu/libjavascriptcoregtk-4.1.so.0", RTLD_LAZY);
        }
        if (m_jscLib) {
            jsc_value_to_json = (char*(*)(void*, int))dlsym(m_jscLib, "jsc_value_to_json");
        }

        m_initialized = (gtk_init && webkit_web_view_new && webkit_web_view_load_uri);
        std::cout << "[WebKitLoader] Engine initialization " << (m_initialized ? "SUCCESSFUL" : "FAILED") << "\n";
        return m_initialized;
    }

    // Function pointers
    void (*gtk_init)(int*, char***){nullptr};
    GtkWidget* (*gtk_window_new)(int){nullptr};
    void (*gtk_window_set_title)(GtkWidget*, const char*){nullptr};
    void (*gtk_window_set_default_size)(GtkWidget*, int, int){nullptr};
    void (*gtk_window_set_decorated)(GtkWidget*, int){nullptr};
    void (*gtk_window_iconify)(GtkWidget*){nullptr};
    void (*gtk_window_maximize)(GtkWidget*){nullptr};
    void (*gtk_window_unmaximize)(GtkWidget*){nullptr};
    void (*gtk_window_close)(GtkWidget*){nullptr};
    GtkWidget* (*gtk_box_new)(int, int){nullptr};
    void (*gtk_box_pack_start)(GtkWidget*, GtkWidget*, int, int, unsigned int){nullptr};
    void (*gtk_container_add)(GtkWidget*, GtkWidget*){nullptr};
    void (*gtk_widget_show)(GtkWidget*){nullptr};
    void (*gtk_widget_show_all)(GtkWidget*){nullptr};
    void (*gtk_main)(void){nullptr};
    void (*gtk_main_quit)(void){nullptr};
    GtkWidget* (*gtk_entry_new)(void){nullptr};
    void (*gtk_entry_set_text)(GtkWidget*, const char*){nullptr};
    const char* (*gtk_entry_get_text)(GtkWidget*){nullptr};
    GtkWidget* (*gtk_button_new_with_label)(const char*){nullptr};
    void (*gtk_button_set_label)(GtkWidget*, const char*){nullptr};
    void (*gtk_widget_set_size_request)(GtkWidget*, int, int){nullptr};
    void (*gtk_widget_set_margin_start)(GtkWidget*, int){nullptr};
    void (*gtk_widget_set_margin_end)(GtkWidget*, int){nullptr};
    void (*gtk_widget_set_margin_top)(GtkWidget*, int){nullptr};
    void (*gtk_widget_set_margin_bottom)(GtkWidget*, int){nullptr};

    // Menu pointers
    GtkWidget* (*gtk_menu_new)(void){nullptr};
    GtkWidget* (*gtk_menu_item_new_with_label)(const char*){nullptr};
    void (*gtk_menu_shell_append)(GtkWidget*, GtkWidget*){nullptr};
    GtkWidget* (*gtk_separator_menu_item_new)(void){nullptr};
    void (*gtk_menu_popup_at_widget)(GtkWidget*, GtkWidget*, int, int, void*){nullptr};

    unsigned long (*g_signal_connect_data)(void*, const char*, void*, void*, void*, int){nullptr};

    GtkCssProvider* (*gtk_css_provider_new)(void){nullptr};
    int (*gtk_css_provider_load_from_data)(GtkCssProvider*, const char*, long, void*){nullptr};
    GdkScreen* (*gdk_screen_get_default)(void){nullptr};
    void (*gtk_style_context_add_provider_for_screen)(GdkScreen*, void*, unsigned int){nullptr};

    GtkWidget* (*webkit_web_view_new)(void){nullptr};
    void (*webkit_web_view_load_uri)(GtkWidget*, const char*){nullptr};
    const char* (*webkit_web_view_get_uri)(GtkWidget*){nullptr};
    const char* (*webkit_web_view_get_title)(GtkWidget*){nullptr};
    void (*webkit_web_view_reload)(GtkWidget*){nullptr};
    void (*webkit_web_view_go_back)(GtkWidget*){nullptr};
    void (*webkit_web_view_go_forward)(GtkWidget*){nullptr};
    WebKitSettings* (*webkit_web_view_get_settings)(GtkWidget*){nullptr};
    void (*webkit_settings_set_enable_developer_extras)(WebKitSettings*, int){nullptr};
    void (*webkit_settings_set_enable_javascript)(WebKitSettings*, int){nullptr};
    void (*webkit_settings_set_user_agent)(WebKitSettings*, const char*){nullptr};
    void (*webkit_settings_set_allow_file_access_from_file_urls)(WebKitSettings*, int){nullptr};
    void (*webkit_settings_set_allow_universal_access_from_file_urls)(WebKitSettings*, int){nullptr};
    void (*g_free)(void*){nullptr};
    void* (*webkit_web_view_get_user_content_manager)(GtkWidget*){nullptr};
    void (*webkit_user_content_manager_register_script_message_handler)(void*, const char*){nullptr};
    void (*webkit_web_view_run_javascript)(GtkWidget*, const char*, void*, void*, void*){nullptr};
    void* (*webkit_javascript_result_get_js_value)(void*){nullptr};
    char* (*jsc_value_to_json)(void*, int){nullptr};

private:
    WebKitEngineLoader() = default;
    void* m_gtkLib{nullptr};
    void* m_webkitLib{nullptr};
    void* m_jscLib{nullptr};
    bool m_initialized{false};
};

} // namespace azokle::linux_app
