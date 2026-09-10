use serde::{Deserialize, Serialize};

#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum BlockCategory {
    None = 0,
    Ad = 1,
    Tracker = 2,
    Telemetry = 3,
    Annoyance = 4,
    Fingerprinting = 5,
    Cryptomining = 6,
    Malware = 7,
}

impl BlockCategory {
    pub fn from_u8(val: u8) -> Self {
        match val {
            1 => BlockCategory::Ad,
            2 => BlockCategory::Tracker,
            3 => BlockCategory::Telemetry,
            4 => BlockCategory::Annoyance,
            5 => BlockCategory::Fingerprinting,
            6 => BlockCategory::Cryptomining,
            7 => BlockCategory::Malware,
            _ => BlockCategory::None,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FilterDecision {
    pub should_block: bool,
    pub category: BlockCategory,
    pub matched_rule: Option<String>,
    pub is_exception: bool,
}

pub struct SovereignWhitelist;

impl SovereignWhitelist {
    pub fn is_sovereign_domain(host: &str) -> bool {
        let clean = host.trim_end_matches('.').to_lowercase();
        clean == "azokle.com"
            || clean.ends_with(".azokle.com")
            || clean == "cloudflare.com"
            || clean.ends_with(".cloudflare.com")
            || clean.ends_with(".cloudflareinsights.com")
            || clean.ends_with(".hcaptcha.com")
            || clean.ends_with(".recaptcha.net")
    }
}
