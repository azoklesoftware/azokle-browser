use azokle_core::{
    AzokleCoreEngine, BlockCategory, ShieldStatsSummary,
};
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::sync::atomic::Ordering;
use std::sync::{Arc, Once, OnceLock};

static INIT_LOGGER: Once = Once::new();
static GLOBAL_CORE: OnceLock<Arc<AzokleCoreEngine>> = OnceLock::new();

fn get_core() -> Arc<AzokleCoreEngine> {
    GLOBAL_CORE.get_or_init(|| Arc::new(AzokleCoreEngine::new())).clone()
}

#[inline]
fn safe_jstring(env: &mut JNIEnv, s: &str) -> jstring {
    match env.new_string(s) {
        Ok(jstr) => jstr.into_raw(),
        Err(e) => {
            log::error!("Failed to create JNI string: {:?}", e);
            std::ptr::null_mut()
        }
    }
}

// -----------------------------------------------------------------------------
// JNI Export Functions (Mapping to com.azokle.search.core.AzokleCore)
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_initNative(
    _env: JNIEnv,
    _class: JClass,
) {
    INIT_LOGGER.call_once(|| {
        #[cfg(target_os = "android")]
        android_logger::init_once(
            android_logger::Config::default()
                .with_tag("AzokleCoreRust")
                .with_max_level(log::LevelFilter::Info),
        );
    });
    let _ = get_core();
    log::info!("AzokleCore Cross-Platform Rust Engine initialized successfully.");
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadFilterRules(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
    category_id: jint,
) -> jint {
    let content_str = match env.get_string(&content) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return 0,
    };
    let cat = BlockCategory::from_u8(category_id as u8);
    let count = get_core().adblock.load_rules_from_text(&content_str, cat);
    count as jint
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadTrackerList(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
) -> jint {
    let content_str = match env.get_string(&content) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return 0,
    };
    let count = get_core().trackers.load_tracker_list(&content_str);
    count as jint
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadCosmeticRules(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
) -> jint {
    let content_str = match env.get_string(&content) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return 0,
    };
    let count = get_core().shield.load_cosmetic_rules(&content_str);
    count as jint
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_checkUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    source_origin: JString,
    is_third_party: jboolean,
) -> jstring {
    let url_str = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, "{}"),
    };

    let source_str: Option<String> = if !source_origin.is_null() {
        env.get_string(&source_origin).ok().map(|s| String::from(s.to_str().unwrap_or("")))
    } else {
        None
    };

    let core = get_core();

    // Check if shield disabled for source origin
    if let Some(ref src) = source_str {
        if let Ok(src_url) = url::Url::parse(src) {
            if let Some(src_host) = src_url.host_str() {
                if core.disabled_hosts.read().contains(&src_host.to_lowercase()) {
                    let res = serde_json::json!({
                        "should_block": false,
                        "category": 0,
                        "matched_rule": null,
                        "is_exception": true
                    });
                    return safe_jstring(&mut env, &res.to_string());
                }
            }
        }
    }

    let decision = core.adblock.check_url(
        &url_str,
        source_str.as_deref(),
        is_third_party == JNI_TRUE,
    );

    if decision.should_block {
        core.stats_total.fetch_add(1, Ordering::Relaxed);
        match decision.category {
            BlockCategory::Ad => {
                core.stats_ads.fetch_add(1, Ordering::Relaxed);
            }
            BlockCategory::Tracker => {
                core.stats_trackers.fetch_add(1, Ordering::Relaxed);
            }
            BlockCategory::Telemetry => {
                core.stats_telemetry.fetch_add(1, Ordering::Relaxed);
            }
            BlockCategory::Fingerprinting => {
                core.stats_fingerprinting.fetch_add(1, Ordering::Relaxed);
            }
            BlockCategory::Annoyance => {
                core.stats_annoyances.fetch_add(1, Ordering::Relaxed);
            }
            BlockCategory::Cryptomining => {
                core.stats_cryptominers.fetch_add(1, Ordering::Relaxed);
            }
            _ => {}
        }
    }

    let json_str = serde_json::to_string(&decision).unwrap_or_else(|_| "{}".to_string());
    safe_jstring(&mut env, &json_str)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_cleanUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jstring {
    let url_str = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, ""),
    };

    let cleaned = get_core().trackers.strip_tracking_params(&url_str);
    safe_jstring(&mut env, &cleaned)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_getCosmeticCss(
    mut env: JNIEnv,
    _class: JClass,
    domain: JString,
) -> jstring {
    let domain_str = match env.get_string(&domain) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, ""),
    };

    let css = get_core().shield.get_cosmetic_css(&domain_str);
    safe_jstring(&mut env, &css)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_getScriptlets(
    mut env: JNIEnv,
    _class: JClass,
    domain: JString,
) -> jstring {
    let domain_str = match env.get_string(&domain) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, ""),
    };

    let scriptlet = get_core()
        .shield
        .get_scriptlets(&domain_str)
        .unwrap_or("");
    safe_jstring(&mut env, scriptlet)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_isTracker(
    mut env: JNIEnv,
    _class: JClass,
    host: JString,
) -> jboolean {
    let host_str = match env.get_string(&host) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };

    if get_core().trackers.is_tracker(&host_str) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_toggleHostShield(
    mut env: JNIEnv,
    _class: JClass,
    host: JString,
    enabled: jboolean,
) -> jboolean {
    let host_str = match env.get_string(&host) {
        Ok(s) => s.to_str().unwrap_or("").to_lowercase().trim().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let core = get_core();
    let mut disabled = core.disabled_hosts.write();
    if enabled == JNI_TRUE {
        disabled.remove(&host_str);
        JNI_TRUE
    } else {
        disabled.insert(host_str);
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_isHostShieldEnabled(
    mut env: JNIEnv,
    _class: JClass,
    host: JString,
) -> jboolean {
    let host_str = match env.get_string(&host) {
        Ok(s) => s.to_str().unwrap_or("").to_lowercase().trim().to_string(),
        Err(_) => return JNI_TRUE,
    };

    if get_core().disabled_hosts.read().contains(&host_str) {
        JNI_FALSE
    } else {
        JNI_TRUE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_getShieldStats(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let core = get_core();
    let stats = core.get_stats();

    let json_str = serde_json::to_string(&stats).unwrap_or_else(|_| "{}".to_string());
    safe_jstring(&mut env, &json_str)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_recordBlock(
    _env: JNIEnv,
    _class: JClass,
    _host: JString,
    category_id: jint,
) {
    let core = get_core();
    core.stats_total.fetch_add(1, Ordering::Relaxed);
    match BlockCategory::from_u8(category_id as u8) {
        BlockCategory::Ad => {
            core.stats_ads.fetch_add(1, Ordering::Relaxed);
        }
        BlockCategory::Tracker => {
            core.stats_trackers.fetch_add(1, Ordering::Relaxed);
        }
        BlockCategory::Telemetry => {
            core.stats_telemetry.fetch_add(1, Ordering::Relaxed);
        }
        BlockCategory::Fingerprinting => {
            core.stats_fingerprinting.fetch_add(1, Ordering::Relaxed);
        }
        BlockCategory::Annoyance => {
            core.stats_annoyances.fetch_add(1, Ordering::Relaxed);
        }
        BlockCategory::Cryptomining => {
            core.stats_cryptominers.fetch_add(1, Ordering::Relaxed);
        }
        _ => {}
    }
}
