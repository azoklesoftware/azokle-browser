import SwiftUI

public struct SettingsScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    let colors: AzokleColors = .dark

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                List {
                    // 1. Search Engine
                    Section(header: Text("SEARCH ENGINE").foregroundColor(colors.textMuted)) {
                        ForEach(SearchEngine.allCases) { engine in
                            Button(action: {
                                state.selectedSearchEngine = engine
                            }) {
                                HStack {
                                    Text(engine.rawValue)
                                        .foregroundColor(colors.textPrimary)
                                    Spacer()
                                    if state.selectedSearchEngine == engine {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(colors.statusAccent)
                                    }
                                }
                            }
                        }
                    }
                    .listRowBackground(colors.surface)

                    // 2. Shield & Security
                    Section(header: Text("AZOKLE SHIELD & PRIVACY").foregroundColor(colors.textMuted)) {
                        Toggle("Global Shield Protection", isOn: $state.isShieldActive)
                            .tint(colors.statusSuccess)
                            .foregroundColor(colors.textPrimary)

                        Toggle("Tor Circuit Mode", isOn: $state.isTorCircuitActive)
                            .tint(colors.statusTor)
                            .foregroundColor(colors.textPrimary)

                        HStack {
                            Text("Desktop Mode")
                                .foregroundColor(colors.textPrimary)
                            Spacer()
                            Toggle("", isOn: $state.desktopMode)
                                .labelsHidden()
                                .tint(colors.statusAccent)
                        }
                    }
                    .listRowBackground(colors.surface)

                    // 3. Clear Data
                    Section(header: Text("DATA & CACHE").foregroundColor(colors.textMuted)) {
                        Button(action: {
                            state.burnAllData()
                            isPresented = false
                        }) {
                            HStack {
                                Image(systemName: "flame.fill")
                                    .foregroundColor(colors.statusFire)
                                Text("Incinerate All Browsing Data")
                                    .foregroundColor(colors.statusFire)
                            }
                        }
                    }
                    .listRowBackground(colors.surface)

                    // 4. About
                    Section(header: Text("ABOUT").foregroundColor(colors.textMuted)) {
                        HStack {
                            Text("Version")
                                .foregroundColor(colors.textPrimary)
                            Spacer()
                            Text("v2.0.0 (Build 10)")
                                .foregroundColor(colors.textSecondary)
                        }

                        HStack {
                            Text("Shield Core")
                                .foregroundColor(colors.textPrimary)
                            Spacer()
                            Text("Azokle Sovereign Shield v6.0")
                                .foregroundColor(colors.statusSuccess)
                        }

                        HStack {
                            Text("Privacy Protocol")
                                .foregroundColor(colors.textPrimary)
                            Spacer()
                            Text("RFC 8828 / Tor 3-Hop Onion")
                                .foregroundColor(colors.statusTor)
                        }
                    }
                    .listRowBackground(colors.surface)
                }
                .listStyle(.insetGrouped)
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        isPresented = false
                    }
                    .foregroundColor(colors.statusAccent)
                }
            }
        }
    }
}
