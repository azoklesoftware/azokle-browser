import SwiftUI

public struct SettingsScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var selectedThemeMode: String = "dark"
    @State private var selectedDohProvider: String = "cloudflare"
    @State private var isDohExpanded: Bool = false
    @State private var showHistoryClearedToast: Bool = false

    let colors: AzokleColors = .dark

    let wallpapers = [
        ("bg_home_1", "Cosmic Horizon"),
        ("bg_home_2", "Emerald Cyber"),
        ("bg_home_3", "Deep Aurora"),
        ("bg_home_4", "Nebula Noir")
    ]

    let dohProviders = [
        ("cloudflare", "Cloudflare 1.1.1.1", "Fastest zero-log Anycast encrypted DNS"),
        ("quad9", "Quad9 (9.9.9.9)", "Swiss non-profit threat & malware blocking"),
        ("adguard", "AdGuard DNS", "Network-level ad & tracker filter"),
        ("system", "System Default DNS", "Standard ISP-provided name servers")
    ]

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                List {
                    // 1. Search Engine (Tap opens sovereign ChooseSearchEngineSheet)
                    Section(header: Text("SEARCH ENGINE").foregroundColor(colors.textMuted)) {
                        Button(action: {
                            state.showChooseEngineSheet = true
                        }) {
                            HStack(spacing: 12) {
                                Image(state.selectedSearchEngine.assetName)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 26, height: 26)

                                VStack(alignment: .leading, spacing: 3) {
                                    Text(state.selectedSearchEngine.name)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(colors.textPrimary)
                                    Text(state.selectedSearchEngine.domain)
                                        .font(.system(size: 12))
                                        .foregroundColor(colors.textSecondary)
                                }

                                Spacer()

                                Text("Change")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(colors.statusAccent)

                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(colors.textMuted)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .listRowBackground(colors.surface)

                    // 2. Appearance & Wallpaper (Android WallpaperManager Parity)
                    Section(header: Text("APPEARANCE & WALLPAPERS").foregroundColor(colors.textMuted)) {
                        // Theme Mode Segmented Control
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Theme Mode")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(colors.textPrimary)

                            Picker("Theme", selection: $selectedThemeMode) {
                                Text("System").tag("system")
                                Text("Dark").tag("dark")
                                Text("Light").tag("light")
                            }
                            .pickerStyle(SegmentedPickerStyle())
                        }
                        .padding(.vertical, 4)

                        // Wallpaper Gallery Selector
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Home Screen Wallpaper")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(colors.textPrimary)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(wallpapers, id: \.0) { wallpaper in
                                        let isSelected = state.selectedWallpaper == wallpaper.0
                                        Button(action: {
                                            state.selectedWallpaper = wallpaper.0
                                        }) {
                                            VStack(spacing: 6) {
                                                ZStack(alignment: .topTrailing) {
                                                    Image(wallpaper.0)
                                                        .resizable()
                                                        .scaledToFill()
                                                        .frame(width: 72, height: 110)
                                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                                        .overlay(
                                                            RoundedRectangle(cornerRadius: 10)
                                                                .stroke(isSelected ? colors.statusAccent : colors.border, lineWidth: isSelected ? 2.5 : 1)
                                                        )

                                                    if isSelected {
                                                        Image(systemName: "checkmark.circle.fill")
                                                            .font(.system(size: 16))
                                                            .foregroundColor(colors.statusAccent)
                                                            .background(Circle().fill(Color.black))
                                                            .padding(6)
                                                    }
                                                }

                                                Text(wallpaper.1)
                                                    .font(.system(size: 10, weight: isSelected ? .bold : .regular))
                                                    .foregroundColor(isSelected ? colors.statusAccent : colors.textMuted)
                                                    .lineLimit(1)
                                                    .frame(width: 72)
                                            }
                                        }
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                        }
                        .padding(.vertical, 6)

                        // Language Selector
                        Button(action: {
                            state.showLanguageSheet = true
                        }) {
                            HStack {
                                Image(systemName: "globe")
                                    .font(.system(size: 16))
                                    .foregroundColor(colors.statusAccent)
                                    .frame(width: 24)

                                Text("Application Language")
                                    .font(.system(size: 15))
                                    .foregroundColor(colors.textPrimary)

                                Spacer()

                                Text(activeLanguageName)
                                    .font(.system(size: 13))
                                    .foregroundColor(colors.textSecondary)

                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(colors.textMuted)
                            }
                            .padding(.vertical, 2)
                        }
                    }
                    .listRowBackground(colors.surface)

                    // 3. Privacy & Sovereign Shield
                    Section(header: Text("AZOKLE SHIELD & PRIVACY").foregroundColor(colors.textMuted)) {
                        Toggle("Global Shield Protection", isOn: $state.isShieldActive)
                            .tint(colors.statusSuccess)
                            .foregroundColor(colors.textPrimary)

                        Toggle("Tor 3-Hop Circuit Mode", isOn: $state.isTorCircuitActive)
                            .tint(colors.statusTor)
                            .foregroundColor(colors.textPrimary)

                        // Encrypted DNS (DoH) Provider
                        VStack(alignment: .leading, spacing: 6) {
                            Button(action: {
                                withAnimation {
                                    isDohExpanded.toggle()
                                }
                            }) {
                                HStack {
                                    Image(systemName: "network")
                                        .font(.system(size: 16))
                                        .foregroundColor(colors.statusAccent)
                                        .frame(width: 24)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Encrypted DNS (DoH)")
                                            .font(.system(size: 15))
                                            .foregroundColor(colors.textPrimary)
                                        Text(selectedDohName)
                                            .font(.system(size: 12))
                                            .foregroundColor(colors.textSecondary)
                                    }

                                    Spacer()

                                    Image(systemName: isDohExpanded ? "chevron.up" : "chevron.down")
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(colors.textMuted)
                                }
                                .padding(.vertical, 2)
                            }

                            if isDohExpanded {
                                VStack(alignment: .leading, spacing: 4) {
                                    ForEach(dohProviders, id: \.0) { provider in
                                        let isSelected = selectedDohProvider == provider.0
                                        Button(action: {
                                            selectedDohProvider = provider.0
                                            withAnimation {
                                                isDohExpanded = false
                                            }
                                        }) {
                                            HStack {
                                                VStack(alignment: .leading, spacing: 2) {
                                                    Text(provider.1)
                                                        .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                                                        .foregroundColor(colors.textPrimary)
                                                    Text(provider.2)
                                                        .font(.system(size: 11))
                                                        .foregroundColor(colors.textMuted)
                                                }
                                                Spacer()
                                                if isSelected {
                                                    Image(systemName: "checkmark")
                                                        .font(.system(size: 12, weight: .bold))
                                                        .foregroundColor(colors.statusSuccess)
                                                }
                                            }
                                            .padding(.vertical, 6)
                                            .padding(.horizontal, 8)
                                            .background(isSelected ? colors.surfaceElevated : Color.clear)
                                            .cornerRadius(8)
                                        }
                                    }
                                }
                                .padding(.top, 4)
                            }
                        }

                        // Passkeys & Biometrics (Tap opens PasskeyVaultSheet)
                        Button(action: {
                            state.showPasskeyVaultSheet = true
                        }) {
                            HStack {
                                Image(systemName: "touchid")
                                    .font(.system(size: 16))
                                    .foregroundColor(colors.statusAccent)
                                    .frame(width: 24)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Passkeys & Biometrics")
                                        .font(.system(size: 15))
                                        .foregroundColor(colors.textPrimary)
                                    Text("FIDO2 WebAuthn & FaceID Vault (\(state.passkeys.count) keys)")
                                        .font(.system(size: 11))
                                        .foregroundColor(colors.textMuted)
                                }

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(colors.textMuted)
                            }
                            .padding(.vertical, 2)
                        }

                        // Azokle Sovereign Player Mode (Android Parity)
                        HStack {
                            Image(systemName: "play.circle.fill")
                                .font(.system(size: 16))
                                .foregroundColor(colors.statusTor)
                                .frame(width: 24)

                            VStack(alignment: .leading, spacing: 2) {
                                Text("Azokle Sovereign Player")
                                    .font(.system(size: 15))
                                    .foregroundColor(colors.textPrimary)
                                Text("Ad-free, zero-cookie YouTube & Vimeo")
                                    .font(.system(size: 11))
                                    .foregroundColor(colors.textMuted)
                            }

                            Spacer()

                            Picker("Player Mode", selection: $state.playerMode) {
                                Text("Ask").tag(AzoklePlayerMode.ask)
                                Text("Always").tag(AzoklePlayerMode.always)
                                Text("Off").tag(AzoklePlayerMode.disabled)
                            }
                            .pickerStyle(MenuPickerStyle())
                        }
                        .padding(.vertical, 2)

                        // Desktop Mode
                        HStack {
                            Image(systemName: "desktopcomputer")
                                .font(.system(size: 16))
                                .foregroundColor(colors.textSecondary)
                                .frame(width: 24)

                            Text("Desktop Mode")
                                .foregroundColor(colors.textPrimary)

                            Spacer()

                            Toggle("", isOn: $state.desktopMode)
                                .labelsHidden()
                                .tint(colors.statusAccent)
                        }
                    }
                    .listRowBackground(colors.surface)

                    // 4. Data & Incinerator
                    Section(header: Text("DATA & INCINERATOR").foregroundColor(colors.textMuted)) {
                        Button(action: {
                            state.clearHistory()
                            showHistoryClearedToast = true
                        }) {
                            HStack {
                                Image(systemName: "clock.arrow.circlepath")
                                    .foregroundColor(colors.statusAccent)
                                    .frame(width: 24)
                                Text("Clear Recent Search History")
                                    .foregroundColor(colors.textPrimary)
                            }
                        }

                        Button(action: {
                            state.burnAllData()
                            isPresented = false
                        }) {
                            HStack {
                                Image(systemName: "flame.fill")
                                    .foregroundColor(colors.statusFire)
                                    .frame(width: 24)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Incinerate All Browsing Data")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(colors.statusFire)
                                    Text("Close all tabs, purge cookies, caches and keystore")
                                        .font(.system(size: 11))
                                        .foregroundColor(colors.textMuted)
                                }
                            }
                            .padding(.vertical, 2)
                        }
                    }
                    .listRowBackground(colors.surface)

                    // 5. About & Specifications
                    Section(header: Text("ABOUT & SPECIFICATIONS").foregroundColor(colors.textMuted)) {
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

                        Button(action: {
                            isPresented = false
                            state.showProUpgradeSheet = true
                        }) {
                            HStack {
                                Image(systemName: "crown.fill")
                                    .foregroundColor(colors.statusAccent)
                                    .frame(width: 24)
                                Text("Azokle Pro Membership")
                                    .foregroundColor(colors.statusAccent)
                            }
                        }

                        Button(action: {
                            isPresented = false
                            state.showOnboarding = true
                        }) {
                            HStack {
                                Image(systemName: "sparkles")
                                    .foregroundColor(colors.statusAccent)
                                    .frame(width: 24)
                                Text("Replay Welcome Walkthrough")
                                    .foregroundColor(colors.statusAccent)
                            }
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
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(colors.statusAccent)
                }
            }
            .alert("Search History Cleared", isPresented: $showHistoryClearedToast) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Local search suggestions and query history have been purged.")
            }
        }
    }

    private var activeLanguageName: String {
        let langs = [
            ("system", "Auto-detect"),
            ("en", "English"),
            ("es", "Español"),
            ("fr", "Français"),
            ("de", "Deutsch"),
            ("zh", "中文"),
            ("ja", "日本語"),
            ("ar", "العربية"),
            ("hi", "हिन्दी")
        ]
        return langs.first(where: { $0.0 == state.selectedLanguageCode })?.1 ?? "English"
    }

    private var selectedDohName: String {
        dohProviders.first(where: { $0.0 == selectedDohProvider })?.1 ?? "Cloudflare 1.1.1.1"
    }
}

