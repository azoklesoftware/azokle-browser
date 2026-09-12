# Comprehensive Multi-Platform Build Guide

This document contains step-by-step instructions for compiling every component of the Azokle Browser across supported target architectures.

---

## 1. Building the Rust Core Workspace

Compile all 18 crates, FFI bridges, and unit test binaries:

```bash
# Debug build
cargo build --workspace

# Release build with LTO and optimizations
cargo build --workspace --release

# Run all workspace unit tests
cargo test --workspace --all-targets
```

---

## 2. Building the C++ Engine & Linux Desktop App

Build the canonical `azokle_engine` library and `azokle-browser-linux` executable:

```bash
# Configure CMake
cmake -B build_linux -S . -DCMAKE_BUILD_TYPE=RelWithDebInfo

# Compile targets
cmake --build build_linux --target azokle-browser-linux -j$(nproc)

# Launch the Linux browser
./build_linux/apps/linux/azokle-browser-linux
```

---

## 3. Building the Android Application

Compile the native Android app with GeckoView and Rust JNI bindings:

```bash
cd apps/android

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Output APK path:
# apps/android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Building the Windows Presentation Shell

On Windows hosts (or via cross-compilation with mingw/clang-cl):

```cmd
cmake -B build_win -S . -DBUILD_WINDOWS=ON
cmake --build build_win --target azokle-browser-windows --config Release
```

---

## 5. Building macOS & iOS Swift Applications

Generate Swift UniFFI bindings and build with SwiftPM:

```bash
# 1. Build Rust cdylib
cargo build -p azokle-uniffi

# 2. Generate Swift bindings
cargo run --bin azokle-bindgen -- generate \
    --library target/debug/libazokle_uniffi.so \
    --language swift \
    --out-dir bindings/swift

# 3. Build macOS App
cd apps/macos
swift build

# 4. Build iOS App
cd ../ios
swift build
```
