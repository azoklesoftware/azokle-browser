uniffi::setup_scaffolding!();

use std::sync::Arc;
use std::sync::atomic::Ordering;
use azokle_core::AzokleCoreEngine;
use azokle_core::BlockCategory;

#[derive(uniffi::Enum, Debug, Clone, Copy, PartialEq, Eq)]
pub enum UniversalFilterAction {
    Allow,
    Block,
    Redirect,
}

#[derive(uniffi::Record, Debug, Clone)]
pub struct UniversalShieldStats {
    pub total_blocked: u64,
    pub ads_blocked: u64,
    pub trackers_blocked: u64,
    pub telemetry_blocked: u64,
    pub annoyances_blocked: u64,
    pub fingerprinting_blocked: u64,
    pub cryptominers_blocked: u64,
}

#[derive(uniffi::Record, Debug, Clone)]
pub struct UniversalFilterDecision {
    pub action: UniversalFilterAction,
    pub category: Option<String>,
    pub redirect_url: Option<String>,
    pub reason: String,
}

#[derive(uniffi::Object)]
pub struct UniversalBrowserEngine {
    core: Arc<AzokleCoreEngine>,
}

#[uniffi::export]
impl UniversalBrowserEngine {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            core: Arc::new(AzokleCoreEngine::new()),
        }
    }

    pub fn should_block(
        &self,
        url: String,
        source_origin: Option<String>,
        _request_type: Option<String>,
        is_third_party: bool,
    ) -> UniversalFilterDecision {
        // Check if disabled for source origin
        if let Some(ref src) = source_origin {
            if let Ok(src_url) = url::Url::parse(src) {
                if let Some(src_host) = src_url.host_str() {
                    if self.core.disabled_hosts.read().contains(&src_host.to_lowercase()) {
                        return UniversalFilterDecision {
                            action: UniversalFilterAction::Allow,
                            category: None,
                            redirect_url: None,
                            reason: "Origin Shield Disabled".to_string(),
                        };
                    }
                }
            }
        }

        let decision = self.core.adblock.check_url(
            &url,
            source_origin.as_deref(),
            is_third_party,
        );

        if decision.should_block {
            self.core.stats_total.fetch_add(1, Ordering::Relaxed);
            match decision.category {
                BlockCategory::Ad => { self.core.stats_ads.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Tracker => { self.core.stats_trackers.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Telemetry => { self.core.stats_telemetry.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Fingerprinting => { self.core.stats_fingerprinting.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Annoyance => { self.core.stats_annoyances.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Cryptomining => { self.core.stats_cryptominers.fetch_add(1, Ordering::Relaxed); }
                _ => {}
            }

            UniversalFilterDecision {
                action: UniversalFilterAction::Block,
                category: Some(format!("{:?}", decision.category)),
                redirect_url: None,
                reason: decision.matched_rule.unwrap_or_else(|| "Blocked by Shield".to_string()),
            }
        } else {
            UniversalFilterDecision {
                action: UniversalFilterAction::Allow,
                category: None,
                redirect_url: None,
                reason: "Allowed".to_string(),
            }
        }
    }

    pub fn strip_tracking_params(&self, url: String) -> String {
        self.core.trackers.strip_tracking_params(&url)
    }

    pub fn check_debounce(&self, url: String) -> Option<String> {
        self.core.trackers.unwrap_bounce_url(&url)
    }

    pub fn get_stats(&self) -> UniversalShieldStats {
        let stats = self.core.get_stats();
        UniversalShieldStats {
            total_blocked: stats.total_lifetime_blocked,
            ads_blocked: stats.ads_blocked,
            trackers_blocked: stats.trackers_blocked,
            telemetry_blocked: stats.telemetry_blocked,
            annoyances_blocked: stats.annoyances_blocked,
            fingerprinting_blocked: stats.fingerprinting_blocked,
            cryptominers_blocked: stats.cryptominers_blocked,
        }
    }

    pub fn set_host_shield_enabled(&self, host: String, enabled: bool) {
        if enabled {
            self.core.disabled_hosts.write().remove(&host.to_lowercase());
        } else {
            self.core.disabled_hosts.write().insert(host.to_lowercase());
        }
    }
}
