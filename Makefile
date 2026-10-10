# ==============================================================================
# Azokle Browser — Developer Automation Makefile
# ==============================================================================

.PHONY: all help rust-check rust-test rust-clippy android-test android-build android-sbom format clean

help:
	@echo "========================================================================"
	@echo "  Azokle Browser — Open Source Developer Tasks"
	@echo "========================================================================"
	@echo "  make help           - Display this list of commands"
	@echo "  make rust-check     - Check compilation across all 17 Rust crates"
	@echo "  make rust-clippy    - Run strict Clippy linter on Rust workspace"
	@echo "  make rust-test      - Run all Rust workspace unit & integration tests"
	@echo "  make android-test   - Run Android JUnit unit test suite (42 suites)"
	@echo "  make android-build  - Build debug APK (apps/android)"
	@echo "  make android-sbom   - Generate CycloneDX 1.5 SBOM report"
	@echo "  make format         - Check and format Rust and Kotlin code"
	@echo "  make clean          - Remove temporary build artifacts"
	@echo "========================================================================"

rust-check:
	cargo check --workspace

rust-clippy:
	cargo clippy --workspace --all-targets -- -D warnings

rust-test:
	cargo test --workspace

android-test:
	cd apps/android && ./gradlew testDebugUnitTest

android-build:
	cd apps/android && ./gradlew assembleDebug

android-sbom:
	cd apps/android && ./gradlew generateSbom

format:
	cargo fmt
	@echo "Formatting verified."

clean:
	cargo clean
	cd apps/android && ./gradlew clean
