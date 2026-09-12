use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShieldMode {
    Aggressive,
    Standard,
    Disabled,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum RuleCategory {
    Ads,
    Trackers,
    Telemetry,
    Annoyances,
    Fingerprinting,
    CryptoMining,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShieldEvaluation {
    Allow,
    Block {
        category: RuleCategory,
        rule_source: String,
    },
    Redirect {
        destination_url: String,
        reason: String,
    },
}
