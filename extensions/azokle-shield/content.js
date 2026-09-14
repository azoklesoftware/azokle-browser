// Azokle Shield Content Engine
// High-Performance Declarative Cosmetic Filtering & Top-Level PWA Bridge

(function() {
    'use strict';

    if (window.__azokleShieldExtLoaded) return;
    window.__azokleShieldExtLoaded = true;

    var hostName = (window.location && window.location.hostname ? window.location.hostname : '').toLowerCase();

    // Sovereign & Benchmark exemption: Never inject on search engine, Cloudflare challenge, or benchmark/local pages
    if (!hostName ||
        hostName === 'azokle.com' || hostName.endsWith('.azokle.com') ||
        hostName === 'cloudflare.com' || hostName.endsWith('.cloudflare.com') ||
        hostName.includes('challenges.cloudflare.com') ||
        hostName.includes('browserbench.org') ||
        hostName === 'localhost' || hostName === '127.0.0.1') {
        return;
    }

    // =========================================================================
    // 2. Declarative Cosmetic CSS Injection (Cached & Non-Blocking)
    // Injected into both top-level and subframes to annihilate nested ad containers
    // =========================================================================
    var cosmeticApplied = false;
    function applyCosmeticStyles(css) {
        if (!css || typeof css !== 'string' || css.trim().length === 0 || cosmeticApplied) return;
        try {
            var existing = document.getElementById('azokle-shield-cosmetic');
            if (!existing) {
                var styleEl = document.createElement('style');
                styleEl.id = 'azokle-shield-cosmetic';
                styleEl.textContent = css;
                (document.head || document.documentElement || document).appendChild(styleEl);
                cosmeticApplied = true;
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
    // 3. W3C Web App Manifest & PWA Scanner (Single Top-Level Dispatch on Load)
    // =========================================================================
    if (window.self !== window.top) return;

    var manifestDispatched = false;
    var scanForManifest = function() {
        if (manifestDispatched) return;
        manifestDispatched = true;

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
                        if (fallbackIcon) notifyApp(null);
                    });
            } else if (fallbackIcon) {
                notifyApp(null);
            }
        } catch(e) {}
    };

    if (document.readyState === 'complete') {
        scanForManifest();
    } else {
        window.addEventListener('load', scanForManifest, { passive: true, once: true });
    }
})();
