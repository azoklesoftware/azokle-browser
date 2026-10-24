#ifndef AZOKLE_CORE_H
#define AZOKLE_CORE_H

/**
 * @file azokle_core.h
 * @brief Sovereign Azokle Native Core C FFI Interface.
 *
 * MEMORY OWNERSHIP AND LIFETIME CONTRACT:
 * 1. Strings returned as `char*` by `azokle_*` functions are dynamically allocated on the
 *    Rust heap via `CString::into_raw()`. Callers MUST release this memory by calling
 *    `azokle_string_free(char* s)` when no longer needed. Passing these pointers to the standard C
 *    `free()` is undefined behavior on platforms with non-standard allocators.
 * 2. The `AzokleFilterDecision` struct contains a dynamically allocated `matched_rule` string.
 *    Callers MUST call `azokle_decision_free(AzokleFilterDecision* decision)` or pass
 *    `decision->matched_rule` to `azokle_string_free()` to avoid memory leaks.
 * 3. Byte buffers returned as `uint8_t*` (e.g. from `azokle_core_serialize_engine`) MUST be
 *    freed by calling `azokle_byte_buffer_free(uint8_t* ptr, size_t len)` with the exact length.
 * 4. All input `const char*` pointers must be valid NUL-terminated UTF-8 strings. Null pointers are
 *    safely handled by all functions and yield default/fallback return values, but non-null pointers
 *    must point to readable memory for the duration of the call.
 * 5. All functions catch Rust panics internally via `std::panic::catch_unwind` and return safe
 *    fallbacks (null pointers, false, or 0) rather than unwinding across the FFI boundary.
 */

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    AZOKLE_BLOCK_CATEGORY_NONE = 0,
    AZOKLE_BLOCK_CATEGORY_AD = 1,
    AZOKLE_BLOCK_CATEGORY_TRACKER = 2,
    AZOKLE_BLOCK_CATEGORY_TELEMETRY = 3,
    AZOKLE_BLOCK_CATEGORY_ANNOYANCE = 4,
    AZOKLE_BLOCK_CATEGORY_FINGERPRINTING = 5,
    AZOKLE_BLOCK_CATEGORY_CRYPTOMINING = 6,
    AZOKLE_BLOCK_CATEGORY_MALWARE = 7
} AzokleBlockCategory;

typedef struct {
    bool should_block;
    uint8_t category;
    char* matched_rule;    /**< Dynamically allocated string. Free via `azokle_decision_free()`. */
    bool is_exception;
} AzokleFilterDecision;

/**
 * Initializes the global Azokle Core engine.
 * @return Opaque pointer to the core engine, or NULL on panic.
 */
void* azokle_core_init(void);

/**
 * Evaluates whether a network request URL should be blocked.
 * @param url Target request URL (NUL-terminated UTF-8).
 * @param source_origin Origin of the page initiating the request, or NULL.
 * @param is_third_party Whether the request is third-party relative to source_origin.
 * @return Decision struct. Caller MUST free `decision.matched_rule` via `azokle_decision_free()`.
 */
AzokleFilterDecision azokle_core_check_url(
    const char* url,
    const char* source_origin,
    bool is_third_party
);

/**
 * Strips tracking query parameters from the specified URL.
 * @return Newly allocated cleaned URL string. Free via `azokle_string_free()`.
 */
char* azokle_core_clean_url(const char* url);

/**
 * Computes cosmetic CSS selector hiding rules for the specified domain.
 * @return Newly allocated CSS string. Free via `azokle_string_free()`.
 */
char* azokle_core_get_cosmetic_css(const char* domain);

/**
 * Retrieves cosmetic scriptlet injections for the specified domain.
 * @return Newly allocated scriptlet string. Free via `azokle_string_free()`.
 */
char* azokle_core_get_scriptlets(const char* domain);

/**
 * Compiles and loads ABP/uBlock-format filter rules.
 * @return Number of rules loaded, or -1 on error.
 */
int32_t azokle_core_load_filter_rules(const char* rules);

/**
 * Loads JSON scriptlet and redirect resources.
 * @return Number of resources loaded, or -1 on error.
 */
int32_t azokle_core_load_resources(const char* resources_json);

/**
 * Restores the engine from serialized FlatBuffers data.
 */
bool azokle_core_load_serialized_engine(const uint8_t* data, size_t len);

/**
 * Serializes the engine state to FlatBuffers.
 * @param out_len Output pointer receiving the buffer length in bytes.
 * @return Pointer to byte buffer. Caller MUST free via `azokle_byte_buffer_free(ptr, *out_len)`.
 */
uint8_t* azokle_core_serialize_engine(size_t* out_len);

/**
 * Frees a byte buffer previously allocated by `azokle_core_serialize_engine`.
 */
void azokle_byte_buffer_free(uint8_t* ptr, size_t len);

/**
 * Checks whether the given hostname belongs to a known tracker.
 */
bool azokle_core_is_tracker(const char* host);

/**
 * Toggles shield protection on or off for the given domain.
 */
bool azokle_core_toggle_host_shield(const char* host, bool enabled);

/**
 * Checks whether shield protection is active for the given domain.
 */
bool azokle_core_is_host_shield_enabled(const char* host);

/**
 * Serializes block telemetry and engine statistics to JSON.
 * @return Newly allocated JSON string. Free via `azokle_string_free()`.
 */
char* azokle_core_get_stats_json(void);

/**
 * Manually records a block event for stats.
 */
void azokle_core_record_block(const char* host, uint8_t category);

/**
 * Creates a WebAuthn / Passkey credential.
 * @return Newly allocated JSON string describing credential. Free via `azokle_string_free()`.
 */
char* azokle_passkey_create(
    const char* rp_id,
    const char* user_name,
    const uint8_t* user_handle,
    size_t handle_len
);

/**
 * Signs an assertion using a stored passkey.
 * @return Newly allocated JSON signature string. Free via `azokle_string_free()`.
 */
char* azokle_passkey_sign(
    const char* credential_id_b64,
    const uint8_t* auth_data,
    size_t auth_len,
    const uint8_t* client_hash,
    size_t hash_len
);

/**
 * Clears the global sovereign passkey in-memory vault.
 */
void azokle_passkey_clear_vault(void);


/**
 * Deallocates a NUL-terminated string returned by any `azokle_*` function.
 * Safe to call with NULL.
 */
void azokle_string_free(char* s);

/**
 * Deallocates the `matched_rule` within `decision` and resets its pointer to NULL.
 * Safe to call with NULL.
 */
void azokle_decision_free(AzokleFilterDecision* decision);

/**
 * Fetches and parses a web page, returning metadata and body as JSON.
 * @return Newly allocated JSON string. Free via `azokle_string_free()`.
 */
char* azokle_core_fetch_page_json(const char* url, bool is_tor);

#ifdef __cplusplus
}
#endif

#endif // AZOKLE_CORE_H
