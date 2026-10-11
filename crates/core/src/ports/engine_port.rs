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

use crate::domain::TabId;

pub trait EnginePort: Send + Sync {
    /// Performs load uri operation.
    fn load_uri(&self, tab_id: TabId, uri: &str) -> Result<(), String>;
    /// Performs close session operation.
    fn close_session(&self, tab_id: TabId) -> Result<(), String>;
    /// Performs reload operation.
    fn reload(&self, tab_id: TabId, bypass_cache: bool) -> Result<(), String>;
}
