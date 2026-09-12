// Azokle Shield Neutral Surrogate: Meta / Facebook Pixel (fbevents.js)
(function() {
    'use strict';
    var noop = function() {};
    var fbq = function() {
        if (arguments[0] === 'track' || arguments[0] === 'trackCustom') {
            return;
        }
    };
    fbq.push = fbq;
    fbq.loaded = true;
    fbq.version = '2.0';
    fbq.queue = [];
    fbq.callMethod = noop;
    window.fbq = fbq;
    window._fbq = fbq;
})();
