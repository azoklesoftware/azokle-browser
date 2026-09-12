use azokle_adblock::AdBlockEngine;
use azokle_common::BlockCategory;

#[test]
fn test_declarative_third_party_and_first_party_isolation() {
    let engine = AdBlockEngine::new();

    let rules = r#"
        ||doubleclick.net^$third-party
        ||analytics.internal.com^$first-party
        ||ads.kick.com^
        @@||video.kick.com^
    "#;
    engine.load_rules_from_text(rules, BlockCategory::Ad);

    // 1. Third-party request to doubleclick.net from nytimes.com -> should block
    let d1 = engine.check_url("https://securepubads.doubleclick.net/gampad/ads", Some("https://nytimes.com"), true);
    assert!(d1.should_block, "Third-party doubleclick request should be blocked");

    // 2. First-party request to doubleclick.net from doubleclick.net -> should NOT block
    let d2 = engine.check_url("https://doubleclick.net/favicon.ico", Some("https://doubleclick.net"), false);
    assert!(!d2.should_block, "First-party request to doubleclick should NOT be blocked");

    // 3. First-party rule: analytics.internal.com from internal.com -> should block
    let d3 = engine.check_url("https://analytics.internal.com/collect", Some("https://internal.com"), false);
    assert!(d3.should_block, "First-party internal tracker should be blocked on same-site");

    // 4. First-party rule requested from unrelated external site -> should NOT block
    let d4 = engine.check_url("https://analytics.internal.com/collect", Some("https://other.com"), true);
    assert!(!d4.should_block, "First-party only rule should not trigger on third-party context");

    // 5. Video streaming exception on Kick
    let d5 = engine.check_url("https://video.kick.com/live/stream.m3u8", Some("https://kick.com"), false);
    assert!(!d5.should_block, "Video stream exception should allow playback");
    assert!(d5.is_exception);

    // 6. Ad on Kick should be blocked
    let d6 = engine.check_url("https://ads.kick.com/banner.png", Some("https://kick.com"), false);
    assert!(d6.should_block, "Ad on Kick should be blocked");
}

#[test]
fn test_multi_platform_video_compatibility_without_hardcoded_logic() {
    let engine = AdBlockEngine::new();

    // Declarative rules across YouTube, Twitch, Vimeo, Kick, Facebook
    let rules = r#"
        ||youtube.com/api/stats/ads^$third-party
        ||youtube.com/pagead/*
        @@||googlevideo.com/videoplayback^
        @@||*.googlevideo.com/videoplayback^
        ||twitch.tv/api/ad/*
        @@||usher.ttvnw.net/api/channel/hls/*
        @@||video-weaver.*.hls.ttvnw.net/*
        ||kick.com/api/v1/ads/*
        @@||stream.kick.com/*
        ||connect.facebook.net/en_US/fbevents.js$third-party
        ||pixel.facebook.com^$third-party
        @@||video.*.fbcdn.net/*
    "#;
    engine.load_rules_from_text(rules, BlockCategory::Ad);

    // YouTube: Ad stats request blocked
    let yt_ad = engine.check_url("https://www.youtube.com/pagead/lvz?test=1", Some("https://news.com"), true);
    assert!(yt_ad.should_block);

    // YouTube: Video playback CDN allowed via declarative exception
    let yt_media = engine.check_url("https://rr2---sn-vgqsrn7e.googlevideo.com/videoplayback?expire=123&sparams=id", Some("https://www.youtube.com"), true);
    assert!(!yt_media.should_block);
    assert!(yt_media.is_exception);

    // Twitch: Media chunk allowed
    let twitch_media = engine.check_url("https://video-weaver.sfo01.hls.ttvnw.net/v1/segment/123.ts", Some("https://www.twitch.tv"), true);
    assert!(!twitch_media.should_block);
    assert!(twitch_media.is_exception);

    // Twitch: Ad endpoint blocked
    let twitch_ad = engine.check_url("https://twitch.tv/api/ad/v1/fetch", Some("https://www.twitch.tv"), false);
    assert!(twitch_ad.should_block);

    // Kick: Video stream allowed
    let kick_stream = engine.check_url("https://stream.kick.com/hls/live/user_master.m3u8", Some("https://kick.com"), true);
    assert!(!kick_stream.should_block);
    assert!(kick_stream.is_exception);

    // Facebook: Pixel blocked on third-party news site
    let fb_pixel = engine.check_url("https://connect.facebook.net/en_US/fbevents.js", Some("https://shopping-site.com"), true);
    assert!(fb_pixel.should_block);

    // Facebook: Video CDN allowed
    let fb_video = engine.check_url("https://video.xx.fbcdn.net/v/t42.17977-2/123.mp4", Some("https://www.facebook.com"), true);
    assert!(!fb_video.should_block);
}

#[test]
fn test_exception_priority_overrides_broad_blocking() {
    let engine = AdBlockEngine::new();

    let rules = r#"
        ||tracker.com^
        @@||tracker.com/auth/login
    "#;
    engine.load_rules_from_text(rules, BlockCategory::Tracker);

    // Standard tracking request blocked
    let d1 = engine.check_url("https://tracker.com/collect?uid=123", Some("https://example.com"), true);
    assert!(d1.should_block);

    // Critical login authentication request allowed by exception
    let d2 = engine.check_url("https://tracker.com/auth/login?session=abc", Some("https://example.com"), true);
    assert!(!d2.should_block);
    assert!(d2.is_exception);
}

#[test]
fn test_fqdn_trailing_dot_and_case_normalization() {
    let engine = AdBlockEngine::new();
    let rules = "||adserver.com^";
    engine.load_rules_from_text(rules, BlockCategory::Ad);

    let d1 = engine.check_url("HTTPS://ADSERVER.COM/banner.js", None, true);
    assert!(d1.should_block);

    let d2 = engine.check_url("https://adserver.com./banner.js", None, true);
    assert!(d2.should_block);

    let d3 = engine.check_url("https://SUB.adserver.com/banner.js", None, true);
    assert!(d3.should_block);
}

#[test]
fn test_multilevel_etld_isolation_and_request_type_preservation() {
    let engine = AdBlockEngine::new();
    let lists = vec![
        ("||tracker.co.uk^$third-party".to_string(), BlockCategory::Tracker),
        ("||analytics.github.io^$script".to_string(), BlockCategory::Tracker),
    ];
    engine.load_filter_lists_bulk(lists, None);

    // 1. Cross-domain on .co.uk (e.g. news.co.uk loading tracker.co.uk)
    let d1 = engine.check_network_request(
        "https://tracker.co.uk/beacon.js",
        Some("https://news.co.uk"),
        Some("script"),
        true,
    );
    assert!(d1.should_block);

    // 2. Type-specific rule matching with $script
    let d2_script = engine.check_network_request(
        "https://analytics.github.io/sdk.js",
        Some("https://victim.github.io"),
        Some("script"),
        true,
    );
    assert!(d2_script.should_block);
}
