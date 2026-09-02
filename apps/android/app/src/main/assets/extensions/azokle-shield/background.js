// Azokle Shield Unified uBlock Core Engine (EasyList + EasyPrivacy + YouTube V3 Annihilator)
// High-Speed O(1) Network Interceptor & Real-Time Threat Annihilator for Mozilla GeckoView Quantum.

(function() {
    'use strict';

    // Comprehensive uBlock Origin EasyList, EasyPrivacy, Peter Lowe's & Fanboy Ad/Tracker Hosts
    var blockedHosts = new Set([
        // Google Ads & Tracking Network
        'doubleclick.net', 'googleads.g.doubleclick.net', 'pagead2.googlesyndication.com',
        'adservice.google.com', 'googleadservices.com', 'www.googleadservices.com',
        'partner.googleadservices.com', 'admob.com', 'ads.google.com', 'app-measurement.com',
        '2mdn.net', 'tpc.googlesyndication.com', 'static.doubleclick.net', 'securepubads.g.doubleclick.net',
        'afs.googlesyndication.com', 'pagead2.googleadservices.com', 'video-stats.l.google.com',
        'google-analytics.com', 'ssl.google-analytics.com', 'analytics.google.com',
        'googletagmanager.com', 'googletagservices.com', 'tagmanager.google.com',

        // Meta / Facebook Telemetry, Pixel & Graph Trackers
        'pixel.facebook.com', 'an.facebook.com', 'connect.facebook.net', 'analytics.facebook.com',
        'graph.instagram.com', 'tr.snapchat.com', 'analytics.tiktok.com', 'ads.tiktok.com',
        'ads-twitter.com', 'static.ads-twitter.com', 'analytics.twitter.com', 't.co',
        'ads.linkedin.com', 'snap.licdn.com', 'ads.pinterest.com', 'ct.pinterest.com', 'log.pinterest.com',

        // Major Ad Exchanges, Header Bidders & DSPs
        'adnxs.com', 'ib.adnxs.com', 'appnexus.com', 'criteo.com', 'criteo.net', 'static.criteo.net',
        'outbrain.com', 'widgets.outbrain.com', 'taboola.com', 'cdn.taboola.com', 'trc.taboola.com',
        'amazon-adsystem.com', 'aax.amazon-adsystem.com', 'adservice.amazon.com', 'advertising.amazon.com',
        'popads.net', 'popcash.net', 'propellerads.com', 'adsterra.com', 'mgid.com', 'jsc.mgid.com',
        'revcontent.com', 'inmobi.com', 'unityads.unity3d.com', 'applovin.com', 'vungle.com',
        'ironsrc.com', 'adcolony.com', 'chartboost.com', 'rubiconproject.com', 'pubmatic.com',
        'openx.net', 'smartadserver.com', 'adtechus.com', 'casalemedia.com', 'bidswitch.net',
        'contextweb.com', 'sovrn.com', 'conversantmedia.com', 'exponential.com', 'sharethrough.com',
        'triplelift.com', 'yieldmo.com', 'undertone.com', 'media.net', 'gumgum.com', 'teads.tv',
        'spotxchange.com', 'spotx.tv', 'tremorhub.com', 'unruly.co', 'stickyads.tv', 'smaato.net',
        'fyber.com', 'mintegral.com', 'pangle.io', 'supersonicads.com', 'leadbolt.com', 'adblade.com',
        'trafficjunky.com', 'exoclick.com', 'monetag.com', 'adcash.com', 'ezoic.net', 'ezoic.com',
        'zergnet.com', 'contentexchange.me', 'adkernel.com', 'adform.net', 'serving-sys.com',
        'bs.serving-sys.com', 'zemanta.com', 'lijit.com', 'applifier.com', 'zergnet.com',
        'adthrive.com', 'mediavine.com', 'monumetric.com', 'buysellads.com', 'carbonads.net',
        'chitika.net', 'infolinks.com', 'skimmr.com', 'adroll.com', 'dianomi.com', 'trafficroots.com',

        // Tracking Beacons, Product Analytics, Session Replayers & Telemetry
        'scorecardresearch.com', 'b.scorecardresearch.com', 'sb.scorecardresearch.com',
        'pixel.quantserve.com', 'quantcast.com', 'rules.quantcount.com', 'moatads.com',
        'geo.moatads.com', 'iasds.com', 'doubleverify.com', 'clarity.ms', 'c.bing.com',
        'bat.bing.com', 'mc.yandex.ru', 'static.hotjar.com', 'script.hotjar.com', 'hotjar.com',
        'mouseflow.com', 'crazyegg.com', 'fullstory.com', 'rs.fullstory.com', 'edge.fullstory.com',
        'segment.io', 'segment.com', 'cdn.segment.com', 'api.segment.io',
        'mixpanel.com', 'api.mixpanel.com', 'cdn.mxpnl.com',
        'amplitude.com', 'api.amplitude.com', 'cdn.amplitude.com',
        'heapanalytics.com', 'branch.io', 'cdn.branch.io', 'appsflyer.com', 'app.adjust.com',
        'adjust.com', 'kochava.com', 'singular.net', 'r.logrocket.io', 'logrocket.com',
        'chartbeat.com', 'chartbeat.net', 'static.chartbeat.com', 'ping.chartbeat.net',
        'parsely.com', 'optimizely.com', 'statcounter.com', 'browser.sentry-cdn.com',
        'telemetry.mozilla.org', 'newrelic.com', 'js-agent.newrelic.com', 'bam.nr-data.net',
        'datadoghq.com', 'browser-intake-datadoghq.com', 'datadoghq-browser-agent.com',
        'bugsnag.com', 'browser.bugsnag.com', 'onesignal.com', 'pushwoosh.com',

        // Data Brokers, CNAME Trackers & Identity Graphs
        'liveramp.com', 'rlcdn.com', 'bluekai.com', 'demdex.net', 'omtrdc.net', 'everesttech.net',
        'exelator.com', 'lotame.com', 'crwdcntrl.net', 'tapad.com', 'neustar.biz', 'intentiq.com',
        'id5-sync.com', 'uidapi.com',

        // Crypto-Miners & Invasive Scripts
        'coinhive.com', 'coin-hive.com', 'crypto-loot.com', 'minr.pw', 'webminepool.com',
        'jsecoin.com', 'authedmine.com'
    ]);

    // High-Precision Path & Query Filters (Excludes video CDNs & first-party stream protocols)
    var blockedPathSubstrings = [
        '/pagead/', '/adservice/', '/ads/', '/telemetry/', '/collect?', '/gtag/js',
        '/pixel.gif', '/beacon/', '/api/stats/ads',
        'googleads', 'googlesyndication', 'doubleclick', 'amazon-adsystem',
        'adnxs.com', 'criteo.com', 'taboola.com', 'outbrain.com',
        '/smartads/', '/adserver/', '/delivery/ajs.php', '/banner/', '/popunder'
    ];

    function extractHostname(url) {
        try {
            var a = new URL(url);
            return a.hostname.toLowerCase();
        } catch(e) {
            return '';
        }
    }

    function isDomainOrSuffixBlocked(host) {
        if (!host) return false;
        if (blockedHosts.has(host)) return true;
        var parts = host.split('.');
        for (var i = 1; i < parts.length - 1; i++) {
            var sub = parts.slice(i).join('.');
            if (blockedHosts.has(sub)) return true;
        }
        return false;
    }

    var whitelistedHosts = new Set([
        'azokle.com', 'search.azokle.com', 'api.azokle.com',
        'cloudflare.com', 'cloudflareinsights.com', 'challenges.cloudflare.com',
        'challenge.cloudflare.com', 'static.cloudflareinsights.com', 'static.cloudflare.com',
        'hcaptcha.com', 'recaptcha.net'
    ]);

    // Real-Time Immediate Native Block Reporting Bridge
    // Uses sendNativeMessage which is fully supported for built-in geckoViewAddons extensions
    function reportBlockToApp(host) {
        if (!host) return;
        var h = host.toLowerCase().trim();
        if (whitelistedHosts.has(h) || h.endsWith('.azokle.com') || h.endsWith('.cloudflare.com') || h.endsWith('.cloudflareinsights.com')) {
            return;
        }
        try {
            browser.runtime.sendNativeMessage('nativeApp', {
                type: 'RECORD_BLOCK',
                host: h
            }).catch(function() {});
        } catch(e) {}
    }

    function isWhitelistedDomain(url, host) {
        if (!host) return false;
        var h = host.toLowerCase().trim();

        // 0. NEVER block googlevideo.com (YouTube Media Video/Audio Streaming CDN)
        if (h.includes('googlevideo.com') || h.endsWith('.googlevideo.com')) {
            return true;
        }

        // 1. Exact or suffix domain matching
        if (whitelistedHosts.has(h)) return true;
        if (h.endsWith('.azokle.com') || h.endsWith('.cloudflare.com') ||
            h.endsWith('.cloudflareinsights.com') || h.endsWith('.hcaptcha.com') ||
            h.endsWith('.recaptcha.net') || h.includes('turnstile')) {
            return true;
        }

        // 2. Cloudflare challenge platform endpoints on any domain
        var u = (url || '').toLowerCase();
        if (u.includes('/cdn-cgi/') ||
            u.includes('challenges.cloudflare.com') ||
            u.includes('challenge.cloudflare.com') ||
            u.includes('/challenge-platform/')) {
            return true;
        }

        return false;
    }

    // Dynamic per-site shield exclusion list (synced from native app)
    var disabledShieldHosts = new Set();

    // WebRequest Blocking Listener (High-Performance Zero-Overhead Interceptor)
    browser.webRequest.onBeforeRequest.addListener(
        function(details) {
            var url = details.url;
            var host = extractHostname(url);
            var urlLower = url.toLowerCase();

            // 0. Fast-path sync on request if stale
            if (Date.now() - lastSyncTimestamp > 500) {
                syncNativeAppStatus();
            }

            // 0.1 Check if user disabled shield for document origin or target host
            var docUrl = details.documentUrl || details.initiator || details.originUrl || '';
            var docHost = extractHostname(docUrl);
            if ((docHost && disabledShieldHosts.has(docHost)) || (host && disabledShieldHosts.has(host))) {
                return { cancel: false };
            }

            // 1. Security & Sovereign Whitelist: NEVER block ANY request for azokle.com, *.azokle.com, Cloudflare, or googlevideo.com
            if (isWhitelistedDomain(urlLower, host)) {
                return { cancel: false };
            }

            // 1.1 Protect first-party YouTube player requests from false positive path blocks
            if ((docHost.includes('youtube.com') || host.includes('youtube.com')) && (
                urlLower.includes('/youtubei/v1/player') || urlLower.includes('/api/stats/') ||
                urlLower.includes('/gen_204') || urlLower.includes('/client_204') ||
                urlLower.includes('/ptracking')
            )) {
                return { cancel: false };
            }

            // 2. Direct Domain or Suffix Match
            if (isDomainOrSuffixBlocked(host)) {
                reportBlockToApp(host);
                return { cancel: true };
            }

            // 3. Path & Substring Match
            for (var j = 0; j < blockedPathSubstrings.length; j++) {
                if (urlLower.includes(blockedPathSubstrings[j])) {
                    reportBlockToApp(host || 'ad-tracker');
                    return { cancel: true };
                }
            }

            return { cancel: false };
        },
        { urls: ['<all_urls>'] },
        ['blocking']
    );

    // =========================================================================
    // Content Script Message Hub (Relays content script requests to native app)
    // =========================================================================
    var currentAppTheme = 'system';
    if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.onMessage) {
        browser.runtime.onMessage.addListener(function(message, sender, sendResponse) {
            if (!message) return;
            if (message.type === 'RECORD_BLOCK' && message.host) {
                reportBlockToApp(message.host);
                if (sendResponse) sendResponse({ ok: true });
            } else if (message.type === 'GET_THEME') {
                if (sendResponse) sendResponse({ theme: currentAppTheme });
            } else if (message.type === 'PWA_MANIFEST_DISCOVERED') {
                // Security check: strictly allow manifest discovery only from the top-level main frame
                if (sender && sender.frameId === 0 && typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendNativeMessage) {
                    browser.runtime.sendNativeMessage('nativeApp', message).then(function(res) {
                        if (sendResponse) sendResponse(res);
                    }).catch(function() {});
                }
            }
        });
    }

    // =========================================================================
    // Tor SOCKS5 Onion Routing Controller & Native App Synchronization
    // Uses sendNativeMessage (GeckoView built-in extension, geckoViewAddons perm)
    // Synchronous, non-blocking proxy.onRequest for instant zero-latency page loading
    // =========================================================================
    var isTorRoutingActive = false;
    var activeSocksPort = 9050;
    var lastSyncTimestamp = 0;

    function syncNativeAppStatus() {
        lastSyncTimestamp = Date.now();
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendNativeMessage) {
            browser.runtime.sendNativeMessage('nativeApp', { type: 'GET_APP_STATUS' })
                .then(function(res) {
                    if (typeof res === 'string') {
                        try {
                            res = JSON.parse(res);
                        } catch(e) {
                            return;
                        }
                    }
                    if (res) {
                        if (typeof res.isTorEnabled === 'boolean') {
                            isTorRoutingActive = res.isTorEnabled;
                        }
                        if (typeof res.socksPort === 'number' && res.socksPort > 0) {
                            activeSocksPort = res.socksPort;
                        }
                        if (typeof res.theme === 'string') {
                            currentAppTheme = res.theme;
                        }
                        if (Array.isArray(res.disabledHosts)) {
                            disabledShieldHosts = new Set(res.disabledHosts.map(function(h) { return (h || '').toLowerCase(); }));
                        }
                    }
                })
                .catch(function(err) {
                    // Safe retry
                });
        }
    }

    // Immediate start sync and rapid interval
    syncNativeAppStatus();
    setInterval(syncNativeAppStatus, 1500);

    // Fast-path sync on navigation events
    if (typeof browser !== 'undefined' && browser.webNavigation && browser.webNavigation.onBeforeNavigate) {
        browser.webNavigation.onBeforeNavigate.addListener(function() {
            if (Date.now() - lastSyncTimestamp > 500) {
                syncNativeAppStatus();
            }
        });
    }

    // browser.proxy.onRequest: Synchronous instant proxy routing.
    // Zero latency, zero hanging, zero white screen.
    // proxyDNS=true guarantees DNS resolution happens on Tor exit relay.
    // Fail-Closed Security: .onion and Tor requests NEVER fall back to direct cleartext.
    browser.proxy.onRequest.addListener(
        function(requestInfo) {
            var url = requestInfo.url || '';
            var isOnion = url.indexOf('.onion') !== -1;
            var isTorCandidate = isOnion || (isTorRoutingActive && requestInfo.incognito === true);

            if (isTorCandidate) {
                // If Tor routing is required, ensure active port and proxy via SOCKS5
                var port = activeSocksPort || 9050;
                return {
                    type: 'socks',
                    host: '127.0.0.1',
                    port: port,
                    proxyDNS: true
                };
            }

            return { type: 'direct' };
        },
        { urls: ['<all_urls>'] }
    );

    console.log('[Azokle Shield] v3.5 — uBlock Core + Real-Time Tor SOCKS5 Zero-Leak Proxy active.');
})();
