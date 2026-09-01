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
    // 0. Sovereign Main-World Video Background Playback Guardian Engine
    // Runs directly in Page Context (Main World) at document_start.
    // Differentiates user-initiated pause actions from synthetic background
    // auto-pause calls (blur, visibilitychange, IntersectionObserver, freeze).
    // =========================================================================
    var pageMainWorldCode = `
    (function() {
        if (window.__azokleBgGuardianInstalled) return;
        window.__azokleBgGuardianInstalled = true;

        try {
            var isUserInteracting = false;
            var userInteractTimer = null;

            var markUserInteraction = function() {
                isUserInteracting = true;
                if (userInteractTimer) clearTimeout(userInteractTimer);
                userInteractTimer = setTimeout(function() {
                    isUserInteracting = false;
                }, 400);
            };

            ['pointerdown', 'pointerup', 'mousedown', 'mouseup', 'click', 'touchend', 'keydown'].forEach(function(evt) {
                window.addEventListener(evt, markUserInteraction, true);
                if (typeof document !== 'undefined') {
                    document.addEventListener(evt, markUserInteraction, true);
                }
            });

            // 1. Guard HTMLMediaElement.prototype.pause
            if (typeof HTMLMediaElement !== 'undefined' && HTMLMediaElement.prototype) {
                var origPause = HTMLMediaElement.prototype.pause;
                var origPlay = HTMLMediaElement.prototype.play;

                HTMLMediaElement.prototype.play = function() {
                    return origPlay.apply(this, arguments);
                };

                HTMLMediaElement.prototype.pause = function() {
                    // Check if pause was triggered without active user interaction
                    var evt = window.event;
                    var isTrustedUserEvent = evt && evt.isTrusted && (
                        evt.type === 'click' || evt.type === 'touchend' || evt.type === 'mouseup' || evt.type === 'keydown'
                    );

                    if (!isUserInteracting && !isTrustedUserEvent) {
                        // Suppress background auto-pause (app switch, screen lock, blur, visibility change)
                        return;
                    }
                    return origPause.apply(this, arguments);
                };
            }

            // 2. Safe Page Visibility & Focus Spoofing
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

            // 3. Block visibilitychange and blur capture events
            var neutralizeEvent = function(e) {
                if (e && !isUserInteracting) {
                    try {
                        e.stopImmediatePropagation();
                    } catch(err) {}
                }
            };

            ['visibilitychange', 'webkitvisibilitychange', 'mozvisibilitychange', 'msvisibilitychange', 'pagehide', 'freeze', 'blur'].forEach(function(evt) {
                try { window.addEventListener(evt, neutralizeEvent, true); } catch(e) {}
                try { if (document) document.addEventListener(evt, neutralizeEvent, true); } catch(e) {}
            });

            try {
                window.onblur = null;
                document.onblur = null;
                window.onpagehide = null;
                window.onvisibilitychange = null;
                document.onvisibilitychange = null;
            } catch(e) {}
        } catch(e) {}
    })();
    `;

    // Inject into Main World at document_start
    try {
        var scriptTag = document.createElement('script');
        scriptTag.textContent = pageMainWorldCode;
        (document.head || document.documentElement || document).appendChild(scriptTag);
        scriptTag.remove();
    } catch(e) {}

    try {
        if (typeof window.wrappedJSObject !== 'undefined' && typeof window.wrappedJSObject.eval === 'function') {
            window.wrappedJSObject.eval(pageMainWorldCode);
        }
    } catch(e) {}


    // =========================================================================
    // 1. App Theme Preference Synchronizer (Non-destructive CSS & Attribute Engine)
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
    // 2. High-Performance YouTube Instant Ad Annihilator & Video Ad Fast-Forward
    // =========================================================================
    var host = (location && location.hostname) ? location.hostname.toLowerCase() : '';
    if (host === 'youtube.com' || host === 'm.youtube.com' || host.endsWith('.youtube.com')) {
        var lastReportedAdTime = 0;
        var handleYouTubeAd = function() {
            try {
                var isAdActive = false;
                var playerContainer = document.querySelector('#movie_player, .html5-video-player');
                if (playerContainer && (playerContainer.classList.contains('ad-showing') || playerContainer.classList.contains('ad-interrupting'))) {
                    isAdActive = true;
                }

                var adText = document.querySelector('.ytp-ad-text, .ytm-ad-text, .ytp-ad-preview-text');
                if (adText && adText.offsetParent !== null) {
                    isAdActive = true;
                }

                var video = document.querySelector('video');

                if (isAdActive && video) {
                    video.muted = true;
                    video.playbackRate = 16.0;
                } else if (!isAdActive && video && video.playbackRate > 2.0) {
                    video.playbackRate = 1.0;
                }

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

        setInterval(handleYouTubeAd, 100);
    }


    // =========================================================================
    // 3. Content Script Reactive DOM Element Pruning for Static Banner Ads
    // =========================================================================
    var adSelectors = 'ytd-promoted-video-renderer, ytd-display-ad-renderer, ytd-statement-banner-renderer, ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer, #player-ads, #masthead-ad, .ytp-ad-overlay-container, ytm-promoted-sparkles-web-renderer, ytm-promoted-video-renderer, .ad-container, #ad-iframe, ytd-enforcement-message-view-model, .yt-playability-error-supported-renderers, ytd-ad-slot-renderer, ytm-ad-slot-renderer';
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


    // =========================================================================
    // 4. Real Web App Manifest & PWA Installability Scanner (Chromium / Firefox Standard)
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
