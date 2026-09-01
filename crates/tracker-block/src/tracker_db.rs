use std::collections::HashSet;

pub struct TrackerDb {
    known_tracker_domains: HashSet<String>,
}

impl Default for TrackerDb {
    fn default() -> Self {
        Self::new()
    }
}

impl TrackerDb {
    pub fn new() -> Self {
        let mut known_tracker_domains = HashSet::new();
        // Seed top tracker networks
        let seed_domains = [
            "google-analytics.com", "ssl.google-analytics.com", "analytics.google.com",
            "googletagmanager.com", "googletagservices.com", "tagmanager.google.com",
            "pixel.facebook.com", "an.facebook.com", "connect.facebook.net", "analytics.facebook.com",
            "tr.snapchat.com", "analytics.tiktok.com", "ads.tiktok.com", "ads-twitter.com",
            "analytics.twitter.com", "t.co", "ads.linkedin.com", "snap.licdn.com",
            "scorecardresearch.com", "pixel.quantserve.com", "quantcast.com", "moatads.com",
            "clarity.ms", "c.bing.com", "bat.bing.com", "mc.yandex.ru", "hotjar.com",
            "static.hotjar.com", "script.hotjar.com", "mouseflow.com", "crazyegg.com",
            "fullstory.com", "segment.io", "segment.com", "mixpanel.com", "amplitude.com",
            "heapanalytics.com", "branch.io", "appsflyer.com", "adjust.com", "kochava.com",
            "singular.net", "logrocket.com", "chartbeat.com", "chartbeat.net", "parsely.com",
            "optimizely.com", "statcounter.com", "sentry.io", "browser.sentry-cdn.com",
            "telemetry.mozilla.org", "newrelic.com", "datadoghq.com", "bugsnag.com",
        ];

        for d in seed_domains {
            known_tracker_domains.insert(d.to_string());
        }

        Self { known_tracker_domains }
    }

    pub fn load_from_text(&mut self, content: &str) -> usize {
        let mut count = 0;
        for line in content.lines() {
            let trimmed = line.trim();
            if trimmed.is_empty() || trimmed.starts_with('#') || trimmed.starts_with('!') {
                continue;
            }
            let host = trimmed
                .trim_start_matches("||")
                .trim_end_matches('^')
                .trim_end_matches('/')
                .to_lowercase();
            if !host.is_empty() {
                self.known_tracker_domains.insert(host);
                count += 1;
            }
        }
        count
    }

    pub fn is_tracker_domain(&self, host: &str) -> bool {
        let clean = host.to_lowercase();
        if self.known_tracker_domains.contains(&clean) {
            return true;
        }

        let parts: Vec<&str> = clean.split('.').collect();
        if parts.len() > 2 {
            for i in 1..parts.len() - 1 {
                let sub = parts[i..].join(".");
                if self.known_tracker_domains.contains(&sub) {
                    return true;
                }
            }
        }

        false
    }
}
