(function() {
    'use strict';

    if (window.__azokleShieldExtLoaded) return;
    var hostName = (window.location.hostname || '').toLowerCase();
    if (hostName === 'azokle.com' || hostName.endsWith('.azokle.com') ||
        hostName === 'cloudflare.com' || hostName.endsWith('.cloudflare.com') ||
        hostName === 'cloudflareinsights.com' || hostName.endsWith('.cloudflareinsights.com') ||
        hostName.includes('cloudflare')) {
        return;
    }
    window.__azokleShieldExtLoaded = true;

    // =========================================================================
    // 0. Continuous Video & Media Background Playback Shield (Main World Injection)
    // Prevents websites (YouTube, Twitch, Vimeo, Soundcloud, HTML5 video/audio)
    // from detecting when the app is backgrounded, minimized, switched, or when
    // the device screen is locked.
    // =========================================================================
    var pageScriptCode = `
    (function() {
        try {
            var docProto = (typeof Document !== 'undefined' && Document.prototype) ? Document.prototype : null;

            var defineProp = function(target, prop, getter) {
                if (!target) return;
                try {
                    Object.defineProperty(target, prop, {
                        get: getter,
                        set: function() {},
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) {}
            };

            var falseGetter = function() { return false; };
            var visibleGetter = function() { return 'visible'; };
            var trueGetter = function() { return true; };

            ['hidden', 'webkitHidden', 'mozHidden', 'msHidden'].forEach(function(prop) {
                defineProp(document, prop, falseGetter);
                if (docProto) defineProp(docProto, prop, falseGetter);
            });

            ['visibilityState', 'webkitVisibilityState', 'mozVisibilityState', 'msVisibilityState'].forEach(function(prop) {
                defineProp(document, prop, visibleGetter);
                if (docProto) defineProp(docProto, prop, visibleGetter);
            });

            try {
                document.hasFocus = trueGetter;
                if (docProto) docProto.hasFocus = trueGetter;
            } catch(e) {}

            // Intercept addEventListener so site visibilitychange listeners are not registered
            try {
                var origAddEventListener = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, listener, options) {
                    if (typeof type === 'string' && (type === 'visibilitychange' || type.endsWith('visibilitychange'))) {
                        return;
                    }
                    return origAddEventListener.call(this, type, listener, options);
                };
            } catch(e) {}

            var eventNames = [
                'visibilitychange',
                'webkitvisibilitychange',
                'mozvisibilitychange',
                'msvisibilitychange'
            ];

            eventNames.forEach(function(evt) {
                try {
                    window.addEventListener(evt, function(e) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                    }, true);
                    document.addEventListener(evt, function(e) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                    }, true);
                } catch(e) {}
            });

            try {
                window.addEventListener('pagehide', function(e) {
                    e.stopImmediatePropagation();
                }, true);
            } catch(e) {}
        } catch(e) {}
    })();
    `;

    // 1. Inject into real page context (Main World) via inline script tag
    try {
        var scriptEl = document.createElement('script');
        scriptEl.textContent = pageScriptCode;
        (document.head || document.documentElement || document).appendChild(scriptEl);
        scriptEl.remove();
    } catch(e) {}

    // 2. Also patch via Firefox/GeckoView wrappedJSObject
    try {
        if (typeof window.wrappedJSObject !== 'undefined') {
            var realWin = window.wrappedJSObject;
            var realDoc = realWin.document;
            var realDocProto = realWin.Document ? realWin.Document.prototype : null;

            var defineRealProp = function(target, prop, getter) {
                if (!target) return;
                try {
                    realWin.Object.defineProperty(target, prop, {
                        get: getter,
                        set: function() {},
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) {}
            };

            ['hidden', 'webkitHidden', 'mozHidden', 'msHidden'].forEach(function(prop) {
                defineRealProp(realDoc, prop, function() { return false; });
                if (realDocProto) defineRealProp(realDocProto, prop, function() { return false; });
            });

            ['visibilityState', 'webkitVisibilityState', 'mozVisibilityState', 'msVisibilityState'].forEach(function(prop) {
                defineRealProp(realDoc, prop, function() { return 'visible'; });
                if (realDocProto) defineRealProp(realDocProto, prop, function() { return 'visible'; });
            });

            if (realDoc) realDoc.hasFocus = function() { return true; };
            if (realDocProto) realDocProto.hasFocus = function() { return true; };
        }
    } catch(e) {}

    // 1. App Theme Preference Synchronizer (Non-destructive CSS & Attribute Engine)
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

    // Query Native App for current App Theme setting (light, dark, system)
    try {
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
            browser.runtime.sendMessage({ type: 'GET_THEME' }).then(function(res) {
                if (res && res.theme) {
                    applyAppTheme(res.theme);
                }
            }).catch(function() {});
        }
    } catch(e) {}

    // 2. High-Performance YouTube Instant Ad Annihilator & Video Ad Fast-Forward
    var host = (location && location.hostname) ? location.hostname.toLowerCase() : '';
    if (host === 'youtube.com' || host === 'm.youtube.com' || host.endsWith('.youtube.com')) {
        var lastReportedAdTime = 0;
        var handleYouTubeAd = function() {
            try {
                // 1. Detect strictly active ad playback state (NOT static container elements)
                var isAdActive = false;
                var player = document.querySelector('#movie_player, .html5-video-player, .video-stream');
                var playerContainer = document.querySelector('#movie_player, .html5-video-player');
                if (playerContainer && (playerContainer.classList.contains('ad-showing') || playerContainer.classList.contains('ad-interrupting'))) {
                    isAdActive = true;
                }

                var adText = document.querySelector('.ytp-ad-text, .ytm-ad-text, .ytp-ad-preview-text');
                if (adText && adText.offsetParent !== null) {
                    isAdActive = true;
                }

                var video = document.querySelector('video');

                // 2. If ad is actively playing, safely accelerate and mute only the ad
                if (isAdActive && video) {
                    video.muted = true;
                    video.playbackRate = 16.0;
                } else if (!isAdActive && video && video.playbackRate > 2.0) {
                    video.playbackRate = 1.0;
                }

                // 3. High-Speed Click all skip button variants
                var skipSelectors = [
                    '.ytp-ad-skip-button',
                    '.ytp-ad-skip-button-modern',
                    '.videoAdUiSkipButton',
                    '.ytm-ad-skip-button',
                    'button.ytp-ad-skip-button-text',
                    '.ytp-skip-ad-button',
                    '.ytp-ad-overlay-close-button',
                    'button[id*="skip-button"]',
                    'button[aria-label*="Skip ad"]',
                    'button[aria-label*="Skip"]'
                ];

                for (var s = 0; s < skipSelectors.length; s++) {
                    var btns = document.querySelectorAll(skipSelectors[s]);
                    for (var b = 0; b < btns.length; b++) {
                        var btn = btns[b];
                        if (btn && btn.offsetParent !== null) {
                            btn.click();
                            isAdActive = true;
                        }
                    }
                }

                // 4. Rate-limited reporting to native app
                if (isAdActive && Date.now() - lastReportedAdTime > 3000) {
                    lastReportedAdTime = Date.now();
                    if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                        browser.runtime.sendMessage({
                            type: 'RECORD_BLOCK',
                            host: 'youtube.com (Video Ad Skipped)'
                        }).catch(function() {});
                    }
                }
            } catch(e) {}
        };

        // Run rapid 100ms interval for instantaneous skip response
        setInterval(handleYouTubeAd, 100);
    }

    // 3. Content Script Reactive DOM Element Pruning for Static Banner Ads
    var adSelectors = 'ytd-promoted-video-renderer, ytd-display-ad-renderer, ytd-statement-banner-renderer, ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer, #player-ads, #masthead-ad, .ytp-ad-overlay-container, ytm-promoted-sparkles-web-renderer, ytm-promoted-video-renderer, .ad-container, #ad-iframe, ytd-enforcement-message-view-model, tp-yt-paper-dialog:has(ytd-enforcement-message-view-model), .yt-playability-error-supported-renderers, ytd-ad-slot-renderer, ytm-ad-slot-renderer, ytd-rich-item-renderer:has(ytd-ad-slot-renderer)';
    var pruneScheduled = false;
    var lastPruneReportTime = 0;

    var cleanDomAds = function() {
        if (pruneScheduled) return;
        pruneScheduled = true;
        (window.requestIdleCallback || window.requestAnimationFrame || function(fn) { setTimeout(fn, 50); })(function() {
            pruneScheduled = false;
            try {
                var ads = document.querySelectorAll(adSelectors);
                if (ads.length > 0) {
                    for (var i = 0; i < ads.length; i++) {
                        ads[i].remove();
                    }
                    if (Date.now() - lastPruneReportTime > 5000) {
                        lastPruneReportTime = Date.now();
                        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                            browser.runtime.sendMessage({
                                type: 'RECORD_BLOCK',
                                host: host || 'Banner Ad'
                            }).catch(function() {});
                        }
                    }
                }
            } catch(e) {}
        });
    };

    if (typeof MutationObserver !== 'undefined' && document.documentElement) {
        var domObserver = new MutationObserver(function(mutations) {
            for (var i = 0; i < mutations.length; i++) {
                if (mutations[i].addedNodes.length > 0) {
                    cleanDomAds();
                    break;
                }
            }
        });
        domObserver.observe(document.documentElement, { childList: true, subtree: true });
        cleanDomAds();
    }

    // 4. Real Web App Manifest & PWA Installability Scanner (Chromium / Firefox Standard)
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

            // Pick the best icon from DOM links (prefer apple-touch-icon or largest icon)
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
                manifestDispatched = true;
                if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                    browser.runtime.sendMessage({
                        type: 'PWA_MANIFEST_DISCOVERED',
                        manifestUrl: manifestUrl,
                        manifestJson: manifestJsonStr,
                        documentUrl: window.location.href,
                        pageTitle: document.title,
                        fallbackIcon: fallbackIcon,
                        themeColor: themeColor
                    }).catch(function() {});
                }
            };

            if (manifestUrl) {
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
