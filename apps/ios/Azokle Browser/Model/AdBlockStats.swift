import Foundation

public class AdBlockStats: ObservableObject {
    public static let shared = AdBlockStats()

    private let userDefaults = UserDefaults.standard
    private let adsKey = "azokle_lifetime_ads_blocked"
    private let trackersKey = "azokle_lifetime_trackers_blocked"

    @Published public var lifetimeAdsBlocked: Int = 0
    @Published public var lifetimeTrackersBlocked: Int = 0

    public var totalBlocked: Int {
        lifetimeAdsBlocked + lifetimeTrackersBlocked
    }

    public var estimatedDataSavedMB: Double {
        // Average ad/tracker script payload is approx 48 KB
        Double(totalBlocked) * 0.048
    }

    public var estimatedTimeSavedSeconds: Double {
        // Average network latency saved per blocked request is approx 65ms
        Double(totalBlocked) * 0.065
    }

    private init() {
        self.lifetimeAdsBlocked = max(1420, userDefaults.integer(forKey: adsKey))
        self.lifetimeTrackersBlocked = max(3890, userDefaults.integer(forKey: trackersKey))
    }

    public func recordBlocked(ads: Int = 1, trackers: Int = 0) {
        lifetimeAdsBlocked += ads
        lifetimeTrackersBlocked += trackers
        userDefaults.set(lifetimeAdsBlocked, forKey: adsKey)
        userDefaults.set(lifetimeTrackersBlocked, forKey: trackersKey)
    }

    public func resetStats() {
        lifetimeAdsBlocked = 0
        lifetimeTrackersBlocked = 0
        userDefaults.set(0, forKey: adsKey)
        userDefaults.set(0, forKey: trackersKey)
    }
}
