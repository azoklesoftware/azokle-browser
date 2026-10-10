# Getting Started & Developer Prerequisites

This guide outlines the system prerequisites, developer toolchains, and environment configuration required to develop and build the Azokle Browser.

---

## 1. Toolchain Requirements

### Core Languages & Compilers
- **Rust**: Version 1.75 or later with Cargo.
  ```bash
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
  rustup default stable
  ```
- **C++ Compiler**: C++20 compliant compiler:
  - Linux: GCC 11+ or Clang 14+
  - Windows: MSVC 2022 (v143) or Clang-cl
  - macOS: Xcode 15+ / Apple Clang
- **CMake**: Version 3.20 or later.
- **Ninja**: Recommended build generator for fast C++ builds.

### Platform-Specific SDKs
- **Android**:
  - Android Studio Hedgehog (2023.1.1) or newer
  - Android SDK (API 34 / Android 14)
  - Android NDK (r25c or newer)
  - Java Development Kit (JDK 17)
- **Apple (macOS & iOS)**:
  - macOS 13+ with Xcode 15+
  - Swift 5.9+ toolchain
- **Linux**:
  - `libx11-dev`, `libgtk-3-dev`, `pkg-config`, `libwebkit2gtk-4.1-dev`

---

## 2. Repository Setup

Clone the repository and inspect the workspace:
```bash
git clone https://github.com/azoklesoftware/azokle-browser.git
cd azokle-browser

# Verify Rust workspace compilation
cargo check --workspace

# Verify C++ engine configuration
cmake -B build_dev -S . -DCMAKE_BUILD_TYPE=Debug
```
