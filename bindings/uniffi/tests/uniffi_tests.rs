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

use azokle_uniffi::{UniversalBrowserEngine, UniversalFilterAction};

#[test]
fn test_universal_browser_engine_lifecycle() {
    let engine = UniversalBrowserEngine::new();

    // 1. Check strip tracking params
    let cleaned = engine
        .strip_tracking_params("https://example.com/?utm_source=test&utm_medium=cpc".to_string());
    assert!(!cleaned.contains("utm_source"));

    // 2. Check stats retrieval
    let stats = engine.get_stats();
    assert_eq!(stats.total_blocked, 0);

    // 3. Test Host shield toggling
    engine.set_host_shield_enabled("example.com".to_string(), false);
    let decision = engine.should_block(
        "https://example.com/script.js".to_string(),
        Some("https://example.com".to_string()),
        Some("script".to_string()),
        false,
    );
    assert_eq!(decision.action, UniversalFilterAction::Allow);
}
