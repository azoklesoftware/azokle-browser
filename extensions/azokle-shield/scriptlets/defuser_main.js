// Azokle Shield Sovereign Defuser - Webpage Main World Scriptlet
// Executes directly inside the page's MAIN execution context (bypassing WebExtension Sandbox isolation).
// Provides synchronous zero-latency payload sanitization for YouTube and anti-adblock neutralization.

(function() {
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

    // 1. Neutralize Anti-Adblock Honeypots & Trap Globals
    try {
        var dummyFn = makeNativeFn(function() {}, '');
        window.canRunAds = true;
        window.isAdBlockActive = false;
        window.adblocker = false;
        window._adblock = false;
        if (!window.google_ad_client) window.google_ad_client = 'ca-pub-0000000000000000';

        var babStub = function() {
            return {
                check: dummyFn,
                clearEvent: dummyFn,
                on: dummyFn,
                onDetected: dummyFn,
                onNotDetected: function(fn) {
                    if (typeof fn === 'function') {
                        try { fn(); } catch(e) {}
                    }
                    return this;
                }
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
    } catch(e) {}

    // 2. YouTube Ad Annihilation (Runs inside the MAIN world where YouTube's player scripts execute)
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

            // High-performance JSON sanitizer replicating Brave-Core & uBlock Origin json-prune
            var sanitizePlayerResponse = function(obj) {
                if (!obj || typeof obj !== 'object') return obj;
                try {
                    if ('playerAds' in obj) delete obj.playerAds;
                    if ('adPlacements' in obj) obj.adPlacements = [];
                    if ('adSlots' in obj) obj.adSlots = [];

                    if (obj.playerResponse && typeof obj.playerResponse === 'object') {
                        if ('playerAds' in obj.playerResponse) delete obj.playerResponse.playerAds;
                        if ('adPlacements' in obj.playerResponse) obj.playerResponse.adPlacements = [];
                        if ('adSlots' in obj.playerResponse) obj.playerResponse.adSlots = [];
                    }
                    if (obj.reelWatchSequenceResponse && typeof obj.reelWatchSequenceResponse === 'object') {
                        if ('adPlacements' in obj.reelWatchSequenceResponse) delete obj.reelWatchSequenceResponse.adPlacements;
                        if ('adSlots' in obj.reelWatchSequenceResponse) delete obj.reelWatchSequenceResponse.adSlots;
                    }
                    if (obj.adBreakHeartbeatParams) delete obj.adBreakHeartbeatParams;
                } catch(e) {}
                return obj;
            };

            // Hook window.ytInitialPlayerResponse before YouTube's scripts initialize
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

            // Hook window.ytInitialData to strip feed and search ad banners
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

            // Hook window.fetch in MAIN world
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

            // Hook window.XMLHttpRequest in MAIN world
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
})();
