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
        'chitika.net', 'infolinks.com', 'skimmr.com', 'adroll.com', 'dianomi.com', 'trafficroots.com'
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
        'coinhive.com', 'coin-hive.com', 'crypto-loot.com', 'minr.pw', 'webminepool.com'
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
        try {
            var a = new URL(url);
            return a.hostname.toLowerCase();
        } catch(e) {
            return '';
        }
    }

    function isSameSite(hostA, hostB) {
        if (!hostA || !hostB) return false;
        if (hostA === hostB) return true;
        var pA = hostA.split('.');
        var pB = hostB.split('.');
        if (pA.length >= 2 && pB.length >= 2) {
            var rootA = pA.slice(-2).join('.');
            var rootB = pB.slice(-2).join('.');
            if (rootA === rootB) return true;
        }
        return false;
    }

    function isDomainInSet(host, set) {
        if (!host) return false;
        if (set.has(host)) return true;
        var parts = host.split('.');
        if (parts.length > 2) {
            for (var i = 1; i < parts.length - 1; i++) {
                var sub = parts.slice(i).join('.');
                if (set.has(sub)) return true;
            }
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

    function reportBlockToApp(host, category) {
        if (!host) return;
        var h = host.toLowerCase().trim();
        if (isSovereignInfrastructure('', h)) return;
        try {
            browser.runtime.sendNativeMessage('nativeApp', {
                type: 'RECORD_BLOCK',
                host: h,
                category: category || 1
            }).catch(function() {});
        } catch(e) {}
    }

    // Dynamic per-site shield exclusion list (synced from native app)
    var disabledShieldHosts = new Set();

    // WebRequest Blocking Listener (Platform-independent rule-driven classification)
    browser.webRequest.onBeforeRequest.addListener(
        function(details) {
            var url = details.url;
            var host = extractHostname(url);
            var urlLower = url.toLowerCase();

            // 0. Periodic status sync if stale
            if (Date.now() - lastSyncTimestamp > 500) {
                syncNativeAppStatus();
            }

            // 0.1 Check if user disabled shield for document origin or target host
            var docUrl = details.documentUrl || details.initiator || details.originUrl || '';
            var docHost = extractHostname(docUrl);
            if ((docHost && disabledShieldHosts.has(docHost)) || (host && disabledShieldHosts.has(host))) {
                return { cancel: false };
            }

            // 1. Sovereign Infrastructure Protection (Never block internal browser or bot verification)
            if (isSovereignInfrastructure(urlLower, host)) {
                return { cancel: false };
            }

            // 2. Classify party context (First-party vs Third-party)
            var isThirdParty = !docHost || !isSameSite(docHost, host);

            // 3. Fast-path cache lookup
            var cacheKey = host + '|' + (isThirdParty ? '3p' : '1p');
            if (decisionCache.has(cacheKey)) {
                var cached = decisionCache.get(cacheKey);
                if (cached.shouldBlock) {
                    reportBlockToApp(host, cached.category);
                    return { cancel: true };
                }
                return { cancel: false };
            }

            // 4. Tracker Evaluation (Trackers are blocked on 3rd party, and known telemetry endpoints)
            if (isThirdParty && isDomainInSet(host, knownTrackerHosts)) {
                if (decisionCache.size < MAX_CACHE_ENTRIES) {
                    decisionCache.set(cacheKey, { shouldBlock: true, category: 2 });
                }
                reportBlockToApp(host, 2);
                return { cancel: true };
            }

            // 5. Ad Evaluation (Ad networks are blocked on 3rd party)
            if (isThirdParty && isDomainInSet(host, knownAdHosts)) {
                if (decisionCache.size < MAX_CACHE_ENTRIES) {
                    decisionCache.set(cacheKey, { shouldBlock: true, category: 1 });
                }
                reportBlockToApp(host, 1);
                return { cancel: true };
            }

            // 6. Path & Substring Evaluation (strictly for 3rd-party requests)
            if (isThirdParty) {
                for (var j = 0; j < thirdPartyPathSubstrings.length; j++) {
                    if (urlLower.includes(thirdPartyPathSubstrings[j])) {
                        reportBlockToApp(host || 'ad-request', 1);
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
                .catch(function() {});
        }
    }

    syncNativeAppStatus();
    setInterval(syncNativeAppStatus, 1500);

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
                    if (msg.type === 'PUSH_TOR_STATUS') {
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

    if (typeof browser !== 'undefined' && browser.webNavigation && browser.webNavigation.onBeforeNavigate) {
        browser.webNavigation.onBeforeNavigate.addListener(function() {
            if (Date.now() - lastSyncTimestamp > 500) {
                syncNativeAppStatus();
            }
        });
    }

    browser.proxy.onRequest.addListener(
        function(requestInfo) {
            var url = requestInfo.url || '';
            var isOnion = url.indexOf('.onion') !== -1;
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

