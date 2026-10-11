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

use crate::domain::Tab;

pub trait StoragePort: Send + Sync {
    /// Performs save tab operation.
    fn save_tab(&self, tab: &Tab) -> Result<(), String>;
    /// Removes the specified tab.
    fn delete_tab(&self, tab_id: u64) -> Result<(), String>;
    /// Performs load all tabs operation.
    fn load_all_tabs(&self) -> Result<Vec<Tab>, String>;
}
