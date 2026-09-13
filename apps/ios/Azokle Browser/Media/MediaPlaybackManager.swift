import Foundation
import AVFoundation
import MediaPlayer
import Combine

// MARK: - Sleep Timer Duration
public enum SleepTimerDuration: Int, CaseIterable, Identifiable {
    case off = 0
    case min15 = 15
    case min30 = 30
    case min45 = 45
    case min60 = 60

    public var id: Int { rawValue }

    public var label: String {
        switch self {
        case .off: return "Off"
        case .min15: return "15 Minutes"
        case .min30: return "30 Minutes"
        case .min45: return "45 Minutes"
        case .min60: return "60 Minutes"
        }
    }
}

// MARK: - Sovereign Media Playback Manager
public final class MediaPlaybackManager: ObservableObject {
    public static let shared = MediaPlaybackManager()

    @Published public var isPlaying: Bool = false
    @Published public var currentTitle: String = "Azokle Sovereign Media"
    @Published public var currentArtist: String = "Ad-Free Stream"
    @Published public var currentTime: Double = 0.0
    @Published public var totalDuration: Double = 0.0
    @Published public var playbackRate: Float = 1.0
    @Published public var activeSleepTimer: SleepTimerDuration = .off
    @Published public var sleepTimerRemainingSeconds: Int = 0

    private var sleepTimer: Timer? = nil

    private init() {
        setupAudioSession()
        setupRemoteCommandCenter()
    }

    // MARK: - Audio Session Setup
    public func setupAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .moviePlayback, options: [.allowAirPlay, .defaultToSpeaker])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("[MediaPlaybackManager] AudioSession configuration failed: \(error.localizedDescription)")
        }
    }

    // MARK: - Lock Screen & Control Center Remote Commands
    private func setupRemoteCommandCenter() {
        let commandCenter = MPRemoteCommandCenter.shared()

        commandCenter.playCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async {
                self?.isPlaying = true
                self?.updateNowPlayingInfo()
            }
            return .success
        }

        commandCenter.pauseCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async {
                self?.isPlaying = false
                self?.updateNowPlayingInfo()
            }
            return .success
        }

        commandCenter.togglePlayPauseCommand.addTarget { [weak self] _ in
            guard let self = self else { return .commandFailed }
            DispatchQueue.main.async {
                self.isPlaying.toggle()
                self.updateNowPlayingInfo()
            }
            return .success
        }

        commandCenter.skipForwardCommand.preferredIntervals = [10]
        commandCenter.skipForwardCommand.addTarget { [weak self] _ in
            guard let self = self else { return .commandFailed }
            DispatchQueue.main.async {
                self.currentTime = min(self.currentTime + 10, self.totalDuration)
                self.updateNowPlayingInfo()
            }
            return .success
        }

        commandCenter.skipBackwardCommand.preferredIntervals = [10]
        commandCenter.skipBackwardCommand.addTarget { [weak self] _ in
            guard let self = self else { return .commandFailed }
            DispatchQueue.main.async {
                self.currentTime = max(self.currentTime - 10, 0)
                self.updateNowPlayingInfo()
            }
            return .success
        }
    }

    // MARK: - Now Playing Info
    public func updateNowPlayingInfo(title: String? = nil, artist: String? = nil, duration: Double? = nil, elapsed: Double? = nil) {
        if Thread.isMainThread {
            self.applyNowPlayingInfo(title: title, artist: artist, duration: duration, elapsed: elapsed)
        } else {
            DispatchQueue.main.async {
                self.applyNowPlayingInfo(title: title, artist: artist, duration: duration, elapsed: elapsed)
            }
        }
    }

    private func applyNowPlayingInfo(title: String? = nil, artist: String? = nil, duration: Double? = nil, elapsed: Double? = nil) {
        if let title = title { self.currentTitle = title }
        if let artist = artist { self.currentArtist = artist }
        if let duration = duration { self.totalDuration = duration }
        if let elapsed = elapsed { self.currentTime = elapsed }

        var nowPlayingInfo: [String: Any] = [
            MPMediaItemPropertyTitle: currentTitle,
            MPMediaItemPropertyArtist: currentArtist,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: currentTime,
            MPMediaItemPropertyPlaybackDuration: totalDuration,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? playbackRate : 0.0
        ]

        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }

    // MARK: - Sleep Timer
    public func setSleepTimer(_ duration: SleepTimerDuration) {
        activeSleepTimer = duration
        sleepTimer?.invalidate()
        sleepTimer = nil

        if duration == .off {
            sleepTimerRemainingSeconds = 0
            return
        }

        sleepTimerRemainingSeconds = duration.rawValue * 60
        sleepTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] timer in
            guard let self = self else { return }
            DispatchQueue.main.async {
                if self.sleepTimerRemainingSeconds > 0 {
                    self.sleepTimerRemainingSeconds -= 1
                } else {
                    self.isPlaying = false
                    self.activeSleepTimer = .off
                    timer.invalidate()
                    self.sleepTimer = nil
                    self.updateNowPlayingInfo()
                }
            }
        }
    }
}
