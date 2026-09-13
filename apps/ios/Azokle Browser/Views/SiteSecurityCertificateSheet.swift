import SwiftUI

// MARK: - Site Security & X.509 Certificate Sheet (1:1 Android SiteSecurityCertificateSheet Parity)
public struct SiteSecurityCertificateSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var showTechDetails: Bool = false
    @State private var copiedFingerprint: Bool = false

    let colors: AzokleColors = .dark

    private var activeUrl: String {
        state.activeTab?.url ?? "https://search.azokle.com"
    }

    private var securityInfo: SiteSecurityInfo {
        state.getSiteSecurityInfo(for: activeUrl)
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 12)
                .padding(.bottom, 10)

            // 1. Sleek Header Bar
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Site Security")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Cryptographic identity & site controls")
                        .font(.system(size: 12, weight: .regular))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()

                Button(action: {
                    isPresented = false
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(colors.textSecondary)
                        .frame(width: 32, height: 32)
                        .background(colors.surfaceElevated)
                        .clipShape(Circle())
                        .overlay(
                            Circle().stroke(colors.border, lineWidth: 1)
                        )
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)

            Divider()
                .background(colors.border)

            ScrollView {
                VStack(spacing: 16) {
                    // 2. Primary Cryptographic Status Hero Banner
                    statusHeroBanner

                    // 3. Host Identity & Azokle Shield Toggle Card
                    hostAndShieldCard

                    // 4. Quick Permissions Overview Card
                    permissionsOverviewCard

                    // 5. Expandable Technical Details Card (X.509 & Cipher Specs)
                    technicalDetailsCard
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.72), .large])
    }

    // MARK: - Status Hero Banner
    private var statusHeroBanner: some View {
        let isTor = securityInfo.isTor
        let isSecure = securityInfo.isSecure

        let iconName = isTor ? "network.badge.shield.half.filled" : (isSecure ? "lock.fill" : "exclamationmark.triangle.fill")
        let iconTint = isTor ? colors.statusTor : (isSecure ? colors.statusSuccess : colors.statusDestructive)
        let badgeBg = iconTint.opacity(0.12)
        let title = isTor ? "Tor Circuit Protected" : (isSecure ? "Connection is Secure" : "Connection is Insecure")
        let desc = isTor ? "Traffic routed through 3 encrypted Tor onion hops" : (isSecure ? "Your information is encrypted before transmission" : "Passwords or credit cards could be intercepted")

        return HStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(badgeBg)
                    .frame(width: 48, height: 48)

                Image(systemName: iconName)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(iconTint)
            }

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(colors.textPrimary)

                    Text(securityInfo.protocolVersion)
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(iconTint.opacity(0.18))
                        .foregroundColor(iconTint)
                        .cornerRadius(AzokleRadius.small)
                }

                Text(desc)
                    .font(.system(size: 12))
                    .foregroundColor(colors.textSecondary)
                    .lineLimit(2)
            }

            Spacer(minLength: 0)
        }
        .padding(14)
        .background(colors.surfaceElevated)
        .cornerRadius(AzokleRadius.medium)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                .stroke(iconTint.opacity(0.3), lineWidth: 1)
        )
    }

    // MARK: - Host & Shield Card
    private var hostAndShieldCard: some View {
        let isShieldActive = state.isShieldEnabled(for: securityInfo.host)

        return VStack(spacing: 12) {
            // Host Row
            HStack(spacing: 10) {
                Image(systemName: "globe")
                    .font(.system(size: 16))
                    .foregroundColor(colors.statusAccent)

                VStack(alignment: .leading, spacing: 2) {
                    Text(securityInfo.host)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(colors.textPrimary)
                    Text("Current Host Authority")
                        .font(.system(size: 11))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()
            }

            Divider().background(colors.border)

            // Shield Toggle Row
            HStack {
                HStack(spacing: 8) {
                    Image(systemName: "shield.fill")
                        .font(.system(size: 15))
                        .foregroundColor(isShieldActive ? colors.statusSuccess : colors.textMuted)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Azokle Sovereign Shield")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(colors.textPrimary)
                        Text(isShieldActive ? "Content & tracker blocking active" : "Shield paused for this domain")
                            .font(.system(size: 11))
                            .foregroundColor(colors.textSecondary)
                    }
                }

                Spacer()

                Toggle("", isOn: Binding(
                    get: { state.isShieldEnabled(for: securityInfo.host) },
                    set: { state.setShieldEnabled($0, for: securityInfo.host) }
                ))
                .labelsHidden()
                .tint(colors.statusSuccess)
            }
        }
        .padding(14)
        .background(colors.surface)
        .cornerRadius(AzokleRadius.medium)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                .stroke(colors.border, lineWidth: 1)
        )
    }

    // MARK: - Permissions Overview Card
    private var permissionsOverviewCard: some View {
        Button(action: {
            isPresented = false
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                state.showSiteSettingsSheet = true
            }
        }) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: AzokleRadius.small)
                        .fill(colors.surfaceElevated)
                        .frame(width: 36, height: 36)

                    Image(systemName: "slider.horizontal.3")
                        .font(.system(size: 16))
                        .foregroundColor(colors.statusAccent)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("Site Permissions & Controls")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(colors.textPrimary)
                    Text("Camera, Mic, Location, Storage Isolation")
                        .font(.system(size: 11))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(colors.textMuted)
            }
            .padding(12)
            .background(colors.surface)
            .cornerRadius(AzokleRadius.medium)
            .overlay(
                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                    .stroke(colors.border, lineWidth: 1)
            )
        }
    }

    // MARK: - Expandable Technical Details Card
    private var technicalDetailsCard: some View {
        VStack(spacing: 0) {
            // Card Header
            Button(action: {
                withAnimation(.easeInOut(duration: 0.22)) {
                    showTechDetails.toggle()
                }
            }) {
                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: "key.fill")
                            .font(.system(size: 14))
                            .foregroundColor(colors.statusAccent)

                        Text("Technical Certificate Details")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(colors.textPrimary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(colors.textMuted)
                        .rotationEffect(.degrees(showTechDetails ? 90 : 0))
                }
                .padding(14)
            }

            if showTechDetails {
                Divider().background(colors.border)

                VStack(spacing: 14) {
                    // Subject
                    techRow(label: "Issued To (Subject)", value: "\(securityInfo.issuedToCn)\n\(securityInfo.issuedToOrg)")

                    // Issuer
                    techRow(label: "Issued By (Authority)", value: "\(securityInfo.issuedByCn)\n\(securityInfo.issuedByOrg)")

                    // Validity
                    techRow(label: "Validity Lifespan", value: "\(securityInfo.validFrom) – \(securityInfo.validTo)")

                    // Cipher & Key
                    techRow(label: "Cipher Suite", value: securityInfo.cipherSuite)
                    techRow(label: "Public Key Algorithm", value: securityInfo.keyAlgorithm)

                    // SHA-256 Fingerprint
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text("SHA-256 Fingerprint")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(colors.textMuted)

                            Spacer()

                            Button(action: {
                                UIPasteboard.general.string = securityInfo.sha256Fingerprint
                                let gen = UIImpactFeedbackGenerator(style: .light)
                                gen.impactOccurred()
                                withAnimation {
                                    copiedFingerprint = true
                                }
                                DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                                    withAnimation {
                                        copiedFingerprint = false
                                    }
                                }
                            }) {
                                HStack(spacing: 4) {
                                    Image(systemName: copiedFingerprint ? "checkmark" : "doc.on.doc")
                                        .font(.system(size: 10, weight: .bold))
                                    Text(copiedFingerprint ? "Copied!" : "Copy")
                                        .font(.system(size: 10, weight: .bold))
                                }
                                .foregroundColor(copiedFingerprint ? colors.statusSuccess : colors.statusAccent)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(colors.surfaceElevated)
                                .cornerRadius(AzokleRadius.small)
                            }
                        }

                        Text(securityInfo.sha256Fingerprint)
                            .font(.system(size: 11, weight: .regular, design: .monospaced))
                            .foregroundColor(colors.textPrimary)
                            .lineLimit(3)
                            .padding(8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(colors.cardBg)
                            .cornerRadius(AzokleRadius.small)
                    }
                }
                .padding(14)
            }
        }
        .background(colors.surface)
        .cornerRadius(AzokleRadius.medium)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                .stroke(colors.border, lineWidth: 1)
        )
    }

    private func techRow(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(colors.textMuted)
            Text(value)
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(colors.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
