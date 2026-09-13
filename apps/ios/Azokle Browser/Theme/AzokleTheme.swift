import SwiftUI

// MARK: - Color Hex Initializer
extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}

// MARK: - Azokle Theme Palette (Mirroring Android Color.kt)
struct AzokleColors {
    let isDark: Bool
    let background: Color
    let surface: Color
    let surfaceElevated: Color
    let cardBg: Color
    let border: Color
    let borderLight: Color
    let borderSubtle: Color
    let textPrimary: Color
    let textSecondary: Color
    let textMuted: Color
    let textDisabled: Color
    let pillBg: Color
    let glassBg: Color
    let bottomNavBg: Color
    let omnibarBg: Color
    let modalBg: Color
    let actionPrimaryBg: Color
    let actionPrimaryText: Color
    let actionSecondaryBg: Color
    let actionSecondaryText: Color
    let statusSuccess: Color
    let statusWarning: Color
    let statusDestructive: Color
    let statusTor: Color
    let statusAccent: Color
    let statusFire: Color
    let scrim: Color

    static let dark = AzokleColors(
        isDark: true,
        background: Color(hex: 0x101010),
        surface: Color(hex: 0x181818),
        surfaceElevated: Color(hex: 0x222222),
        cardBg: Color(hex: 0x181818),
        border: Color(hex: 0x282828),
        borderLight: Color.white.opacity(0.15),
        borderSubtle: Color.white.opacity(0.08),
        textPrimary: Color(hex: 0xF3F3F3),
        textSecondary: Color(hex: 0xA0A0A0),
        textMuted: Color(hex: 0x707070),
        textDisabled: Color(hex: 0x424242),
        pillBg: Color.white.opacity(0.14),
        glassBg: Color(hex: 0x101010, alpha: 0.85),
        bottomNavBg: Color(hex: 0x181818, alpha: 0.95),
        omnibarBg: Color(hex: 0x181818),
        modalBg: Color(hex: 0x141414),
        actionPrimaryBg: Color(hex: 0xF3F3F3),
        actionPrimaryText: Color(hex: 0x101010),
        actionSecondaryBg: Color(hex: 0x222222),
        actionSecondaryText: Color(hex: 0xF3F3F3),
        statusSuccess: Color(hex: 0x22C55E),
        statusWarning: Color(hex: 0xF59E0B),
        statusDestructive: Color(hex: 0xEF4444),
        statusTor: Color(hex: 0xA855F7),
        statusAccent: Color(hex: 0x38BDF8),
        statusFire: Color(hex: 0xFF4500),
        scrim: Color.black.opacity(0.75)
    )

    static let light = AzokleColors(
        isDark: false,
        background: Color(hex: 0xFAF8F5),
        surface: Color(hex: 0xFFFFFF),
        surfaceElevated: Color(hex: 0xF2EFEB),
        cardBg: Color(hex: 0xFFFFFF),
        border: Color(hex: 0xE8E4DE),
        borderLight: Color.black.opacity(0.08),
        borderSubtle: Color.black.opacity(0.04),
        textPrimary: Color(hex: 0x151515),
        textSecondary: Color(hex: 0x666666),
        textMuted: Color(hex: 0x888888),
        textDisabled: Color(hex: 0xBBBBBB),
        pillBg: Color.black.opacity(0.06),
        glassBg: Color(hex: 0xFAF8F5, alpha: 0.88),
        bottomNavBg: Color(hex: 0xFFFFFF, alpha: 0.96),
        omnibarBg: Color(hex: 0xF2EFEB),
        modalBg: Color(hex: 0xFFFFFF),
        actionPrimaryBg: Color(hex: 0x151515),
        actionPrimaryText: Color(hex: 0xFFFFFF),
        actionSecondaryBg: Color(hex: 0xE8E4DE),
        actionSecondaryText: Color(hex: 0x151515),
        statusSuccess: Color(hex: 0x16A34A),
        statusWarning: Color(hex: 0xD97706),
        statusDestructive: Color(hex: 0xDC2626),
        statusTor: Color(hex: 0x9333EA),
        statusAccent: Color(hex: 0x0284C7),
        statusFire: Color(hex: 0xEA580C),
        scrim: Color.black.opacity(0.4)
    )
}

// MARK: - Radius Tokens
struct AzokleRadius {
    static let small: CGFloat = 8.0
    static let medium: CGFloat = 14.0
    static let large: CGFloat = 20.0
    static let pill: CGFloat = 999.0
}
