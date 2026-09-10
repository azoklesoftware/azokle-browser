package com.azokle.search.adblock

import java.util.Locale

/**
 * Categorization of third-party trackers based on industry standard Tracker Data Sets (TDS).
 */
enum class TrackerCategory(val label: String) {
    ADVERTISING("Advertising Network"),
    ANALYTICS("Site Analytics & Telemetry"),
    SOCIAL("Social Network Tracker"),
    CONTENT("Content & Sponsored Feeds"),
    FINGERPRINTING("Fingerprinting & Cryptomining"),
    DATA_BROKER("Data Broker & Profiling"),
    GENERAL("Third-Party Tracker")
}

/**
 * Entity describing the corporate owner behind a tracking or advertising network.
 */
data class TrackerEntity(
    val companyName: String,
    val displayName: String,
    val category: TrackerCategory
)

/**
 * Detailed record of a blocked tracker instance for transparency reporting.
 */
data class BlockedTrackerRecord(
    val host: String,
    val entity: TrackerEntity,
    val blockCount: Int = 1
)

/**
 * High-performance, O(1) in-memory Entity Resolution Engine.
 * Resolves raw tracking domain strings into verified parent companies (Google, Meta, Amazon,
 * Microsoft, ByteDance, etc.) following DuckDuckGo TDS entity mapping standards.
 */
object EntityLookup {

    private val domainToEntityMap = HashMap<String, TrackerEntity>(128)

    init {
        // --- Alphabet / Google LLC ---
        val google = TrackerEntity("Alphabet Inc.", "Google LLC", TrackerCategory.ADVERTISING)
        val googleAnalytics = TrackerEntity("Alphabet Inc.", "Google Analytics", TrackerCategory.ANALYTICS)
        listOf(
            "doubleclick.net", "googleads.g.doubleclick.net", "googlesyndication.com",
            "pagead2.googlesyndication.com", "adservice.google.com", "googleadservices.com",
            "www.googleadservices.com", "partner.googleadservices.com", "admob.com",
            "ads.google.com", "2mdn.net", "tpc.googlesyndication.com", "static.doubleclick.net",
            "securepubads.g.doubleclick.net", "afs.googlesyndication.com",
            "pagead2.googleadservices.com", "video-stats.l.google.com", "google.com"
        ).forEach { domainToEntityMap[it] = google }

        listOf(
            "google-analytics.com", "ssl.google-analytics.com", "analytics.google.com",
            "googletagmanager.com", "googletagservices.com", "tagmanager.google.com"
        ).forEach { domainToEntityMap[it] = googleAnalytics }

        // --- Meta Platforms, Inc. ---
        val meta = TrackerEntity("Meta Platforms, Inc.", "Meta / Facebook", TrackerCategory.SOCIAL)
        listOf(
            "pixel.facebook.com", "an.facebook.com", "connect.facebook.net", "analytics.facebook.com",
            "graph.instagram.com", "facebook.net", "atdmt.com"
        ).forEach { domainToEntityMap[it] = meta }

        // --- Amazon.com, Inc. ---
        val amazon = TrackerEntity("Amazon.com, Inc.", "Amazon Advertising", TrackerCategory.ADVERTISING)
        listOf(
            "amazon-adsystem.com", "aax.amazon-adsystem.com", "adservice.amazon.com", "advertising.amazon.com"
        ).forEach { domainToEntityMap[it] = amazon }

        // --- Microsoft Corporation ---
        val microsoft = TrackerEntity("Microsoft Corporation", "Microsoft Advertising", TrackerCategory.ADVERTISING)
        val msClarity = TrackerEntity("Microsoft Corporation", "Microsoft Clarity", TrackerCategory.ANALYTICS)
        listOf(
            "adnxs.com", "ib.adnxs.com", "appnexus.com", "c.bing.com", "bat.bing.com"
        ).forEach { domainToEntityMap[it] = microsoft }
        listOf("clarity.ms").forEach { domainToEntityMap[it] = msClarity }

        // --- ByteDance Ltd. ---
        val bytedance = TrackerEntity("ByteDance Ltd.", "TikTok / ByteDance", TrackerCategory.SOCIAL)
        listOf(
            "analytics.tiktok.com", "ads.tiktok.com", "pangle.io", "byteoversea.com"
        ).forEach { domainToEntityMap[it] = bytedance }

        // --- Criteo SA ---
        val criteo = TrackerEntity("Criteo SA", "Criteo Advertising", TrackerCategory.ADVERTISING)
        listOf("criteo.com", "criteo.net", "static.criteo.net").forEach { domainToEntityMap[it] = criteo }

        // --- Taboola, Inc. & Outbrain Inc. ---
        val taboola = TrackerEntity("Taboola, Inc.", "Taboola Feed", TrackerCategory.CONTENT)
        listOf("taboola.com", "cdn.taboola.com", "trc.taboola.com").forEach { domainToEntityMap[it] = taboola }

        val outbrain = TrackerEntity("Outbrain Inc.", "Outbrain Widget", TrackerCategory.CONTENT)
        listOf("outbrain.com", "widgets.outbrain.com").forEach { domainToEntityMap[it] = outbrain }

        // --- Snap Inc. ---
        val snap = TrackerEntity("Snap Inc.", "Snapchat Pixel", TrackerCategory.SOCIAL)
        listOf("tr.snapchat.com", "snap.licdn.com").forEach { domainToEntityMap[it] = snap }

        // --- X Corp. (Twitter) ---
        val xCorp = TrackerEntity("X Corp.", "Twitter / X Ads", TrackerCategory.SOCIAL)
        listOf("ads-twitter.com", "static.ads-twitter.com", "analytics.twitter.com", "t.co").forEach { domainToEntityMap[it] = xCorp }

        // --- Analytics Platforms ---
        val hotjar = TrackerEntity("Hotjar Ltd.", "Hotjar Session Recorder", TrackerCategory.ANALYTICS)
        listOf("static.hotjar.com", "script.hotjar.com", "hotjar.com").forEach { domainToEntityMap[it] = hotjar }

        val segment = TrackerEntity("Twilio Inc.", "Segment Analytics", TrackerCategory.ANALYTICS)
        listOf("segment.io", "segment.com", "cdn.segment.com", "api.segment.io").forEach { domainToEntityMap[it] = segment }

        val mixpanel = TrackerEntity("Mixpanel, Inc.", "Mixpanel", TrackerCategory.ANALYTICS)
        listOf("mixpanel.com", "api.mixpanel.com", "cdn.mxpnl.com").forEach { domainToEntityMap[it] = mixpanel }

        val amplitude = TrackerEntity("Amplitude, Inc.", "Amplitude Analytics", TrackerCategory.ANALYTICS)
        listOf("amplitude.com", "api.amplitude.com", "cdn.amplitude.com").forEach { domainToEntityMap[it] = amplitude }

        val sentry = TrackerEntity("Functional Software, Inc.", "Sentry Error Telemetry", TrackerCategory.ANALYTICS)
        listOf("browser.sentry-cdn.com", "sentry.io").forEach { domainToEntityMap[it] = sentry }

        val datadog = TrackerEntity("Datadog, Inc.", "Datadog RUM Telemetry", TrackerCategory.ANALYTICS)
        listOf("datadoghq.com", "browser-intake-datadoghq.com", "datadoghq-browser-agent.com").forEach { domainToEntityMap[it] = datadog }

        val newrelic = TrackerEntity("New Relic, Inc.", "New Relic Browser", TrackerCategory.ANALYTICS)
        listOf("newrelic.com", "js-agent.newrelic.com", "bam.nr-data.net").forEach { domainToEntityMap[it] = newrelic }

        // --- Data Brokers & Attribution SDKs ---
        val liveramp = TrackerEntity("LiveRamp Holdings, Inc.", "LiveRamp Identity Graph", TrackerCategory.DATA_BROKER)
        listOf("liveramp.com", "rlcdn.com", "id5-sync.com", "uidapi.com").forEach { domainToEntityMap[it] = liveramp }

        val appsflyer = TrackerEntity("AppsFlyer Ltd.", "AppsFlyer Attribution", TrackerCategory.ANALYTICS)
        listOf("appsflyer.com", "app.appsflyer.com").forEach { domainToEntityMap[it] = appsflyer }

        val adjust = TrackerEntity("Adjust GmbH", "Adjust Attribution", TrackerCategory.ANALYTICS)
        listOf("adjust.com", "app.adjust.com").forEach { domainToEntityMap[it] = adjust }

        val scorecard = TrackerEntity("Full Circle Studies, Inc.", "ScorecardResearch", TrackerCategory.DATA_BROKER)
        listOf("scorecardresearch.com", "b.scorecardresearch.com", "sb.scorecardresearch.com").forEach { domainToEntityMap[it] = scorecard }

        val quantcast = TrackerEntity("Quantcast Corp.", "Quantcast Measure", TrackerCategory.ADVERTISING)
        listOf("pixel.quantserve.com", "quantcast.com", "rules.quantcount.com").forEach { domainToEntityMap[it] = quantcast }

        // --- Cryptominers ---
        val cryptominer = TrackerEntity("Disclosed Cryptominer", "Cryptominer Script", TrackerCategory.FINGERPRINTING)
        listOf("coinhive.com", "coin-hive.com", "crypto-loot.com", "minr.pw", "webminepool.com").forEach { domainToEntityMap[it] = cryptominer }
    }

