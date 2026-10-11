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

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct TabId(pub u64);

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum TabState {
    Active,
    Suspended,
    Closed,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Tab {
    pub id: TabId,
    pub url: String,
    pub title: String,
    pub is_private: bool,
    pub is_tor: bool,
    pub state: TabState,
    pub created_at_ms: u64,
    pub last_accessed_ms: u64,
}

impl Tab {
    /// Creates a new instance with default configuration.
    pub fn new(id: TabId, url: String, is_private: bool, is_tor: bool, now_ms: u64) -> Self {
        Self {
            id,
            url: url.clone(),
            title: String::new(),
            is_private,
            is_tor,
            state: TabState::Active,
            created_at_ms: now_ms,
            last_accessed_ms: now_ms,
        }
    }

    /// Performs suspend operation.
    pub fn suspend(&mut self) {
        if self.state == TabState::Active {
            self.state = TabState::Suspended;
        }
    }

    /// Performs activate operation.
    pub fn activate(&mut self, now_ms: u64) {
        self.state = TabState::Active;
        self.last_accessed_ms = now_ms;
    }
}
