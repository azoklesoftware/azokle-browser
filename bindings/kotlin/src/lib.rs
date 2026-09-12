use azokle_core::{
    AzokleCoreEngine, BlockCategory,
};
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jstring, JNI_FALSE, JNI_TRUE};
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

/// Panic-safe JNI execution wrapper returning a fallback value if a Rust panic occurs.
///
/// Prevents undefined behavior and instant VM aborts when bridging across the JNI C boundary.
macro_rules! safe_jni {
    ($fallback:expr, $body:expr) => {
        match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body)) {
            Ok(v) => v,
            Err(e) => {
                log::error!("Azokle Rust core caught panic at JNI boundary: {:?}", e);
                $fallback
            }
        }
    };
}

/// Panic-safe JNI execution wrapper for void-returning native methods.
macro_rules! safe_jni_void {
    ($body:expr) => {
        if let Err(e) = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body)) {
            log::error!("Azokle Rust core caught panic at JNI boundary: {:?}", e);
        }
    };
}

// -----------------------------------------------------------------------------
// JNI Export Functions (Mapping to com.azokle.search.core.AzokleCore)
// -----------------------------------------------------------------------------

/// Initializes Android logging and prepares the shared singleton [`AzokleCoreEngine`].
#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_initNative(
    _env: JNIEnv,
    _class: JClass,
) {
    safe_jni_void!({
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
    });
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadFilterRules(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
    category_id: jint,
) -> jint {
    safe_jni!(0, {
        let content_str = match env.get_string(&content) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return 0,
        };
        let cat = BlockCategory::from_u8(category_id as u8);
        let count = get_core().adblock.load_rules_from_text(&content_str, cat);
        count as jint
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadTrackerList(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
) -> jint {
    safe_jni!(0, {
        let content_str = match env.get_string(&content) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return 0,
        };
        let count = get_core().trackers.load_tracker_list(&content_str);
        count as jint
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadCosmeticRules(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
) -> jint {
    safe_jni!(0, {
        let content_str = match env.get_string(&content) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return 0,
        };
        let count = get_core().shield.load_cosmetic_rules(&content_str);
        count as jint
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadResources(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
) -> jint {
    safe_jni!(0, {
        let content_str = match env.get_string(&content) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return 0,
        };
        match get_core().adblock.load_resources_json(&content_str) {
            Ok(count) => count as jint,
            Err(e) => {
                log::error!("Failed to load adblock resources JSON: {}", e);
                0
            }
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadDebounceRules(
    mut env: JNIEnv,
    _class: JClass,
    content: JString,
) -> jint {
    safe_jni!(0, {
        let content_str = match env.get_string(&content) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return 0,
        };
        match get_core().trackers.load_debounce_rules(&content_str) {
            Ok(count) => count as jint,
            Err(e) => {
                log::error!("Failed to load debounce rules: {}", e);
                0
            }
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_unwrapBounceUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jstring {
    let fallback = safe_jstring(&mut env, "");
    safe_jni!(fallback, {
        let url_str = match env.get_string(&url) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return safe_jstring(&mut env, ""),
        };
        if let Some(unwrapped) = get_core().trackers.unwrap_bounce_url(&url_str) {
            safe_jstring(&mut env, &unwrapped)
        } else {
            safe_jstring(&mut env, "")
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_serializeEngine(
    env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    safe_jni!(std::ptr::null_mut(), {
        let data = get_core().adblock.serialize_to_dat();
        match env.byte_array_from_slice(&data) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                log::error!("Failed to serialize adblock engine: {:?}", e);
                std::ptr::null_mut()
            }
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_loadSerializedEngine(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jboolean {
    safe_jni!(JNI_FALSE, {
        if data.is_null() {
            return JNI_FALSE;
        }
        let bytes = match env.convert_byte_array(&data) {
            Ok(b) => b,
            Err(e) => {
                log::error!("Failed to convert byte array for engine deserialize: {:?}", e);
                return JNI_FALSE;
            }
        };
        match get_core().adblock.deserialize_from_dat(&bytes) {
            Ok(_) => JNI_TRUE,
            Err(e) => {
                log::warn!("Failed to deserialize adblock engine: {}", e);
                JNI_FALSE
            }
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_checkUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    source_origin: JString,
    is_third_party: jboolean,
) -> jstring {
    let fallback = safe_jstring(&mut env, "{}");
    safe_jni!(fallback, {
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
    })
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_checkNetworkRequest(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    source_origin: JString,
    request_type: JString,
    is_third_party: jboolean,
) -> jstring {
    let fallback = safe_jstring(&mut env, "{}");
    safe_jni!(fallback, {
        let url_str = match env.get_string(&url) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return safe_jstring(&mut env, "{}"),
        };

        let source_str: Option<String> = if !source_origin.is_null() {
            env.get_string(&source_origin).ok().map(|s| String::from(s.to_str().unwrap_or("")))
        } else {
            None
        };

        let req_type_str: Option<String> = if !request_type.is_null() {
            env.get_string(&request_type).ok().map(|s| String::from(s.to_str().unwrap_or("")))
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

        let decision = core.adblock.check_network_request(
            &url_str,
            source_str.as_deref(),
            req_type_str.as_deref(),
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
    })
}

/// High-speed zero-allocation network request filtering for GeckoView on Android.
///
/// Returns a packed 64-bit primitive integer avoiding JSON serialization overhead:
/// - Bit 0: `should_block` (1 = block, 0 = allow)
/// - Bits 1..7: `category_id` (0..127)
/// - Bit 8: `is_exception`
/// - Bit 9: `has_redirect_url`
/// - Bit 10: `has_rewritten_url`
#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_checkUrlFast(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    source_origin: JString,
    is_third_party: jboolean,
) -> jlong {
    safe_jni!(0 as jlong, {
        let url_str = match env.get_string(&url) {
            Ok(s) => String::from(s.to_str().unwrap_or("")),
            Err(_) => return 0,
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
                        return 0;
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

        let mut packed: jlong = 0;
        if decision.should_block {
            packed |= 1;
        }
        let cat_id = decision.category as u8 as jlong;
        packed |= (cat_id & 0x7F) << 1;
        if decision.is_exception {
            packed |= 1 << 8;
        }
        if decision.redirect_url.is_some() {
            packed |= 1 << 9;
        }
        if decision.rewritten_url.is_some() {
            packed |= 1 << 10;
        }
        packed
    })
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

    let brave_css = get_core().adblock.get_cosmetic_css(&domain_str);
    let shield_css = get_core().shield.get_cosmetic_css(&domain_str);
    let combined = if brave_css.is_empty() {
        shield_css
    } else if shield_css.is_empty() {
        brave_css
    } else {
        format!("{shield_css}\n{brave_css}")
    };
    safe_jstring(&mut env, &combined)
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

    let brave_scriptlet = get_core().adblock.get_scriptlets(&domain_str);
    let shield_scriptlet = get_core().shield.get_scriptlets(&domain_str).unwrap_or_default();
    let combined = if brave_scriptlet.is_empty() {
        shield_scriptlet
    } else if shield_scriptlet.is_empty() {
        brave_scriptlet
    } else {
        format!("{shield_scriptlet}\n{brave_scriptlet}")
    };
    safe_jstring(&mut env, &combined)
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

// -----------------------------------------------------------------------------
// Tor & Sovereign DNS JNI Entry Points
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_torProbeSocks5(
    _env: JNIEnv,
    _class: JClass,
    port: jint,
    timeout_ms: jint,
) -> jboolean {
    let ok = azokle_core::TorController::probe_socks5_handshake(port as u16, timeout_ms as u64);
    if ok { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_torRotateCircuit(
    mut env: JNIEnv,
    _class: JClass,
    control_port: jint,
    auth: JString,
) -> jstring {
    let auth_str = match env.get_string(&auth) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };

    let res = match azokle_core::TorController::rotate_circuit(control_port as u16, Some(&auth_str)) {
        Ok(circuit_id) => serde_json::json!({ "success": true, "circuit_id": circuit_id }),
        Err(err) => serde_json::json!({ "success": false, "error": err }),
    };

    safe_jstring(&mut env, &res.to_string())
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_torVerifyExitIp(
    mut env: JNIEnv,
    _class: JClass,
    socks_port: jint,
) -> jstring {
    let res = match azokle_core::TorController::verify_tor_exit(socks_port as u16) {
        Ok(info) => serde_json::json!({
            "success": true,
            "is_tor": info.is_tor,
            "ip": info.ip,
            "guard_node": info.guard_node,
            "guard_country": info.guard_country,
            "middle_node": info.middle_node,
            "middle_country": info.middle_country,
            "exit_node": info.exit_node,
            "exit_country": info.exit_country,
            "circuit_id": info.circuit_id,
            "latency_ms": info.latency_ms
        }),
        Err(err) => serde_json::json!({ "success": false, "error": err }),
    };

    safe_jstring(&mut env, &res.to_string())
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_torResolveUrl(
    mut env: JNIEnv,
    _class: JClass,
    raw_input: JString,
    search_query_url: JString,
) -> jstring {
    let input = match env.get_string(&raw_input) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, "about:tor"),
    };
    let query_url = match env.get_string(&search_query_url) {
        Ok(s) => String::from(s.to_str().unwrap_or("https://duckduckgo.com/?q=")),
        Err(_) => "https://duckduckgo.com/?q=".to_string(),
    };

    let resolved = azokle_core::TorController::resolve_tor_url(&input, &query_url);
    safe_jstring(&mut env, &resolved)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_torIsOnionUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jboolean {
    let url_str = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };

    if azokle_core::TorController::is_onion_url(&url_str) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_dnsGetDoHEndpoint(
    mut env: JNIEnv,
    _class: JClass,
    provider_name: JString,
) -> jstring {
    let name = match env.get_string(&provider_name) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };

    let provider = azokle_core::DnsProvider::from_id(&name);
    let url = provider.get_doh_url().unwrap_or("https://cloudflare-dns.com/dns-query");
    safe_jstring(&mut env, url)
}

// -----------------------------------------------------------------------------
// Website Permissions JNI Entry Points
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permSet(
    mut env: JNIEnv,
    _class: JClass,
    origin: JString,
    perm_type_str: JString,
    state_str: JString,
    is_private: jboolean,
) {
    let o = match env.get_string(&origin) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return,
    };
    let pt = match env.get_string(&perm_type_str) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return,
    };
    let st = match env.get_string(&state_str) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return,
    };

    if let Some(perm) = azokle_core::PermissionType::from_str_name(&pt) {
        let state = azokle_core::PermissionState::from_str_name(&st);
        get_core().permissions.set_permission(&o, perm, state, is_private == JNI_TRUE);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permGet(
    mut env: JNIEnv,
    _class: JClass,
    origin: JString,
    perm_type_str: JString,
    is_private: jboolean,
) -> jstring {
    let o = match env.get_string(&origin) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, "PROMPT"),
    };
    let pt = match env.get_string(&perm_type_str) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, "PROMPT"),
    };

    if let Some(perm) = azokle_core::PermissionType::from_str_name(&pt) {
        let state = get_core().permissions.get_permission(&o, perm, is_private == JNI_TRUE);
        safe_jstring(&mut env, state.to_str_name())
    } else {
        safe_jstring(&mut env, "PROMPT")
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permClearForOrigin(
    mut env: JNIEnv,
    _class: JClass,
    origin: JString,
) {
    let o = match env.get_string(&origin) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return,
    };
    get_core().permissions.clear_for_origin(&o);
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permClearAll(
    _env: JNIEnv,
    _class: JClass,
) {
    get_core().permissions.clear_all();
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permClearEphemeral(
    _env: JNIEnv,
    _class: JClass,
) {
    get_core().permissions.clear_ephemeral();
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permGetSitePermissionsJson(
    mut env: JNIEnv,
    _class: JClass,
    origin: JString,
    is_private: jboolean,
) -> jstring {
    let o = match env.get_string(&origin) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, "{}"),
    };
    let json = get_core().permissions.get_site_permissions_json(&o, is_private == JNI_TRUE);
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permGetAllCustomizedOrigins(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let origins = get_core().permissions.get_all_customized_origins();
    let json = serde_json::to_string(&origins).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permExportJson(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json = get_core().permissions.export_persistent_json();
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_permImportJson(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jint {
    let data = match env.get_string(&json_data) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return 0,
    };
    get_core().permissions.import_persistent_json(&data) as jint
}

// -----------------------------------------------------------------------------
// Browsing History JNI Entry Points
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyRecordVisit(
    mut env: JNIEnv,
    _class: JClass,
    title: JString,
    url: JString,
    is_private: jboolean,
) -> jstring {
    let t = match env.get_string(&title) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };

    let entry = get_core().history.record_visit(&t, &u, is_private == JNI_TRUE);
    let json = serde_json::to_string(&entry).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historySearch(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
    limit: jint,
) -> jstring {
    let q = match env.get_string(&query) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let results = get_core().history.search(&q, limit as usize);
    let json = serde_json::to_string(&results).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyGetRecent(
    mut env: JNIEnv,
    _class: JClass,
    limit: jint,
    offset: jint,
) -> jstring {
    let results = get_core().history.get_recent(limit as usize, offset as usize);
    let json = serde_json::to_string(&results).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyDeleteEntry(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) -> jboolean {
    let i = match env.get_string(&id) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    if get_core().history.delete_entry(&i) { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyDeleteUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jboolean {
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    if get_core().history.delete_url(&u) { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyClearRange(
    _env: JNIEnv,
    _class: JClass,
    from_ms: jlong,
    to_ms: jlong,
) -> jint {
    get_core().history.clear_time_range(from_ms as u64, to_ms as u64) as jint
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyClearAll(
    _env: JNIEnv,
    _class: JClass,
) {
    get_core().history.clear_all();
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyExportJson(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json = get_core().history.export_json();
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_historyImportJson(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jint {
    let data = match env.get_string(&json_data) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return 0,
    };
    get_core().history.import_json(&data) as jint
}

// -----------------------------------------------------------------------------
// Bookmarks JNI Entry Points
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkAdd(
    mut env: JNIEnv,
    _class: JClass,
    title: JString,
    url: JString,
    parent_folder: JString,
    favicon_url: JString,
) -> jstring {
    let t = match env.get_string(&title) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let p = match env.get_string(&parent_folder) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };
    let f = match env.get_string(&favicon_url) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };

    let item = get_core().bookmarks.add_bookmark(&t, &u, p.as_deref(), f.as_deref());
    let json = serde_json::to_string(&item).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkCreateFolder(
    mut env: JNIEnv,
    _class: JClass,
    title: JString,
    parent_folder: JString,
) -> jstring {
    let t = match env.get_string(&title) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let p = match env.get_string(&parent_folder) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };

    let item = get_core().bookmarks.create_folder(&t, p.as_deref());
    let json = serde_json::to_string(&item).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkRemove(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) -> jboolean {
    let i = match env.get_string(&id) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    if get_core().bookmarks.remove_item(&i) { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkIsBookmarked(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jboolean {
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    if get_core().bookmarks.is_bookmarked(&u) { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkGetAll(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let items = get_core().bookmarks.get_all();
    let json = serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkSearch(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
) -> jstring {
    let q = match env.get_string(&query) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let items = get_core().bookmarks.search(&q);
    let json = serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkExportJson(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json = get_core().bookmarks.export_json();
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_bookmarkImportJson(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jint {
    let data = match env.get_string(&json_data) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return 0,
    };
    get_core().bookmarks.import_json(&data) as jint
}

// -----------------------------------------------------------------------------
// Downloads JNI Entry Points
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_downloadCreate(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    filename: JString,
    destination_path: JString,
    total_bytes: jlong,
    mime_type: JString,
) -> jstring {
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let fn_val = match env.get_string(&filename) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };
    let dest = match env.get_string(&destination_path) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let mime = match env.get_string(&mime_type) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };

    let item = get_core().downloads.create_download(&u, fn_val.as_deref(), &dest, total_bytes as u64, mime.as_deref());
    let json = serde_json::to_string(&item).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_downloadUpdateProgress(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
    downloaded_bytes: jlong,
    total_bytes: jlong,
    state_str: JString,
) -> jboolean {
    let i = match env.get_string(&id) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    let st = match env.get_string(&state_str) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };

    let state = azokle_core::DownloadState::from_str_name(&st);
    if get_core().downloads.update_progress(&i, downloaded_bytes as u64, total_bytes as u64, state) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_downloadGetAll(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let items = get_core().downloads.get_all();
    let json = serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_downloadDeleteItem(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) -> jboolean {
    let i = match env.get_string(&id) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    if get_core().downloads.delete_item(&i) { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_downloadExtractFilename(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    content_disposition: JString,
    mime_type: JString,
) -> jstring {
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let cd = match env.get_string(&content_disposition) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };
    let mime = match env.get_string(&mime_type) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };

    let name = azokle_core::DownloadManager::extract_filename(&u, cd.as_deref(), mime.as_deref());
    safe_jstring(&mut env, &name)
}

// -----------------------------------------------------------------------------
// Settings JNI Entry Points
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_settingsGetSearchEngine(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let engine = get_core().settings.get_search_engine();
    safe_jstring(&mut env, &engine)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_settingsSetSearchEngine(
    mut env: JNIEnv,
    _class: JClass,
    engine: JString,
) {
    let e = match env.get_string(&engine) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return,
    };
    get_core().settings.set_search_engine(&e);
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_settingsGetRecentSearches(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let list = get_core().settings.get_recent_searches();
    let json = serde_json::to_string(&list).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_settingsAddRecentSearch(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
) {
    let q = match env.get_string(&query) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return,
    };
    get_core().settings.add_recent_search(&q);
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_settingsClearRecentSearches(
    _env: JNIEnv,
    _class: JClass,
) {
    get_core().settings.clear_recent_searches();
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_settingsExportJson(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json = get_core().settings.export_json();
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_settingsImportJson(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jboolean {
    let data = match env.get_string(&json_data) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    if get_core().settings.import_json(&data) { JNI_TRUE } else { JNI_FALSE }
}

// -----------------------------------------------------------------------------
// Tab Management JNI Entry Points
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabOpen(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    title: JString,
    is_private: jboolean,
    is_tor: jboolean,
    group_id: JString,
    group_title: JString,
) -> jstring {
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let t = match env.get_string(&title) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let gid = match env.get_string(&group_id) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };
    let gtitle = match env.get_string(&group_title) {
        Ok(s) => {
            let str_val = String::from(s.to_str().unwrap_or(""));
            if str_val.is_empty() { None } else { Some(str_val) }
        }
        Err(_) => None,
    };

    let session = get_core().tabs.open_tab(&u, &t, is_private == JNI_TRUE, is_tor == JNI_TRUE, gid.as_deref(), gtitle.as_deref());
    let json = serde_json::to_string(&session).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabClose(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) -> jstring {
    let i = match env.get_string(&id) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, "null"),
    };
    let closed = get_core().tabs.close_tab(&i);
    let json = serde_json::to_string(&closed).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabRestoreRecentlyClosed(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let restored = get_core().tabs.restore_recently_closed();
    let json = serde_json::to_string(&restored).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabGetActive(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let active = get_core().tabs.get_active_tab();
    let json = serde_json::to_string(&active).unwrap_or_else(|_| "null".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabSetActive(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) -> jboolean {
    let i = match env.get_string(&id) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    if get_core().tabs.set_active_tab(&i) { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabUpdate(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
    url: JString,
    title: JString,
) -> jboolean {
    let i = match env.get_string(&id) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return JNI_FALSE,
    };
    let u = match env.get_string(&url) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    let t = match env.get_string(&title) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => String::new(),
    };
    if get_core().tabs.update_tab_url_and_title(&i, &u, &t) { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabGetAll(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let tabs = get_core().tabs.get_all_tabs();
    let json = serde_json::to_string(&tabs).unwrap_or_else(|_| "[]".to_string());
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabCloseAll(
    _env: JNIEnv,
    _class: JClass,
    include_private: jboolean,
) {
    get_core().tabs.close_all_tabs(include_private == JNI_TRUE);
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabClosePrivate(
    _env: JNIEnv,
    _class: JClass,
) {
    get_core().tabs.close_private_tabs();
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabAutoGroupByDomain(
    _env: JNIEnv,
    _class: JClass,
) {
    get_core().tabs.auto_group_by_domain();
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabExportJson(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json = get_core().tabs.export_persistent_json();
    safe_jstring(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_tabImportJson(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jint {
    let data = match env.get_string(&json_data) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return 0,
    };
    get_core().tabs.import_persistent_json(&data) as jint
}

// -----------------------------------------------------------------------------
// Universal Protocol Dispatcher JNI Entry Point
// -----------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_azokle_search_core_AzokleCore_protocolDispatch(
    mut env: JNIEnv,
    _class: JClass,
    envelope_json: JString,
) -> jstring {
    let json_str = match env.get_string(&envelope_json) {
        Ok(s) => String::from(s.to_str().unwrap_or("")),
        Err(_) => return safe_jstring(&mut env, "{}"),
    };

    match azokle_core::ProtocolEnvelope::from_json(&json_str) {
        Ok(incoming_envelope) => {
            let response_envelope = get_core().dispatch_protocol(incoming_envelope);
            let response_json = response_envelope.to_json().unwrap_or_else(|_| "{}".to_string());
            safe_jstring(&mut env, &response_json)
        }
        Err(e) => {
            let error_envelope = azokle_core::ProtocolEnvelope::new(
                azokle_core::MessageEndpoint::RustCore,
                azokle_core::MessageEndpoint::AndroidUI,
                azokle_core::MessagePayload::Error(e),
            );
            let response_json = error_envelope.to_json().unwrap_or_else(|_| "{}".to_string());
            safe_jstring(&mut env, &response_json)
        }
    }
}




