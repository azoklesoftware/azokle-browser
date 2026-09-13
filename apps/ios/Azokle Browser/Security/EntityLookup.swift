import Foundation

// MARK: - Tracker Category
public enum TrackerCategory: String, CaseIterable, Identifiable {
    case advertising = "Advertising Network"
    case analytics = "Site Analytics & Telemetry"
    case social = "Social Network Tracker"
    case content = "Content & Sponsored Feeds"
    case fingerprinting = "Fingerprinting & Cryptomining"
    case dataBroker = "Data Broker & Profiling"
    case general = "Third-Party Tracker"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .advertising: return "megaphone.fill"
        case .analytics: return "chart.xyaxis.line"
        case .social: return "person.2.fill"
        case .content: return "newspaper.fill"
        case .fingerprinting: return "touchid"
        case .dataBroker: return "server.rack"
        case .general: return "shield.slash.fill"
        }
    }
}

// MARK: - Tracker Entity
public struct TrackerEntity: Hashable, Identifiable {
    public var id: String { "\(companyName):\(displayName)" }
    public let companyName: String
    public let displayName: String
    public let category: TrackerCategory

    public init(companyName: String, displayName: String, category: TrackerCategory) {
        self.companyName = companyName
        self.displayName = displayName
        self.category = category
    }
}

// MARK: - Blocked Tracker Record
public struct BlockedTrackerRecord: Identifiable, Hashable {
    public let id = UUID()
    public let host: String
    public let entity: TrackerEntity
    public var blockCount: Int

    public init(host: String, entity: TrackerEntity, blockCount: Int = 1) {
        self.host = host
        self.entity = entity
        self.blockCount = blockCount
    }
}

// MARK: - Entity Lookup Engine
public final class EntityLookup {
    public static let shared = EntityLookup()

    private var domainToEntityMap: [String: TrackerEntity] = [:]

    private init() {
        populateEntityMap()
    }

