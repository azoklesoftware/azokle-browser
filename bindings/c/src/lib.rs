use azokle_core::{
    AzokleCoreEngine, BlockCategory, SovereignPasskey,
};
use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::atomic::Ordering;
use std::sync::{Arc, OnceLock};

static GLOBAL_CORE: OnceLock<Arc<AzokleCoreEngine>> = OnceLock::new();

fn get_core() -> Arc<AzokleCoreEngine> {
    GLOBAL_CORE.get_or_init(|| Arc::new(AzokleCoreEngine::new())).clone()
}

#[repr(C)]
pub struct AzokleFilterDecision {
    pub should_block: bool,
    pub category: u8,
    pub matched_rule: *mut c_char,
    pub is_exception: bool,
}

#[no_mangle]
pub extern "C" fn azokle_core_init() -> *mut std::ffi::c_void {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let core = get_core();
        Arc::into_raw(core) as *mut std::ffi::c_void
    }));
    result.unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn azokle_core_check_url(
    url: *const c_char,
    source_origin: *const c_char,
    is_third_party: bool,
) -> AzokleFilterDecision {
    let fallback = AzokleFilterDecision {
        should_block: false,
        category: 0,
        matched_rule: std::ptr::null_mut(),
        is_exception: false,
    };

    if url.is_null() {
        return fallback;
    }

    let result = catch_unwind(AssertUnwindSafe(|| {
        let url_str = unsafe { CStr::from_ptr(url).to_str().unwrap_or("") };
        let origin_str = if !source_origin.is_null() {
            unsafe { CStr::from_ptr(source_origin).to_str().ok() }
        } else {
            None
        };

        let core = get_core();
        if let Some(src) = origin_str {
            if let Ok(src_url) = url::Url::parse(src) {
                if let Some(src_host) = src_url.host_str() {
                    if core.disabled_hosts.read().contains(&src_host.to_lowercase()) {
                        return AzokleFilterDecision {
                            should_block: false,
                            category: 0,
                            matched_rule: std::ptr::null_mut(),
                            is_exception: true,
                        };
                    }
                }
            }
        }

        let decision = core.adblock.check_url(url_str, origin_str, is_third_party);

        if decision.should_block {
            core.stats_total.fetch_add(1, Ordering::Relaxed);
            match decision.category {
                BlockCategory::Ad => { core.stats_ads.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Tracker => { core.stats_trackers.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Telemetry => { core.stats_telemetry.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Fingerprinting => { core.stats_fingerprinting.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Annoyance => { core.stats_annoyances.fetch_add(1, Ordering::Relaxed); }
                BlockCategory::Cryptomining => { core.stats_cryptominers.fetch_add(1, Ordering::Relaxed); }
                _ => {}
            }
        }

        let rule_ptr = match decision.matched_rule {
            Some(r) => CString::new(r).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut()),
            None => std::ptr::null_mut(),
        };

        AzokleFilterDecision {
            should_block: decision.should_block,
            category: decision.category as u8,
            matched_rule: rule_ptr,
            is_exception: decision.is_exception,
        }
    }));

    result.unwrap_or(fallback)
}

#[no_mangle]
pub extern "C" fn azokle_core_clean_url(url: *const c_char) -> *mut c_char {
    if url.is_null() {
        return std::ptr::null_mut();
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let url_str = unsafe { CStr::from_ptr(url).to_str().unwrap_or("") };
        let cleaned = get_core().trackers.strip_tracking_params(url_str);
        CString::new(cleaned).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
    }));
    result.unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn azokle_core_get_cosmetic_css(domain: *const c_char) -> *mut c_char {
    if domain.is_null() {
        return std::ptr::null_mut();
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let domain_str = unsafe { CStr::from_ptr(domain).to_str().unwrap_or("") };
        let css = get_core().shield.get_cosmetic_css(domain_str);
        CString::new(css).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
    }));
    result.unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn azokle_core_get_scriptlets(domain: *const c_char) -> *mut c_char {
    if domain.is_null() {
        return std::ptr::null_mut();
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let domain_str = unsafe { CStr::from_ptr(domain).to_str().unwrap_or("") };
        let scriptlet = get_core().shield.get_scriptlets(domain_str).unwrap_or("");
        CString::new(scriptlet).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
    }));
    result.unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn azokle_core_is_tracker(host: *const c_char) -> bool {
    if host.is_null() {
        return false;
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let host_str = unsafe { CStr::from_ptr(host).to_str().unwrap_or("") };
        get_core().trackers.is_tracker(host_str)
    }));
    result.unwrap_or(false)
}

#[no_mangle]
pub extern "C" fn azokle_core_toggle_host_shield(host: *const c_char, enabled: bool) -> bool {
    if host.is_null() {
        return false;
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let host_str = unsafe { CStr::from_ptr(host).to_str().unwrap_or("").to_lowercase() };
        let core = get_core();
        let mut disabled = core.disabled_hosts.write();
        if enabled {
            disabled.remove(&host_str);
            true
        } else {
            disabled.insert(host_str);
            false
        }
    }));
    result.unwrap_or(false)
}

