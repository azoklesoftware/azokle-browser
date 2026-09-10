pub struct ScriptletEngine;

impl ScriptletEngine {
    pub fn get_defuser_scriptlet(domain: &str) -> Option<&'static str> {
        let clean = domain.to_lowercase();
        if clean.contains("youtube.com") {
            // YouTube ad-skip defuser
            Some(
                r#"
                (function() {
                    'use strict';
                    window._yt_ad_defuser = true;
                    // Neutralize ytInitialPlayerResponse adPlacements, playerAds, adSlots
                    const origParse = JSON.parse;
                    JSON.parse = function(text, reviver) {
                        const res = origParse.apply(this, arguments);
                        if (res && typeof res === 'object') {
                            if (res.adPlacements) res.adPlacements = [];
                            if (res.playerAds) res.playerAds = [];
                            if (res.adSlots) res.adSlots = [];
                            if (res.playerResponse && typeof res.playerResponse === 'object') {
                                if (res.playerResponse.adPlacements) res.playerResponse.adPlacements = [];
                                if (res.playerResponse.playerAds) res.playerResponse.playerAds = [];
                                if (res.playerResponse.adSlots) res.playerResponse.adSlots = [];
                            }
                            if (res.auxiliaryUi && res.auxiliaryUi.messageRenderers) delete res.auxiliaryUi;
                        }
                        return res;
                    };
                })();
                "#,
            )
        } else {
            None
        }
    }
}
