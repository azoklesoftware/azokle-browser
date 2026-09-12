// Azokle Shield Neutral Surrogate: Matomo / Piwik (matomo.js)
(function() {
    'use strict';
    window._paq = window._paq || [];
    var noop = function() {};
    var tracker = {
        trackEvent: noop,
        trackPageView: noop,
        trackSiteSearch: noop,
        trackGoal: noop,
        trackLink: noop,
        enableLinkTracking: noop
    };
    window.Matomo = { getTracker: function() { return tracker; }, getAsyncTracker: function() { return tracker; } };
    window.Piwik = window.Matomo;
})();
