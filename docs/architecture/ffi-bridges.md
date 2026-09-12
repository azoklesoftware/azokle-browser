# Universal FFI Bridge Architecture

Foreign Function Interface (FFI) boundaries connect the safe Rust Core kernel to the C++20 engine, Kotlin Android application, and Swift Apple clients. All FFI bridges enforce strict zero-panic guarantees and explicit memory lifecycle semantics.

---

## 1. C-ABI Bridge (`bindings/c`)

The C-ABI layer exports a portable C interface declared in `bindings/c/include/azokle_core.h` and compiled into `libazokle_c.a` (static) and `libazokle_c.so` / `libazokle_c.dylib` / `azokle_c.dll` (dynamic).

### Panic Boundary Pattern
No Rust panic may ever cross the C-ABI boundary into foreign caller stacks. Every exported function is wrapped in `std::panic::catch_unwind`:

```rust
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
        // Safe string extraction & core dispatch...
    }));

    result.unwrap_or(fallback)
}
```

### Fast-Path Bitmask Checking
To support multi-gigabit throughput and avoid heap allocations during high-frequency resource requests (images, scripts, styles), `azokle_shield_check_url_fast` returns an unallocated 64-bit decision bitmask:
- Bit 0: `should_block` (1 = block, 0 = allow)
- Bits 1–8: `BlockCategory` enum
- Bits 9–10: `Action` (Allow, Block, Redirect)

---

## 2. Kotlin / JNI Bridge (`bindings/kotlin`)

The Android client integrates via high-speed JNI primitives in `bindings/kotlin/src/lib.rs` and Kotlin bindings in `AzokleShieldJni.kt`:
- **Zero-Copy Evaluation**: Pass raw UTF-8 string byte pointers without allocating intermediate Java strings where possible.
- **Atomic Telemetry Snapshots**: Direct JNI integer access for reading lifetime blocked ads and trackers.

---

## 3. Swift / UniFFI Bridge (`bindings/uniffi` & `bindings/swift`)

For macOS and iOS, Mozilla's `UniFFI` framework compiles interface definitions directly from Rust proc-macros into type-safe Swift bindings:
- **Rust Implementation**: `bindings/uniffi/src/lib.rs` exports `UniversalBrowserEngine`, `UniversalFilterDecision`, and `UniversalShieldStats`.
- **Code Generation Tool**: `bindings/uniffi/src/bin/bindgen.rs` invokes the generator to produce `azokle_uniffi.swift`, `azokle_uniffiFFI.h`, and `azokle_uniffiFFI.modulemap` into `bindings/swift/`.
- **SwiftPM Integration**: Referenced by `apps/macos/Package.swift` and `apps/ios/Package.swift` as a local Swift package dependency.
