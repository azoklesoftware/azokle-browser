/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
