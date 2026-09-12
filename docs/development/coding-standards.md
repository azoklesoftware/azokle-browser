# Coding Standards & Guidelines

Azokle Browser maintains strict engineering standards across Rust, C++20, Kotlin, and Swift.

---

## 1. Rust Conventions

1. **Explicit Error Handling**: Avoid unprincipled `unwrap()` or `expect()` in library crates (`crates/`). Always use `Result<T, AzokleError>` with context-rich error variants.
2. **Panic Barriers**: All public `extern "C"` functions in `bindings/c` must wrap their logic in `std::panic::catch_unwind(AssertUnwindSafe(...))`.
3. **Zeroization of Sensitive State**: Any struct containing cryptographic keys, passkey material, or authentication tokens must implement `zeroize::ZeroizeOnDrop`.
4. **Clippy Enforcement**: Run `cargo clippy --workspace --all-targets -- -D warnings` before committing.

---

## 2. Modern C++20 Guidelines

1. **Memory Ownership**: Raw pointers (`void*`, `T*`) are strictly reserved for non-owning native window handles and C-ABI interop. All engine objects and sessions must use `std::shared_ptr` or `std::unique_ptr`.
2. **No Raw Threading**: Use `std::atomic` for flags, `std::mutex` with `std::lock_guard` for shared state, and task runners for asynchronous work.
3. **Header Self-Containment**: Every header file must use `#pragma once` and include all necessary forward declarations or headers to be self-sufficient.
4. **Virtual Interfaces**: Abstract interfaces (`IBrowserEngine`, `IBrowserSession`, `IEngineDelegate`) must declare virtual destructors `= default`.

---

## 3. Kotlin & Android Guidelines

1. **Jetpack Compose State**: State must flow unidirectionally. Use `StateFlow` and `collectAsStateWithLifecycle()`.
2. **Coroutine Scopes**: Never use `GlobalScope`. Bind all background operations to `viewModelScope` or structured lifecycle scopes.
3. **JNI Safety**: Check JNI exception status (`env->ExceptionCheck()`) after every JNI invocation.

---

## 4. Swift & Apple Guidelines

1. **SwiftUI State Binding**: View state must be held by `@StateObject` or `@ObservedObject` adhering to ObservableObject protocols.
2. **Memory Safety**: Avoid force unwrapping (`!`). Use `guard let` or `if let` for safe optional unwrapping.
