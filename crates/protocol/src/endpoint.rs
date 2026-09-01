use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum MessageEndpoint {
    RustCore,
    GeckoEngine,
    AndroidUI,
    IosUI,
    DesktopUI,
    ExtensionBridge,
}

impl MessageEndpoint {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::RustCore => "rust_core",
            Self::GeckoEngine => "gecko_engine",
            Self::AndroidUI => "android_ui",
            Self::IosUI => "ios_ui",
            Self::DesktopUI => "desktop_ui",
            Self::ExtensionBridge => "extension_bridge",
        }
    }
}
