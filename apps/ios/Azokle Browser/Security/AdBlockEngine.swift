import Foundation
import WebKit

// MARK: - Shield Host State
public enum ShieldHostState {
    case enabled
    case disabledUntilRelaunch
    case disabledPermanently
}

// MARK: - Filter Decision
public struct FilterDecision {
    public let shouldBlock: Bool
    public let matchedRule: String?
    public let category: TrackerCategory?

    public init(shouldBlock: Bool, matchedRule: String? = nil, category: TrackerCategory? = nil) {
        self.shouldBlock = shouldBlock
        self.matchedRule = matchedRule
        self.category = category
    }
}

// MARK: - Sovereign Ad Block & Shield Engine for iOS (WebKit native rules + Scriptlets)
public final class AdBlockEngine {
    public static let shared = AdBlockEngine()

    private var permanentDisabledHosts: Set<String> = []
    private var sessionDisabledHosts: Set<String> = []
    private var compiledRuleList: WKContentRuleList? = nil

    private let userDefaultsKey = "com.azokle.browser.shield.disabled_hosts"

    private init() {
        loadPersistedState()
    }

    private func loadPersistedState() {
        if let saved = UserDefaults.standard.stringArray(forKey: userDefaultsKey) {
            permanentDisabledHosts = Set(saved.map { $0.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) })
        }
    }

    private func persistState() {
        UserDefaults.standard.set(Array(permanentDisabledHosts), forKey: userDefaultsKey)
    }

    // MARK: - Host State Management
    public func getShieldState(host: String) -> ShieldHostState {
        let clean = cleanHost(host)
        if sessionDisabledHosts.contains(clean) {
            return .disabledUntilRelaunch
        } else if permanentDisabledHosts.contains(clean) {
            return .disabledPermanently
        } else {
            return .enabled
        }
    }

    public func isShieldEnabled(host: String) -> Bool {
        let clean = cleanHost(host)
        return !sessionDisabledHosts.contains(clean) && !permanentDisabledHosts.contains(clean)
    }

    public func enableShield(host: String) {
        let clean = cleanHost(host)
        sessionDisabledHosts.remove(clean)
        permanentDisabledHosts.remove(clean)
        persistState()
    }

    public func pauseShieldUntilRelaunch(host: String) {
        let clean = cleanHost(host)
        permanentDisabledHosts.remove(clean)
        sessionDisabledHosts.insert(clean)
        persistState()
    }

    public func disableShieldPermanently(host: String) {
        let clean = cleanHost(host)
        sessionDisabledHosts.remove(clean)
        permanentDisabledHosts.insert(clean)
        persistState()
    }

    public func toggleShield(host: String) -> Bool {
        let clean = cleanHost(host)
        if isShieldEnabled(host: clean) {
            disableShieldPermanently(host: clean)
            return false
        } else {
            enableShield(host: clean)
            return true
        }
    }

    private func cleanHost(_ raw: String) -> String {
        var h = raw.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        if h.hasPrefix("www.") {
            h = String(h.dropFirst(4))
        }
        return h
    }

    // MARK: - Native WebKit Content Rule List Generator
    public func compileNativeRuleList(completion: @escaping (WKContentRuleList?) -> Void) {
        if let existing = compiledRuleList {
            completion(existing)
            return
        }

        // Generate high-speed native WebKit JSON rules (Safari Content Blocker format)
        let rulesJson = """
        [
            {
                "trigger": {
                    "url-filter": ".*",
                    "if-domain": [
                        "*doubleclick.net", "*googlesyndication.com", "*googleadservices.com",
                        "*amazon-adsystem.com", "*adnxs.com", "*criteo.com", "*criteo.net",
                        "*taboola.com", "*outbrain.com", "*scorecardresearch.com",
                        "*quantserve.com", "*pixel.facebook.com", "*analytics.tiktok.com",
                        "*coinhive.com", "*coin-hive.com", "*crypto-loot.com",
                        "*clarity.ms", "*hotjar.com", "*segment.io", "*mixpanel.com"
                    ]
                },
                "action": {
                    "type": "block"
                }
            },
            {
                "trigger": {
                    "url-filter": ".*(adservice|pagead|ads/|/ads\\\\.|/pixel\\\\.|/analytics\\\\.js|googletagmanager\\\\.com/gtm\\\\.js).*"
                },
                "action": {
                    "type": "block"
                }
            },
            {
                "trigger": {
                    "url-filter": ".*",
                    "resource-type": ["style-sheet"],
                    "if-domain": ["*"]
                },
                "action": {
                    "type": "css-display-none",
                    "selector": ".ad-banner, .adsbox, .advertisement-holder, .sponsored-post, [data-ad-unit], [id^='google_ads_iframe'], [class*='adsbygoogle'], .ytp-ad-overlay-container, .ytp-ad-message-container"
                }
            }
        ]
        """

        WKContentRuleListStore.default().compileContentRuleList(
            forIdentifier: "AzokleSovereignShieldRules",
            encodedContentRuleList: rulesJson
        ) { [weak self] ruleList, error in
            if let error = error {
                print("[AdBlockEngine] Failed to compile WKContentRuleList: \(error.localizedDescription)")
                completion(nil)
            } else {
                self?.compiledRuleList = ruleList
                completion(ruleList)
            }
        }
    }

    // MARK: - Cosmetic CSS & Defuser Scriptlets
    public func getCosmeticCss(domain: String) -> String {
        return """
        .adsbygoogle, [id^='google_ads_iframe'], [id^='div-gpt-ad'],
        .ad-container, .ad-wrapper, .advertisement, .sponsored-content,
        .ytp-ad-overlay-container, .ytp-ad-message-container, .video-ads,
        #player-ads, .ad-interrupting {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            width: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
        """
    }

    public func getDefuserScriptlet(domain: String) -> String {
        return """
        (function() {
            'use strict';
            if (window.__azokle_defuser_injected) return;
            window.__azokle_defuser_injected = true;

            // 1. Google Tag Manager / Analytics Surrogate
            window.ga = window.ga || function() {};
            window.gtag = window.gtag || function() {};
            window.dataLayer = window.dataLayer || [];

            // 2. Anti-Adblock Defuser (noop common detection flags)
            try {
                window.canRunAds = true;
                window.isAdBlockActive = false;
                window.adblock = false;
            } catch(e) {}

            // 3. RFC 8828 WebRTC Local IP Obfuscation
            try {
                var OrigPeerConn = window.RTCPeerConnection || window.webkitRTCPeerConnection;
                if (OrigPeerConn && OrigPeerConn.prototype) {
                    var origCreateOffer = OrigPeerConn.prototype.createOffer;
                    if (origCreateOffer) {
                        OrigPeerConn.prototype.createOffer = function() {
                            return origCreateOffer.apply(this, arguments).then(function(offer) {
                                if (offer && offer.sdp) {
                                    offer.sdp = offer.sdp.split('\\r\\n').filter(function(line) {
                                        return line.indexOf('a=candidate:') !== 0 || line.indexOf(' typ host') === -1;
                                    }).join('\\r\\n');
                                }
                                return offer;
                            });
                        };
                    }
                }
            } catch(e) {}
        })();
        """
    }
}
