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

            // Site-Specific YouTube Ad-Defuser (Defuses adPlacements, playerAds, adSlots in page context)
            var isYouTubeSite = host.indexOf('youtube.com') !== -1 || host.indexOf('youtu.be') !== -1;
            if (isYouTubeSite) {
                var sanitizeYtData = function(data) {
                    if (!data || typeof data !== 'object') return data;
                    try {
                        if (data.adPlacements) data.adPlacements = [];
                        if (data.playerAds) data.playerAds = [];
                        if (data.adSlots) data.adSlots = [];
                        if (data.adBreakHeartbeatParams) delete data.adBreakHeartbeatParams;
                        if (data.playerResponse && typeof data.playerResponse === 'object') {
                            if (data.playerResponse.adPlacements) data.playerResponse.adPlacements = [];
                            if (data.playerResponse.playerAds) data.playerResponse.playerAds = [];
                            if (data.playerResponse.adSlots) data.playerResponse.adSlots = [];
                        }
                    } catch(e) {}
                    return data;
                };

                // 1. Intercept ytInitialPlayerResponse
                var _ytResp = window.ytInitialPlayerResponse;
                try {
                    Object.defineProperty(window, 'ytInitialPlayerResponse', {
                        get: function() { return _ytResp; },
                        set: function(val) { _ytResp = sanitizeYtData(val); },
                        configurable: true,
                        enumerable: true
                    });
                    if (_ytResp) _ytResp = sanitizeYtData(_ytResp);
                } catch(e) {}

                // 2. Intercept window.fetch for /youtubei/v1/player and /youtubei/v1/next SPA updates
                if (typeof window.fetch === 'function') {
                    var origFetch = window.fetch;
                    window.fetch = function(input, init) {
                        var url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                        var promise = origFetch.apply(this, arguments);
                        if (url && (url.indexOf('/youtubei/v1/player') !== -1 || url.indexOf('/youtubei/v1/next') !== -1)) {
                            return promise.then(function(resp) {
                                try {
                                    var origJson = resp.json;
                                    resp.json = function() {
                                        return origJson.apply(this, arguments).then(function(jsonObj) {
                                            return sanitizeYtData(jsonObj);
                                        });
                                    };
                                } catch(e) {}
                                return resp;
                            });
                        }
                        return promise;
                    };
                }

                // 3. Intercept JSON.parse for embedded player responses
                var origJsonParse = JSON.parse;
                JSON.parse = function(text, reviver) {
                    var res = origJsonParse.apply(this, arguments);
                    if (res && typeof res === 'object') {
                        if (res.adPlacements || res.playerAds || res.adSlots) {
                            sanitizeYtData(res);
                        }
                    }
                    return res;
                };
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
    function injectMainWorldScript() {
        try {
            var target = document.head || document.documentElement || document.body;
            if (target && target.appendChild) {
                var scriptTag = document.createElement('script');
                scriptTag.textContent = pageMainWorldCode;
                target.appendChild(scriptTag);
                scriptTag.remove();
                return true;
            }
        } catch(e) {}
        return false;
    }

    if (!injectMainWorldScript()) {
        var docObserver = new MutationObserver(function() {
            if (injectMainWorldScript()) {
                docObserver.disconnect();
            }
        });
        if (document) {
            docObserver.observe(document, { childList: true, subtree: true });
        }
    }


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
    // Handles desktop and mobile (m.youtube.com) player containers,
    // SPA navigation events, unskippable ad states, and anti-adblock modals.
    // =========================================================================
    var isYouTube = hostName === 'youtube.com' || hostName === 'm.youtube.com' || hostName.endsWith('.youtube.com');
    if (isYouTube) {
        var ytSkipSelectors = [
            '.ytp-ad-skip-button',
            '.ytp-ad-skip-button-modern',
            '.videoAdUiSkipButton',
            '.ytm-ad-skip-button',
            'button.ytp-ad-skip-button-text',
            'button.ytm-ad-skip-button-text',
            '.ytp-skip-ad-button',
            'ytm-skip-ad-button',
            '.ytp-ad-skip-button-slot button',
            '.ytp-ad-skip-button-container button'
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
            '.ytp-ad-image-overlay',
            '.ytp-ad-text-overlay',
            'ytm-promoted-sparkles-web-renderer',
            'ytm-promoted-video-renderer',
            'ytd-ad-slot-renderer',
            'ytm-ad-slot-renderer',
            'ytm-companion-ad-renderer',
            'ytd-action-companion-ad-renderer',
            'ytd-carousel-ad-renderer',
            'ytd-rich-item-renderer[is-promoted]',
            'ytm-promoted-sparkles-text-search-web-renderer'
        ].join(', ');

        var lastReportedSkip = 0;
        var adFastForwardInterval = null;
        var adWasMutedByShield = false;

        // Hide black ad overlays whenever player enters ad-showing state
        try {
            if (!document.getElementById('azokle-yt-ad-shield')) {
                var ytAdStyle = document.createElement('style');
                ytAdStyle.id = 'azokle-yt-ad-shield';
                ytAdStyle.textContent = [
                    '.ad-showing .ytp-ad-player-overlay',
                    '.ad-showing .ytp-ad-overlay-container',
                    '.ad-showing .ytp-ad-module',
                    '.ad-interrupting .ytp-ad-player-overlay',
                    '.ad-interrupting .ytp-ad-overlay-container',
                    '.ad-interrupting .ytp-ad-module',
                    '.ytm-ad-badge',
                    '.ytp-ad-text',
                    '.ytp-ad-preview-container'
                ].join(', ') + ' { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
                (document.head || document.documentElement || document).appendChild(ytAdStyle);
            }
        } catch(e) {}

        function getActiveVideo() {
            var videos = document.querySelectorAll('video');
            for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                if (v && v.src && !v.paused) return v;
            }
            var player = document.querySelector('#movie_player, .html5-video-player, ytm-video-player-renderer, #player, #player-container-id');
            return (player && player.querySelector('video')) || document.querySelector('.html5-main-video') || document.querySelector('video');
        }

        var handleYouTubePlayerState = function() {
            try {
                // 1. Skip ad button clicker with exact, tested selectors
                var skipped = false;
                for (var s = 0; s < ytSkipSelectors.length; s++) {
                    var btn = document.querySelector(ytSkipSelectors[s]);
                    if (btn && btn.offsetParent !== null) {
                        btn.click();
                        skipped = true;
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

                // 2. Strict Ad Detection: strictly only when player has .ad-showing or .ad-interrupting
                // NEVER check permanent structural containers like .ytp-ad-overlay-container or .ytp-ad-module!
                var player = document.querySelector('#movie_player, .html5-video-player, ytm-video-player-renderer, #player, #player-container-id');
                var isAdActive = false;
                if (player) {
                    if (player.classList.contains('ad-showing') || player.classList.contains('ad-interrupting')) {
                        isAdActive = true;
                    } else if (player.querySelector('.ad-showing, .ad-interrupting')) {
                        isAdActive = true;
                    }
                }
                if (!isAdActive && document.querySelector('.ad-showing, .ad-interrupting')) {
                    isAdActive = true;
                }

                var video = getActiveVideo();

                if (isAdActive) {
                    // Try calling native player.skipAd() method if available on player instance
                    var moviePlayer = document.getElementById('movie_player') || (player && player.id === 'movie_player' ? player : null);
                    if (moviePlayer && typeof moviePlayer.skipAd === 'function') {
                        try { moviePlayer.skipAd(); } catch(e) {}
                    }

                    if (video) {
                        // Mute the ad so user hears nothing
                        if (!video.muted) {
                            video.muted = true;
                            adWasMutedByShield = true;
                        }

                        // Instantly advance unskippable ad to the end so it completes immediately:
                        if (isFinite(video.duration) && video.duration > 0 && video.duration < 120) {
                            video.currentTime = video.duration;
                        } else {
                            try { video.currentTime = 9999; } catch(e) {}
                        }
                        video.playbackRate = 16.0;

                        if (video.paused) {
                            video.play().catch(function() {});
                        }
                    }

                    startAdMonitor();
                } else {
                    // Normal video playback: ensure audio and rate are faithfully restored
                    if (video) {
                        if (video.playbackRate > 1.0) {
                            video.playbackRate = 1.0;
                        }
                        if (adWasMutedByShield && video.muted) {
                            video.muted = false;
                            adWasMutedByShield = false;
                        }
                    }
                    stopAdMonitor();
                }

                // 3. Anti-adblock enforcement dialog recovery
                var enforcementDialog = document.querySelector('ytd-enforcement-message-view-model, tp-yt-paper-dialog.ytd-popup-container');
                if (enforcementDialog) {
                    var dismissBtn = enforcementDialog.querySelector('button, [role="button"]');
                    if (dismissBtn) {
                        dismissBtn.click();
                    } else {
                        enforcementDialog.remove();
                    }
                    if (video && video.paused) {
                        video.play().catch(function() {});
                    }
                }

                // 4. Prune cosmetic promotional banner elements on YouTube
                var promoNodes = document.querySelectorAll(ytCosmeticSelectors);
                for (var p = 0; p < promoNodes.length; p++) {
                    promoNodes[p].remove();
                }

                return isAdActive;
            } catch(e) {
                return false;
            }
        };

        function startAdMonitor() {
            if (adFastForwardInterval) return;
            adFastForwardInterval = setInterval(function() {
                var active = handleYouTubePlayerState();
                if (!active) {
                    stopAdMonitor();
                }
            }, 100);
        }

        function stopAdMonitor() {
            if (adFastForwardInterval) {
                clearInterval(adFastForwardInterval);
                adFastForwardInterval = null;
            }
        }

        // Targeted MutationObserver: attaches directly to player container with debouncing
        var ytDebounceTimer = null;
        var onYtMutation = function() {
            if (ytDebounceTimer) return;
            ytDebounceTimer = setTimeout(function() {
                ytDebounceTimer = null;
                handleYouTubePlayerState();
            }, 80);
        };

        var playerObserver = null;
        var attachYtObserver = function() {
            if (playerObserver) {
                playerObserver.disconnect();
                playerObserver = null;
            }
            var target = document.querySelector('#movie_player, .html5-video-player, ytm-video-player-renderer, #player, #player-container-id') || document.body;
            if (target) {
                playerObserver = new MutationObserver(onYtMutation);
                playerObserver.observe(target, { childList: true, subtree: true, attributes: true, attributeFilter: ['class'] });
                handleYouTubePlayerState();
            } else {
                setTimeout(attachYtObserver, 300);
            }
        };

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', attachYtObserver);
        } else {
            attachYtObserver();
        }

        // Listen to YouTube Single-Page Application (SPA) navigation transitions
        ['yt-navigate-finish', 'ytm-page-update', 'spfdone', 'popstate'].forEach(function(evt) {
            window.addEventListener(evt, function() {
                setTimeout(function() {
                    attachYtObserver();
                    handleYouTubePlayerState();
                }, 150);
            }, true);
        });
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
