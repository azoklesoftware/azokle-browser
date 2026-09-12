use crate::domain::{NavigationRequest, ShieldEvaluation};

pub trait ShieldPort: Send + Sync {
    fn evaluate(&self, request: &NavigationRequest) -> ShieldEvaluation;
    fn strip_tracking_params(&self, url: &str) -> String;
    fn check_debounce_redirect(&self, url: &str) -> Option<String>;
}
