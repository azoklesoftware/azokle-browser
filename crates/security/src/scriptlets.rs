use parking_lot::RwLock;
use std::collections::HashMap;

pub struct ScriptletEngine {
    domain_scriptlets: RwLock<HashMap<String, String>>,
}

impl Default for ScriptletEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl ScriptletEngine {
    pub fn new() -> Self {
        Self {
            domain_scriptlets: RwLock::new(HashMap::new()),
        }
    }

    pub fn register_scriptlet(&self, domain: &str, script_content: &str) {
        let clean = domain.trim().to_lowercase();
        if !clean.is_empty() && !script_content.is_empty() {
            self.domain_scriptlets.write().insert(clean, script_content.to_string());
        }
    }

    pub fn get_scriptlet_for_domain(&self, domain: &str) -> Option<String> {
        let clean = domain.trim_end_matches('.').to_lowercase();
        let map = self.domain_scriptlets.read();
        if let Some(s) = map.get(&clean) {
            return Some(s.clone());
        }
        let parts: Vec<&str> = clean.split('.').collect();
        if parts.len() > 2 {
            for i in 1..parts.len() - 1 {
                let sub = parts[i..].join(".");
                if let Some(s) = map.get(&sub) {
                    return Some(s.clone());
                }
            }
        }
        None
    }
}

