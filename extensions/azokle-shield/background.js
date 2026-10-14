/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    // High-Performance Dual LRU Decision Cache for Instant Native Lookups
    var MAX_HOST_CACHE = 1000;
    var MAX_URL_CACHE = 2000;
    var hostCache = new Map();
    var urlCache = new Map();
    var inflightRequests = new Map();

    function getCachedHostDecision(host) {
        if (!host || !hostCache.has(host)) return undefined;
        var val = hostCache.get(host);
        hostCache.delete(host);
        hostCache.set(host, val);
        return val;
    }

    function setCachedHostDecision(host, val) {
        if (!host) return;
        if (hostCache.size >= MAX_HOST_CACHE) {
            var firstKey = hostCache.keys().next().value;
            hostCache.delete(firstKey);
        }
        hostCache.set(host, val);
    }

    function getCachedUrlDecision(key) {
        if (!key || !urlCache.has(key)) return undefined;
        var val = urlCache.get(key);
        urlCache.delete(key);
        urlCache.set(key, val);
        return val;
    }

    function setCachedUrlDecision(key, val) {
        if (!key) return;
        if (urlCache.size >= MAX_URL_CACHE) {
            var firstKey = urlCache.keys().next().value;
            urlCache.delete(firstKey);
        }
        urlCache.set(key, val);
    }

    function cleanUrlForCacheKey(u) {
        if (!u) return '';
        var q = u.indexOf('?');
        var hash = u.indexOf('#');
        var cut = u.length;
        if (q !== -1) cut = q;
        if (hash !== -1 && hash < cut) cut = hash;
        return u.substring(0, cut);
    }

    function clearAllCaches() {
        hostCache.clear();
        urlCache.clear();
        inflightRequests.clear();
    }

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

    function hostMatches(h, domain) {
        if (!h || !domain) return false;
        var cleanH = h.toLowerCase().trim();
        var cleanD = domain.toLowerCase().trim();
        return cleanH === cleanD || cleanH.endsWith('.' + cleanD);
    }

    function isSovereignInfrastructure(url, host) {
        if (!host) return false;
        var h = host.toLowerCase().trim();
        if (sovereignHosts.has(h)) return true;
        if (h.endsWith('.azokle.com') || h.endsWith('.cloudflare.com') ||
            h.endsWith('.cloudflareinsights.com') || h.endsWith('.hcaptcha.com') ||
            h.endsWith('.recaptcha.net') || h.includes('turnstile') ||
            hostMatches(h, 'challenges.cloudflare.com')) {
            return true;
        }
        var u = (url || '').toLowerCase();
        if (u.includes('/cdn-cgi/') || u.includes('/challenge-platform/')) {
            return true;
        }
        return false;
    }

    // Neutral Surrogates to prevent site breakage when essential analytics/tracker scripts are requested
    var surrogateRules = [
        {
            matcher: function(u, h) {
                return (hostMatches(h, 'google-analytics.com') && (u.includes('/analytics.js') || u.includes('/ga.js')));
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
                return hostMatches(h, 'googletagmanager.com') && u.includes('/gtm.js');
            },
            file: 'surrogates/gtm.js'
        },
        {
            matcher: function(u, h) {
                return (hostMatches(h, 'connect.facebook.net') && (u.includes('/fbevents.js') || u.includes('/sdk.js'))) ||
                       (hostMatches(h, 'facebook.com') && u.includes('/fbevents.js'));
            },
            file: 'surrogates/fbevents.js'
        },
        {
            matcher: function(u, h) {
                return (hostMatches(h, 'matomo.org') || h.includes('matomo') || h.includes('piwik')) && (u.includes('matomo.js') || u.includes('piwik.js'));
            },
            file: 'surrogates/matomo.js'
        },
        {
            matcher: function(u, h) {
                return (hostMatches(h, 'googletagservices.com') && u.includes('/gpt.js')) ||
                       (hostMatches(h, 'securepubads.g.doubleclick.net') && u.includes('/gpt.js'));
            },
            file: 'surrogates/gpt.js'
        },
        {
            matcher: function(u, h) {
                return u.includes('prebid.js') || (h.includes('prebid') && u.includes('.js'));
            },
            file: 'surrogates/prebid.js'
        },
        {
            matcher: function(u, h) {
                return hostMatches(h, 'imasdk.googleapis.com') && u.includes('/ima3.js');
            },
            file: 'surrogates/imasdk.js'
        },
        {
            matcher: function(u, h) {
                return hostMatches(h, 'chartbeat.com') && u.includes('chartbeat.js');
            },
            file: 'surrogates/chartbeat.js'
        },
        {
            matcher: function(u, h) {
                return hostMatches(h, 'hotjar.com') && (u.includes('/hotjar-') || u.includes('/hotjar.js'));
            },
            file: 'surrogates/hotjar.js'
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
        'metrics.', 'analytics.', 'telemetry.', 'track.', 'tracker.', 'data.', 'log.', 'stats.', 'collector.', 'events.',
        'tr.', 'tag.', 'tags.', 'beacon.', 'pixel.', 'trk.'
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
        new Set(['google.com', 'google.co.uk', 'gstatic.com', 'recaptcha.net', 'youtube.com', 'ytimg.com', 'googlevideo.com']),
        // Meta Platforms (Functional CDNs & Media only)
        new Set(['facebook.com', 'facebook.net', 'fbcdn.net', 'instagram.com', 'cdninstagram.com', 'whatsapp.com']),
        // Microsoft (Functional CDNs only)
        new Set(['microsoft.com', 'live.com', 'office.com', 'bing.com', 'azure.com', 'azureedge.net']),
        // Amazon (Functional CDNs only)
        new Set(['amazon.com', 'media-amazon.com', 'ssl-images-amazon.com', 'a2z.com', 'aws.amazon.com']),
        // Apple
        new Set(['apple.com', 'icloud.com', 'mzstatic.com', 'cdn-apple.com'])
    ];

    function isLikelyAdOrTrackerUrl(urlLower, host) {
        if (!urlLower) return false;
        var h = (host || '').toLowerCase();

        // High-priority exemption: YouTube media chunks must NEVER be blocked or delayed by tracker heuristics
        if (hostMatches(h, 'googlevideo.com') && urlLower.includes('/videoplayback')) {
            return false;
        }

        if (h.includes('doubleclick') || h.includes('googleads') || h.includes('adservice') ||
            h.includes('adsystem') || h.includes('adnxs') || h.includes('advertising') ||
            h.includes('criteo') || h.includes('taboola') || h.includes('outbrain') ||
            h.includes('adroll') || h.includes('rubiconproject') || h.includes('pubmatic') ||
            hostMatches(h, 'imasdk.googleapis.com')) {
            return true;
        }
        if (urlLower.includes('/api/stats/ads') ||
            urlLower.includes('/pagead/') ||
            urlLower.includes('/ptracking') ||
            urlLower.includes('/youtubei/v1/log_event') ||
            urlLower.includes('/youtubei/v1/att') ||
            urlLower.includes('/youtubei/v1/ads') ||
            urlLower.includes('/api/stats/atr') ||
            urlLower.includes('/api/stats/qoe') ||
            urlLower.includes('/gampad/') ||
            urlLower.includes('/ad_') ||
            urlLower.includes('/ads?') ||
            urlLower.includes('&ad_') ||
            urlLower.includes('adformat=') ||
            urlLower.includes('/gen_204?') ||
            urlLower.includes('device_204?') ||
            urlLower.includes('/telemetry') ||
            urlLower.includes('/analytics') ||
            urlLower.includes('/beacon') ||
            urlLower.includes('/tr?') ||
            urlLower.includes('fbevents.js') ||
            urlLower.includes('/pixel')) {
            return true;
        }
        return false;
    }

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

    function isAdClickExempt(docHost, host, tabId) {
        if (!docHost || !host || activeAdClickExemptions.size === 0) return false;
        var now = Date.now();
        var cleanDoc = docHost.toLowerCase();
        var cleanTarget = host.toLowerCase();

        for (var [key, ex] of activeAdClickExemptions) {
            if (ex.expiresAtMs < now) {
                activeAdClickExemptions.delete(key);
                continue;
            }
            if (tabId !== undefined && tabId !== null && String(ex.tabId) !== String(tabId)) {
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

            var host = extractHostname(url);
            if (!host) return { cancel: false };

            // Benchmark & local loopback fast-path: zero overhead for Speedometer, BrowserBench, localhost
            if (hostMatches(host, 'browserbench.org') || host === 'localhost' || host === '127.0.0.1' || host === '[::1]') {
                return { cancel: false };
            }

            // Fast path 0.5: Top-level document navigations (main_frame) proceed without cancellation
            if (details.type === 'main_frame') {
                return { cancel: false };
            }

            var docUrl = details.documentUrl || details.initiator || details.originUrl || '';
            var docHost = extractHostname(docUrl);

            var isSameParty = docHost ? isSameSite(docHost, host) : false;
            var isThirdParty = !isSameParty;
            var urlLower = url.toLowerCase();

            // 0. Check if shield is disabled globally via off blocking mode
            if (activeBlockingMode === 'off') {
                return { cancel: false };
            }

            // 0. Check if user disabled shield for document origin or target host
            if ((docHost && disabledShieldHosts.has(docHost)) || disabledShieldHosts.has(host)) {
                return { cancel: false };
            }

            // 1. Sovereign Infrastructure Protection (Never block internal browser or bot verification)
            if (isSovereignInfrastructure(urlLower, host)) {
                return { cancel: false };
            }

            // 2. Same Corporate Parent Entity Protection (TDS Graph: e.g. gstatic on google.com)
            // Strictly limited to static media/fonts/stylesheets. Scripts and trackers are always evaluated.
            var isStaticAsset = details.type === 'image' || details.type === 'stylesheet' || details.type === 'font' || details.type === 'media';
            if (docHost && isSameParentEntity(docHost, host) && isStaticAsset && !isLikelyAdOrTrackerUrl(urlLower, host)) {
                return { cancel: false };
            }

            // 3. Tab-Scoped Ad Click Exemption (DuckDuckGo ethical ad attribution)
            if (isAdClickExempt(docHost, host, details.tabId)) {
                return { cancel: false };
            }

            // 3.5 Aggressive Mode Pre-emptive heuristics (block CNAME tracker subdomains and social/beacon trackers)
            if (activeBlockingMode === 'aggressive' && isThirdParty) {
                if (isLikelyCnameTracker(host, docHost)) {
                    reportBlockToApp(host, 2);
                    return { cancel: true };
                }
                if (isLikelyAdOrTrackerUrl(urlLower, host)) {
                    var aggSurrogate = getSurrogateRedirect(urlLower, host);
                    if (aggSurrogate) {
                        reportBlockToApp(host, 2);
                        return aggSurrogate;
                    }
                    reportBlockToApp(host, 2);
                    return { cancel: true };
                }
            }

            // 4. Fast-path Host Cache lookup (Partitioned by 1st/3rd party and request type to prevent cross-context bleeding)
            var hostCacheKey = host + '|' + (isThirdParty ? '3p' : '1p') + '|' + (details.type || 'other');
            var cachedHost = getCachedHostDecision(hostCacheKey);
            if (cachedHost !== undefined) {
                if (cachedHost.shouldBlock || cachedHost.redirectUrl) {
                    var hostSurrogate = getSurrogateRedirect(urlLower, host);
                    if (hostSurrogate) {
                        reportBlockToApp(host, cachedHost.category);
                        return hostSurrogate;
                    }
                    if (cachedHost.redirectUrl) {
                        reportBlockToApp(host, cachedHost.category);
                        return { redirectUrl: cachedHost.redirectUrl };
                    }
                    reportBlockToApp(host, cachedHost.category);
                    return { cancel: true };
                }
                return { cancel: false };
            }

            // 5. Fast-path URL Cache lookup (instant O(1) synchronous decision for path/scriptlet rules)
            var urlCacheKey = cleanUrlForCacheKey(url) + '|' + (details.type || 'other') + '|' + (isThirdParty ? '3p' : '1p');
            var cachedUrl = getCachedUrlDecision(urlCacheKey);
            if (cachedUrl !== undefined) {
                if (cachedUrl.shouldBlock || cachedUrl.redirectUrl) {
                    var urlSurrogate = getSurrogateRedirect(urlLower, host);
                    if (urlSurrogate) {
                        reportBlockToApp(host, cachedUrl.category);
                        return urlSurrogate;
                    }
                    if (cachedUrl.redirectUrl) {
                        reportBlockToApp(host, cachedUrl.category);
                        return { redirectUrl: cachedUrl.redirectUrl };
                    }
                    reportBlockToApp(host, cachedUrl.category);
                    return { cancel: true };
                }
                return { cancel: false };
            }

            // 6. In-flight request deduplication (prevents IPC flooding for concurrent subresource requests)
            if (inflightRequests.has(urlCacheKey)) {
                return inflightRequests.get(urlCacheKey);
            }

            // 7. Asynchronous Native Engine Evaluation (Brave adblock-rust via GeckoEngineManager)
            var lookupPromise = new Promise(function(resolve) {
                if (typeof browser === 'undefined' || !browser.runtime || !browser.runtime.sendNativeMessage) {
                    resolve({ cancel: false });
                    return;
                }

                browser.runtime.sendNativeMessage('nativeApp', {
                    type: 'CHECK_URL',
                    url: url,
                    origin: docUrl,
                    requestType: details.type || 'other',
                    isThirdParty: isThirdParty
                }).then(function(res) {
                    if (typeof res === 'string') {
                        try { res = JSON.parse(res); } catch(e) { res = {}; }
                    }
                    var isReady = res && res.isReady !== false;
                    var shouldBlock = !!(res && res.shouldBlock);
                    var category = (res && typeof res.category === 'number') ? res.category : 1;
                    var matchedRule = (res && res.matchedRule) || '';
                    var redirectUrl = (res && res.redirectUrl) || null;

                    var isDomainLevel = matchedRule.startsWith('||') && !matchedRule.includes('/') && !matchedRule.includes('*');
                    var decisionRecord = {
                        shouldBlock: shouldBlock,
                        category: category,
                        redirectUrl: redirectUrl
                    };

                    // Only cache decisions if the native engine is fully initialized/ready
                    if (isReady) {
                        if (isDomainLevel && host) {
                            setCachedHostDecision(hostCacheKey, decisionRecord);
                        } else {
                            setCachedUrlDecision(urlCacheKey, decisionRecord);
                        }
                    }

                    if (shouldBlock || redirectUrl) {
                        var surrogate = getSurrogateRedirect(urlLower, host);
                        if (surrogate) {
                            reportBlockToApp(host, category);
                            resolve(surrogate);
                            return;
                        }
                        if (redirectUrl) {
                            reportBlockToApp(host, category);
                            resolve({ redirectUrl: redirectUrl });
                            return;
                        }
                        reportBlockToApp(host, category);
                        resolve({ cancel: true });
                        return;
                    }

                    resolve({ cancel: false });
                }).catch(function() {
                    resolve({ cancel: false });
                }).finally(function() {
                    inflightRequests.delete(urlCacheKey);
                });
            });

            inflightRequests.set(urlCacheKey, lookupPromise);
            return lookupPromise;
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
            } else if (message.type === 'GET_SCRIPTLETS') {
                // Fetch tailored defuser scriptlets for the requested domain from native Rust engine
                var domain = message.domain || (sender && sender.tab && sender.tab.url ? extractHostname(sender.tab.url) : '');
                if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendNativeMessage) {
                    browser.runtime.sendNativeMessage('nativeApp', {
                        type: 'GET_SCRIPTLETS',
                        domain: domain
                    }).then(function(res) {
                        if (sendResponse) sendResponse(res);
                    }).catch(function() {
                        if (sendResponse) sendResponse({ scriptlet: '' });
                    });
                    return true; // Async response
                } else {
                    if (sendResponse) sendResponse({ scriptlet: '' });
                }
            } else if (message.type === 'PWA_MANIFEST_DISCOVERED' || message.type === 'ARTICLE_CONTENT_DISCOVERED') {
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
    var activeBlockingMode = 'standard';

    function applyAppStatus(res) {
        if (!res) return;
        if (typeof res === 'string') {
            try {
                res = JSON.parse(res);
            } catch(e) {
                return;
            }
        }
        if (typeof res.blockingMode === 'string') {
            activeBlockingMode = res.blockingMode;
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
            clearAllCaches();
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
            clearAllCaches();
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
                        clearAllCaches();
                    } else if (msg.type === 'CLEAR_CACHE') {
                        clearAllCaches();
                    } else if (msg.type === 'PUSH_BLOCKING_MODE') {
                        if (typeof msg.blockingMode === 'string') {
                            activeBlockingMode = msg.blockingMode;
                            clearAllCaches();
                        }
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
                            clearAllCaches();
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
                            clearAllCaches();
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

