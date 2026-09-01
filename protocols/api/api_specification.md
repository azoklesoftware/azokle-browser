# Azokle Universal FFI & UniFFI API Specification

**Specification**: AUBP-API-1.0  
**Target Languages**: Kotlin (Android), Swift (iOS/macOS), C++ (Desktop Qt/Slint), Rust  

---

## 1. Native Dispatch Entry Points

### A. JNI (Android Kotlin):
```kotlin
// In com.azokle.search.core.AzokleCore
external fun protocolDispatch(envelopeJson: String): String
```

### B. C-ABI (Desktop / Embedding):
```c
char* azokle_protocol_dispatch(const char* envelope_json);
void azokle_string_free(char* ptr);
```

### C. UniFFI (Swift / Kotlin Object Orientation):
```udl
namespace azokle_protocol {
    string dispatch_json(string envelope_json);
    bytes dispatch_cbor(bytes envelope_cbor);
};
```
