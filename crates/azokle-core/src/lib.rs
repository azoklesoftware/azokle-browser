pub use azokle_common::*;
pub use azokle_adblock::*;
pub use azokle_tracker_block::*;
pub use azokle_crypto::*;
pub use azokle_security::*;

use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ShieldStatsSummary {
    pub total_lifetime_blocked: u64,
    pub ads_blocked: u64,
    pub trackers_blocked: u64,
    pub telemetry_blocked: u64,
    pub annoyances_blocked: u64,
    pub fingerprinting_blocked: u64,
    pub cryptominers_blocked: u64,
}

pub struct AzokleCoreEngine {
    pub adblock: Arc<AdBlockEngine>,
    pub trackers: Arc<TrackerEngine>,
    pub shield: Arc<ShieldEngine>,
    pub disabled_hosts: Arc<RwLock<HashSet<String>>>,
    pub stats_total: AtomicU64,
    pub stats_ads: AtomicU64,
    pub stats_trackers: AtomicU64,
    pub stats_telemetry: AtomicU64,
    pub stats_annoyances: AtomicU64,
    pub stats_fingerprinting: AtomicU64,
    pub stats_cryptominers: AtomicU64,
}

impl AzokleCoreEngine {
    pub fn new() -> Self {
        Self {
            adblock: Arc::new(AdBlockEngine::new()),
            trackers: Arc::new(TrackerEngine::new()),
            shield: Arc::new(ShieldEngine::new()),
            disabled_hosts: Arc::new(RwLock::new(HashSet::new())),
            stats_total: AtomicU64::new(0),
            stats_ads: AtomicU64::new(0),
            stats_trackers: AtomicU64::new(0),
            stats_telemetry: AtomicU64::new(0),
            stats_annoyances: AtomicU64::new(0),
            stats_fingerprinting: AtomicU64::new(0),
            stats_cryptominers: AtomicU64::new(0),
        }
    }

    pub fn get_stats(&self) -> ShieldStatsSummary {
        ShieldStatsSummary {
            total_lifetime_blocked: self.stats_total.load(Ordering::Relaxed),
            ads_blocked: self.stats_ads.load(Ordering::Relaxed),
            trackers_blocked: self.stats_trackers.load(Ordering::Relaxed),
            telemetry_blocked: self.stats_telemetry.load(Ordering::Relaxed),
            annoyances_blocked: self.stats_annoyances.load(Ordering::Relaxed),
            fingerprinting_blocked: self.stats_fingerprinting.load(Ordering::Relaxed),
            cryptominers_blocked: self.stats_cryptominers.load(Ordering::Relaxed),
        }
    }
}

impl Default for AzokleCoreEngine {
    fn default() -> Self {
        Self::new()
    }
}
