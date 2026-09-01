use std::collections::HashSet;
use url::{form_urlencoded, Url};

pub struct QueryStripper {
    tracking_params: HashSet<&'static str>,
}

impl Default for QueryStripper {
    fn default() -> Self {
        Self::new()
    }
}

impl QueryStripper {
    pub fn new() -> Self {
        let mut tracking_params = HashSet::new();
        // Google Tracking
        tracking_params.insert("gclid");
        tracking_params.insert("gclsrc");
        tracking_params.insert("dclid");
        tracking_params.insert("gbraid");
        tracking_params.insert("wbraid");
        tracking_params.insert("_ga");
        tracking_params.insert("_gl");

        // Meta / Facebook
        tracking_params.insert("fbclid");
        tracking_params.insert("fref");

        // Twitter / X
        tracking_params.insert("twclid");

        // Microsoft / Bing
        tracking_params.insert("msclkid");

        // Mailchimp / Marketing
        tracking_params.insert("mc_cid");
        tracking_params.insert("mc_eid");

        // Yandex
        tracking_params.insert("yclid");
        tracking_params.insert("ym_debug");

        // TikTok / Snapchat / Pinterest
        tracking_params.insert("ttclid");
        tracking_params.insert("sc_lid");
        tracking_params.insert("epik");

        // Generic Analytics & Campaigns
        tracking_params.insert("utm_source");
        tracking_params.insert("utm_medium");
        tracking_params.insert("utm_campaign");
        tracking_params.insert("utm_term");
        tracking_params.insert("utm_content");
        tracking_params.insert("utm_id");
        tracking_params.insert("utm_name");
        tracking_params.insert("utm_reader");
        tracking_params.insert("utm_brand");
        tracking_params.insert("utm_social-type");
        tracking_params.insert("igshid");

        Self { tracking_params }
    }

    pub fn strip_tracking_params(&self, raw_url: &str) -> String {
        let mut parsed = match Url::parse(raw_url) {
            Ok(u) => u,
            Err(_) => return raw_url.to_string(),
        };

        if parsed.query().is_none() {
            return raw_url.to_string();
        }

        let filtered_pairs: Vec<(String, String)> = parsed
            .query_pairs()
            .filter(|(k, _)| {
                let lower_k = k.to_lowercase();
                !self.tracking_params.contains(lower_k.as_str()) && !lower_k.starts_with("utm_")
            })
            .map(|(k, v)| (k.into_owned(), v.into_owned()))
            .collect();

        if filtered_pairs.is_empty() {
            parsed.set_query(None);
        } else {
            let mut serializer = form_urlencoded::Serializer::new(String::new());
            for (k, v) in filtered_pairs {
                serializer.append_pair(&k, &v);
            }
            let query_str = serializer.finish();
            parsed.set_query(Some(&query_str));
        }

        parsed.to_string()
    }
}
