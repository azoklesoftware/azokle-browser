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

    private let publicKey: P256.Signing.PublicKey?

    public init(rawPublicKey: Data? = nil) {
        if let raw = rawPublicKey {
            self.publicKey = try? P256.Signing.PublicKey(rawRepresentation: raw)
        } else {
            self.publicKey = nil
        }
    }

    /**
     * AZK-MED-09: Cryptographically verifies scriptlet content integrity and authenticity.
     * Enforces strict UTF-8 validation and ECDSA P-256 signature verification.
     */
    public func validate(content: Data, signatureBase64: String) -> ScriptletValidationResult {
        // 1. Verify strict UTF-8 text encoding
        guard String(data: content, encoding: .utf8) != nil else {
            return .invalidEncoding
        }

        // 2. Signature parameter is mandatory
        let trimmedSig = signatureBase64.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let signatureData = Data(base64Encoded: trimmedSig) else {
            return .invalidSignatureFormat
        }

        // 3. Cryptographically verify signature using ECDSA P-256
        if let pubKey = self.publicKey,
           let ecdsaSig = try? P256.Signing.ECDSASignature(derRepresentation: signatureData) {
            if pubKey.isValidSignature(ecdsaSig, for: content) {
                return .valid
            }
        }

        return .signatureVerificationFailed
    }
}
