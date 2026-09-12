# Integration Test Suite

This suite exercises end-to-end multi-crate and engine workflows:
1. **Navigation Security**: Tests URL sanitization, bounce tracking unwrapping, and engine request interception.
2. **Tab Lifecycle & Privacy Invariants**: Verifies that private and Tor browsing tabs maintain zero disk artifacts.
3. **Shield Evaluation**: Verifies that eTLD+1 boundary checks and surrogate defusers execute with sub-millisecond latencies.
