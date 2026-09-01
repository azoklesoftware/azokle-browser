use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum TorStatus {
    Disabled,
    Starting,
    Connected,
    CircuitRotating,
    Error,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TorConfig {
    pub socks_port: u16,
    pub control_port: u16,
    pub data_dir: String,
    pub bridges_enabled: bool,
}

impl Default for TorConfig {
    fn default() -> Self {
        Self {
            socks_port: 9050,
            control_port: 9051,
            data_dir: "~/.azokle/tor".to_string(),
            bridges_enabled: false,
        }
    }
}

#[derive(Clone)]
pub struct TorController {
    config: Arc<RwLock<TorConfig>>,
    status: Arc<RwLock<TorStatus>>,
}

impl Default for TorController {
    fn default() -> Self {
        Self::new()
    }
}

impl TorController {
    pub fn new() -> Self {
        Self {
            config: Arc::new(RwLock::new(TorConfig::default())),
            status: Arc::new(RwLock::new(TorStatus::Disabled)),
        }
    }

    pub fn is_onion_url(url: &str) -> bool {
        url.contains(".onion/") || url.ends_with(".onion")
    }

    pub fn get_status(&self) -> TorStatus {
        *self.status.read()
    }

    pub fn set_status(&self, new_status: TorStatus) {
        *self.status.write() = new_status;
    }

    pub fn get_socks_proxy(&self) -> String {
        format!("socks5://127.0.0.1:{}", self.config.read().socks_port)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_tor_controller() {
        let tor = TorController::new();
        assert_eq!(tor.get_status(), TorStatus::Disabled);
        tor.set_status(TorStatus::Connected);
        assert_eq!(tor.get_status(), TorStatus::Connected);
        assert!(TorController::is_onion_url("http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion"));
        assert!(!TorController::is_onion_url("https://search.azokle.com"));
    }
}
