#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BUILD_TYPE="${1:-Release}"
BUILD_DIR="${ROOT_DIR}/build_linux"

echo "============================================================"
echo "  Azokle Sovereign Browser — Linux Native Build (${BUILD_TYPE})"
echo "============================================================"

# 1. Build Rust Core C-ABI library
echo "--> Compiling Rust C-ABI core (profile: ${BUILD_TYPE})..."
if [ "${BUILD_TYPE}" = "Release" ]; then
    cargo build -p azokle-c-bridge --release --manifest-path "${ROOT_DIR}/Cargo.toml"
else
    cargo build -p azokle-c-bridge --manifest-path "${ROOT_DIR}/Cargo.toml"
fi

# 2. Configure CMake
echo "--> Configuring CMake in ${BUILD_DIR}..."
cmake -B "${BUILD_DIR}" \
      -S "${ROOT_DIR}" \
      -DCMAKE_BUILD_TYPE="${BUILD_TYPE}"

# 3. Compile Native C++ Targets
echo "--> Compiling C++20 desktop browser targets..."
cmake --build "${BUILD_DIR}" -- -j"$(nproc)"

BINARY_PATH="${BUILD_DIR}/apps/linux/azokle-browser-linux"
if [ -f "${BINARY_PATH}" ]; then
    echo "============================================================"
    echo "  BUILD SUCCESSFUL!"
    echo "  Binary: ${BINARY_PATH}"
    echo "  Size:   $(du -h "${BINARY_PATH}" | cut -f1)"
    echo "============================================================"
else
    echo "Error: Binary not generated at ${BINARY_PATH}" >&2
    exit 1
fi
