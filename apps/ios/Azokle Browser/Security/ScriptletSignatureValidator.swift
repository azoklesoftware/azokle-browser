import Foundation
import CryptoKit

// MARK: - Scriptlet Validation Result (1:1 Android ScriptletSignatureValidator.kt Parity)
public enum ScriptletValidationResult {
    case valid
    case invalidEncoding
    case invalidSignatureFormat
    case signatureVerificationFailed
}

// MARK: - Enterprise Cryptographic Scriptlet Validator
public class ScriptletSignatureValidator {
    public static let shared = ScriptletSignatureValidator()

    private init() {}

    /**
     * Validates that scriptlet content is valid UTF-8 and integrity-intact.
     */
    public func validate(content: Data, expectedSha256: String? = nil) -> ScriptletValidationResult {
        // 1. Verify strict UTF-8 text encoding
        guard String(data: content, encoding: .utf8) != nil else {
            return .invalidEncoding
        }

        // 2. Check SHA-256 digest if expected hash is supplied
        if let expected = expectedSha256?.lowercased().replacingOccurrences(of: ":", with: "").trimmingCharacters(in: .whitespacesAndNewlines) {
            let digest = SHA256.hash(data: content)
            let hexDigest = digest.map { String(format: "%02x", $0) }.joined()
            if hexDigest != expected {
                return .signatureVerificationFailed
            }
        }

        return .valid
    }
}
