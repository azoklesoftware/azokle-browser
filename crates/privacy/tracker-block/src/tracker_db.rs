use std::collections::{HashMap, HashSet};

/// Database of known tracking domains and CNAME-cloaking unmasking mappings.
///
/// Many trackers bypass browser third-party cookie restrictions by instructing site owners
/// to configure DNS CNAME records pointing subdomains (e.g. `track.merchant.com`) to tracker
/// servers (`cname.segment.io`). `TrackerDb` maintains both the raw domain blacklist and
/// uncloaking mappings to expose and block CNAME-disguised tracking endpoints.
pub struct TrackerDb {
    known_tracker_domains: HashSet<String>,
    cname_cloaked_mappings: HashMap<String, String>,
}

impl Default for TrackerDb {
    fn default() -> Self {
        Self::new()
    }
}

impl TrackerDb {
    /// Initializes the database seeded with top global tracker networks and common CNAME cloaking targets.
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

        let mut cname_cloaked_mappings = HashMap::new();
        // Seed top CNAME cloaking targets from TDS
        cname_cloaked_mappings.insert("cname.segment.com".to_string(), "segment.com".to_string());
        cname_cloaked_mappings.insert("cname.segment.io".to_string(), "segment.io".to_string());
        cname_cloaked_mappings.insert("sc.omtrdc.net".to_string(), "omtrdc.net".to_string());
        cname_cloaked_mappings.insert("cname.branch.io".to_string(), "branch.io".to_string());
        cname_cloaked_mappings.insert("cname.appsflyer.com".to_string(), "appsflyer.com".to_string());

        Self {
            known_tracker_domains,
            cname_cloaked_mappings,
        }
    }

    /// Registers an uncloaking alias mapping a pseudo-first-party hostname to its actual tracking destination.
    pub fn add_cname_mapping(&mut self, cloaked_host: &str, uncloaked_host: &str) {
        self.cname_cloaked_mappings.insert(
            cloaked_host.to_lowercase(),
            uncloaked_host.to_lowercase(),
        );
    }

    /// Resolves the actual tracking hostname if the provided host is registered in the CNAME alias map.
    pub fn uncloak_host<'a>(&'a self, host: &'a str) -> &'a str {
        let clean = host.to_lowercase();
        if let Some(target) = self.cname_cloaked_mappings.get(&clean) {
            target.as_str()
        } else {
            host
        }
    }

    /// Ingests tracker domains from a line-delimited filter list (e.g. EasyPrivacy syntax).
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

    /// Evaluates whether a hostname belongs to a tracker domain or a CNAME-cloaked tracker alias.
    ///
    /// Checks exact hostname and parent domain segments against known trackers. If no match
    /// is found directly, checks whether the host is a known CNAME alias and re-evaluates
    /// the uncloaked target.
    pub fn is_tracker_domain(&self, host: &str) -> bool {
        let clean = host.to_lowercase();

        // 1. Direct check
        if self.check_domain_or_parents(&clean) {
            return true;
        }

        // 2. Uncloak CNAME if mapped and check uncloaked target
        if let Some(uncloaked) = self.cname_cloaked_mappings.get(&clean) {
            if self.check_domain_or_parents(uncloaked) {
                return true;
            }
        }

        false
    }

    /// Traverses the domain label hierarchy upwards (e.g. `a.b.c.com` -> `b.c.com` -> `c.com`)
    /// to determine whether any parent domain is registered as a tracker.
    fn check_domain_or_parents(&self, host: &str) -> bool {
        if self.known_tracker_domains.contains(host) {
            return true;
        }

        let parts: Vec<&str> = host.split('.').collect();
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
