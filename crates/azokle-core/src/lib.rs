pub use azokle_common::*;
pub use azokle_adblock::*;
pub use azokle_tracker_block::*;
pub use azokle_crypto::*;
pub use azokle_security::*;
pub use azokle_tor::*;
pub use azokle_permissions::*;
pub use azokle_history::*;
pub use azokle_bookmarks::*;
pub use azokle_downloads::*;
pub use azokle_storage::*;
pub use azokle_networking::*;
pub use azokle_tabs::*;
pub use azokle_protocol::{
    CommandPayload, EventPayload, MessageEndpoint, MessagePayload, ProtocolEnvelope, ProtocolError,
    ProtocolVersion, RequestPayload, ResponsePayload,
};

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
    pub tor: Arc<TorController>,
    pub permissions: Arc<PermissionsManager>,
    pub history: Arc<HistoryManager>,
    pub bookmarks: Arc<BookmarkManager>,
    pub downloads: Arc<DownloadManager>,
    pub settings: Arc<SettingsManager>,
    pub networking: Arc<SovereignNetworkingEngine>,
    pub tabs: Arc<TabManager>,
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
            tor: Arc::new(TorController::new()),
            permissions: Arc::new(PermissionsManager::new()),
            history: Arc::new(HistoryManager::new()),
            bookmarks: Arc::new(BookmarkManager::new()),
            downloads: Arc::new(DownloadManager::new()),
            settings: Arc::new(SettingsManager::new()),
            networking: Arc::new(SovereignNetworkingEngine::new()),
            tabs: Arc::new(TabManager::new()),
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

    /// Dispatches an incoming universal protocol message and returns the response envelope.
    pub fn dispatch_protocol(&self, envelope: ProtocolEnvelope) -> ProtocolEnvelope {
        let correlation = envelope.correlation_id.clone().unwrap_or_else(|| envelope.message_id.clone());
        let source = envelope.source;

        let response_payload = match envelope.payload {
            MessagePayload::Command(cmd) => match cmd {
                CommandPayload::TabOpen { url, title, is_private, is_tor, group_id, group_title } => {
                    let tab = self.tabs.open_tab(&url, &title, is_private, is_tor, group_id.as_deref(), group_title.as_deref());
                    let json = serde_json::to_string(&tab).unwrap_or_default();
                    MessagePayload::Response(ResponsePayload::TabSessionJson(json))
                }
                CommandPayload::TabClose { tab_id } => {
                    let tab = self.tabs.close_tab(&tab_id);
                    let json = serde_json::to_string(&tab).unwrap_or_default();
                    MessagePayload::Response(ResponsePayload::TabSessionJson(json))
                }
                CommandPayload::TabSetActive { tab_id } => {
                    let ok = self.tabs.set_active_tab(&tab_id);
                    MessagePayload::Response(ResponsePayload::Success { action: format!("setActive: {}", ok) })
                }
                CommandPayload::TabRestoreRecentlyClosed => {
                    let tab = self.tabs.restore_recently_closed();
                    let json = serde_json::to_string(&tab).unwrap_or_default();
                    MessagePayload::Response(ResponsePayload::TabSessionJson(json))
                }
                CommandPayload::PermissionSet { origin, permission_type, state, is_private } => {
                    if let Some(pt) = PermissionType::from_str_name(&permission_type) {
                        let st = PermissionState::from_str_name(&state);
                        self.permissions.set_permission(&origin, pt, st, is_private);
                    }
                    MessagePayload::Response(ResponsePayload::Success { action: "permissionSet".to_string() })
                }
                _ => MessagePayload::Response(ResponsePayload::Success { action: "commandExecuted".to_string() }),
            },
            MessagePayload::Request(req) => match req {
                RequestPayload::CapabilityNegotiate { client_version: _, features: _ } => {
                    MessagePayload::Response(ResponsePayload::CapabilityNegotiate {
                        server_version: ProtocolVersion::CURRENT,
                        supported_features: vec![
                            "tabs_v1".to_string(),
                            "permissions_v1".to_string(),
                            "history_v1".to_string(),
                            "tor_v3".to_string(),
                            "doh_rfc8484".to_string(),
                            "chacha20_vault".to_string(),
                        ],
                    })
                }
                RequestPayload::AutosuggestQuery { query } => {
                    let suggestions = self.networking.search_api.get_autosuggestions(&query).unwrap_or_default();
                    MessagePayload::Response(ResponsePayload::Autosuggest { query, suggestions })
                }
                RequestPayload::TabGetActive => {
                    let tab = self.tabs.get_active_tab();
                    let json = serde_json::to_string(&tab).unwrap_or_default();
                    MessagePayload::Response(ResponsePayload::TabSessionJson(json))
                }
                RequestPayload::TabGetAll => {
                    let tabs = self.tabs.get_all_tabs();
                    let json = serde_json::to_string(&tabs).unwrap_or_default();
                    MessagePayload::Response(ResponsePayload::TabListJson(json))
                }
                RequestPayload::DnsResolve { host } => {
                    let ips = self.networking.dns.resolve(&host).map(|ips| ips.into_iter().map(|ip| ip.to_string()).collect()).unwrap_or_default();
                    MessagePayload::Response(ResponsePayload::DnsResolve { host, ips })
                }
                _ => MessagePayload::Response(ResponsePayload::Success { action: "requestHandled".to_string() }),
            },
            MessagePayload::Error(err) => MessagePayload::Error(err),
            _ => MessagePayload::Response(ResponsePayload::Success { action: "noop".to_string() }),
        };

        ProtocolEnvelope::new(MessageEndpoint::RustCore, source, response_payload)
            .with_correlation(&correlation)
    }
}

impl Default for AzokleCoreEngine {
    fn default() -> Self {
        Self::new()
    }
}
