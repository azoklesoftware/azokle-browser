use azokle_uniffi::{UniversalBrowserEngine, UniversalFilterAction};

#[test]
fn test_universal_browser_engine_lifecycle() {
    let engine = UniversalBrowserEngine::new();

    // 1. Check strip tracking params
    let cleaned = engine.strip_tracking_params("https://example.com/?utm_source=test&utm_medium=cpc".to_string());
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
