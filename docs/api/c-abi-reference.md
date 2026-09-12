# C-ABI Reference Specification

Header File: `bindings/c/include/azokle_core.h`
C-ABI Library: `libazokle_c.a` / `libazokle_c.so` / `azokle_c.dll`

---

## 1. Types & Data Structures

### `AzokleFilterDecision`
```c
typedef struct {
    bool should_block;       /* True if the request violates privacy policies */
    uint8_t category;        /* BlockCategory: 1=Ad, 2=Tracker, 3=Telemetry, etc. */
    char* matched_rule;      /* Owning pointer to rule string (free with free_string) */
    bool is_exception;       /* True if an unbreak exception rule matched */
} AzokleFilterDecision;
```

### `AzokleShieldStats`
```c
typedef struct {
    uint64_t total_blocked;
    uint64_t ads_blocked;
    uint64_t trackers_blocked;
    uint64_t telemetry_blocked;
    uint64_t annoyances_blocked;
    uint64_t fingerprinting_blocked;
    uint64_t cryptominers_blocked;
} AzokleShieldStats;
```

---

## 2. Function Catalog

### Core Lifecycle

#### `void* azokle_core_init(void);`
Initializes the global Rust core kernel and returns an opaque pointer to the `AzokleCoreEngine` singleton.

#### `void azokle_core_destroy(void* core_ptr);`
Decrements the atomic reference counter of the core engine instance.

---

### Privacy & Filtering

#### `AzokleFilterDecision azokle_core_check_url(const char* url, const char* source_origin, bool is_third_party);`
Evaluates a candidate request URL against loaded filter lists and tracker rules.
- **Parameters**:
  - `url`: Null-terminated destination URL string.
  - `source_origin`: Null-terminated initiating document origin (or `NULL`).
  - `is_third_party`: Flag indicating if request crosses the eTLD+1 origin boundary.
- **Returns**: `AzokleFilterDecision` struct.

#### `uint64_t azokle_shield_check_url_fast(const char* url, const char* source_origin, bool is_third_party);`
High-speed bitmask evaluation returning an unallocated 64-bit integer encoding decision flags.

#### `char* azokle_core_strip_tracking_params(const char* url);`
Removes tracking parameters (`utm_*`, `fbclid`, `gclid`, etc.) and returns a newly allocated sanitized URL string. Caller must release with `azokle_core_free_string`.

#### `char* azokle_core_unwrap_bounce_url(const char* url);`
Unwraps intermediate bounce-tracking redirect hops. Returns destination URL or `NULL` if no wrap was detected.

---

### Memory Management

#### `void azokle_core_free_string(char* s);`
Frees a C string previously allocated by the Rust core runtime.
