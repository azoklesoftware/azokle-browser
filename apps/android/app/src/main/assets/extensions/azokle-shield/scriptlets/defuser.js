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
            if (document.body) {
                document.body.style.overflow = 'auto';
                document.body.style.position = 'static';
            }
            if (document.documentElement) {
                document.documentElement.style.overflow = 'auto';
            }
        } catch(e) {}
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', unfreezePage, { once: true });
    } else {
        unfreezePage();
    }
})();
