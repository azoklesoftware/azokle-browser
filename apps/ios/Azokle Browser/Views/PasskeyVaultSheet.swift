import SwiftUI
import LocalAuthentication

public struct PasskeyVaultSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var isUnlocked: Bool = false
    @State private var authFailed: Bool = false
    @State private var selectedTab: Int = 0 // 0: Passkeys, 1: Password Generator
    @State private var searchQuery: String = ""
    @State private var passkeyToDelete: PasskeyItem? = nil
    @State private var showDeleteAlert: Bool = false

    // Password generator states
    @State private var generatedPassword: String = ""
    @State private var passwordLength: Double = 20
    @State private var includeSymbols: Bool = true
    @State private var copiedToClipboard: Bool = false

    let colors: AzokleColors = .dark

    private var filteredPasskeys: [PasskeyItem] {
        if searchQuery.isEmpty {
            return state.passkeys
        } else {
            return state.passkeys.filter {
                $0.rpId.localizedCaseInsensitiveContains(searchQuery) ||
                $0.userName.localizedCaseInsensitiveContains(searchQuery)
            }
        }
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 10)
                .padding(.bottom, 14)

            if !isUnlocked {
                // Biometric Lock Screen
                VStack(spacing: 20) {
                    Spacer()

                    ZStack {
                        Circle()
                            .fill(colors.statusAccent.opacity(0.12))
                            .frame(width: 72, height: 72)

                        Image(systemName: "faceid")
                            .font(.system(size: 38))
                            .foregroundColor(colors.statusAccent)
                    }

                    VStack(spacing: 6) {
                        Text("Sovereign Vault Locked")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(colors.textPrimary)

                        Text("Authenticate using Face ID or device passcode to inspect and manage your sovereign passkeys and credentials.")
                            .font(.system(size: 13))
                            .foregroundColor(colors.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 30)
                    }

                    if authFailed {
                        Text("Biometric verification failed. Please try again.")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(colors.statusDestructive)
                    }

                    Button(action: {
                        authenticateWithBiometrics()
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "lock.open.fill")
                                .font(.system(size: 14, weight: .bold))
                            Text("Unlock Vault")
                                .font(.system(size: 14, weight: .bold))
                        }
                        .foregroundColor(colors.actionPrimaryText)
                        .padding(.horizontal, 28)
                        .padding(.vertical, 12)
                        .background(colors.actionPrimaryBg)
                        .cornerRadius(AzokleRadius.pill)
                    }

                    Spacer()
                }
                .padding(24)
            } else {
                // Unlocked Vault View
                VStack(spacing: 12) {
                    // Header Bar
                    HStack {
                        HStack(spacing: 8) {
                            Image(systemName: "key.fill")
                                .font(.system(size: 16))
                                .foregroundColor(colors.statusAccent)

                            VStack(alignment: .leading, spacing: 2) {
                                Text("SOVEREIGN VAULT")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(colors.textPrimary)
                                Text("\(state.passkeys.count) passkeys • Hardware Encrypted")
                                    .font(.system(size: 11))
                                    .foregroundColor(colors.textSecondary)
                            }
                        }

                        Spacer()

                        Button(action: {
                            isPresented = false
                        }) {
                            Text("Done")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(colors.statusAccent)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                        }
                    }
                    .padding(.horizontal, 20)

                    // Picker for Passkeys vs Password Generator
                    Picker("Vault Section", selection: $selectedTab) {
                        Text("Passkeys (\(state.passkeys.count))").tag(0)
                        Text("Password Generator").tag(1)
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .padding(.horizontal, 20)

                    if selectedTab == 0 {
                        // Search Filter Bar
                        HStack(spacing: 8) {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 13))
                                .foregroundColor(colors.textMuted)

                            TextField("Filter passkeys...", text: $searchQuery)
                                .font(.system(size: 13))
                                .foregroundColor(colors.textPrimary)
                                .autocapitalization(.none)

                            if !searchQuery.isEmpty {
                                Button(action: { searchQuery = "" }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.system(size: 13))
                                        .foregroundColor(colors.textMuted)
                                }
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(colors.surfaceElevated)
                        .cornerRadius(AzokleRadius.small)
                        .padding(.horizontal, 20)

                        Divider().background(colors.border)

                        // Passkeys List
                        if filteredPasskeys.isEmpty {
                            VStack(spacing: 12) {
                                Spacer()
                                Image(systemName: "key.slash")
                                    .font(.system(size: 40))
                                    .foregroundColor(colors.textMuted)

                                Text(state.passkeys.isEmpty ? "No Passkeys Stored" : "No matching passkeys found")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(colors.textPrimary)

                                Text("When websites offer passkey sign-in, Azokle securely stores your cryptographic credentials on-device.")
                                    .font(.system(size: 12))
                                    .foregroundColor(colors.textSecondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 32)
                                Spacer()
                            }
                        } else {
                            ScrollView {
                                VStack(spacing: 10) {
                                    ForEach(filteredPasskeys) { item in
                                        passkeyCard(item)
                                    }
                                }
                                .padding(.horizontal, 20)
                                .padding(.vertical, 8)
                            }
                        }
                    } else {
                        // Password Generator View
                        VStack(spacing: 16) {
                            // Generated Password Field
                            VStack(alignment: .leading, spacing: 6) {
                                Text("GENERATED PASSWORD")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(colors.textMuted)

                                HStack {
                                    Text(generatedPassword.isEmpty ? "Tap Generate" : generatedPassword)
                                        .font(.system(size: 15, weight: .bold, design: .monospaced))
                                        .foregroundColor(colors.textPrimary)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.7)

                                    Spacer()

                                    Button(action: {
                                        UIPasteboard.general.string = generatedPassword
                                        copiedToClipboard = true
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                                            copiedToClipboard = false
                                        }
                                    }) {
                                        Image(systemName: copiedToClipboard ? "checkmark.circle.fill" : "doc.on.doc")
                                            .font(.system(size: 14))
                                            .foregroundColor(copiedToClipboard ? colors.statusSuccess : colors.statusAccent)
                                            .padding(8)
                                            .background(colors.cardBg)
                                            .clipShape(Circle())
                                    }
                                }
                                .padding(12)
                                .background(colors.surfaceElevated)
                                .cornerRadius(AzokleRadius.small)
                            }

                            // Strength indicator
                            let strength = EncryptedVaultManager.shared.calculatePasswordStrength(password: generatedPassword)
                            HStack {
                                Text("Strength: \(strength.label)")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(colors.textSecondary)
                                Spacer()
                                ProgressView(value: strength.score)
                                    .frame(width: 100)
                                    .tint(strength.score > 0.7 ? colors.statusSuccess : colors.statusWarning)
                            }

                            // Length Slider
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text("Length: \(Int(passwordLength))")
                                        .font(.system(size: 13, weight: .medium))
                                        .foregroundColor(colors.textPrimary)
                                    Spacer()
                                }
                                Slider(value: $passwordLength, in: 12...36, step: 1)
                                    .tint(colors.statusAccent)
                            }

                            // Symbols toggle
                            Toggle("Include Special Symbols", isOn: $includeSymbols)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(colors.textPrimary)
                                .tint(colors.statusAccent)

                            // Generate Button
                            Button(action: {
                                generatedPassword = EncryptedVaultManager.shared.generateSecurePassword(
                                    length: Int(passwordLength),
                                    includeSymbols: includeSymbols
                                )
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "dice.fill")
                                    Text("Generate New Password")
                                }
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(colors.actionPrimaryText)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(colors.actionPrimaryBg)
                                .cornerRadius(AzokleRadius.medium)
                            }

                            Spacer()
                        }
                        .padding(20)
                        .onAppear {
                            if generatedPassword.isEmpty {
                                generatedPassword = EncryptedVaultManager.shared.generateSecurePassword(length: 20)
                            }
                        }
                    }
                }
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.75), .large])
        .onAppear {
            authenticateWithBiometrics()
        }
        .alert("Delete Passkey?", isPresented: $showDeleteAlert) {
            Button("Delete", role: .destructive) {
                if let target = passkeyToDelete {
                    state.deletePasskey(id: target.id)
                    passkeyToDelete = nil
                }
            }
            Button("Cancel", role: .cancel) {
                passkeyToDelete = nil
            }
        } message: {
            if let target = passkeyToDelete {
                Text("Are you sure you want to remove the passkey for \(target.rpId) (\(target.userName))? You may not be able to log in to this service without an alternative method.")
            }
        }
    }

    private func passkeyCard(_ item: PasskeyItem) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(colors.statusAccent.opacity(0.12))
                    .frame(width: 40, height: 40)

                Image(systemName: "key.horizontal.fill")
                    .font(.system(size: 16))
                    .foregroundColor(colors.statusAccent)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(item.rpId)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Text(item.userName)
                    .font(.system(size: 12))
                    .foregroundColor(colors.textSecondary)

                Text("Created \(item.createdAt.formatted(date: .abbreviated, time: .omitted))")
                    .font(.system(size: 10))
                    .foregroundColor(colors.textMuted)
            }

            Spacer()

            Button(action: {
                passkeyToDelete = item
                showDeleteAlert = true
            }) {
                Image(systemName: "trash")
                    .font(.system(size: 13))
                    .foregroundColor(colors.statusDestructive)
                    .padding(8)
                    .background(colors.surfaceElevated)
                    .clipShape(Circle())
            }
        }
        .padding(12)
        .background(colors.surfaceElevated)
        .cornerRadius(AzokleRadius.medium)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                .stroke(colors.border, lineWidth: 1)
        )
    }

    private func authenticateWithBiometrics() {
        EncryptedVaultManager.shared.authenticate { success in
            self.isUnlocked = success
            self.authFailed = !success
        }
    }
}
