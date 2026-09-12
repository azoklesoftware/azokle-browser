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

    // 3. YouTube Ad & Anti-Adblock Defuser (Conforms to GeckoView Browser Security Principles)
    try {
        if (location.hostname.includes('youtube.com')) {
            var handleYouTubePlayerState = function() {
                // Dismiss enforcement dialog without destroying polymer DOM trees
                var dismissBtn = document.querySelector('ytd-enforcement-message-view-model button, yt-playability-error-supported-renderers button');
                if (dismissBtn && typeof dismissBtn.click === 'function') {
                    try { dismissBtn.click(); } catch(e) {}
                }

                // If skip button is available, click it immediately
                var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-skip-button-slot button');
                if (skipBtn && typeof skipBtn.click === 'function') {
                    try { skipBtn.click(); } catch(e) {}
                }
            };

            // Event-driven observation on player containers rather than battery-draining polling
            var initObserver = function() {
                var player = document.getElementById('movie_player') || document.querySelector('.html5-video-player') || document.body;
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

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', unfreezePage, { once: true });
    } else {
        unfreezePage();
    }
})();
