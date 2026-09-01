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
                    // Neutralize ytInitialPlayerResponse adPlacements
                    const origParse = JSON.parse;
                    JSON.parse = function(text, reviver) {
                        const res = origParse.apply(this, arguments);
                        if (res && res.adPlacements) {
                            delete res.adPlacements;
                        }
                        if (res && res.playerAds) {
                            delete res.playerAds;
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