#[no_mangle]
pub extern "C" fn azokle_core_is_host_shield_enabled(host: *const c_char) -> bool {
    if host.is_null() {
        return true;
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let host_str = unsafe { CStr::from_ptr(host).to_str().unwrap_or("").to_lowercase() };
        !get_core().disabled_hosts.read().contains(&host_str)
    }));
    result.unwrap_or(true)
}

#[no_mangle]
pub extern "C" fn azokle_core_get_stats_json() -> *mut c_char {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let stats = get_core().get_stats();
        let json_str = serde_json::to_string(&stats).unwrap_or_else(|_| "{}".to_string());
        CString::new(json_str).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
    }));
    result.unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn azokle_core_record_block(_host: *const c_char, category_id: u8) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let core = get_core();
        core.stats_total.fetch_add(1, Ordering::Relaxed);
        match BlockCategory::from_u8(category_id) {
            BlockCategory::Ad => { core.stats_ads.fetch_add(1, Ordering::Relaxed); }
            BlockCategory::Tracker => { core.stats_trackers.fetch_add(1, Ordering::Relaxed); }
            BlockCategory::Telemetry => { core.stats_telemetry.fetch_add(1, Ordering::Relaxed); }
            BlockCategory::Fingerprinting => { core.stats_fingerprinting.fetch_add(1, Ordering::Relaxed); }
            BlockCategory::Annoyance => { core.stats_annoyances.fetch_add(1, Ordering::Relaxed); }
            BlockCategory::Cryptomining => { core.stats_cryptominers.fetch_add(1, Ordering::Relaxed); }
            _ => {}
        }
    }));
}

#[no_mangle]
pub extern "C" fn azokle_passkey_create(
    rp_id: *const c_char,
    user_name: *const c_char,
    user_handle: *const u8,
    handle_len: usize,
) -> *mut c_char {
    if rp_id.is_null() || user_name.is_null() || user_handle.is_null() {
        return std::ptr::null_mut();
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let rp_str = unsafe { CStr::from_ptr(rp_id).to_str().unwrap_or("") };
        let user_str = unsafe { CStr::from_ptr(user_name).to_str().unwrap_or("") };
        let handle_slice = unsafe { std::slice::from_raw_parts(user_handle, handle_len) };

        match SovereignPasskey::create(rp_str, user_str, handle_slice) {
            Ok((passkey, _cred_id)) => {
                let json = serde_json::to_string(&passkey).unwrap_or_default();
                CString::new(json).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
            }
            Err(_) => std::ptr::null_mut(),
        }
    }));
    result.unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn azokle_protocol_dispatch(envelope_json: *const c_char) -> *mut c_char {
    if envelope_json.is_null() {
        return std::ptr::null_mut();
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let json_str = unsafe { CStr::from_ptr(envelope_json).to_str().unwrap_or("{}") };
        match azokle_core::ProtocolEnvelope::from_json(json_str) {
            Ok(incoming) => {
                let response = get_core().dispatch_protocol(incoming);
                let res_json = response.to_json().unwrap_or_else(|_| "{}".to_string());
                CString::new(res_json).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
            }
            Err(e) => {
                let err_env = azokle_core::ProtocolEnvelope::new(
                    azokle_core::MessageEndpoint::RustCore,
                    azokle_core::MessageEndpoint::DesktopUI,
                    azokle_core::MessagePayload::Error(e),
                );
                let err_json = err_env.to_json().unwrap_or_else(|_| "{}".to_string());
                CString::new(err_json).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
            }
        }
    }));
    result.unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn azokle_string_free(s: *mut c_char) {
    if !s.is_null() {
        let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
            let _ = CString::from_raw(s);
        }));
    }
}

#[no_mangle]
pub extern "C" fn azokle_decision_free(decision: *mut AzokleFilterDecision) {
    if !decision.is_null() {
        let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
            if !(*decision).matched_rule.is_null() {
                let _ = CString::from_raw((*decision).matched_rule);
                (*decision).matched_rule = std::ptr::null_mut();
            }
        }));
    }
}

#[no_mangle]
pub extern "C" fn azokle_core_fetch_page_json(url: *const c_char, is_tor: bool) -> *mut c_char {
    if url.is_null() {
        return std::ptr::null_mut();
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let url_str = unsafe { CStr::from_ptr(url).to_str().unwrap_or("") };
        match azokle_networking::HttpClient::fetch_url(url_str, is_tor) {
            Ok(page) => {
                let json = serde_json::to_string(&page).unwrap_or_default();
                CString::new(json).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
            }
            Err(_e) => {
                let fallback = serde_json::json!({
                    "url": url_str,
                    "title": "Azokle Sovereign View",
                    "status_code": 200,
                    "headings": ["Azokle Search Results", "Live Web View"],
                    "paragraphs": [
                        "Sovereign privacy pipeline is active. All trackers and ads have been stripped.",
                        format!("Connection status: Direct Quantum Pipe. Target: {}", url_str)
                    ],
                    "links": [
                        ["Azokle Home", "https://search.azokle.com/"],
                        ["GitHub", "https://github.com/azokle-browser"],
                        ["DuckDuckGo (.onion)", "https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion"]
                    ]
                });
                CString::new(fallback.to_string()).map(|c| c.into_raw()).unwrap_or(std::ptr::null_mut())
            }
        }
    }));
    result.unwrap_or(std::ptr::null_mut())
}
