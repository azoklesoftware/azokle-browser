// Azokle Shield Sovereign Defuser Scriptlets
// Defuses anti-adblock walls, popup ad loops, and honeypot traps cleanly without breaking page layout

(function() {
    'use strict';

    if (window.__azokleDefuserLoaded) return;
    window.__azokleDefuserLoaded = true;

    // 1. Defuse common anti-adblock global indicators
    try {
        var dummyFn = function() {};
        var trueFn = function() { return true; };

        // Neutralize BlockAdBlock / FuckAdBlock honeypots
        window.canRunAds = true;
        window.isAdBlockActive = false;
        window.adblocker = false;

        // Stub out common ad tracking objects to prevent null pointer exceptions in site scripts
        if (!window.google_ad_client) {
            window.google_ad_client = 'ca-pub-0000000000000000';
        }

        // Prevent-BAB (BlockAdBlock) Defuser
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

    // 2. Hide common anti-adblock modal backdrops and re-enable scrolling if frozen
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

    // 3. YouTube Ad & Anti-Adblock Defuser
    try {
        if (location.hostname.includes('youtube.com')) {
            var defuseYouTube = function() {
                // Dismiss anti-adblock modal if displayed
                var dismissBtn = document.querySelector('ytd-enforcement-message-view-model button, yt-playability-error-supported-renderers button');
                if (dismissBtn) {
                    try { dismissBtn.click(); } catch(e) {}
                }
                var overlay = document.querySelector('tp-yt-iron-overlay-backdrop, ytd-popup-container');
                if (overlay && overlay.innerHTML && overlay.innerHTML.includes('ad blocker')) {
                    overlay.remove();
                }

                // Fast-forward and skip video ads
                var video = document.querySelector('video.html5-main-video');
                var adShowing = document.querySelector('.ad-showing, .ad-interrupting');
                if (video && adShowing) {
                    video.muted = true;
                    if (isFinite(video.duration) && video.duration > 0) {
                        video.currentTime = video.duration;
                    }
                    var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');
                    if (skipBtn) {
                        try { skipBtn.click(); } catch(e) {}
                    }
                }
            };
            setInterval(defuseYouTube, 500);
        }
    } catch(e) {}

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', unfreezePage, { once: true });
    } else {
        unfreezePage();
    }
})();
