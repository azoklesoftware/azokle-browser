use azokle_adblock::matcher::RuleMatcher;
use azokle_adblock::rules::NetworkRule;
use azokle_adblock::AdBlockEngine;
use azokle_common::BlockCategory;

#[test]
fn test_declarative_third_party_and_first_party_isolation() {
    let mut matcher = RuleMatcher::new();

    // Ad rules: doubleclick is 3p only; internal-tracker is 1p only
    let r1 = NetworkRule::parse("||doubleclick.net^$third-party", BlockCategory::Ad).unwrap();
    let r2 = NetworkRule::parse("||analytics.internal.com^$first-party", BlockCategory::Tracker).unwrap();
    let r3 = NetworkRule::parse("||ads.kick.com^", BlockCategory::Ad).unwrap();
    let r4 = NetworkRule::parse("@@||video.kick.com^", BlockCategory::None).unwrap();

    matcher.add_rule(r1);
    matcher.add_rule(r2);
    matcher.add_rule(r3);
    matcher.add_rule(r4);
    matcher.rebuild_indexes();

    // 1. Third-party request to doubleclick.net from nytimes.com -> should block
    let d1 = matcher.check_url("https://securepubads.doubleclick.net/gampad/ads", Some("https://nytimes.com"), true);
    assert!(d1.should_block, "Third-party doubleclick request should be blocked");
    assert_eq!(d1.category, BlockCategory::Ad);

    // 2. First-party request to doubleclick.net from doubleclick.net -> should NOT block
    let d2 = matcher.check_url("https://doubleclick.net/favicon.ico", Some("https://doubleclick.net"), false);
    assert!(!d2.should_block, "First-party request to doubleclick should NOT be blocked");

    // 3. First-party rule: analytics.internal.com from internal.com -> should block
    let d3 = matcher.check_url("https://analytics.internal.com/collect", Some("https://internal.com"), false);
    assert!(d3.should_block, "First-party internal tracker should be blocked on same-site");

    // 4. First-party rule requested from unrelated external site -> should NOT block
    let d4 = matcher.check_url("https://analytics.internal.com/collect", Some("https://other.com"), true);
    assert!(!d4.should_block, "First-party only rule should not trigger on third-party context");

    // 5. Video streaming exception on Kick
    let d5 = matcher.check_url("https://video.kick.com/live/stream.m3u8", Some("https://kick.com"), false);
    assert!(!d5.should_block, "Video stream exception should allow playback");
    assert!(d5.is_exception);

    // 6. Ad on Kick should be blocked
    let d6 = matcher.check_url("https://ads.kick.com/banner.png", Some("https://kick.com"), false);
    assert!(d6.should_block, "Ad on Kick should be blocked");
}

