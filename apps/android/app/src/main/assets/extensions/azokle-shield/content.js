// Azokle Shield Content Engine
// Platform-Independent Media Protection, Dynamic Cosmetic Filtering, Theme & PWA Bridge

(function() {
    'use strict';

    if (window.__azokleShieldExtLoaded) return;
    var hostName = (window.location && window.location.hostname ? window.location.hostname : '').toLowerCase();

    // Sovereign exemption: Never inject on internal search engine or Cloudflare challenge pages
    if (hostName === 'azokle.com' || hostName.endsWith('.azokle.com') ||
        hostName === 'cloudflare.com' || hostName.endsWith('.cloudflare.com') ||
        hostName.includes('challenges.cloudflare.com')) {
        return;
    }
    window.__azokleShieldExtLoaded = true;

    // =========================================================================
    // 1. Privacy & RFP (Resist Fingerprinting) Standard Mitigations
    // =========================================================================
    try {
        if (typeof navigator !== 'undefined' && navigator.getBattery) {
            navigator.getBattery = function() {
                return Promise.reject(new Error("Battery API disabled for privacy"));
            };
        }
    } catch(e) {}

    // =========================================================================
    // 2. Dynamic Domain-Specific Cosmetic CSS Injection
    // Fetches tailored cosmetic rules compiled by native Rust CosmeticEngine
    // =========================================================================
    function applyCosmeticStyles(css) {
        if (!css || typeof css !== 'string' || css.trim().length === 0) return;
        try {
            var existing = document.getElementById('azokle-shield-cosmetic');
            if (!existing) {
                var styleEl = document.createElement('style');
                styleEl.id = 'azokle-shield-cosmetic';
                styleEl.textContent = css;
                (document.head || document.documentElement || document).appendChild(styleEl);
            } else {
                existing.textContent = css;
            }
        } catch(e) {}
    }

    try {
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
            browser.runtime.sendMessage({
                type: 'GET_COSMETIC_CSS',
                domain: hostName
            }).then(function(res) {
                if (res && res.css) {
                    applyCosmeticStyles(res.css);
                }
            }).catch(function() {});
        }
    } catch(e) {}

    // =========================================================================
    // 3. App Theme Preference Synchronizer
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
    // 4. Platform-Independent Generic HTML5 Media Ad Handler
    // Works across YouTube, Kick, Twitch, Facebook, Instagram, Reddit, X,
    // Vimeo, news sites, and arbitrary HTML5 video players.
    // =========================================================================
    var adSkipSelectors = [
        '.ytp-ad-skip-button',
        '.ytp-ad-skip-button-modern',
        '.videoAdUiSkipButton',
        '.ytm-ad-skip-button',
        'button.ytp-ad-skip-button-text',
        'button.ytm-ad-skip-button-text',
        '.ytp-skip-ad-button',
        'ytm-skip-ad-button',
        '.ytp-ad-skip-button-slot button',
        '.ytp-ad-skip-button-container button',
        'button[class*="skip-button" i]',
        'button[class*="skipAd" i]',
        'button[aria-label*="skip" i]',
        '[data-testid*="skip-ad" i]',
        '[data-test-selector*="skip" i]'
    ];

    var adContainerSelectors = [
        '.ad-showing',
        '.ad-interrupting',
        '.stream-ad-indicator',
        '[class*="videoAdUi" i]',
        '[class*="video-ad-overlay" i]',
        '[class*="preroll-ad" i]',
        '[class*="midroll-ad" i]'
    ];

    var lastSkipTime = 0;
    var wasMutedByShield = false;

    function getActiveMediaElement() {
        var videos = document.querySelectorAll('video');
        for (var i = 0; i < videos.length; i++) {
            var v = videos[i];
            if (v && !v.paused && v.duration > 0) return v;
        }
        return document.querySelector('video') || document.querySelector('audio');
    }

    function isElementVisible(el) {
        if (!el) return false;
        return !!(el.offsetWidth || el.offsetHeight || (typeof el.getClientRects === 'function' && el.getClientRects().length > 0));
    }

    function handleGenericMediaState() {
        try {
            // 1. Detect active ad container or state
            var isAdActive = false;
            for (var c = 0; c < adContainerSelectors.length; c++) {
                var container = document.querySelector(adContainerSelectors[c]);
                if (container && isElementVisible(container)) {
                    isAdActive = true;
                    break;
                }
            }

            // 2. Click standard Skip Ad buttons if present and visible
            for (var s = 0; s < adSkipSelectors.length; s++) {
                var btn = document.querySelector(adSkipSelectors[s]);
                if (btn && isElementVisible(btn)) {
                    btn.click();
                    isAdActive = true;
                    if (Date.now() - lastSkipTime > 3000) {
                        lastSkipTime = Date.now();
                        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                            browser.runtime.sendMessage({
                                type: 'RECORD_BLOCK',
                                host: hostName + ' (Video Ad Skipped)',
                                category: 1
                            }).catch(function() {});
                        }
                    }
                    break;
                }
            }

            var media = getActiveMediaElement();
            if (isAdActive && media) {
                // Safely silence ad segment without breaking player lifecycle
                if (!media.muted) {
                    media.muted = true;
                    wasMutedByShield = true;
                }
            } else if (!isAdActive && media && wasMutedByShield) {
                // Restore audio when main content resumes
                media.muted = false;
                wasMutedByShield = false;
            }

            // 3. Handle modal overlays & anti-adblock blocking dialogs
            var antiAdblockModal = document.querySelector('ytd-enforcement-message-view-model, [class*="anti-adblock" i], [class*="adblock-overlay" i]');
            if (antiAdblockModal && isElementVisible(antiAdblockModal)) {
                var dismiss = antiAdblockModal.querySelector('button, [role="button"]');
                if (dismiss) {
                    dismiss.click();
                } else {
                    antiAdblockModal.remove();
                }
                if (media && media.paused) {
                    media.play().catch(function() {});
                }
            }
        } catch(e) {}
    }

    // Debounced MutationObserver on body for DOM changes
    var mediaDebounceTimer = null;
    var onMediaMutation = function() {
        if (mediaDebounceTimer) return;
        mediaDebounceTimer = setTimeout(function() {
            mediaDebounceTimer = null;
            handleGenericMediaState();
        }, 150);
    };

    var bodyObserver = null;
    function attachMediaObserver() {
        if (bodyObserver) {
            bodyObserver.disconnect();
        }
        var target = document.body || document.documentElement;
        if (target) {
            bodyObserver = new MutationObserver(onMediaMutation);
            bodyObserver.observe(target, { childList: true, subtree: true, attributes: true, attributeFilter: ['class'] });
            handleGenericMediaState();
        } else {
            setTimeout(attachMediaObserver, 300);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', attachMediaObserver);
    } else {
        attachMediaObserver();
    }

    // Handle Single-Page Application (SPA) client-side routing transitions
    ['popstate', 'hashchange'].forEach(function(evt) {
        window.addEventListener(evt, function() {
            setTimeout(handleGenericMediaState, 200);
        }, true);
    });

    // =========================================================================
    // 5. W3C Web App Manifest & PWA Scanner
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

