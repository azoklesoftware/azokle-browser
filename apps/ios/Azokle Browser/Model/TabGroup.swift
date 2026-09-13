import Foundation
import SwiftUI

// MARK: - Tab Group Model (1:1 Android TabGroup.kt Parity)
public struct TabGroup: Identifiable, Hashable {
    public let id: UUID
    public var title: String
    public var colorHex: UInt32
    public var tabIds: [UUID]
    public var isCollapsed: Bool
    public let createdAt: Date

    public init(
        id: UUID = UUID(),
        title: String,
        colorHex: UInt32 = 0x38BDF8,
        tabIds: [UUID] = [],
        isCollapsed: Bool = false,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.title = title
        self.colorHex = colorHex
        self.tabIds = tabIds
        self.isCollapsed = isCollapsed
        self.createdAt = createdAt
    }

    public var color: Color {
        Color(hex: colorHex)
    }

    // 8 Standard Sovereign Group Palette Colors (matching Android GROUP_PALETTE)
    public static let paletteHexes: [UInt32] = [
        0x38BDF8, // Sky Blue
        0x34D399, // Emerald Green
        0xF43F5E, // Rose / Coral
        0xA78BFA, // Lavender / Purple
        0xFBBF24, // Amber / Gold
        0xF97316, // Orange
        0x06B6D4, // Cyan
        0xEC4899  // Pink
    ]
}
