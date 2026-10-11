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

use crate::domain::{NavigationRequest, ShieldEvaluation};

pub trait ShieldPort: Send + Sync {
    /// Performs evaluate operation.
    fn evaluate(&self, request: &NavigationRequest) -> ShieldEvaluation;
    /// Performs strip tracking params operation.
    fn strip_tracking_params(&self, url: &str) -> String;
    /// Validates debounce redirect against security and filtering rules.
    fn check_debounce_redirect(&self, url: &str) -> Option<String>;
}
