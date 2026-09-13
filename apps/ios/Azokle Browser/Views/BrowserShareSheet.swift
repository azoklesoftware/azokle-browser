import SwiftUI
import CoreImage.CIFilterBuiltins

// MARK: - Privacy-First Browser Share Sheet (1:1 Android BrowserShareSheet Parity)
public struct BrowserShareSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var stripTracking: Bool = true
    @State private var showQrCode: Bool = false
    @State private var copiedFeedback: Bool = false

    let colors: AzokleColors = .dark

    private var activeUrl: String {
        state.activeTab?.url ?? "https://search.azokle.com"
    }

    private var activeTitle: String {
        state.activeTab?.title ?? "Azokle Sovereign Search"
    }

    private var finalShareUrl: String {
        stripTracking ? state.cleanUrlForSharing(activeUrl) : activeUrl
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 12)
                .padding(.bottom, 12)

            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Share Privacy-Clean Link")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Removes profiling tags and trackers")
                        .font(.system(size: 12))
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
            .padding(.bottom, 14)

            Divider().background(colors.border)

            ScrollView {
                VStack(spacing: 16) {
                    // Preview Card
                    VStack(alignment: .leading, spacing: 6) {
                        Text(activeTitle)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(colors.textPrimary)
                            .lineLimit(1)

                        Text(finalShareUrl)
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundColor(colors.textSecondary)
                            .lineLimit(2)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // Tracker Stripping Toggle
                    HStack {
                        HStack(spacing: 8) {
                            Image(systemName: "shield.checkered")
                                .font(.system(size: 16))
                                .foregroundColor(colors.statusSuccess)

                            VStack(alignment: .leading, spacing: 2) {
                                Text("Sanitize Tracking Parameters")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(colors.textPrimary)
                                Text("Removes utm_*, fbclid, gclid, and referral IDs")
                                    .font(.system(size: 11))
                                    .foregroundColor(colors.textSecondary)
                            }
                        }

                        Spacer()

                        Toggle("", isOn: $stripTracking)
                            .labelsHidden()
                            .tint(colors.statusSuccess)
                    }
                    .padding(12)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // QR Code Generator Card
                    VStack(spacing: 12) {
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                showQrCode.toggle()
                            }
                        }) {
                            HStack {
                                Image(systemName: "qrcode")
                                    .font(.system(size: 16))
                                    .foregroundColor(colors.statusAccent)

                                Text(showQrCode ? "Hide QR Code" : "Show On-Device QR Code")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(colors.textPrimary)

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(colors.textMuted)
                                    .rotationEffect(.degrees(showQrCode ? 90 : 0))
                            }
                        }

                        if showQrCode {
                            Divider().background(colors.border)

                            VStack(spacing: 8) {
                                if let qrImage = generateQrCode(from: finalShareUrl) {
                                    Image(uiImage: qrImage)
                                        .interpolation(.none)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 170, height: 170)
                                        .padding(10)
                                        .background(Color.white)
                                        .cornerRadius(AzokleRadius.medium)
                                        .shadow(color: Color.black.opacity(0.3), radius: 8)
                                }

                                Text("Scan with any camera to open securely")
                                    .font(.system(size: 11))
                                    .foregroundColor(colors.textSecondary)
                            }
                            .padding(.top, 6)
                        }
                    }
                    .padding(14)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // Quick Copy Action
                    Button(action: {
                        UIPasteboard.general.string = finalShareUrl
                        let gen = UIImpactFeedbackGenerator(style: .medium)
                        gen.impactOccurred()
                        withAnimation {
                            copiedFeedback = true
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                            withAnimation {
                                copiedFeedback = false
                            }
                        }
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: copiedFeedback ? "checkmark" : "doc.on.doc")
                                .font(.system(size: 14, weight: .bold))
                            Text(copiedFeedback ? "Clean Link Copied!" : "Copy Clean Link")
                                .font(.system(size: 14, weight: .bold))
                        }
                        .foregroundColor(colors.actionPrimaryText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(colors.actionPrimaryBg)
                        .cornerRadius(AzokleRadius.pill)
                    }

                    // Native System Share Sheet Trigger
                    Button(action: {
                        if let url = URL(string: finalShareUrl) {
                            let activityVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
                            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                               let rootVC = windowScene.windows.first?.rootViewController {
                                isPresented = false
                                rootVC.present(activityVC, animated: true)
                            }
                        }
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "square.and.arrow.up")
                                .font(.system(size: 14, weight: .medium))
                            Text("More Sharing Options...")
                                .font(.system(size: 14, weight: .medium))
                        }
                        .foregroundColor(colors.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(colors.surfaceElevated)
                        .cornerRadius(AzokleRadius.pill)
                        .overlay(
                            RoundedRectangle(cornerRadius: AzokleRadius.pill)
                                .stroke(colors.border, lineWidth: 1)
                        )
                    }
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.68), .large])
    }

    private func generateQrCode(from string: String) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.setValue(Data(string.utf8), forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")

        guard let outputImage = filter.outputImage else { return nil }
        let context = CIContext()
        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: 5, y: 5))
        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
