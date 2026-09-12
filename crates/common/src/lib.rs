use serde::{Deserialize, Serialize};

#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
pub enum BlockCategory {
    #[default]
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

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct FilterDecision {
    pub should_block: bool,
    pub category: BlockCategory,
    pub matched_rule: Option<String>,
    pub is_exception: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub redirect_url: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub rewritten_url: Option<String>,
}

impl FilterDecision {
    pub fn allow() -> Self {
        Self::default()
    }

    pub fn block(category: BlockCategory, matched_rule: Option<String>) -> Self {
        Self {
            should_block: true,
            category,
            matched_rule,
            is_exception: false,
            redirect_url: None,
            rewritten_url: None,
        }
    }

    pub fn exception(matched_rule: Option<String>) -> Self {
        Self {
            should_block: false,
            category: BlockCategory::None,
            matched_rule,
            is_exception: true,
            redirect_url: None,
            rewritten_url: None,
        }
    }
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
