# Testing & Quality Assurance Strategy

Azokle Browser employs a rigorous multi-tier testing strategy encompassing Rust unit testing, C++ headless engine mocks, Android unit/instrumentation testing, and end-to-end regression validation.

---

## 1. Testing Pyramid

```
                / \
               /   \      E2E Integration & Live Navigation Tests
              /-----\
             /       \    Headless MockBrowserEngine Navigation Tests
            /---------\
           /           \  Android Gradle Unit Tests (Robolectric / JUnit)
          /-------------\
         /               \ Rust Workspace Unit & Property Tests (18 Crates)
        +-----------------+
```

---

## 2. Test Execution Commands

### Rust Workspace Tests
Executes unit tests across all 18 crates, integration tests, and documentation tests:
```bash
cargo test --workspace --all-targets
```

### UniFFI Foreign Interface Tests
Validates that Rust object constructors, methods, and record serialization work over foreign bindings:
```bash
cargo test -p azokle-uniffi
```

### Android Unit & Architecture Tests
Validates Kotlin coroutines, ViewModel state, JNI bindings, and security intent parsers:
```bash
cd apps/android && ./gradlew testDebugUnitTest
```

### C++ Mock Engine Headless Tests
Verifies that `MockBrowserEngine` faithfully simulates navigation lifecycle events (`OnPageStarted`, `OnProgressChange`, `OnPageFinished`, `OnSecurityChange`) without requiring a display server.

---

## 3. Continuous Integration Invariants
All pull requests must satisfy the following zero-compromise quality gates:
1. `cargo test --workspace` passes with 0 failures and 0 warnings.
2. `cmake --build build_linux` completes without compiler warnings.
3. Android Gradle build succeeds with configuration cache enabled.
4. No secrets or private keys are committed in code or git history.
