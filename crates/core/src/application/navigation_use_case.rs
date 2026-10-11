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

use crate::domain::{NavigationOutcome, NavigationRequest, ShieldEvaluation};
use crate::ports::{EnginePort, ShieldPort};
use std::sync::Arc;

/// Orchestrates the sovereign navigation security pipeline prior to engine page loading.
///
/// Ensures all web navigations traverse a sequential three-stage defense:
/// 1. **Bounce-Tracking Mitigation**: Checks if target URL is a known redirector bounce and unwraps direct destination.
/// 2. **Query Stripping**: Removes known tracking query parameters (UTM, fbclid, etc.) from top-level URLs.
/// 3. **Shield Evaluation**: Evaluates domain rules and scriptlet defusers before dispatching to the engine port.
pub struct NavigationUseCase {
    shield_port: Arc<dyn ShieldPort>,
    engine_port: Arc<dyn EnginePort>,
}

impl NavigationUseCase {
    /// Creates a new navigation interactor bound to abstract Shield and Engine ports.
    pub fn new(shield_port: Arc<dyn ShieldPort>, engine_port: Arc<dyn EnginePort>) -> Self {
        Self {
            shield_port,
            engine_port,
        }
    }

    /// Executes the complete navigation defense pipeline for an incoming request.
    ///
    /// Returns `NavigationOutcome::Blocked` if disallowed by policy, or `NavigationOutcome::Proceed`
    /// with the sanitized final URL loaded into the target session.
    pub fn execute(&self, request: NavigationRequest) -> NavigationOutcome {
        // Step 1: Check bounce-tracking redirect bypass
        let effective_url = if let Some(direct_dest) = self
            .shield_port
            .check_debounce_redirect(&request.target_url)
        {
            direct_dest
        } else {
            request.target_url.clone()
        };

        // Step 2: Strip recognized tracking parameters
        let sanitized_url = self.shield_port.strip_tracking_params(&effective_url);

        // Step 3: Evaluate Shields
        let mut eval_request = request.clone();
        eval_request.target_url = sanitized_url.clone();

        match self.shield_port.evaluate(&eval_request) {
            ShieldEvaluation::Block {
                category,
                rule_source,
            } => NavigationOutcome::Blocked {
                reason: format!("Blocked category {:?} by {}", category, rule_source),
            },
            ShieldEvaluation::Redirect {
                destination_url, ..
            } => {
                let _ = self.engine_port.load_uri(request.tab_id, &destination_url);
                NavigationOutcome::Proceed {
                    final_url: destination_url,
                }
            }
            ShieldEvaluation::Allow => {
                if let Err(e) = self.engine_port.load_uri(request.tab_id, &sanitized_url) {
                    NavigationOutcome::Blocked { reason: e }
                } else {
                    NavigationOutcome::Proceed {
                        final_url: sanitized_url,
                    }
                }
            }
        }
    }
}
