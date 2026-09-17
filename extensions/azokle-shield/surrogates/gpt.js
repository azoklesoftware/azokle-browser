// Google Publisher Tag (GPT) Neutral Surrogate Mock
(function() {
    'use strict';
    var dummyFn = function() { return dummyObj; };
    var dummyObj = {
        addService: dummyFn,
        defineSizeMapping: dummyFn,
        build: dummyFn,
        setTargeting: dummyFn,
        clearTargeting: dummyFn,
        setCategoryExclusion: dummyFn,
        clearCategoryExclusions: dummyFn,
        setCentering: dummyFn,
        setPublisherProvidedId: dummyFn,
        enableSingleRequest: dummyFn,
        enableAsyncRendering: dummyFn,
        disableInitialLoad: dummyFn,
        collapseEmptyDivs: dummyFn,
        setPrivacySettings: dummyFn,
        setForceSafeFrame: dummyFn,
        setSafeFrameConfig: dummyFn,
        setLocation: dummyFn,
        updateCorrelator: dummyFn,
        display: dummyFn,
        refresh: dummyFn,
        clear: dummyFn,
        fillSlot: dummyFn,
        destroySlots: dummyFn,
        addEventListener: dummyFn,
        removeEventListener: dummyFn,
        renderEnded: dummyFn,
        get: function() { return null; },
        getAttributeKeys: function() { return []; },
        getTargeting: function() { return []; },
        getTargetingKeys: function() { return []; },
        getCategoryExclusions: function() { return []; },
        getServices: function() { return []; },
        getSlots: function() { return []; },
        getSlotId: function() { return { getId: function() { return ''; }, getDomId: function() { return ''; } }; }
    };

    var gpt = window.googletag || {};
    var cmdQueue = gpt.cmd || [];
    gpt.cmd = [];
    gpt.cmd.push = function(fn) {
        if (typeof fn === 'function') {
            try { fn(); } catch(e) {}
        }
        return 1;
    };

    gpt.apiReady = true;
    gpt.pubadsReady = true;
    gpt.defineSlot = function() { return dummyObj; };
    gpt.defineOutOfPageSlot = function() { return dummyObj; };
    gpt.defineUnit = function() { return dummyObj; };
    gpt.destroySlots = dummyFn;
    gpt.display = dummyFn;
    gpt.enableServices = dummyFn;
    gpt.getVersion = function() { return '1.0.0-azokle-surrogate'; };
    gpt.pubads = function() { return dummyObj; };
    gpt.companionAds = function() { return dummyObj; };
    gpt.sizeMapping = function() { return dummyObj; };
    gpt.content = function() { return dummyObj; };

    window.googletag = gpt;

    while (cmdQueue.length > 0) {
        var fn = cmdQueue.shift();
        if (typeof fn === 'function') {
            try { fn(); } catch(e) {}
        }
    }
})();