#[test]
fn test_multi_platform_video_compatibility_without_hardcoded_logic() {
    let mut matcher = RuleMatcher::new();

    // Declarative rules across YouTube, Twitch, Vimeo, Kick, Facebook
    let rules = vec![
        // YouTube ad endpoints
        "||youtube.com/api/stats/ads^$third-party",
        "||youtube.com/pagead/*",
        // Declarative media exception for GoogleVideo streaming CDN
        "@@||googlevideo.com/videoplayback^",
        "@@||*.googlevideo.com/videoplayback^",
        // Twitch ad endpoints vs media chunks
        "||twitch.tv/api/ad/*",
        "@@||usher.ttvnw.net/api/channel/hls/*",
        "@@||video-weaver.*.hls.ttvnw.net/*",
        // Kick ad endpoints vs stream chunks
        "||kick.com/api/v1/ads/*",
        "@@||stream.kick.com/*",
        // Facebook tracking pixels vs media
        "||connect.facebook.net/en_US/fbevents.js$third-party",
        "||pixel.facebook.com^$third-party",
        "@@||video.*.fbcdn.net/*",
    ];

    for r in rules {
        if let Some(rule) = NetworkRule::parse(r, BlockCategory::Ad) {
            matcher.add_rule(rule);
        }
    }
    matcher.rebuild_indexes();

    // YouTube: Ad stats request blocked
    let yt_ad = matcher.check_url("https://www.youtube.com/pagead/lvz?test=1", Some("https://news.com"), true);
    assert!(yt_ad.should_block);

    // YouTube: Video playback CDN allowed via declarative exception
    let yt_media = matcher.check_url("https://rr2---sn-vgqsrn7e.googlevideo.com/videoplayback?expire=123&sparams=id", Some("https://www.youtube.com"), true);
    assert!(!yt_media.should_block);
    assert!(yt_media.is_exception);

    // Twitch: Media chunk allowed
    let twitch_media = matcher.check_url("https://video-weaver.sfo01.hls.ttvnw.net/v1/segment/123.ts", Some("https://www.twitch.tv"), true);
    assert!(!twitch_media.should_block);
    assert!(twitch_media.is_exception);

    // Twitch: Ad endpoint blocked
    let twitch_ad = matcher.check_url("https://twitch.tv/api/ad/v1/fetch", Some("https://www.twitch.tv"), false);
    assert!(twitch_ad.should_block);

    // Kick: Video stream allowed
    let kick_stream = matcher.check_url("https://stream.kick.com/hls/live/user_master.m3u8", Some("https://kick.com"), true);
    assert!(!kick_stream.should_block);
    assert!(kick_stream.is_exception);

    // Facebook: Pixel blocked on third-party news site
    let fb_pixel = matcher.check_url("https://connect.facebook.net/en_US/fbevents.js", Some("https://shopping-site.com"), true);
    assert!(fb_pixel.should_block);

    // Facebook: Video CDN allowed
    let fb_video = matcher.check_url("https://video.xx.fbcdn.net/v/t42.17977-2/123.mp4", Some("https://www.facebook.com"), true);
    assert!(!fb_video.should_block);
}

#[test]
fn test_exception_priority_overrides_broad_blocking() {
    let mut matcher = RuleMatcher::new();

    // Broad rule: block all tracking domains
    matcher.add_rule(NetworkRule::parse("||tracker.com^", BlockCategory::Tracker).unwrap());
    // Specific exception: allow authenticated login auth endpoint on tracker.com
    matcher.add_rule(NetworkRule::parse("@@||tracker.com/auth/login", BlockCategory::None).unwrap());
    matcher.rebuild_indexes();

    // Standard tracking request blocked
    let d1 = matcher.check_url("https://tracker.com/collect?uid=123", Some("https://example.com"), true);
    assert!(d1.should_block);

    // Critical login authentication request allowed by exception
    let d2 = matcher.check_url("https://tracker.com/auth/login?session=abc", Some("https://example.com"), true);
    assert!(!d2.should_block);
    assert!(d2.is_exception);
}

#[test]
fn test_fqdn_trailing_dot_and_case_normalization() {
    let mut matcher = RuleMatcher::new();
    matcher.add_rule(NetworkRule::parse("||adserver.com^", BlockCategory::Ad).unwrap());
    matcher.rebuild_indexes();

    let d1 = matcher.check_url("HTTPS://ADSERVER.COM/banner.js", None, true);
    assert!(d1.should_block);

    let d2 = matcher.check_url("https://adserver.com./banner.js", None, true);
    assert!(d2.should_block);

    let d3 = matcher.check_url("https://SUB.adserver.com/banner.js", None, true);
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
    // MUST be identified as third-party, not same-site!
    let d1 = engine.check_network_request(
        "https://tracker.co.uk/beacon.js",
        Some("https://news.co.uk"),
        Some("script"),
        false, // caller mistakenly passed false, engine PSL must detect different domain!
    );
    assert!(d1.should_block);
    assert_eq!(d1.category, BlockCategory::Tracker);

    // 2. Type-specific rule matching with $script
    let d2_script = engine.check_network_request(
        "https://analytics.github.io/sdk.js",
        Some("https://victim.github.io"),
        Some("script"),
        true,
    );
    assert!(d2_script.should_block);
}
