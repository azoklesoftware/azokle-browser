// Azokle Shield Neutral Surrogate: Google Tag (gtag.js)
(function() {
    'use strict';
    window.dataLayer = window.dataLayer || [];
    var gtag = function() {
        window.dataLayer.push(arguments);
        var len = arguments.length;
        if (len >= 3 && typeof arguments[2] === 'object' && arguments[2] !== null) {
            var cb = arguments[2].event_callback;
            if (typeof cb === 'function') {
                try { cb(); } catch(e) {}
            }
        }
    };
    window.gtag = window.gtag || gtag;
})();
