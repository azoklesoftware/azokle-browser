use crate::error::ProtocolError;
use crate::version::ProtocolVersion;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum MessagePayload {
    Command(CommandPayload),
    Request(RequestPayload),
    Response(ResponsePayload),
    Event(EventPayload),
    Error(ProtocolError),
}

// -----------------------------------------------------------------------------
// 1. Commands (State Mutating Operations)
// -----------------------------------------------------------------------------
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub enum CommandPayload {
    // Tabs Domain
    TabOpen {
        url: String,
        title: String,
        is_private: bool,
        is_tor: bool,
        group_id: Option<String>,
        group_title: Option<String>,
    },
    TabClose {
        tab_id: String,
    },
    TabSetActive {
        tab_id: String,
    },
    TabUpdate {
        tab_id: String,
        url: String,
        title: String,
    },
    TabRestoreRecentlyClosed,
    TabCloseAll {
        include_private: bool,
    },
    TabAutoGroupByDomain,

    // Permissions Domain
    PermissionSet {
        origin: String,
        permission_type: String,
        state: String,
        is_private: bool,
    },
    PermissionClearOrigin {
        origin: String,
    },
    PermissionClearAll,

    // Tor & Privacy Domain
    TorStart {
        socks_port: u16,
        control_port: u16,
    },
    TorCircuitRotate {
        control_port: u16,
        auth_cookie: String,
    },
    TorSetSocksPort {
        port: u16,
    },

    // History Domain
    HistoryRecordVisit {
        title: String,
        url: String,
        is_private: bool,
    },
    HistoryDeleteEntry {
        id: String,
    },
    HistoryClearRange {
        from_ms: u64,
        to_ms: u64,
    },
    HistoryClearAll,

    // Bookmarks Domain
    BookmarkAdd {
        title: String,
        url: String,
        parent_folder: Option<String>,
        favicon_url: Option<String>,
    },
    BookmarkRemove {
        id: String,
    },

    // Settings Domain
    SettingsSetSearchEngine {
        engine: String,
    },
    SettingsAddRecentSearch {
        query: String,
    },
    SettingsClearRecentSearches,
}

// -----------------------------------------------------------------------------
// 2. Requests (Queries Expecting Response)
// -----------------------------------------------------------------------------
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub enum RequestPayload {
    CapabilityNegotiate {
        client_version: ProtocolVersion,
        features: Vec<String>,
    },
    AutosuggestQuery {
        query: String,
    },
    SearchQuery {
        query: String,
        page: u32,
    },
    PermissionQuery {
        origin: String,
        permission_type: String,
        is_private: bool,
    },
    DnsResolve {
        host: String,
    },
    TabGetActive,
    TabGetAll,
    HistoryGetRecent {
        limit: usize,
        offset: usize,
    },
    HistorySearch {
        query: String,
        limit: usize,
    },
    BookmarkGetAll,
    BookmarkSearch {
        query: String,
    },
}

// -----------------------------------------------------------------------------
// 3. Responses (Results of Requests)
// -----------------------------------------------------------------------------
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum ResponsePayload {
    CapabilityNegotiate {
        server_version: ProtocolVersion,
        supported_features: Vec<String>,
    },
    Autosuggest {
        query: String,
        suggestions: Vec<String>,
    },
    Search {
        query: String,
        page: u32,
        total_results: u64,
        search_time_secs: f64,
        results_json: String,
    },
    Permission {
        origin: String,
        permission_type: String,
        state: String,
    },
    DnsResolve {
        host: String,
        ips: Vec<String>,
    },
    TabSessionJson(String),
    TabListJson(String),
    HistoryListJson(String),
    BookmarkListJson(String),
    Success {
        action: String,
    },
}

// -----------------------------------------------------------------------------
// 4. Events (Asynchronous Broadcasts)
// -----------------------------------------------------------------------------
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub enum EventPayload {
    TabStateChanged {
        tab_id: String,
        url: String,
        title: String,
        is_loading: bool,
        progress: u8,
    },
    TorStatusChanged {
        status: String,
        exit_ip: Option<String>,
    },
    SecurityAlert {
        alert_type: String,
        blocked_target: String,
        reason: String,
    },
    ShieldBlocked {
        category: String,
        host: String,
        rule: Option<String>,
    },
    DownloadProgress {
        download_id: String,
        downloaded_bytes: u64,
        total_bytes: u64,
        state: String,
    },
}
