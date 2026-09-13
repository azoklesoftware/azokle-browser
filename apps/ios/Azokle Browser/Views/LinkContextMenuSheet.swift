import SwiftUI

// MARK: - Link & Media Long-Press Context Menu Sheet (1:1 Android LinkContextMenuSheet Parity)
public struct LinkContextMenuSheet: View {
    let target: LinkContextTarget
    @ObservedObject var state: BrowserState
    let colors: AzokleColors = .dark

    @State private var copiedFeedback: Bool = false

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 12)
                .padding(.bottom, 12)

            // Header Preview
            HStack(spacing: 12) {
                if let imageSrc = target.imageSrc, let url = URL(string: imageSrc) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable()
                                .scaledToFill()
                                .frame(width: 44, height: 44)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        default:
                            Image(systemName: "photo")
                                .font(.system(size: 20))
                                .foregroundColor(colors.textSecondary)
                                .frame(width: 44, height: 44)
                                .background(colors.surfaceElevated)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                } else {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(colors.surfaceElevated)
                            .frame(width: 44, height: 44)
                        Image(systemName: "link")
                            .font(.system(size: 20))
                            .foregroundColor(colors.statusAccent)
                    }
                }

                VStack(alignment: .leading, spacing: 3) {
                    if !target.title.isEmpty {
                        Text(target.title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(colors.textPrimary)
                            .lineLimit(1)
                    }

                    Text(target.url)
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                        .lineLimit(2)
                }

                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 14)

            Divider().background(colors.border)

            // Action Items
            ScrollView {
                VStack(spacing: 4) {
                    // Open in New Tab
                    actionRow(icon: "plus.square", label: "Open in New Tab") {
                        state.openNewTab(url: target.url, title: target.title.isEmpty ? "New Tab" : target.title)
                        state.activeLinkContext = nil
                    }

                    // Open in Private Tab
                    actionRow(icon: "eye.slash", label: "Open in Private Tab") {
                        state.openNewTab(url: target.url, title: target.title.isEmpty ? "Private Tab" : target.title, isPrivate: true)
                        state.activeLinkContext = nil
                    }

                    // Open in Tor Tab
                    actionRow(icon: "network.badge.shield.half.filled", label: "Open in Tor Circuit Tab", tint: colors.statusTor) {
                        state.openNewTab(url: target.url, title: target.title.isEmpty ? "Tor Tab" : target.title, isTor: true)
                        state.activeLinkContext = nil
                    }

                    Divider().background(colors.border.opacity(0.6)).padding(.horizontal, 14).padding(.vertical, 4)

                    // Copy Link Address
                    actionRow(icon: copiedFeedback ? "checkmark" : "doc.on.doc", label: copiedFeedback ? "Copied to Clipboard!" : "Copy Link Address", tint: copiedFeedback ? colors.statusSuccess : nil) {
                        UIPasteboard.general.string = target.url
                        let gen = UIImpactFeedbackGenerator(style: .medium)
                        gen.impactOccurred()
                        withAnimation {
                            copiedFeedback = true
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                            state.activeLinkContext = nil
                        }
                    }

                    // Share Link
                    actionRow(icon: "square.and.arrow.up", label: "Share Link...") {
                        state.activeLinkContext = nil
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                            state.showShareSheet = true
                        }
                    }

                    // Download Link / Media
                    if target.imageSrc != nil || target.url.hasSuffix(".pdf") || target.url.hasSuffix(".zip") || target.url.hasSuffix(".png") || target.url.hasSuffix(".jpg") {
                        actionRow(icon: "arrow.down.circle", label: "Download Linked File") {
                            state.downloads.insert(DownloadItem(
                                fileName: URL(string: target.url)?.lastPathComponent ?? "downloaded_file",
                                url: target.url,
                                fileSize: "Saved",
                                date: Date()
                            ), at: 0)
                            state.activeLinkContext = nil
                        }
                    }
                }
                .padding(.vertical, 10)
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.52)])
    }

    private func actionRow(icon: String, label: String, tint: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(tint ?? colors.textPrimary)
                    .frame(width: 24)

                Text(label)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(tint ?? colors.textPrimary)

                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
    }
}
