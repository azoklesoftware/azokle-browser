use std::collections::HashSet;
use url::{form_urlencoded, Url};

/// Strips known cross-site tracking parameters, ad click IDs, and telemetry queries from URLs.
///
/// Prevents cross-origin link-decoration tracking where user IDs are passed through query strings
/// to correlate identities across different domains.
pub struct QueryStripper {
    tracking_params: HashSet<&'static str>,
}

impl Default for QueryStripper {
    fn default() -> Self {
        Self::new()
    }
}

impl QueryStripper {
    /// Initializes the stripper with pre-seeded tracking parameter dictionaries spanning
    /// Google/DoubleClick, Meta/Instagram, Twitter/X, Bing, Yandex, Mailchimp, Hubspot,
    /// and standard UTM marketing campaign tags.
    pub fn new() -> Self {
        let mut tracking_params = HashSet::new();
        // Google & Ad Services Tracking
        tracking_params.insert("gclid");
        tracking_params.insert("gclsrc");
        tracking_params.insert("dclid");
        tracking_params.insert("gbraid");
        tracking_params.insert("wbraid");
        tracking_params.insert("_ga");
        tracking_params.insert("_gl");
        tracking_params.insert("srsltid");

        // Meta / Facebook / Instagram
        tracking_params.insert("fbclid");
        tracking_params.insert("fb_action_ids");
        tracking_params.insert("fb_comment_id");
        tracking_params.insert("fref");
        tracking_params.insert("igshid");
        tracking_params.insert("igsh");

        // Twitter / X
        tracking_params.insert("twclid");

        // Microsoft / Bing
        tracking_params.insert("msclkid");

        // Mailchimp & Marketing Platforms
        tracking_params.insert("mc_cid");
        tracking_params.insert("mc_eid");
        tracking_params.insert("ml_subscriber");
        tracking_params.insert("ml_subscriber_hash");
        tracking_params.insert("sfmc_activityid");
        tracking_params.insert("sfmc_id");
        tracking_params.insert("bbeml");
        tracking_params.insert("bsft_clkid");
        tracking_params.insert("bsft_uid");
        tracking_params.insert("ss_email_id");
        tracking_params.insert("vero_conv");
        tracking_params.insert("vero_id");
        tracking_params.insert("vgo_ee");

        // Hubspot Tracking
        tracking_params.insert("__hsfp");
        tracking_params.insert("__hssc");
        tracking_params.insert("__hstc");
        tracking_params.insert("__s");
        tracking_params.insert("_hsenc");
        tracking_params.insert("hsctatracking");

        // Yandex
        tracking_params.insert("yclid");
        tracking_params.insert("ym_debug");
        tracking_params.insert("ymclid");
        tracking_params.insert("ysclid");

        // TikTok / Snapchat / Pinterest / Branch
        tracking_params.insert("ttclid");
        tracking_params.insert("sc_lid");
        tracking_params.insert("epik");
        tracking_params.insert("_branch_match_id");
        tracking_params.insert("_branch_referrer");

        // Affiliate & Attribution Networks
        tracking_params.insert("irclickid");
        tracking_params.insert("rb_clickid");
        tracking_params.insert("unicorn_click_id");
        tracking_params.insert("wickedid");
        tracking_params.insert("syclid");
        tracking_params.insert("guce_referrer");
        tracking_params.insert("guce_referrer_sig");
        tracking_params.insert("at_recipient_id");
        tracking_params.insert("at_recipient_list");
        tracking_params.insert("et_rid");
        tracking_params.insert("pk_cid");
        tracking_params.insert("mtm_cid");
        tracking_params.insert("s_cid");
        tracking_params.insert("sc_customer");
        tracking_params.insert("sc_eh");
        tracking_params.insert("sc_uid");
        tracking_params.insert("sms_click");
        tracking_params.insert("sms_source");
        tracking_params.insert("sms_uph");
        tracking_params.insert("_openstat");
        tracking_params.insert("_bhlid");

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

        Self { tracking_params }
    }

    /// Strips known tracking parameters from a URL while preserving legitimate functional queries.
    ///
    /// Also applies platform-specific rules (e.g. removing YouTube `si` share tracking
    /// and Twitter `ref_src`/`ref_url` referral tracking). If all query parameters are removed,
    /// the trailing question mark is cleanly dropped.
    pub fn strip_tracking_params(&self, raw_url: &str) -> String {
        let mut parsed = match Url::parse(raw_url) {
            Ok(u) => u,
            Err(_) => return raw_url.to_string(),
        };

        if parsed.query().is_none() {
            return raw_url.to_string();
        }

        let host = parsed.host_str().unwrap_or("").to_lowercase();
        let is_youtube = host.contains("youtube.com") || host.contains("youtu.be");
        let is_twitter = host.contains("twitter.com") || host.contains("x.com");

        let filtered_pairs: Vec<(String, String)> = parsed
            .query_pairs()
            .filter(|(k, _)| {
                let lower_k = k.to_lowercase();
                if self.tracking_params.contains(lower_k.as_str()) || lower_k.starts_with("utm_") {
                    return false;
                }
                if is_youtube && lower_k == "si" {
                    return false;
                }
                if is_twitter && (lower_k == "ref_src" || lower_k == "ref_url") {
                    return false;
                }
                true
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
