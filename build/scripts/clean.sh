#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "--> Cleaning Azokle build outputs..."

# Clean CMake outputs
rm -rf "${ROOT_DIR}/build_linux"
rm -rf "${ROOT_DIR}/build_test"
rm -rf "${ROOT_DIR}/build_engine"
rm -rf "${ROOT_DIR}/build_output"

# Clean Rust target
cargo clean --manifest-path "${ROOT_DIR}/Cargo.toml"

# Clean Android build
if [ -d "${ROOT_DIR}/apps/android" ]; then
    (cd "${ROOT_DIR}/apps/android" && ./gradlew clean || true)
fi

echo "--> Clean complete!"
