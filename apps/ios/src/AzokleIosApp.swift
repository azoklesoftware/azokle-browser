import SwiftUI

@main
struct AzokleIosApp: App {
    var body: some Scene {
        WindowGroup {
            IosBrowserRootView()
        }
    }
}

class IosBrowserState: ObservableObject {
    @Published var activeUrl: String = "https://search.azokle.com"
    @Published var activeTitle: String = "Azokle Sovereign Search"
    @Published var isPrivate: Bool = false
    @Published var isTorCircuitActive: Bool = false
    @Published var isShieldActive: Bool = true
    @Published var blockedRequestsCount: UInt64 = 0
    @Published var showTabGrid: Bool = false
    @Published var showShieldSheet: Bool = false

    func navigate(to url: String) {
        self.activeUrl = url
        print("[AzokleIos] Navigating to: \(url)")
    }

    func toggleTor() {
        self.isTorCircuitActive.toggle()
        print("[AzokleIos] Tor Circuit toggled: \(isTorCircuitActive)")
    }
}

struct IosBrowserRootView: View {
    @StateObject private var state = IosBrowserState()
    @State private var urlInputText: String = ""

    var body: some View {
        ZStack(alignment: .bottom) {
            // Main Web Rendering Viewport
            VStack(spacing: 0) {
                // Top Security Status Banner
                HStack {
                    if state.isTorCircuitActive {
                        Label("Tor Circuit Active", systemImage: "network.badge.shield.half.filled")
                            .font(.caption2)
                            .foregroundColor(.purple)
                    } else {
                        Label("Sovereign Shield Active", systemImage: "shield.fill")
                            .font(.caption2)
                            .foregroundColor(.blue)
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
                .background(Color(.systemBackground))

                Divider()

                // Web Surface Placeholder / Gecko-iOS Container
                ZStack {
                    Color(.systemGroupedBackground)
                    VStack(spacing: 12) {
                        Image(systemName: "safari.fill")
                            .font(.system(size: 64))
                            .foregroundColor(.blue)
                        Text(state.activeTitle)
                            .font(.headline)
                        Text(state.activeUrl)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            // Bottom Navigation & URL Bar (Mobile Ergonomics)
            VStack(spacing: 8) {
                HStack(spacing: 12) {
                    Button(action: {}) {
                        Image(systemName: "chevron.left")
                    }

                    Button(action: {}) {
                        Image(systemName: "chevron.right")
                    }

                    // Mobile Address Bar
                    HStack {
                        Image(systemName: state.isShieldActive ? "shield.fill" : "shield")
                            .foregroundColor(.blue)
                            .onTapGesture {
                                state.showShieldSheet = true
                            }

                        TextField("Search or enter website", text: $urlInputText, onCommit: {
                            state.navigate(to: urlInputText)
                        })
                        .font(.subheadline)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    }
                    .padding(10)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)

                    Button(action: { state.showTabGrid = true }) {
                        Image(systemName: "square.on.square")
                    }

                    Button(action: { state.toggleTor() }) {
                        Image(systemName: "globe")
                            .foregroundColor(state.isTorCircuitActive ? .purple : .primary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color(.systemBackground).shadow(radius: 2))
            }
        }
        .sheet(isPresented: $state.showShieldSheet) {
            ShieldDashboardSheet(state: state)
        }
    }
}

struct ShieldDashboardSheet: View {
    @ObservedObject var state: IosBrowserState
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Privacy Protection")) {
                    Toggle("Azokle Shield", isOn: $state.isShieldActive)
                    HStack {
                        Text("Trackers & Ads Blocked")
                        Spacer()
                        Text("\(state.blockedRequestsCount)")
                            .foregroundColor(.secondary)
                    }
                }

                Section(header: Text("Anonymity & Routing")) {
                    Toggle("Tor Onion Circuit", isOn: $state.isTorCircuitActive)
                    Toggle("Private Browsing Mode", isOn: $state.isPrivate)
                }
            }
            .navigationTitle("Azokle Shield")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}
