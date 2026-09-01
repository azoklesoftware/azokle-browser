#pragma once

#include <cstdint>

namespace azokle::linux_app::theme {

// Color Tokens matching Android Material 3 / Sovereign Glassmorphism
constexpr uint32_t BG_DARK        = 0x0b0f19; // Ultra dark slate background
constexpr uint32_t SURFACE_DARK   = 0x111827; // Header & Toolbar dark slate
constexpr uint32_t CARD_DARK      = 0x1e293b; // Card & Tab container
constexpr uint32_t CARD_HOVER     = 0x334155; // Hover state
constexpr uint32_t BORDER_SUBTLE  = 0x2d3748; // Subtle border

constexpr uint32_t PRIMARY_BLUE   = 0x0284c7; // Sky 600
constexpr uint32_t ACCENT_SKY     = 0x38bdf8; // Sky 400
constexpr uint32_t SHIELD_GREEN   = 0x22c55e; // Green 500 (Active Shield)
constexpr uint32_t SHIELD_BG      = 0x14532d; // Dark Green Container
constexpr uint32_t TOR_PURPLE     = 0xa855f7; // Purple 500
constexpr uint32_t TOR_BG         = 0x581c87; // Dark Purple Container
constexpr uint32_t WARNING_AMBER  = 0xf59e0b; // Amber 500
constexpr uint32_t DANGER_RED     = 0xef4444; // Red 500

constexpr uint32_t TEXT_PRIMARY   = 0xf8fafc; // Slate 50
constexpr uint32_t TEXT_SECONDARY = 0x94a3b8; // Slate 400
constexpr uint32_t TEXT_MUTED     = 0x64748b; // Slate 500

} // namespace azokle::linux_app::theme