    /**
     * Resolves a raw domain or subdomain to its known corporate owner entity.
     * Checks exact match first, then walks up parent domains.
     */
    fun resolve(domain: String): TrackerEntity {
        val cleanHost = domain.lowercase().trim()
        if (cleanHost.isBlank()) {
            return TrackerEntity("Unknown Origin", "Unknown Tracker", TrackerCategory.GENERAL)
        }

        // 1. Exact match
        domainToEntityMap[cleanHost]?.let { return it }

        // 2. Subdomain check (e.g. sub.analytics.google.com -> google.com)
        var dotIndex = cleanHost.indexOf('.')
        while (dotIndex != -1) {
            val parentDomain = cleanHost.substring(dotIndex + 1)
            domainToEntityMap[parentDomain]?.let { return it }
            dotIndex = cleanHost.indexOf('.', dotIndex + 1)
        }

        // 3. Fallback: Clean capitalized base domain name
        val parts = cleanHost.split('.')
        val baseName = if (parts.size >= 2) parts[parts.size - 2] else cleanHost
        val formattedName = baseName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val inferredCategory = when {
            cleanHost.contains("ad") || cleanHost.contains("banner") || cleanHost.contains("pop") -> TrackerCategory.ADVERTISING
            cleanHost.contains("analytic") || cleanHost.contains("metric") || cleanHost.contains("stat") -> TrackerCategory.ANALYTICS
            cleanHost.contains("track") || cleanHost.contains("pixel") || cleanHost.contains("beacon") -> TrackerCategory.DATA_BROKER
            else -> TrackerCategory.GENERAL
        }

        return TrackerEntity(formattedName, formattedName, inferredCategory)
    }
}
