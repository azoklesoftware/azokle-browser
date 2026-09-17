// Prebid.js Neutral Surrogate Mock
(function() {
    'use strict';
    var dummyFn = function() {};
    var pb = window.pbjs || {};
    var que = pb.que || [];
    pb.que = [];
    pb.que.push = function(fn) {
        if (typeof fn === 'function') {
            try { fn(); } catch(e) {}
        }
        return 1;
    };

    pb.addAdUnits = dummyFn;
    pb.adUnits = [];
    pb.requestBids = function(obj) {
        if (obj && typeof obj.bidsBackHandler === 'function') {
            try { obj.bidsBackHandler(); } catch(e) {}
        }
    };
    pb.setTargetingForGPTAsync = dummyFn;
    pb.setTargetingForAst = dummyFn;
    pb.enableAnalytics = dummyFn;
    pb.setConfig = dummyFn;
    pb.getConfig = function() { return {}; };
    pb.getHighestCpmBids = function() { return []; };
    pb.getBidResponses = function() { return {}; };
    pb.getBidResponsesForAdUnitCode = function() { return { bids: [] }; };
    pb.onEvent = dummyFn;
    pb.offEvent = dummyFn;
    pb.libLoaded = true;

    window.pbjs = pb;

    while (que.length > 0) {
        var fn = que.shift();
        if (typeof fn === 'function') {
            try { fn(); } catch(e) {}
        }
    }
})();
