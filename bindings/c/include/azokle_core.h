#ifndef AZOKLE_CORE_H
#define AZOKLE_CORE_H

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
    char* matched_rule;
    bool is_exception;
} AzokleFilterDecision;

void* azokle_core_init(void);

AzokleFilterDecision azokle_core_check_url(
    const char* url,
    const char* source_origin,
    bool is_third_party
);

char* azokle_core_clean_url(const char* url);

char* azokle_core_get_cosmetic_css(const char* domain);

char* azokle_core_get_scriptlets(const char* domain);

int32_t azokle_core_load_filter_rules(const char* rules);

int32_t azokle_core_load_resources(const char* resources_json);

bool azokle_core_load_serialized_engine(const uint8_t* data, size_t len);

uint8_t* azokle_core_serialize_engine(size_t* out_len);

void azokle_byte_buffer_free(uint8_t* ptr, size_t len);

bool azokle_core_is_tracker(const char* host);

bool azokle_core_toggle_host_shield(const char* host, bool enabled);

bool azokle_core_is_host_shield_enabled(const char* host);

char* azokle_core_get_stats_json(void);

void azokle_core_record_block(const char* host, uint8_t category);

char* azokle_passkey_create(
    const char* rp_id,
    const char* user_name,
    const uint8_t* user_handle,
    size_t handle_len
);

char* azokle_passkey_sign(
    const char* credential_id_b64,
    const uint8_t* auth_data,
    size_t auth_len,
    const uint8_t* client_hash,
    size_t hash_len
);

void azokle_string_free(char* s);

void azokle_decision_free(AzokleFilterDecision* decision);

char* azokle_core_fetch_page_json(const char* url, bool is_tor);

#ifdef __cplusplus
}
#endif

#endif // AZOKLE_CORE_H
