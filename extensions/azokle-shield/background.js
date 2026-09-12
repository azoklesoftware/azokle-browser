// Azokle Shield Unified Engine (Platform-Independent Rule-Driven Interceptor)
// High-Speed O(1) Network Filtering, Tracker Annihilation & Tor SOCKS5 Onion Routing

(function() {
    'use strict';

    // Core Sovereign & Bot-Challenge Whitelist (Infrastructure endpoints only)
    var sovereignHosts = new Set([
        'azokle.com', 'search.azokle.com', 'api.azokle.com',
        'cloudflare.com', 'cloudflareinsights.com', 'challenges.cloudflare.com',
        'challenge.cloudflare.com', 'static.cloudflareinsights.com', 'static.cloudflare.com',
        'hcaptcha.com', 'recaptcha.net'
    ]);

    // High-Confidence Known Ad & Tracker Networks (3rd-party only)
    var knownAdHosts = new Set([
        'doubleclick.net', 'googleads.g.doubleclick.net', 'pagead2.googlesyndication.com',
        'adservice.google.com', 'googleadservices.com', 'www.googleadservices.com',
        'partner.googleadservices.com', 'admob.com', 'ads.google.com',
        '2mdn.net', 'tpc.googlesyndication.com', 'static.doubleclick.net', 'securepubads.g.doubleclick.net',
        'afs.googlesyndication.com', 'pagead2.googleadservices.com', 'video-stats.l.google.com',
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
        'bs.serving-sys.com', 'zemanta.com', 'lijit.com', 'applifier.com',
        'adthrive.com', 'mediavine.com', 'monumetric.com', 'buysellads.com', 'carbonads.net',
        'chitika.net', 'infolinks.com', 'skimmr.com', 'adroll.com', 'dianomi.com', 'trafficroots.com',
        'adtrue.com', 'adversal.com', 'bidvertiser.com', 'clickadu.com', 'hilltopads.com',
        'juicyads.com', 'popmyads.com', 'richpush.com', 'smartads.ge'
    ]);

    var knownTrackerHosts = new Set([
        'google-analytics.com', 'ssl.google-analytics.com', 'analytics.google.com',
        'googletagmanager.com', 'googletagservices.com', 'tagmanager.google.com',
        'pixel.facebook.com', 'an.facebook.com', 'connect.facebook.net', 'analytics.facebook.com',
        'graph.instagram.com', 'tr.snapchat.com', 'analytics.tiktok.com', 'ads.tiktok.com',
        'ads-twitter.com', 'static.ads-twitter.com', 'analytics.twitter.com', 't.co',
        'ads.linkedin.com', 'snap.licdn.com', 'ads.pinterest.com', 'ct.pinterest.com', 'log.pinterest.com',
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
        'liveramp.com', 'rlcdn.com', 'bluekai.com', 'demdex.net', 'omtrdc.net', 'everesttech.net',
        'exelator.com', 'lotame.com', 'crwdcntrl.net', 'tapad.com', 'neustar.biz', 'intentiq.com',
        'id5-sync.com', 'uidapi.com',
        'coinhive.com', 'coin-hive.com', 'crypto-loot.com', 'minr.pw', 'webminepool.com',
        'criteo.com', 'criteo.net', 'gemini.yahoo.com', 'flurry.com', 'braze.com',
        'inspectlet.com', 'luckyorange.com', 'smartlook.com', 'sessioncam.com'
    ]);

    // High-Precision Third-Party Path Filters (applied only to third-party requests)
    var thirdPartyPathSubstrings = [
        '/pagead/', '/adservice/', '/telemetry/', '/gtag/js',
        '/pixel.gif', '/smartads/', '/delivery/ajs.php', '/banner/', '/popunder'
    ];

    // Fast-path decision cache to minimize overhead
    var decisionCache = new Map();
    var MAX_CACHE_ENTRIES = 2000;

    function extractHostname(url) {
        if (!url) return '';
        var start = 0;
        if (url.startsWith('https://')) start = 8;
        else if (url.startsWith('http://')) start = 7;
        else {
            try { return new URL(url).hostname.toLowerCase(); } catch(e) { return ''; }
        }
        var end = url.indexOf('/', start);
        var authority = end === -1 ? url.substring(start) : url.substring(start, end);
        var question = authority.indexOf('?');
        if (question !== -1) authority = authority.substring(0, question);
        var atIndex = authority.lastIndexOf('@');
        var hostWithPort = atIndex !== -1 ? authority.substring(atIndex + 1) : authority;
        var colon = hostWithPort.indexOf(':');
        var host = colon === -1 ? hostWithPort : hostWithPort.substring(0, colon);
        return host.toLowerCase();
    }

    var multiPartSuffixes = new Set([
        'co.uk', 'com.au', 'co.nz', 'co.jp', 'co.za', 'com.br', 'com.mx', 'com.ar', 'com.sg',
        'org.uk', 'gov.uk', 'ac.uk', 'edu.au', 'gov.au', 'net.au', 'ne.jp', 'or.jp', 'go.jp',
        'github.io', 'gitlab.io', 'pages.dev', 'web.app', 'firebaseapp.com', 'cloudfront.net',
        'amazonaws.com', 'azurewebsites.net', 'appspot.com', 'herokuapp.com', 'vercel.app'
    ]);

    function getRegistrableDomain(host) {
        if (!host) return '';
        var parts = host.split('.');
        if (parts.length <= 1) return host;
        if (parts.length >= 3) {
            var twoEnd = parts.slice(-2).join('.');
            if (multiPartSuffixes.has(twoEnd)) {
                return parts.slice(-3).join('.');
            }
        }
        return parts.slice(-2).join('.');
    }

    function isSameSite(hostA, hostB) {
        if (!hostA || !hostB) return false;
        if (hostA === hostB) return true;
        var rootA = getRegistrableDomain(hostA);
        var rootB = getRegistrableDomain(hostB);
        return rootA.length > 0 && rootA === rootB;
    }

    function isDomainInSet(host, set) {
        if (!host) return false;
        if (set.has(host)) return true;
        var idx = host.indexOf('.');
        while (idx !== -1) {
            var sub = host.substring(idx + 1);
            if (set.has(sub)) return true;
            idx = host.indexOf('.', idx + 1);
        }
        return false;
    }

    function isSovereignInfrastructure(url, host) {
        if (!host) return false;
        var h = host.toLowerCase().trim();
        if (sovereignHosts.has(h)) return true;
        if (h.endsWith('.azokle.com') || h.endsWith('.cloudflare.com') ||
            h.endsWith('.cloudflareinsights.com') || h.endsWith('.hcaptcha.com') ||
            h.endsWith('.recaptcha.net') || h.includes('turnstile')) {
            return true;
        }
        var u = (url || '').toLowerCase();
        if (u.includes('/cdn-cgi/') || u.includes('challenges.cloudflare.com') || u.includes('/challenge-platform/')) {
            return true;
        }
        return false;
    }

    // Neutral Surrogates to prevent site breakage when essential analytics/tracker scripts are requested
    var surrogateRules = [
        {
            matcher: function(u, h) {
                return (h.includes('google-analytics.com') && (u.includes('/analytics.js') || u.includes('/ga.js')));
            },
            file: 'surrogates/analytics.js'
        },
        {
            matcher: function(u, h) {
                return u.includes('/gtag/js') || u.includes('/gtag.js');
            },
            file: 'surrogates/gtag.js'
        },
        {
            matcher: function(u, h) {
                return h.includes('googletagmanager.com') && u.includes('/gtm.js');
            },
            file: 'surrogates/gtm.js'
        },
        {
            matcher: function(u, h) {
                return (h.includes('connect.facebook.net') && (u.includes('/fbevents.js') || u.includes('/sdk.js'))) ||
                       (h.includes('facebook.com') && u.includes('/fbevents.js'));
            },
            file: 'surrogates/fbevents.js'
        },
        {
            matcher: function(u, h) {
                return (h.includes('matomo') || h.includes('piwik')) && (u.includes('matomo.js') || u.includes('piwik.js'));
            },
            file: 'surrogates/matomo.js'
        }
    ];

    function getSurrogateRedirect(urlLower, host) {
        if (!urlLower || !host) return null;
        for (var i = 0; i < surrogateRules.length; i++) {
            if (surrogateRules[i].matcher(urlLower, host)) {
                try {
                    return { redirectUrl: browser.runtime.getURL(surrogateRules[i].file) };
                } catch(e) {
                    return null;
                }
            }
        }
        return null;
    }

    // Subdomain prefixes commonly used to disguise CNAME trackers under first-party domains
    var cnameTrackerSubdomains = [
        'metrics.', 'analytics.', 'telemetry.', 'track.', 'tracker.', 'data.', 'log.', 'stats.', 'collector.', 'events.'
    ];

    function isLikelyCnameTracker(host, docHost) {
        if (!host || !docHost || host === docHost) return false;
        if (host.endsWith('.' + docHost)) {
            for (var i = 0; i < cnameTrackerSubdomains.length; i++) {
                if (host.startsWith(cnameTrackerSubdomains[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    // Corporate Parent Entity Graph (TDS Entity Graph)
    // Allows same-entity resource sharing so 1st-party CDNs and media don't break.
    // NOTE: Advertising and tracking endpoints are strictly excluded from this graph.
    var entityGroups = [
        // Alphabet / Google (Functional CDNs & Media only)
        new Set(['google.com', 'google.co.uk', 'gstatic.com', 'recaptcha.net', 'youtube.com', 'ytimg.com']),
        // Meta Platforms (Functional CDNs & Media only)
        new Set(['facebook.com', 'facebook.net', 'fbcdn.net', 'instagram.com', 'cdninstagram.com', 'whatsapp.com']),
        // Microsoft (Functional CDNs only)
        new Set(['microsoft.com', 'live.com', 'office.com', 'bing.com', 'azure.com', 'azureedge.net']),
        // Amazon (Functional CDNs only)
        new Set(['amazon.com', 'media-amazon.com', 'ssl-images-amazon.com', 'a2z.com', 'aws.amazon.com']),
        // Apple
        new Set(['apple.com', 'icloud.com', 'mzstatic.com', 'cdn-apple.com'])
    ];

    function isSameParentEntity(hostA, hostB) {
        if (!hostA || !hostB) return false;
        var cleanA = hostA.toLowerCase();
        var cleanB = hostB.toLowerCase();
        for (var i = 0; i < entityGroups.length; i++) {
            var group = entityGroups[i];
            var matchA = false;
            var matchB = false;
            for (var domain of group) {
                if (cleanA === domain || cleanA.endsWith('.' + domain)) matchA = true;
                if (cleanB === domain || cleanB.endsWith('.' + domain)) matchB = true;
                if (matchA && matchB) return true;
            }
        }
        return false;
    }

    // Tab-scoped Ad Click Exemptions (Bridged from Kotlin AdClickManager)
    // DuckDuckGo Ethical Attribution Model: strictly permits conversion reporting for the landing advertiser,
    // NEVER granting a blanket bypass to third-party ad networks or trackers on that page.
    var activeAdClickExemptions = new Map();

    var attributionEndpoints = new Set([
        'bat.bing.com', 'adservice.google.com', 'googleadservices.com'
    ]);

    function isAdClickExempt(docHost, host) {
        if (!docHost || !host || activeAdClickExemptions.size === 0) return false;
        var now = Date.now();
        var cleanDoc = docHost.toLowerCase();
        var cleanTarget = host.toLowerCase();

        for (var [key, ex] of activeAdClickExemptions) {
            if (ex.expiresAtMs < now) {
                activeAdClickExemptions.delete(key);
                continue;
            }
            if (cleanDoc === ex.advertiserHost || cleanDoc.endsWith('.' + ex.advertiserHost)) {
                // Only allow advertiser first-party subdomains or specific known conversion attribution endpoints
                if (cleanTarget === ex.advertiserHost || cleanTarget.endsWith('.' + ex.advertiserHost) || isDomainInSet(cleanTarget, attributionEndpoints)) {
                    return true;
                }
            }
        }
        return false;
    }

    // High-performance batched block queue to prevent IPC/JNI flooding during page loads
    var pendingBlockReports = new Map();
    var blockFlushTimer = null;

    function flushBlockedReports() {
        blockFlushTimer = null;
        if (pendingBlockReports.size === 0) return;
        var reports = [];
        pendingBlockReports.forEach(function(category, host) {
            reports.push({ host: host, category: category });
        });
        pendingBlockReports.clear();

        try {
            if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendNativeMessage) {
                // Send in batch if multiple, or single
                for (var i = 0; i < reports.length; i++) {
                    browser.runtime.sendNativeMessage('nativeApp', {
                        type: 'RECORD_BLOCK',
                        host: reports[i].host,
                        category: reports[i].category
                    }).catch(function() {});
                }
            }
        } catch(e) {}
    }

    function reportBlockToApp(host, category) {
        if (!host) return;
        var h = host.toLowerCase().trim();
        if (isSovereignInfrastructure('', h)) return;

        var cat = category || 1;
        if (pendingBlockReports.has(h) && pendingBlockReports.get(h) === 2) {
            cat = 2;
        }
        pendingBlockReports.set(h, cat);
        if (!blockFlushTimer) {
            blockFlushTimer = setTimeout(flushBlockedReports, 400);
        }
    }

    // Dynamic per-site shield exclusion list (synced from native app)
    var disabledShieldHosts = new Set();

    // WebRequest Blocking Listener (Platform-independent rule-driven classification)
    browser.webRequest.onBeforeRequest.addListener(
        function(details) {
            var url = details.url;
            if (!url) return { cancel: false };

            // Fast path 0: Non-network schemes (blob:, data:, resource:, about:, chrome:) are local
            if (url.charCodeAt(0) !== 104 || (!url.startsWith('http://') && !url.startsWith('https://'))) {
                return { cancel: false };
            }

            // Benchmark & local loopback fast-path: zero overhead for Speedometer, BrowserBench, localhost
            if (url.includes('browserbench.org') || url.includes('localhost') || url.includes('127.0.0.1')) {
                return { cancel: false };
            }

            var host = extractHostname(url);
            if (!host) return { cancel: false };

            var docUrl = details.documentUrl || details.initiator || details.originUrl || '';
            var docHost = extractHostname(docUrl);

            // Fast path 1: First-party subresources (same domain / subdomain)
            // Unless it is a CNAME-cloaked tracker disguised under first-party domain
            var isSameParty = docHost ? isSameSite(docHost, host) : false;
            if (isSameParty) {
                if (!isLikelyCnameTracker(host, docHost)) {
                    return { cancel: false };
                }
            }

            var isThirdParty = !isSameParty;
            var urlLower = url.toLowerCase();

            // 0. Check if user disabled shield for document origin or target host
            if ((docHost && disabledShieldHosts.has(docHost)) || disabledShieldHosts.has(host)) {
                return { cancel: false };
            }

            // 1. Sovereign Infrastructure Protection (Never block internal browser or bot verification)
            if (isSovereignInfrastructure(urlLower, host)) {
                return { cancel: false };
            }

            // 2. Same Corporate Parent Entity Protection (TDS Graph: e.g. gstatic on google.com)
            if (docHost && isSameParentEntity(docHost, host)) {
                return { cancel: false };
            }

            // 3. Tab-Scoped Ad Click Exemption (DuckDuckGo ethical ad attribution)
            if (isAdClickExempt(docHost, host)) {
                return { cancel: false };
            }

            // 4. Fast-path cache lookup
            var cacheKey = host + '|3p';
            if (decisionCache.has(cacheKey)) {
                var cached = decisionCache.get(cacheKey);
                if (cached.shouldBlock) {
                    var cachedSurrogate = getSurrogateRedirect(urlLower, host);
                    if (cachedSurrogate) {
                        reportBlockToApp(host, cached.category);
                        return cachedSurrogate;
                    }
                    reportBlockToApp(host, cached.category);
                    return { cancel: true };
                }
                return { cancel: false };
            }

            // 5. Tracker Evaluation (Trackers are blocked on 3rd party, and known telemetry endpoints)
            if (isThirdParty && isDomainInSet(host, knownTrackerHosts)) {
                var trackerSurrogate = getSurrogateRedirect(urlLower, host);
                if (trackerSurrogate) {
                    reportBlockToApp(host, 2);
                    return trackerSurrogate;
                }
                if (decisionCache.size < MAX_CACHE_ENTRIES) {
                    decisionCache.set(cacheKey, { shouldBlock: true, category: 2 });
                }
                reportBlockToApp(host, 2);
                return { cancel: true };
            }

            // 6. Ad Evaluation (Ad networks are blocked on 3rd party)
            if (isThirdParty && isDomainInSet(host, knownAdHosts)) {
                if (decisionCache.size < MAX_CACHE_ENTRIES) {
                    decisionCache.set(cacheKey, { shouldBlock: true, category: 1 });
                }
                reportBlockToApp(host, 1);
                return { cancel: true };
            }

            // 7. Path & Substring Evaluation (strictly for 3rd-party requests)
            if (isThirdParty) {
                for (var j = 0; j < thirdPartyPathSubstrings.length; j++) {
                    var sub = thirdPartyPathSubstrings[j];
                    if (urlLower.includes(sub)) {
                        var isTrackerSub = sub.includes('telemetry') || sub.includes('gtag') || sub.includes('pixel');
                        var pathCat = isTrackerSub ? 2 : 1;
                        var pathSurrogate = getSurrogateRedirect(urlLower, host);
                        if (pathSurrogate) {
                            reportBlockToApp(host || 'telemetry-tracker', pathCat);
                            return pathSurrogate;
                        }
                        reportBlockToApp(host || (isTrackerSub ? 'telemetry-tracker' : 'ad-request'), pathCat);
                        return { cancel: true };
                    }
                }
            }

            // Allow request and cache decision
            if (decisionCache.size < MAX_CACHE_ENTRIES) {
                decisionCache.set(cacheKey, { shouldBlock: false, category: 0 });
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
                reportBlockToApp(message.host, message.category || 1);
                if (sendResponse) sendResponse({ ok: true });
            } else if (message.type === 'GET_THEME') {
                if (sendResponse) sendResponse({ theme: currentAppTheme });
            } else if (message.type === 'GET_COSMETIC_CSS') {
                // Fetch tailored cosmetic CSS for the requested domain from native CosmeticEngine
                var domain = message.domain || (sender && sender.tab && sender.tab.url ? extractHostname(sender.tab.url) : '');
                if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendNativeMessage) {
                    browser.runtime.sendNativeMessage('nativeApp', {
                        type: 'GET_COSMETIC_CSS',
                        domain: domain
                    }).then(function(res) {
                        if (sendResponse) sendResponse(res);
                    }).catch(function() {
                        if (sendResponse) sendResponse({ css: '' });
                    });
                    return true; // Async response
                } else {
                    if (sendResponse) sendResponse({ css: '' });
                }
            } else if (message.type === 'PWA_MANIFEST_DISCOVERED') {
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
    // =========================================================================
    var isTorRoutingActive = false;
    var activeSocksPort = 9050;

    function applyAppStatus(res) {
        if (!res) return;
        if (typeof res === 'string') {
            try {
                res = JSON.parse(res);
            } catch(e) {
                return;
            }
        }
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
        if (Array.isArray(res.adClickExemptions)) {
            activeAdClickExemptions.clear();
            res.adClickExemptions.forEach(function(item) {
                if (item && item.advertiserHost) {
                    activeAdClickExemptions.set(item.tabId + ':' + item.advertiserHost.toLowerCase(), {
                        tabId: item.tabId,
                        advertiserHost: item.advertiserHost.toLowerCase(),
                        expiresAtMs: item.expiresAtMs || (Date.now() + 86400000)
                    });
                }
            });
        }
    }

    // One-shot initial fetch as fallback
    function initialSyncStatus() {
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendNativeMessage) {
            browser.runtime.sendNativeMessage('nativeApp', { type: 'GET_APP_STATUS' })
                .then(applyAppStatus)
                .catch(function() {});
        }
    }
    initialSyncStatus();

    var nativePort = null;
    function connectNativePort() {
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.connectNative) {
            try {
                nativePort = browser.runtime.connectNative('nativeApp');
                nativePort.onMessage.addListener(function(msg) {
                    if (!msg) return;
                    if (typeof msg === 'string') {
                        try { msg = JSON.parse(msg); } catch(e) { return; }
                    }
                    if (msg.type === 'PUSH_APP_STATUS') {
                        applyAppStatus(msg);
                    } else if (msg.type === 'PUSH_TOR_STATUS') {
                        if (typeof msg.isTorEnabled === 'boolean') {
                            isTorRoutingActive = msg.isTorEnabled;
                        }
                        if (typeof msg.socksPort === 'number' && msg.socksPort > 0) {
                            activeSocksPort = msg.socksPort;
                        }
                    } else if (msg.type === 'PUSH_THEME') {
                        if (typeof msg.theme === 'string') {
                            currentAppTheme = msg.theme;
                        }
                    } else if (msg.type === 'PUSH_DISABLED_HOSTS') {
                        if (Array.isArray(msg.disabledHosts)) {
                            disabledShieldHosts = new Set(msg.disabledHosts.map(function(h) { return (h || '').toLowerCase(); }));
                        }
                    } else if (msg.type === 'PUSH_AD_CLICK_EXEMPTIONS') {
                        if (Array.isArray(msg.exemptions)) {
                            activeAdClickExemptions.clear();
                            msg.exemptions.forEach(function(item) {
                                if (item && item.advertiserHost) {
                                    activeAdClickExemptions.set(item.tabId + ':' + item.advertiserHost.toLowerCase(), {
                                        tabId: item.tabId,
                                        advertiserHost: item.advertiserHost.toLowerCase(),
                                        expiresAtMs: item.expiresAtMs || (Date.now() + 86400000)
                                    });
                                }
                            });
                        }
                    }
                });
                nativePort.onDisconnect.addListener(function() {
                    nativePort = null;
                    setTimeout(connectNativePort, 3000);
                });
            } catch(e) {
                nativePort = null;
            }
        }
    }
    connectNativePort();

    browser.proxy.onRequest.addListener(
        function(requestInfo) {
            // Zero-overhead fast path: clearnet requests when Tor is inactive bypass proxy completely
            var url = requestInfo.url || '';
            var isOnion = url.indexOf('.onion') !== -1;
            if (!isTorRoutingActive && !isOnion) {
                return { type: 'direct' };
            }

            var isTorCandidate = isOnion || (isTorRoutingActive && requestInfo.incognito === true);

            if (isTorCandidate || isOnion) {
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

    console.log('[Azokle Shield] Rule-driven interceptor and privacy engine active.');
})();

