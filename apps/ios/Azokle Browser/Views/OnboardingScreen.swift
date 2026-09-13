import SwiftUI

// MARK: - Sovereign Onboarding Screen (1:1 Android OnboardingScreen.kt Parity)
public struct OnboardingScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var currentStep: Int = 0
    let colors: AzokleColors = .dark

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Skip Action
                HStack {
                    Spacer()
                    if currentStep < 3 {
                        Button("Skip") {
                            finishOnboarding()
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(colors.textSecondary)
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                    }
                }

                // Paged Onboarding Steps
                TabView(selection: $currentStep) {
                    // Step 1: Sovereign AI Search
                    stepOneView.tag(0)

                    // Step 2: Azokle Shield Protection
                    stepTwoView.tag(1)

                    // Step 3: Tor Onion Circuits
                    stepThreeView.tag(2)

                    // Step 4: Search Engine Selection & Launch
                    stepFourView.tag(3)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.3), value: currentStep)

                // Bottom Indicator & Next Button
                VStack(spacing: 18) {
                    // Page Dots Indicator
                    HStack(spacing: 8) {
                        ForEach(0..<4, id: \.self) { index in
                            Capsule()
                                .fill(currentStep == index ? colors.statusAccent : colors.border)
                                .frame(width: currentStep == index ? 24 : 8, height: 8)
                                .animation(.spring(), value: currentStep)
                        }
                    }

                    // Main Action Button
                    Button(action: {
                        if currentStep < 3 {
                            withAnimation {
                                currentStep += 1
                            }
                        } else {
                            finishOnboarding()
                        }
                    }) {
                        HStack(spacing: 6) {
                            Text(currentStep == 3 ? "Get Started" : "Continue")
                                .font(.system(size: 16, weight: .bold))
                            Image(systemName: "arrow.right")
                                .font(.system(size: 14, weight: .bold))
                        }
                        .foregroundColor(colors.actionPrimaryText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                        .background(colors.actionPrimaryBg)
                        .cornerRadius(AzokleRadius.pill)
                        .shadow(color: colors.statusAccent.opacity(0.3), radius: 10, y: 4)
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
                }
            }
        }
    }

    // Step 1 View
    private var stepOneView: some View {
        VStack(spacing: 24) {
            Spacer()

            Text("azokle")
                .font(.audiowide(size: 58))
                .foregroundColor(.white)
                .tracking(-1.0)
                .shadow(color: colors.statusAccent.opacity(0.4), radius: 16)

            VStack(spacing: 10) {
                Text("Sovereign AI Search")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Text("Search the world's knowledge with complete sovereignty. Zero telemetry, zero user tracking, and absolute data autonomy.")
                    .font(.system(size: 14))
                    .foregroundColor(colors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Spacer()
        }
    }

    // Step 2 View
    private var stepTwoView: some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                Circle()
                    .fill(colors.statusSuccess.opacity(0.12))
                    .frame(width: 140, height: 140)

                Image(systemName: "shield.fill")
                    .font(.system(size: 64))
                    .foregroundColor(colors.statusSuccess)
            }

            VStack(spacing: 10) {
                Text("Azokle Sovereign Shield")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Text("Built-in content suppression engine that blocks advertising networks, tracking beacons, fingerprinting scripts, and WebRTC IP leaks.")
                    .font(.system(size: 14))
                    .foregroundColor(colors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Spacer()
        }
    }

    // Step 3 View
    private var stepThreeView: some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                Circle()
                    .fill(colors.statusTor.opacity(0.12))
                    .frame(width: 140, height: 140)

                Image(systemName: "network.badge.shield.half.filled")
                    .font(.system(size: 64))
                    .foregroundColor(colors.statusTor)
            }

            VStack(spacing: 10) {
                Text("Tor Onion Circuits")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Text("Seamlessly browse dark web onion services and bypass surveillance with 3-hop encrypted onion anonymity routing.")
                    .font(.system(size: 14))
                    .foregroundColor(colors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Spacer()
        }
    }

    // Step 4 View
    private var stepFourView: some View {
        VStack(spacing: 20) {
            Spacer()

            VStack(spacing: 6) {
                Text("Choose Search Engine")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Text("Select your primary sovereign search provider")
                    .font(.system(size: 13))
                    .foregroundColor(colors.textSecondary)
            }

            VStack(spacing: 8) {
                ForEach(SearchEngine.allCases) { engine in
                    let isSelected = state.selectedSearchEngine == engine
                    Button(action: {
                        state.selectedSearchEngine = engine
                    }) {
                        HStack(spacing: 12) {
                            Image(engine.assetName)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 28, height: 28)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(engine.name)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(colors.textPrimary)
                                Text(engine.domain)
                                    .font(.system(size: 11))
                                    .foregroundColor(colors.textSecondary)
                            }

                            Spacer()

                            if isSelected {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(colors.statusAccent)
                            }
                        }
                        .padding(12)
                        .background(colors.surface)
                        .cornerRadius(AzokleRadius.medium)
                        .overlay(
                            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                .stroke(isSelected ? colors.statusAccent : colors.border, lineWidth: 1)
                        )
                    }
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
    }

    private func finishOnboarding() {
        UserDefaults.standard.set(true, forKey: "hasCompletedOnboarding")
        isPresented = false
    }
}
