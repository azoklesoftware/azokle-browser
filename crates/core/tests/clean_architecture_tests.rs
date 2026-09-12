use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use azokle_core::*;

struct MockEnginePort {
    loaded_uris: Mutex<Vec<(TabId, String)>>,
}

impl MockEnginePort {
    fn new() -> Self {
        Self {
            loaded_uris: Mutex::new(Vec::new()),
        }
    }
}

impl EnginePort for MockEnginePort {
    fn load_uri(&self, tab_id: TabId, uri: &str) -> Result<(), String> {
        self.loaded_uris.lock().unwrap().push((tab_id, uri.to_string()));
        Ok(())
    }

    fn close_session(&self, _tab_id: TabId) -> Result<(), String> {
        Ok(())
    }

    fn reload(&self, _tab_id: TabId, _bypass_cache: bool) -> Result<(), String> {
        Ok(())
    }
}

struct MockShieldPort {
    should_block: AtomicBool,
}

impl ShieldPort for MockShieldPort {
    fn evaluate(&self, _request: &NavigationRequest) -> ShieldEvaluation {
        if self.should_block.load(Ordering::Relaxed) {
            ShieldEvaluation::Block {
                category: RuleCategory::Trackers,
                rule_source: "EasyPrivacy".to_string(),
            }
        } else {
            ShieldEvaluation::Allow
        }
    }

    fn strip_tracking_params(&self, url: &str) -> String {
        // Strip ?fbclid=...
        if let Some(idx) = url.find("?fbclid=") {
            url[..idx].to_string()
        } else {
            url.to_string()
        }
    }

    fn check_debounce_redirect(&self, url: &str) -> Option<String> {
        if url.contains("bounce-tracker.com?dest=") {
            let dest = url.split("dest=").nth(1)?;
            Some(dest.to_string())
        } else {
            None
        }
    }
}

struct MockStoragePort {
    saved_tabs: Mutex<Vec<Tab>>,
}

impl MockStoragePort {
    fn new() -> Self {
        Self {
            saved_tabs: Mutex::new(Vec::new()),
        }
    }
}

impl StoragePort for MockStoragePort {
    fn save_tab(&self, tab: &Tab) -> Result<(), String> {
        self.saved_tabs.lock().unwrap().push(tab.clone());
        Ok(())
    }

    fn delete_tab(&self, tab_id: u64) -> Result<(), String> {
        self.saved_tabs.lock().unwrap().retain(|t| t.id.0 != tab_id);
        Ok(())
    }

    fn load_all_tabs(&self) -> Result<Vec<Tab>, String> {
        Ok(self.saved_tabs.lock().unwrap().clone())
    }
}

#[test]
fn test_navigation_use_case_debounce_and_strip() {
    let mock_engine = Arc::new(MockEnginePort::new());
    let mock_shield = Arc::new(MockShieldPort {
        should_block: AtomicBool::new(false),
    });

    let use_case = NavigationUseCase::new(mock_shield, mock_engine.clone());

    let req = NavigationRequest {
        tab_id: TabId(1),
        target_url: "https://bounce-tracker.com?dest=https://example.com/page?fbclid=12345".to_string(),
        source_origin: None,
        request_type: "main_frame".to_string(),
        is_user_initiated: true,
    };

    let outcome = use_case.execute(req);

    match outcome {
        NavigationOutcome::Proceed { final_url } => {
            assert_eq!(final_url, "https://example.com/page");
        }
        _ => panic!("Expected navigation to proceed after bounce unwrapping and query stripping"),
    }

    let loaded = mock_engine.loaded_uris.lock().unwrap();
    assert_eq!(loaded.len(), 1);
    assert_eq!(loaded[0].1, "https://example.com/page");
}

#[test]
fn test_navigation_use_case_shield_block() {
    let mock_engine = Arc::new(MockEnginePort::new());
    let mock_shield = Arc::new(MockShieldPort {
        should_block: AtomicBool::new(true),
    });

    let use_case = NavigationUseCase::new(mock_shield, mock_engine.clone());

    let req = NavigationRequest {
        tab_id: TabId(2),
        target_url: "https://telemetry-tracker.com/track.js".to_string(),
        source_origin: Some("https://example.com".to_string()),
        request_type: "script".to_string(),
        is_user_initiated: false,
    };

    let outcome = use_case.execute(req);

    match outcome {
        NavigationOutcome::Blocked { reason } => {
            assert!(reason.contains("EasyPrivacy"));
        }
        _ => panic!("Expected navigation to be blocked by shield"),
    }

    let loaded = mock_engine.loaded_uris.lock().unwrap();
    assert_eq!(loaded.len(), 0, "Blocked requests must never reach the engine port");
}

#[test]
fn test_tab_use_case_private_mode_isolation() {
    let storage = Arc::new(MockStoragePort::new());
    let use_case = TabUseCase::new(Some(storage.clone()));

    // Normal Tab should be saved
    let normal_tab = use_case.create_tab(TabId(101), "https://example.com".to_string(), false, false, 1000);
    assert_eq!(normal_tab.state, TabState::Active);
    assert_eq!(storage.saved_tabs.lock().unwrap().len(), 1);

    // Private Tab must NEVER be persisted
    let private_tab = use_case.create_tab(TabId(102), "https://secret.org".to_string(), true, false, 1001);
    assert!(private_tab.is_private);
    assert_eq!(storage.saved_tabs.lock().unwrap().len(), 1, "Private tab must not be persisted to storage port");

    // Tor Tab must NEVER be persisted
    let tor_tab = use_case.create_tab(TabId(103), "http://onionaddress.onion".to_string(), false, true, 1002);
    assert!(tor_tab.is_tor);
    assert_eq!(storage.saved_tabs.lock().unwrap().len(), 1, "Tor tab must not be persisted to storage port");
}
