import SwiftUI
import WebKit

public struct AzoklePlayerSheet: View {
    let videoId: String
    let originalUrl: String
    @Binding var isPresented: Bool

    @State private var playbackSpeed: Double = 1.0
    @State private var showControls: Bool = true
    let colors: AzokleColors = .dark

    let speeds: [Double] = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0]

    private var embedUrlString: String {
        AzoklePlayerHelper.buildZeroCookieEmbedUrl(videoId: videoId)
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Header Bar
                HStack(spacing: 12) {
                    Button(action: {
                        isPresented = false
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .padding(8)
                            .background(Color.white.opacity(0.12))
                            .clipShape(Circle())
                    }

                    HStack(spacing: 6) {
                        Image(systemName: "shield.lefthalf.filled")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(colors.statusSuccess)

                        Text("Azokle Sovereign Player")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)

                        Text("Zero Cookies")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(colors.statusTor)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(colors.statusTor.opacity(0.2))
                            .cornerRadius(6)
                    }

                    Spacer()

                    // Playback Speed Menu
                    Menu {
                        ForEach(speeds, id: \.self) { speed in
                            Button(action: {
                                playbackSpeed = speed
                            }) {
                                HStack {
                                    Text("\(String(format: "%.2f", speed))x")
                                    if playbackSpeed == speed {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack(spacing: 3) {
                            Text("\(String(format: "%.2g", playbackSpeed))x")
                                .font(.system(size: 12, weight: .bold))
                            Image(systemName: "chevron.down")
                                .font(.system(size: 8, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(Color.white.opacity(0.15))
                        .cornerRadius(6)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.black.opacity(0.85))

                // Video Container
                AzoklePlayerWebView(urlString: embedUrlString)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Bottom Privacy Guarantee Bar
                HStack(spacing: 8) {
                    Image(systemName: "lock.shield.fill")
                        .font(.system(size: 12))
                        .foregroundColor(colors.statusSuccess)

                    Text("Tracking cookies stripped • Ad-free isolated container")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.white.opacity(0.7))

                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.black.opacity(0.95))
            }
        }
    }
}

// Minimal Privacy-Hardened WebView for Player Embed
struct AzoklePlayerWebView: UIViewRepresentable {
    let urlString: String

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        config.websiteDataStore = WKWebsiteDataStore.nonPersistent()

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.backgroundColor = .black
        webView.isOpaque = false
        webView.scrollView.isScrollEnabled = false

        if let url = URL(string: urlString) {
            var request = URLRequest(url: url)
            request.setValue("https://search.azokle.com", forHTTPHeaderField: "Referer")
            webView.load(request)
        }

        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}
}
