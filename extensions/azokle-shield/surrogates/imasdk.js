// Google IMA SDK Neutral Surrogate Mock for Video Players
(function() {
    'use strict';
    var dummyFn = function() {};
    var dummyClass = function() {
        return {
            addEventListener: dummyFn,
            removeEventListener: dummyFn,
            start: dummyFn,
            stop: dummyFn,
            destroy: dummyFn,
            init: dummyFn,
            requestAds: function() {
                var self = this;
                setTimeout(function() {
                    if (self._listeners && self._listeners['adsManagerLoaded']) {
                        self._listeners['adsManagerLoaded']({
                            getAdsManager: function() {
                                return {
                                    init: dummyFn,
                                    start: dummyFn,
                                    stop: dummyFn,
                                    destroy: dummyFn,
                                    addEventListener: dummyFn,
                                    removeEventListener: dummyFn,
                                    resize: dummyFn,
                                    collapse: dummyFn
                                };
                            }
                        });
                    }
                }, 10);
            }
        };
    };

    window.google = window.google || {};
    window.google.ima = {
        AdDisplayContainer: dummyClass,
        AdsLoader: dummyClass,
        AdsManager: dummyClass,
        AdsRequest: dummyClass,
        AdsRenderingSettings: dummyClass,
        ImaSdkSettings: {
            getInstance: function() {
                return {
                    setAutoPlayAdBreaks: dummyFn,
                    setVpaidMode: dummyFn,
                    setLocale: dummyFn,
                    setNumRedirects: dummyFn,
                    setPlayerType: dummyFn,
                    setPlayerVersion: dummyFn
                };
            }
        },
        AdErrorEvent: { Type: { AD_ERROR: 'adError' } },
        AdsManagerLoadedEvent: { Type: { ADS_MANAGER_LOADED: 'adsManagerLoaded' } },
        AdEvent: { Type: { ALL_ADS_COMPLETED: 'allAdsCompleted', COMPLETE: 'complete', CONTENT_PAUSE_REQUESTED: 'contentPauseRequested', CONTENT_RESUME_REQUESTED: 'contentResumeRequested' } },
        ViewMode: { NORMAL: 0, FULLSCREEN: 1 },
        settings: {}
    };
})();
