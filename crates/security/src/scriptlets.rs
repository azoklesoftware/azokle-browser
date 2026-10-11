// Copyright (c) 2026 The Azokle Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use parking_lot::RwLock;
use std::collections::HashMap;

pub struct ScriptletEngine {
    domain_scriptlets: RwLock<HashMap<String, String>>,
}

impl Default for ScriptletEngine {
    /// Returns the default instance.
    fn default() -> Self {
        Self::new()
    }
}

impl ScriptletEngine {
    /// Creates a new instance with default configuration.
    pub fn new() -> Self {
        Self {
            domain_scriptlets: RwLock::new(HashMap::new()),
        }
    }

    /// Performs register scriptlet operation.
    pub fn register_scriptlet(&self, domain: &str, script_content: &str) {
        let clean = domain.trim().to_lowercase();
        if !clean.is_empty() && !script_content.is_empty() {
            self.domain_scriptlets
                .write()
                .insert(clean, script_content.to_string());
        }
    }

    /// Returns the scriptlet for domain.
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
