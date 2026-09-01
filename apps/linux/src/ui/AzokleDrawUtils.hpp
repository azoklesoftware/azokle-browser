#pragma once

#include <X11/Xlib.h>
#include <string>
#include <cstdint>

namespace azokle::linux_app::ui {

class DrawUtils {
public:
    static void FillRect(Display* display, Drawable d, GC gc, uint32_t color, int x, int y, int w, int h);
    static void DrawRect(Display* display, Drawable d, GC gc, uint32_t color, int x, int y, int w, int h);
    static void DrawPill(Display* display, Drawable d, GC gc, uint32_t bgColor, uint32_t textColor,
                         const std::string& text, int x, int y, int w, int h);
    static void DrawCard(Display* display, Drawable d, GC gc, uint32_t bgColor, uint32_t borderColor,
                         int x, int y, int w, int h);
    static void DrawText(Display* display, Drawable d, GC gc, uint32_t color,
                         const std::string& text, int x, int y);
    static void DrawModal(Display* display, Drawable d, GC gc, int screenW, int screenH,
                          int modalW, int modalH, const std::string& title,
                          int& outX, int& outY);
};

} // namespace azokle::linux_app::ui
