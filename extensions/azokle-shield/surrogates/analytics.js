// Azokle Shield Neutral Surrogate: Google Analytics (Universal Analytics)
(function() {
    'use strict';
    var noop = function() {};
    var ga = function() {
        var len = arguments.length;
        if (len === 0) return;
        var last = arguments[len - 1];
        if (typeof last === 'function') {
            try { last(); } catch(e) {}
        }
    };
    ga.l = Date.now();
    ga.q = [];
    ga.create = function() { return { get: noop, set: noop, send: noop }; };
    ga.getByName = function() { return { get: noop, set: noop, send: noop }; };
    ga.getAll = function() { return []; };
    ga.remove = noop;
    ga.loaded = true;
    window.ga = ga;
    window.GoogleAnalyticsObject = 'ga';
})();
