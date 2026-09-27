// Azokle Shield Sovereign Defuser Scriptlets
// Defuses anti-adblock walls, popup ad loops, and YouTube/streaming video ads cleanly without breaking page layout
// Operates cooperatively across WebExtension Sandbox and Page MAIN Worlds

(function() {
    'use strict';

    if (window.__azokleDefuserLoaded) return;
    window.__azokleDefuserLoaded = true;

    // ============================================================================
    // STEP 1: Main-World Scriptlet Injection
    // Ensures fetch/XHR interceptors and ytInitialPlayerResponse hooks run directly
    // inside the webpage's MAIN execution realm, overcoming WebExtension sandbox isolation.
    // ============================================================================
    function injectScriptletIntoPage(fn) {
        var code = '(' + fn.toString() + ')();';
        var script = document.createElement('script');
        script.textContent = code;

        // Copy CSP nonce from existing page script if present
        var nonceHolder = document.querySelector('script[nonce]');
        if (nonceHolder) {
            var n = nonceHolder.nonce || nonceHolder.getAttribute('nonce');
            if (n) script.setAttribute('nonce', n);
        }

        var target = document.head || document.documentElement;
        if (target) {
            try {
                target.appendChild(script);
                script.remove();
            } catch(e) {
                fallbackScriptInjection();
            }
        } else {
            var observer = new MutationObserver(function() {
                var t = document.head || document.documentElement;
                if (t) {
                    observer.disconnect();
                    try {
                        t.appendChild(script);
                        script.remove();
                    } catch(e) {
                        fallbackScriptInjection();
                    }
                }
            });
            observer.observe(document, { childList: true, subtree: true });
        }
    }

    function fallbackScriptInjection() {
        try {
            if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.getURL) {
                var extScript = document.createElement('script');
                extScript.async = false;
                extScript.src = browser.runtime.getURL('scriptlets/defuser_main.js');
                (document.head || document.documentElement || document).appendChild(extScript);
                extScript.remove();
            }
        } catch(e) {}
    }

    // Main World Defuser Payload (executed directly in page context)
    function pageWorldPayload() {
        'use strict';
        if (window.__azokle_main_world_initialized) return;
        window.__azokle_main_world_initialized = true;

        // 0. Native Function Stub Engine (prevents anti-adblock detection via Function.prototype.toString)
        try {
            var nativeToString = Function.prototype.toString;
            var customNativeFns = new WeakSet();
            var makeNativeFn = function(fn, name) {
                if (typeof fn !== 'function') return fn;
                customNativeFns.add(fn);
                if (name) {
                    try {
                        Object.defineProperty(fn, 'name', { value: name, configurable: true });
                    } catch(e) {}
                }
                return fn;
            };

            var patchedToString = function toString() {
                if (customNativeFns.has(this)) {
                    return 'function ' + (this.name || '') + '() { [native code] }';
                }
                return nativeToString.apply(this, arguments);
            };
            customNativeFns.add(patchedToString);
            Function.prototype.toString = patchedToString;
        } catch(e) {
            var makeNativeFn = function(fn) { return fn; };
        }

        // 1. Defuse anti-adblock honeypots & bait variables
        try {
            var dummyFn = makeNativeFn(function() {}, '');

            // Ad Status & Ad Blocker Detection Traps
            window.canRunAds = true;
            window.isAdBlockActive = false;
            window.adsBlocked = false;
            window.adblocker = false;
            window._adblock = false;
            window.__adblocker = false;
            window.showAds = true;
            window.ads_blocked = false;
            window.adBlockDetected = false;
            window.hasAdblocker = false;
            window.AdBlockEnabled = false;
            window.google_ad_status = 1;
            if (!window.google_ad_client) window.google_ad_client = 'ca-pub-0000000000000000';
            if (!window.google_ad_modifications) window.google_ad_modifications = { eids: [] };

            // Admiral & Publisher Firewall Defusers
            window.admiral = makeNativeFn(function() {}, 'admiral');
            window.admiral.loaded = true;
            window.admiralLoaded = true;

            // BlockAdBlock / FuckAdBlock / SnackAdBlock Universal Polyfill
            var babStub = function() {
                return {
                    check: dummyFn,
                    clearEvent: dummyFn,
                    on: function(isDetected, fn) {
                        if (!isDetected && typeof fn === 'function') {
                            try { fn(); } catch(e) {}
                        }
                        return this;
                    },
                    onDetected: dummyFn,
                    onNotDetected: function(fn) {
                        if (typeof fn === 'function') {
                            try { fn(); } catch(e) {}
                        }
                        return this;
                    },
                    setOption: function() { return this; }
                };
            };
            makeNativeFn(babStub, 'BlockAdBlock');
            if (typeof window.BlockAdBlock === 'undefined') {
                window.BlockAdBlock = babStub;
                window.blockAdBlock = new babStub();
            }
            if (typeof window.FuckAdBlock === 'undefined') {
                window.FuckAdBlock = babStub;
                window.fuckAdBlock = new babStub();
            }
            if (typeof window.SnackAdBlock === 'undefined') {
                window.SnackAdBlock = babStub;
                window.snackAdBlock = new babStub();
            }

            // Global Helper: abort-on-property-read & set-constant
            window.__azokle_abort_on_property = function(prop) {
                try {
                    Object.defineProperty(window, prop, {
                        get: function() { throw new ReferenceError(prop + ' is not accessible'); },
                        set: function() {},
                        configurable: true
                    });
                } catch(e) {}
            };

            window.__azokle_set_constant = function(prop, val) {
                try {
                    Object.defineProperty(window, prop, {
                        value: val,
                        writable: false,
                        configurable: true
                    });
                } catch(e) {}
            };
        } catch(e) {}

        // 2. YouTube Ad Annihilation (Brave-Core style response sanitizer)
        try {
            var host = location.hostname;
            var isYouTube = host === 'youtube.com' ||
                            host.endsWith('.youtube.com') ||
                            host === 'youtube-nocookie.com' ||
                            host.endsWith('.youtube-nocookie.com');

            if (isYouTube) {
                // Background & Lockscreen Playback: Spoof Page Visibility API in Main Execution Realm
                try {
                    Object.defineProperty(document, 'hidden', {
                        get: function() { return false; },
                        configurable: true
                    });
                    Object.defineProperty(document, 'visibilityState', {
                        get: function() { return 'visible'; },
                        configurable: true
                    });
                    Object.defineProperty(document, 'webkitVisibilityState', {
                        get: function() { return 'visible'; },
                        configurable: true
                    });
                    Object.defineProperty(document, 'webkitHidden', {
                        get: function() { return false; },
                        configurable: true
                    });
                    document.hasFocus = function() { return true; };
                } catch(e) {}

                // Intercept & neutralize visibility/lifecycle pause events in capture phase in Main Realm
                var suppressVisibilityEvent = function(e) {
                    if (e) {
                        e.stopImmediatePropagation();
                    }
                };
                document.addEventListener('visibilitychange', suppressVisibilityEvent, true);
                window.addEventListener('visibilitychange', suppressVisibilityEvent, true);
                document.addEventListener('webkitvisibilitychange', suppressVisibilityEvent, true);
                window.addEventListener('webkitvisibilitychange', suppressVisibilityEvent, true);
                window.addEventListener('pagehide', suppressVisibilityEvent, true);

                var sanitizePlayerResponse = function(obj) {
                    if (!obj || typeof obj !== 'object') return obj;
                    try {
                        // Top-level ad placements & breaks
                        if ('playerAds' in obj) delete obj.playerAds;
                        if ('adPlacements' in obj) obj.adPlacements = [];
                        if ('adSlots' in obj) obj.adSlots = [];
                        if ('adBreakResponse' in obj) delete obj.adBreakResponse;
                        if ('adBreakHeartbeatParams' in obj) delete obj.adBreakHeartbeatParams;
                        if ('adSignalsInfo' in obj) delete obj.adSignalsInfo;
                        if ('interstitials' in obj) delete obj.interstitials;

                        // Nested playerResponse wrappers
                        if (obj.playerResponse && typeof obj.playerResponse === 'object') {
                            sanitizePlayerResponse(obj.playerResponse);
                        }
                        if (obj.reelWatchSequenceResponse && typeof obj.reelWatchSequenceResponse === 'object') {
                            if ('adPlacements' in obj.reelWatchSequenceResponse) delete obj.reelWatchSequenceResponse.adPlacements;
                            if ('adSlots' in obj.reelWatchSequenceResponse) delete obj.reelWatchSequenceResponse.adSlots;
                        }

                        // Enforce pristine playability status (zero adblock blocker error screens)
                        if (obj.playabilityStatus && typeof obj.playabilityStatus === 'object') {
                            if (obj.playabilityStatus.status && obj.playabilityStatus.status !== 'OK') {
                                obj.playabilityStatus.status = 'OK';
                            }
                            if ('reason' in obj.playabilityStatus) delete obj.playabilityStatus.reason;
                            if ('errorScreen' in obj.playabilityStatus) delete obj.playabilityStatus.errorScreen;
                            if ('messages' in obj.playabilityStatus) delete obj.playabilityStatus.messages;
                        }

                        // Strip upsell / anti-adblock dialog renderers
                        if (obj.auxiliaryUi && typeof obj.auxiliaryUi === 'object') {
                            if (obj.auxiliaryUi.messageRenderers && obj.auxiliaryUi.messageRenderers.upsellDialogRenderer) {
                                delete obj.auxiliaryUi.messageRenderers.upsellDialogRenderer;
                            }
                        }

                        // Strip tracking beacon URLs that trigger ad-telemetry loops
                        if (obj.playbackTracking && typeof obj.playbackTracking === 'object') {
                            if (obj.playbackTracking.atrUrl) delete obj.playbackTracking.atrUrl;
                            if (obj.playbackTracking.videostatsPlaybackUrl) delete obj.playbackTracking.videostatsPlaybackUrl;
                            if (obj.playbackTracking.qoeUrl) delete obj.playbackTracking.qoeUrl;
                        }
                    } catch(e) {}
                    return obj;
                };

                // Hook window.ytcfg to inject Brave-grade ad-disabling experiment flags
                try {
                    var overrideYtcfg = function(cfg) {
                        if (!cfg || typeof cfg !== 'object') return cfg;
                        try {
                            var exp = cfg.EXPERIMENT_FLAGS || {};
                            exp.disable_ad_video_renderers = true;
                            exp.html5_disable_ad_signals = true;
                            exp.web_player_enable_ad_insertion = false;
                            exp.web_enable_ab_in_ad_layout = false;
                            exp.desktop_ad_block_support = false;
                            cfg.EXPERIMENT_FLAGS = exp;
                        } catch(e) {}
                        return cfg;
                    };

                    if (window.ytcfg) {
                        if (typeof window.ytcfg.set === 'function') {
                            var origYtcfgSet = window.ytcfg.set;
                            window.ytcfg.set = makeNativeFn(function(k, v) {
                                if (typeof k === 'object') {
                                    overrideYtcfg(k);
                                } else if (k === 'EXPERIMENT_FLAGS' && typeof v === 'object') {
                                    overrideYtcfg({ EXPERIMENT_FLAGS: v });
                                }
                                return origYtcfgSet.apply(this, arguments);
                            }, 'set');
                        }
                        if (window.ytcfg.data_) {
                            overrideYtcfg(window.ytcfg.data_);
                        }
                    }
                } catch(e) {}

                // Hook window.ytInitialPlayerResponse
                var _ytInitialPlayerResponse = window.ytInitialPlayerResponse;
                if (_ytInitialPlayerResponse) {
                    sanitizePlayerResponse(_ytInitialPlayerResponse);
                }
                try {
                    Object.defineProperty(window, 'ytInitialPlayerResponse', {
                        get: function() { return _ytInitialPlayerResponse; },
                        set: function(val) {
                            _ytInitialPlayerResponse = sanitizePlayerResponse(val);
                        },
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) {}

                // Hook window.ytInitialData
                var _ytInitialData = window.ytInitialData;
                if (_ytInitialData) {
                    sanitizePlayerResponse(_ytInitialData);
                }
                try {
                    Object.defineProperty(window, 'ytInitialData', {
                        get: function() { return _ytInitialData; },
                        set: function(val) {
                            _ytInitialData = sanitizePlayerResponse(val);
                        },
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) {}

                // Intercept window.fetch in MAIN world
                if (window.fetch) {
                    var origFetch = window.fetch;
                    window.fetch = makeNativeFn(function() {
                        var args = arguments;
                        var url = (typeof args[0] === 'string') ? args[0] : (args[0] && args[0].url ? args[0].url : '');
                        var isPlayerApi = url && (
                            url.indexOf('/youtubei/v1/player') !== -1 ||
                            url.indexOf('/player?') !== -1 ||
                            url.indexOf('/reel_watch_sequence') !== -1 ||
                            url.indexOf('/reel_item_watch') !== -1
                        );

                        if (!isPlayerApi) {
                            return origFetch.apply(this, args);
                        }

                        return origFetch.apply(this, args).then(function(response) {
                            if (!response || !response.ok) return response;
                            var cloned = response.clone();
                            return cloned.text().then(function(text) {
                                try {
                                    var data = JSON.parse(text);
                                    sanitizePlayerResponse(data);
                                    var sanitizedText = JSON.stringify(data);

                                    // Crucial for instant zero-latency loading:
                                    // Remove content-encoding and content-length so browser does not
                                    // attempt decompression or wait on stale byte lengths.
                                    var newHeaders = new Headers(response.headers);
                                    newHeaders.delete('content-encoding');
                                    newHeaders.delete('content-length');
                                    newHeaders.set('content-type', 'application/json; charset=utf-8');

                                    var newResponse = new Response(sanitizedText, {
                                        status: response.status,
                                        statusText: response.statusText,
                                        headers: newHeaders
                                    });
                                    Object.defineProperties(newResponse, {
                                        ok: { value: response.ok },
                                        redirected: { value: response.redirected },
                                        type: { value: response.type },
                                        url: { value: response.url }
                                    });
                                    return newResponse;
                                } catch(parseErr) {
                                    return response;
                                }
                            }).catch(function() {
                                return response;
                            });
                        });
                    }, 'fetch');
                }

                // Intercept window.XMLHttpRequest in MAIN world
                if (window.XMLHttpRequest) {
                    var origOpen = XMLHttpRequest.prototype.open;
                    var origSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.open = makeNativeFn(function(method, url) {
                        this._azokleUrl = url;
                        return origOpen.apply(this, arguments);
                    }, 'open');
                    XMLHttpRequest.prototype.send = makeNativeFn(function() {
                        var url = this._azokleUrl;
                        if (url && typeof url === 'string' &&
                            (url.indexOf('/youtubei/v1/player') !== -1 ||
                             url.indexOf('/player?') !== -1 ||
                             url.indexOf('/reel_watch_sequence') !== -1)) {
                            var self = this;
                            var origGetter = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype, 'responseText');
                            var origResponseGetter = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype, 'response');
                            var patchedText = null;
                            var patchedObj = null;

                            this.addEventListener('readystatechange', function() {
                                if (self.readyState === 4 && self.status === 200) {
                                    try {
                                        var rawData = null;
                                        if (self.responseType === 'json' && self.response) {
                                            rawData = self.response;
                                        } else {
                                            var rawText = origGetter ? origGetter.get.call(self) : self.responseText;
                                            rawData = JSON.parse(rawText);
                                        }
                                        if (rawData && typeof rawData === 'object') {
                                            sanitizePlayerResponse(rawData);
                                            patchedObj = rawData;
                                            patchedText = JSON.stringify(rawData);
                                        }
                                    } catch(e) {}
                                }
                            }, true);

                            try {
                                Object.defineProperty(this, 'responseText', {
                                    get: function() {
                                        return (patchedText !== null) ? patchedText : (origGetter ? origGetter.get.call(this) : '');
                                    },
                                    configurable: true
                                });
                                Object.defineProperty(this, 'response', {
                                    get: function() {
                                        if (patchedObj !== null && this.responseType === 'json') {
                                            return patchedObj;
                                        }
                                        if (patchedText !== null) {
                                            return (this.responseType === '' || this.responseType === 'text') ? patchedText : patchedObj;
                                        }
                                        return origResponseGetter ? origResponseGetter.get.call(this) : '';
                                    },
                                    configurable: true
                                });
                            } catch(e) {}
                        }
                        return origSend.apply(this, arguments);
                    }, 'send');
                }
            }
        } catch(e) {}
    }

    // Execute Main-World Injection
    injectScriptletIntoPage(pageWorldPayload);

    // Defense-in-depth: If GeckoView wrappedJSObject is accessible, apply properties directly
    try {
        if (typeof window.wrappedJSObject !== 'undefined' && window.wrappedJSObject) {
            window.wrappedJSObject.canRunAds = true;
            window.wrappedJSObject.isAdBlockActive = false;
        }
    } catch(e) {}


    // ============================================================================
    // STEP 2: Content-Script Isolated Sandbox Controls
    // DOM-level Anti-Adblock modal unfreezing, background playback, and ad skipping
    // ============================================================================

    // Neutralize anti-adblock modal backdrops and re-enable scrolling if frozen
    function unfreezePage() {
        try {
            var overlaySelectors = [
                '#adblock-overlay',
                '.adblock-overlay',
                '.modal-backdrop[class*="adblock"]',
                '.ot-modal-open',
                '.didomi-popup-open'
            ];
            var hasOverlay = false;
            for (var i = 0; i < overlaySelectors.length; i++) {
                if (document.querySelector(overlaySelectors[i])) {
                    hasOverlay = true;
                    break;
                }
            }
            if (hasOverlay) {
                if (document.body && document.body.style.overflow === 'hidden') {
                    document.body.style.removeProperty('overflow');
                }
                if (document.documentElement && document.documentElement.style.overflow === 'hidden') {
                    document.documentElement.style.removeProperty('overflow');
                }
                if (document.body) {
                    document.body.classList.remove('modal-open', 'ot-modal-open', 'didomi-popup-open');
                }
                if (document.documentElement) {
                    document.documentElement.classList.remove('modal-open', 'ot-modal-open', 'didomi-popup-open');
                }
            }
        } catch(e) {}
    }

    // YouTube DOM-level Controller
    try {
        var host = location.hostname;
        var isYouTube = host === 'youtube.com' ||
                        host.endsWith('.youtube.com') ||
                        host === 'youtube-nocookie.com' ||
                        host.endsWith('.youtube-nocookie.com');

        if (isYouTube) {
            // Safe Non-Destructive Ad-Skipper & Prompt Auto-Dismissal
            var handleYouTubePlayerState = function() {
                var player = document.getElementById('movie_player') ||
                             document.querySelector('.html5-video-player');
                if (!player) return;

                // An ad is ONLY active if the player explicitly has ad-showing or ad-interrupting classes.
                // NEVER check static container elements (like .video-ads.ytp-ad-module) which are always present.
                var isAdActive = player.classList.contains('ad-showing') ||
                                 player.classList.contains('ad-interrupting');

                if (isAdActive) {
                    // 1. Native player skip & cancel methods
                    if (typeof player.skipAd === 'function') {
                        try { player.skipAd(true); } catch(e) {}
                    }
                    if (typeof player.cancelPlayback === 'function') {
                        try { player.cancelPlayback(); } catch(e) {}
                    }

                    // 2. Immediately click any rendered skip buttons
                    var skipBtn = document.querySelector(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
                        '.ytp-ad-skip-button-slot button, button.ytp-ad-skip-button, ' +
                        '[class*="ytp-ad-skip-button"], .ytp-ad-overlay-close-button'
                    );
                    if (skipBtn && typeof skipBtn.click === 'function') {
                        try { skipBtn.click(); } catch(e) {}
                    }

                    // 3. Instant 0-ms Seek to End without playbackRate acceleration
                    var video = player.querySelector('video') || document.querySelector('video');
                    if (video) {
                        video.muted = true;
                        if (isFinite(video.duration) && video.duration > 0 && video.currentTime < video.duration) {
                            try {
                                video.currentTime = video.duration;
                            } catch(e) {}
                        }
                    }
                } else {
                    // Normal content: Keep normal playback rate without speed or buffer stalls
                    var video = player.querySelector('video') || document.querySelector('video');
                    if (video && video.playbackRate !== 1.0 && video.playbackRate > 2.0) {
                        video.playbackRate = 1.0;
                    }
                }

                // Dismiss anti-adblock enforcement dialogs without destroying polymer DOM trees
                var dismissBtn = document.querySelector(
                    'ytd-enforcement-message-view-model button, ' +
                    'yt-playability-error-supported-renderers button, ' +
                    '#dismiss-button, tp-yt-paper-dialog button, ' +
                    'yt-button-renderer[dialog-dismiss] button'
                );
                if (dismissBtn && typeof dismissBtn.click === 'function') {
                    try { dismissBtn.click(); } catch(e) {}
                }
            };

            // Event-driven observation on player containers
            var initObserver = function() {
                var player = document.getElementById('movie_player') ||
                             document.querySelector('.html5-video-player') ||
                             document.body;
                if (player) {
                    var obs = new MutationObserver(function() {
                        handleYouTubePlayerState();
                    });
                    obs.observe(player, { attributes: true, attributeFilter: ['class'], childList: true, subtree: true });
                }
            };

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initObserver, { once: true });
            } else {
                initObserver();
            }
        }
    } catch(e) {}

    // Universal HTML5 Video Player Ad Defuser (Strictly for NON-YouTube streaming websites)
    try {
        var host = location.hostname;
        var isYouTube = host === 'youtube.com' ||
                        host.endsWith('.youtube.com') ||
                        host === 'youtube-nocookie.com' ||
                        host.endsWith('.youtube-nocookie.com');

        if (!isYouTube) {
            var skipGenericVideoAds = function() {
                var adContainers = document.querySelectorAll(
                    '.videoAdUi, .ima-ad-container, [class*="preroll-ad"], [class*="video-ad-overlay"], ' +
                    '.vjs-ad-playing'
                );
                if (adContainers.length > 0) {
                    for (var i = 0; i < adContainers.length; i++) {
                        var container = adContainers[i];
                        var video = container.querySelector('video') || (container.parentElement ? container.parentElement.querySelector('video') : null);
                        if (video) {
                            video.muted = true;
                            video.playbackRate = 16.0;
                        }
                        var skip = container.querySelector(
                            'button[class*="skip"], [class*="skipButton"], [id*="skip"], button[aria-label*="Skip"], button[aria-label*="skip"]'
                        );
                        if (skip && typeof skip.click === 'function') {
                            try { skip.click(); } catch(e) {}
                        }
                    }
                }
            };

            var genericAdObserver = new MutationObserver(function() {
                skipGenericVideoAds();
            });
            var targetRoot = document.documentElement || document.body;
            if (targetRoot) {
                genericAdObserver.observe(targetRoot, { childList: true, subtree: true });
            }
        }
    } catch(e) {}

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', unfreezePage, { once: true });
    } else {
        unfreezePage();
    }
})();
