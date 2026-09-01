#include "AzokleDrawUtils.hpp"

namespace azokle::linux_app::ui {

void DrawUtils::FillRect(Display* display, Drawable d, GC gc, uint32_t color, int x, int y, int w, int h) {
    XSetForeground(display, gc, color);
    XFillRectangle(display, d, gc, x, y, w, h);
}

void DrawUtils::DrawRect(Display* display, Drawable d, GC gc, uint32_t color, int x, int y, int w, int h) {
    XSetForeground(display, gc, color);
    XDrawRectangle(display, d, gc, x, y, w, h);
}

void DrawUtils::DrawPill(Display* display, Drawable d, GC gc, uint32_t bgColor, uint32_t textColor,
                         const std::string& text, int x, int y, int w, int h) {
    FillRect(display, d, gc, bgColor, x, y, w, h);
    DrawRect(display, d, gc, textColor, x, y, w, h);
    XSetForeground(display, gc, textColor);
    DrawText(display, d, gc, textColor, text, x + (w / 2 - (int)text.length() * 3), y + h / 2 + 5);
}

void DrawUtils::DrawCard(Display* display, Drawable d, GC gc, uint32_t bgColor, uint32_t borderColor,
                         int x, int y, int w, int h) {
    FillRect(display, d, gc, bgColor, x, y, w, h);
    DrawRect(display, d, gc, borderColor, x, y, w, h);
}

void DrawUtils::DrawText(Display* display, Drawable d, GC gc, uint32_t color,
                         const std::string& text, int x, int y) {
    XSetForeground(display, gc, color);
    XDrawString(display, d, gc, x, y, text.c_str(), (int)text.length());
}

void DrawUtils::DrawModal(Display* display, Drawable d, GC gc, int screenW, int screenH,
                          int modalW, int modalH, const std::string& title,
                          int& outX, int& outY) {
    outX = screenW / 2 - modalW / 2;
    outY = screenH / 2 - modalH / 2;

    // Dim background
    FillRect(display, d, gc, 0x0f172a, outX - 2, outY - 2, modalW + 4, modalH + 4);
    // Modal container
    FillRect(display, d, gc, 0x1e293b, outX, outY, modalW, modalH);
    DrawRect(display, d, gc, 0x38bdf8, outX, outY, modalW, modalH);

    // Header bar
    FillRect(display, d, gc, 0x111827, outX, outY, modalW, 42);
    DrawText(display, d, gc, 0x38bdf8, title, outX + 16, outY + 26);

    // Close button 'X' in top right
    FillRect(display, d, gc, 0xef4444, outX + modalW - 36, outY + 8, 26, 26);
    DrawText(display, d, gc, 0xffffff, "x", outX + modalW - 27, outY + 25);
}

} // namespace azokle::linux_app::ui