    private func populateEntityMap() {
        // Alphabet / Google LLC
        let google = TrackerEntity(companyName: "Alphabet Inc.", displayName: "Google LLC", category: .advertising)
        let googleAnalytics = TrackerEntity(companyName: "Alphabet Inc.", displayName: "Google Analytics", category: .analytics)
        [
            "doubleclick.net", "googleads.g.doubleclick.net", "googlesyndication.com",
            "pagead2.googlesyndication.com", "adservice.google.com", "googleadservices.com",
            "www.googleadservices.com", "partner.googleadservices.com", "admob.com",
            "ads.google.com", "2mdn.net", "tpc.googlesyndication.com", "static.doubleclick.net",
            "securepubads.g.doubleclick.net", "afs.googlesyndication.com",
            "pagead2.googleadservices.com", "video-stats.l.google.com", "google.com"
        ].forEach { domainToEntityMap[$0] = google }

        [
            "google-analytics.com", "ssl.google-analytics.com", "analytics.google.com",
            "googletagmanager.com", "googletagservices.com", "tagmanager.google.com"
        ].forEach { domainToEntityMap[$0] = googleAnalytics }

        // Meta Platforms, Inc.
        let meta = TrackerEntity(companyName: "Meta Platforms, Inc.", displayName: "Meta / Facebook", category: .social)
        [
            "pixel.facebook.com", "an.facebook.com", "connect.facebook.net", "analytics.facebook.com",
            "graph.instagram.com", "facebook.net", "atdmt.com"
        ].forEach { domainToEntityMap[$0] = meta }

        // Amazon.com, Inc.
        let amazon = TrackerEntity(companyName: "Amazon.com, Inc.", displayName: "Amazon Advertising", category: .advertising)
        [
            "amazon-adsystem.com", "aax.amazon-adsystem.com", "adservice.amazon.com", "advertising.amazon.com"
        ].forEach { domainToEntityMap[$0] = amazon }

        // Microsoft Corporation
        let microsoft = TrackerEntity(companyName: "Microsoft Corporation", displayName: "Microsoft Advertising", category: .advertising)
        let msClarity = TrackerEntity(companyName: "Microsoft Corporation", displayName: "Microsoft Clarity", category: .analytics)
        [
            "adnxs.com", "ib.adnxs.com", "appnexus.com", "c.bing.com", "bat.bing.com"
        ].forEach { domainToEntityMap[$0] = microsoft }
        domainToEntityMap["clarity.ms"] = msClarity

        // ByteDance Ltd.
        let bytedance = TrackerEntity(companyName: "ByteDance Ltd.", displayName: "TikTok / ByteDance", category: .social)
        [
            "analytics.tiktok.com", "ads.tiktok.com", "pangle.io", "byteoversea.com"
        ].forEach { domainToEntityMap[$0] = bytedance }

        // Criteo SA
        let criteo = TrackerEntity(companyName: "Criteo SA", displayName: "Criteo Advertising", category: .advertising)
        ["criteo.com", "criteo.net", "static.criteo.net"].forEach { domainToEntityMap[$0] = criteo }

        // Taboola, Inc. & Outbrain Inc.
        let taboola = TrackerEntity(companyName: "Taboola, Inc.", displayName: "Taboola Feed", category: .content)
        ["taboola.com", "cdn.taboola.com", "trc.taboola.com"].forEach { domainToEntityMap[$0] = taboola }

        let outbrain = TrackerEntity(companyName: "Outbrain Inc.", displayName: "Outbrain Widget", category: .content)
        ["outbrain.com", "widgets.outbrain.com"].forEach { domainToEntityMap[$0] = outbrain }

        // Snap Inc.
        let snap = TrackerEntity(companyName: "Snap Inc.", displayName: "Snapchat Pixel", category: .social)
        ["tr.snapchat.com", "snap.licdn.com"].forEach { domainToEntityMap[$0] = snap }

        // X Corp. (Twitter)
        let xCorp = TrackerEntity(companyName: "X Corp.", displayName: "Twitter / X Ads", category: .social)
        ["ads-twitter.com", "static.ads-twitter.com", "analytics.twitter.com", "t.co"].forEach { domainToEntityMap[$0] = xCorp }

        // Analytics Platforms
        let hotjar = TrackerEntity(companyName: "Hotjar Ltd.", displayName: "Hotjar Session Recorder", category: .analytics)
        ["static.hotjar.com", "script.hotjar.com", "hotjar.com"].forEach { domainToEntityMap[$0] = hotjar }

        let segment = TrackerEntity(companyName: "Twilio Inc.", displayName: "Segment Analytics", category: .analytics)
        ["segment.io", "segment.com", "cdn.segment.com", "api.segment.io"].forEach { domainToEntityMap[$0] = segment }

        let mixpanel = TrackerEntity(companyName: "Mixpanel, Inc.", displayName: "Mixpanel", category: .analytics)
        ["mixpanel.com", "api.mixpanel.com", "cdn.mxpnl.com"].forEach { domainToEntityMap[$0] = mixpanel }

        let amplitude = TrackerEntity(companyName: "Amplitude, Inc.", displayName: "Amplitude Analytics", category: .analytics)
        ["amplitude.com", "api.amplitude.com", "cdn.amplitude.com"].forEach { domainToEntityMap[$0] = amplitude }

        let sentry = TrackerEntity(companyName: "Functional Software, Inc.", displayName: "Sentry Error Telemetry", category: .analytics)
        ["browser.sentry-cdn.com", "sentry.io"].forEach { domainToEntityMap[$0] = sentry }

        let datadog = TrackerEntity(companyName: "Datadog, Inc.", displayName: "Datadog RUM Telemetry", category: .analytics)
        ["datadoghq.com", "browser-intake-datadoghq.com", "datadoghq-browser-agent.com"].forEach { domainToEntityMap[$0] = datadog }

        let newrelic = TrackerEntity(companyName: "New Relic, Inc.", displayName: "New Relic Browser", category: .analytics)
        ["newrelic.com", "js-agent.newrelic.com", "bam.nr-data.net"].forEach { domainToEntityMap[$0] = newrelic }

        // Data Brokers
        let liveramp = TrackerEntity(companyName: "LiveRamp Holdings, Inc.", displayName: "LiveRamp Identity Graph", category: .dataBroker)
        ["liveramp.com", "rlcdn.com", "id5-sync.com", "uidapi.com"].forEach { domainToEntityMap[$0] = liveramp }

        let appsflyer = TrackerEntity(companyName: "AppsFlyer Ltd.", displayName: "AppsFlyer Attribution", category: .analytics)
        ["appsflyer.com", "app.appsflyer.com"].forEach { domainToEntityMap[$0] = appsflyer }

        let adjust = TrackerEntity(companyName: "Adjust GmbH", displayName: "Adjust Attribution", category: .analytics)
        ["adjust.com", "app.adjust.com"].forEach { domainToEntityMap[$0] = adjust }

        let scorecard = TrackerEntity(companyName: "Full Circle Studies, Inc.", displayName: "ScorecardResearch", category: .dataBroker)
        ["scorecardresearch.com", "b.scorecardresearch.com", "sb.scorecardresearch.com"].forEach { domainToEntityMap[$0] = scorecard }

        let quantcast = TrackerEntity(companyName: "Quantcast Corp.", displayName: "Quantcast Measure", category: .advertising)
        ["pixel.quantserve.com", "quantcast.com", "rules.quantcount.com"].forEach { domainToEntityMap[$0] = quantcast }

        // Cryptominers
        let cryptominer = TrackerEntity(companyName: "Disclosed Cryptominer", displayName: "Cryptominer Script", category: .fingerprinting)
        ["coinhive.com", "coin-hive.com", "crypto-loot.com", "minr.pw", "webminepool.com"].forEach { domainToEntityMap[$0] = cryptominer }
    }

    public func resolve(domain: String) -> TrackerEntity {
        let cleanHost = domain.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        if cleanHost.isEmpty {
            return TrackerEntity(companyName: "Unknown Origin", displayName: "Unknown Tracker", category: .general)
        }

        // 1. Exact match
        if let entity = domainToEntityMap[cleanHost] {
            return entity
        }

        // 2. Subdomain check (walk up hierarchy)
        var searchDomain = cleanHost
        while let dotIndex = searchDomain.firstIndex(of: ".") {
            searchDomain = String(searchDomain[searchDomain.index(after: dotIndex)...])
            if let entity = domainToEntityMap[searchDomain] {
                return entity
            }
        }

        // 3. Fallback: Titlecased base domain
        let parts = cleanHost.split(separator: ".")
        let baseName = parts.count >= 2 ? String(parts[parts.count - 2]) : cleanHost
        let formattedName = baseName.prefix(1).uppercased() + baseName.dropFirst()

        let inferredCategory: TrackerCategory
        if cleanHost.contains("ad") || cleanHost.contains("banner") || cleanHost.contains("pop") {
            inferredCategory = .advertising
        } else if cleanHost.contains("analytic") || cleanHost.contains("metric") || cleanHost.contains("stat") {
            inferredCategory = .analytics
        } else if cleanHost.contains("track") || cleanHost.contains("pixel") || cleanHost.contains("beacon") {
            inferredCategory = .dataBroker
        } else {
            inferredCategory = .general
        }

        return TrackerEntity(companyName: formattedName, displayName: formattedName, category: inferredCategory)
    }
}
