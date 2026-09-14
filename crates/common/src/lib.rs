use serde::{Deserialize, Serialize};

#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
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

impl From<BlockCategory> for u8 {
    fn from(cat: BlockCategory) -> Self {
        cat as u8
    }
}

impl From<u8> for BlockCategory {
    fn from(val: u8) -> Self {
        Self::from_u8(val)
    }
}

impl Serialize for BlockCategory {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_u8(*self as u8)
    }
}

impl<'de> Deserialize<'de> for BlockCategory {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        struct Visitor;
        impl<'de> serde::de::Visitor<'de> for Visitor {
            type Value = BlockCategory;
            fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
                formatter.write_str("an integer (0-7) or category string")
            }
            fn visit_u64<E>(self, v: u64) -> Result<Self::Value, E>
            where
                E: serde::de::Error,
            {
                Ok(BlockCategory::from_u8(v as u8))
            }
            fn visit_i64<E>(self, v: i64) -> Result<Self::Value, E>
            where
                E: serde::de::Error,
            {
                Ok(BlockCategory::from_u8(v as u8))
            }
            fn visit_str<E>(self, v: &str) -> Result<Self::Value, E>
            where
                E: serde::de::Error,
            {
                match v.to_ascii_lowercase().as_str() {
                    "ad" => Ok(BlockCategory::Ad),
                    "tracker" => Ok(BlockCategory::Tracker),
                    "telemetry" => Ok(BlockCategory::Telemetry),
                    "annoyance" => Ok(BlockCategory::Annoyance),
                    "fingerprinting" => Ok(BlockCategory::Fingerprinting),
                    "cryptomining" => Ok(BlockCategory::Cryptomining),
                    "malware" => Ok(BlockCategory::Malware),
                    _ => Ok(BlockCategory::None),
                }
            }
        }
        deserializer.deserialize_any(Visitor)
    }
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
    #[serde(default)]
    pub category: BlockCategory,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub matched_rule: Option<String>,
    #[serde(default)]
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_block_category_numeric_serialization() {
        let decision = FilterDecision::block(BlockCategory::Ad, Some("||doubleclick.net^".to_string()));
        let json = serde_json::to_string(&decision).unwrap();
        assert!(json.contains("\"category\":1"), "Expected category:1 in {}", json);

        // Deserializing from numeric
        let parsed: FilterDecision = serde_json::from_str(&json).unwrap();
        assert_eq!(parsed.category, BlockCategory::Ad);

        // Deserializing from string
        let str_json = r#"{"should_block":true,"category":"Tracker"}"#;
        let parsed_str: FilterDecision = serde_json::from_str(str_json).unwrap();
        assert_eq!(parsed_str.category, BlockCategory::Tracker);
    }
}
