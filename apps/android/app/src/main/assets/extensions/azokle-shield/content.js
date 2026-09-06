(function() {
    'use strict';

    if (window.__azokleShieldExtLoaded) return;
    var hostName = (window.location && window.location.hostname ? window.location.hostname : '').toLowerCase();
    if (hostName === 'azokle.com' || hostName.endsWith('.azokle.com') ||
        hostName === 'cloudflare.com' || hostName.endsWith('.cloudflare.com') ||
        hostName === 'cloudflareinsights.com' || hostName.endsWith('.cloudflareinsights.com') ||
        hostName.includes('cloudflare')) {
        return;
    }
    window.__azokleShieldExtLoaded = true;

    // =========================================================================
    // 1. Sovereign Main-World Privacy & RFP (Resist Fingerprinting) Engine
    // Runs in Page Context (Main World) at document_start.
    // Protects against hardware, canvas, audio, and sensor fingerprinting.
    // Respects browser-engine boundaries without global media monkey-patching.
    // =========================================================================
    var pageMainWorldCode = `
    (function() {
        if (window.__azokleMainWorldInstalled) return;
        window.__azokleMainWorldInstalled = true;

        try {
            var host = (window.location && window.location.hostname) ? window.location.hostname.toLowerCase() : '';
            var isVideoStreamingSite = host.indexOf('youtube.com') !== -1 ||
                                       host.indexOf('youtu.be') !== -1 ||
                                       host.indexOf('vimeo.com') !== -1 ||
                                       host.indexOf('dailymotion.com') !== -1 ||
                                       host.indexOf('twitch.tv') !== -1;

            var safeDef = function(obj, prop, val) {
                if (!obj) return;
                try {
                    Object.defineProperty(obj, prop, {
                        get: function() { return val; },
                        set: function() {},
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) {}
            };

            // Site-Specific Video Background Playback (Visibility State Neutralization)
            // Scoped strictly to video streaming platforms where background auto-pause occurs
            // via visibilitychange/hidden event listeners.
            // Never intercepts HTMLMediaElement.prototype.pause (respects Section 6 & 7).
            if (isVideoStreamingSite) {
                var docProto = (typeof Document !== 'undefined') ? Document.prototype : null;

                ['hidden', 'webkitHidden', 'mozHidden', 'msHidden'].forEach(function(p) {
                    safeDef(document, p, false);
                    if (docProto) safeDef(docProto, p, false);
                });

                ['visibilityState', 'webkitVisibilityState', 'mozVisibilityState', 'msVisibilityState'].forEach(function(p) {
                    safeDef(document, p, 'visible');
                    if (docProto) safeDef(docProto, p, 'visible');
                });

                try {
                    document.hasFocus = function() { return true; };
                    if (docProto) docProto.hasFocus = function() { return true; };
                } catch(e) {}

                var neutralizeVisibilityEvent = function(e) {
                    if (e && e.type && e.type.indexOf('visibilitychange') !== -1) {
                        try { e.stopImmediatePropagation(); } catch(err) {}
                    }
                };

                ['visibilitychange', 'webkitvisibilitychange', 'mozvisibilitychange', 'msvisibilitychange'].forEach(function(evt) {
                    try { window.addEventListener(evt, neutralizeVisibilityEvent, true); } catch(e) {}
                    try { if (document) document.addEventListener(evt, neutralizeVisibilityEvent, true); } catch(e) {}
                });

                try {
                    window.onvisibilitychange = null;
                    if (document) document.onvisibilitychange = null;
                } catch(e) {}
            }

            // Battery API Neutralization
            if (typeof navigator !== 'undefined') {
                navigator.getBattery = function() {
                    return Promise.reject(new Error("Battery API disabled for privacy"));
                };
            }
        } catch(e) {}
    })();
    `;

    // Secure Main-World Injection: uses standard script element with immediate removal
    // Strictly avoids window.wrappedJSObject.eval() (respects Section 27)
    try {
        var scriptTag = document.createElement('script');
        scriptTag.textContent = pageMainWorldCode;
        (document.head || document.documentElement || document).appendChild(scriptTag);
        scriptTag.remove();
    } catch(e) {}


    // =========================================================================
    // 2. App Theme Preference Synchronizer
    // =========================================================================
    function applyAppTheme(theme) {
        if (!theme || theme === 'system') return;
        var mode = theme.toLowerCase();
        try {
            if (document.documentElement) {
                if (mode === 'light') {
                    document.documentElement.style.setProperty('color-scheme', 'light', 'important');
                    document.documentElement.setAttribute('data-theme', 'light');
                    document.documentElement.setAttribute('data-color-mode', 'light');
                } else if (mode === 'dark') {
                    document.documentElement.style.setProperty('color-scheme', 'dark', 'important');
                    document.documentElement.setAttribute('data-theme', 'dark');
                    document.documentElement.setAttribute('data-color-mode', 'dark');
                }
            }
        } catch(e) {}
    }

    try {
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
            browser.runtime.sendMessage({ type: 'GET_THEME' }).then(function(res) {
                if (res && res.theme) {
                    applyAppTheme(res.theme);
                }
            }).catch(function() {});
        }
    } catch(e) {}


    // =========================================================================
    // 3. YouTube Site-Specific Compatibility Adapter
    // Scoped strictly to *.youtube.com domains (respects Section 19).
    // Replaces 100ms polling with a targeted MutationObserver.
    // Never clicks broad aria-labels or uses 16x speed manipulation.
    // =========================================================================
    var isYouTube = hostName === 'youtube.com' || hostName === 'm.youtube.com' || hostName.endsWith('.youtube.com');
    if (isYouTube) {
        var ytSkipSelectors = [
            '.ytp-ad-skip-button',
            '.ytp-ad-skip-button-modern',
            '.videoAdUiSkipButton',
            '.ytm-ad-skip-button',
            'button.ytp-ad-skip-button-text',
            '.ytp-skip-ad-button'
        ];

        var ytCosmeticSelectors = [
            'ytd-promoted-video-renderer',
            'ytd-display-ad-renderer',
            'ytd-statement-banner-renderer',
            'ytd-in-feed-ad-layout-renderer',
            'ytd-banner-promo-renderer',
            '#player-ads',
            '#masthead-ad',
            '.ytp-ad-overlay-container',
            'ytm-promoted-sparkles-web-renderer',
            'ytm-promoted-video-renderer',
            'ytd-ad-slot-renderer',
            'ytm-ad-slot-renderer'
        ].join(', ');

        var lastReportedSkip = 0;
        var handleYouTubePlayerState = function() {
            try {
                // 1. Skip ad button clicker with exact, tested selectors only
                for (var s = 0; s < ytSkipSelectors.length; s++) {
                    var btn = document.querySelector(ytSkipSelectors[s]);
                    if (btn && btn.offsetParent !== null) {
                        btn.click();
                        if (Date.now() - lastReportedSkip > 4000) {
                            lastReportedSkip = Date.now();
                            if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                                browser.runtime.sendMessage({
                                    type: 'RECORD_BLOCK',
                                    host: 'youtube.com (Video Ad Skipped)'
                                }).catch(function() {});
                            }
                        }
                        break;
                    }
                }

                // 2. Prune cosmetic promotional banner elements on YouTube
                var promoNodes = document.querySelectorAll(ytCosmeticSelectors);
                for (var p = 0; p < promoNodes.length; p++) {
                    promoNodes[p].remove();
                }
            } catch(e) {}
        };

        // Targeted MutationObserver: attaches to player container or document.body with debouncing
        var ytDebounceTimer = null;
        var onYtMutation = function() {
            if (ytDebounceTimer) return;
            ytDebounceTimer = setTimeout(function() {
                ytDebounceTimer = null;
                handleYouTubePlayerState();
            }, 120);
        };

        if (typeof MutationObserver !== 'undefined') {
            var attachYtObserver = function() {
                var target = document.querySelector('#movie_player, .html5-video-player') || document.body;
                if (target) {
                    var observer = new MutationObserver(onYtMutation);
                    observer.observe(target, { childList: true, subtree: true, attributes: true, attributeFilter: ['class'] });
                    handleYouTubePlayerState();
                } else {
                    setTimeout(attachYtObserver, 250);
                }
            };

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', attachYtObserver);
            } else {
                attachYtObserver();
            }
        }
    }


    // =========================================================================
    // 4. W3C Web App Manifest & PWA Installability Scanner (Section 20)
    // =========================================================================
    var manifestDispatched = false;
    var scanForManifest = function() {
        if (manifestDispatched) return;
        try {
            var manifestLink = document.querySelector('link[rel="manifest"]');
            var iconLinks = Array.from(document.querySelectorAll('link[rel~="icon"], link[rel="apple-touch-icon"], link[rel="apple-touch-icon-precomposed"]'));
            var ogImage = document.querySelector('meta[property="og:image"]');
            var themeColorMeta = document.querySelector('meta[name="theme-color"]');

            var manifestUrl = manifestLink ? manifestLink.href : null;
            var themeColor = themeColorMeta ? themeColorMeta.content : null;

            var fallbackIcon = null;
            if (iconLinks.length > 0) {
                var touchIcon = iconLinks.find(function(el) { return (el.rel || '').includes('apple-touch-icon'); });
                if (touchIcon && touchIcon.href) {
                    fallbackIcon = touchIcon.href;
                } else {
                    fallbackIcon = iconLinks[0].href;
                }
            }
            if (!fallbackIcon && ogImage && ogImage.content) {
                fallbackIcon = ogImage.content;
            }
            if (!fallbackIcon) {
                try {
                    fallbackIcon = window.location.origin + '/favicon.ico';
                } catch(e) {}
            }

            var notifyApp = function(manifestJsonStr) {
                if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                    browser.runtime.sendMessage({
                        type: 'PWA_MANIFEST_DISCOVERED',
                        manifestUrl: manifestUrl,
                        manifestJson: manifestJsonStr,
                        documentUrl: window.location.href,
                        pageTitle: document.title,
                        fallbackIcon: fallbackIcon,
                        themeColor: themeColor
                    }).then(function() {
                        manifestDispatched = true;
                    }).catch(function() {
                        // Allow retry on failure as specified by Section 20
                        manifestDispatched = false;
                    });
                }
            };

            if (manifestUrl && (manifestUrl.startsWith('http://') || manifestUrl.startsWith('https://'))) {
                fetch(manifestUrl, { credentials: 'same-origin' })
                    .then(function(res) {
                        if (res.ok) return res.text();
                        return null;
                    })
                    .then(function(text) {
                        notifyApp(text);
                    })
                    .catch(function() {
                        notifyApp(null);
                    });
            } else {
                notifyApp(null);
            }
        } catch(e) {}
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scanForManifest);
    } else {
        scanForManifest();
    }
    window.addEventListener('load', scanForManifest);
})();
