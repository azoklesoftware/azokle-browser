// Chartbeat Neutral Surrogate Mock
(function() {
    'use strict';
    var dummyFn = function() {};
    window._sf_async_config = window._sf_async_config || {};
    window._sf_endpt = Date.now();
    window.pSUPERFLY = {
        virtualPage: dummyFn,
        activity: dummyFn
    };
})();
