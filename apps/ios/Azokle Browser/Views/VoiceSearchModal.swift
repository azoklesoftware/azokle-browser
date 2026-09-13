import SwiftUI
import Speech
import AVFoundation

// MARK: - Voice Search Modal with Real Speech Recognition (1:1 Android Parity)
public struct VoiceSearchModal: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var waveScale1: CGFloat = 1.0
    @State private var waveScale2: CGFloat = 1.0
    @State private var statusText: String = "Listening..."
    @State private var recognizedText: String = ""
    @State private var isRecording: Bool = false

    @State private var audioEngine = AVAudioEngine()
    @State private var speechRecognizer = SFSpeechRecognizer(locale: Locale.current)
    @State private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    @State private var recognitionTask: SFSpeechRecognitionTask?

    let colors: AzokleColors = .dark

    let suggestedPrompts = [
        "What is quantum computing?",
        "Latest tech privacy news",
        "How does Tor encryption work?",
        "Decentralized AI search"
    ]

    public var body: some View {
        ZStack {
            Color.black.opacity(0.85)
                .ignoresSafeArea()
                .onTapGesture {
                    stopRecording()
                    isPresented = false
                }

            VStack(spacing: 28) {
                // Top Close Bar
                HStack {
                    Spacer()
                    Button(action: {
                        stopRecording()
                        isPresented = false
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(colors.textSecondary)
                            .frame(width: 36, height: 36)
                            .background(colors.surfaceElevated)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(colors.border, lineWidth: 1))
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)

                Spacer()

                // Animated Concentric Pulsing Rings & Microphone
                ZStack {
                    // Outer Ring
                    Circle()
                        .stroke(colors.statusAccent.opacity(0.2), lineWidth: 2)
                        .frame(width: 140, height: 140)
                        .scaleEffect(waveScale2)
                        .opacity(2.0 - Double(waveScale2))

                    // Middle Ring
                    Circle()
                        .stroke(colors.statusAccent.opacity(0.4), lineWidth: 3)
                        .frame(width: 100, height: 100)
                        .scaleEffect(waveScale1)
                        .opacity(1.8 - Double(waveScale1))

                    // Center Microphone Core
                    Button(action: {
                        if isRecording {
                            if !recognizedText.isEmpty {
                                submitQuery(recognizedText)
                            } else {
                                stopRecording()
                            }
                        } else {
                            startSpeechRecognition()
                        }
                    }) {
                        Circle()
                            .fill(colors.actionPrimaryBg)
                            .frame(width: 76, height: 76)
                            .shadow(color: colors.statusAccent.opacity(0.5), radius: 16)
                            .overlay(
                                Image(systemName: "mic.fill")
                                    .font(.system(size: 32, weight: .semibold))
                                    .foregroundColor(colors.actionPrimaryText)
                            )
                    }
                }
                .padding(.vertical, 16)

                // Status & Recognized Query
                VStack(spacing: 10) {
                    Text(statusText)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(colors.statusAccent)

                    if !recognizedText.isEmpty {
                        Text("\"\(recognizedText)\"")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(colors.textPrimary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    } else {
                        Text("Speak clearly to search sovereign web")
                            .font(.system(size: 13))
                            .foregroundColor(colors.textSecondary)
                    }
                }

                // Sample Query Pill Shortcuts
                VStack(spacing: 8) {
                    Text("TRY ASKING:")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(colors.textMuted)

                    VStack(spacing: 6) {
                        ForEach(suggestedPrompts, id: \.self) { prompt in
                            Button(action: {
                                stopRecording()
                                submitQuery(prompt)
                            }) {
                                Text(prompt)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(colors.textPrimary)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(colors.surfaceElevated.opacity(0.8))
                                    .cornerRadius(AzokleRadius.pill)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: AzokleRadius.pill)
                                            .stroke(colors.border, lineWidth: 1)
                                    )
                            }
                        }
                    }
                }

                Spacer()
            }
        }
        .onAppear {
            startAudioAnimation()
            startSpeechRecognition()
        }
        .onDisappear {
            stopRecording()
        }
    }

    private func startAudioAnimation() {
        withAnimation(
            Animation.easeInOut(duration: 1.2)
                .repeatForever(autoreverses: true)
        ) {
            waveScale1 = 1.35
        }

        withAnimation(
            Animation.easeInOut(duration: 1.6)
                .repeatForever(autoreverses: true)
                .delay(0.3)
        ) {
            waveScale2 = 1.6
        }
    }

    // MARK: - Native Apple Speech Recognition
    private func startSpeechRecognition() {
        SFSpeechRecognizer.requestAuthorization { authStatus in
            DispatchQueue.main.async {
                switch authStatus {
                case .authorized:
                    self.beginAudioRecording()
                case .denied, .restricted, .notDetermined:
                    self.statusText = "Microphone or Speech Access Required"
                    self.isRecording = false
                @unknown default:
                    self.statusText = "Speech Recognition Unavailable"
                }
            }
        }
    }

    private func beginAudioRecording() {
        // Cancel previous task if active
        recognitionTask?.cancel()
        recognitionTask = nil

        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            statusText = "Audio Recording Error"
            return
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else { return }
        recognitionRequest.shouldReportPartialResults = true

        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)

        inputNode.removeTap(onBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            recognitionRequest.append(buffer)
        }

        audioEngine.prepare()
        do {
            try audioEngine.start()
            self.isRecording = true
            self.statusText = "Listening..."
        } catch {
            self.statusText = "Could not start audio engine"
            return
        }

        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { result, error in
            if let result = result {
                DispatchQueue.main.async {
                    self.recognizedText = result.bestTranscription.formattedString
                    if result.isFinal {
                        self.submitQuery(self.recognizedText)
                    }
                }
            }

            if error != nil || (result?.isFinal ?? false) {
                self.stopRecording()
            }
        }
    }

    private func stopRecording() {
        if audioEngine.isRunning {
            audioEngine.stop()
            audioEngine.inputNode.removeTap(onBus: 0)
            recognitionRequest?.endAudio()
            recognitionTask?.cancel()
            recognitionTask = nil
            recognitionRequest = nil
        }
        isRecording = false
        try? AVAudioSession.sharedInstance().setActive(false)
    }

    private func submitQuery(_ query: String) {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        stopRecording()
        let gen = UIImpactFeedbackGenerator(style: .medium)
        gen.impactOccurred()
        state.navigateActiveTab(to: trimmed)
        isPresented = false
    }
}
