import Foundation
import CryptoKit
import LocalAuthentication
import Security

// MARK: - Vault Credential Item
public struct VaultCredential: Identifiable, Hashable, Codable {
    public var id: UUID = UUID()
    public let service: String
    public let username: String
    public let passwordEncrypted: String
    public let dateCreated: Date

    public init(service: String, username: String, passwordEncrypted: String, dateCreated: Date = Date()) {
        self.service = service
        self.username = username
        self.passwordEncrypted = passwordEncrypted
        self.dateCreated = dateCreated
    }
}

// MARK: - Encrypted Sovereign Vault Manager
public final class EncryptedVaultManager: ObservableObject {
    public static let shared = EncryptedVaultManager()

    @Published public var isUnlocked: Bool = false
    @Published public var savedCredentials: [VaultCredential] = []

    private let keychainService = "com.azokle.browser.vault"
    private let masterKeyTag = "com.azokle.browser.masterkey"

    private init() {
        loadOrGenerateKey()
    }

    // MARK: - Biometric & Passcode Authentication (Fail-Closed Security)
    public func authenticate(reason: String = "Unlock Sovereign Vault", completion: @escaping (Bool) -> Void) {
        let context = LAContext()
        context.localizedCancelTitle = "Cancel"
        var error: NSError?

        // Check if device owner authentication (Biometrics or Passcode) is available
        if context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) {
            context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, _ in
                DispatchQueue.main.async {
                    self.isUnlocked = success
                    completion(success)
                }
            }
        } else {
            // Fail closed securely if device owner authentication cannot be evaluated
            DispatchQueue.main.async {
                self.isUnlocked = false
                completion(false)
            }
        }
    }

    public func lock() {
        isUnlocked = false
        cachedMasterKey = nil
    }

    private var cachedMasterKey: SymmetricKey? = nil

    // MARK: - Master Encryption Key (Secure Enclave / Hardware-Bound Keychain)
    private func loadOrGenerateKey() -> SymmetricKey {
        if let cached = cachedMasterKey {
            return cached
        }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: masterKeyTag,
            kSecReturnData as String: true,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecSuccess, let keyData = result as? Data {
            let key = SymmetricKey(data: keyData)
            self.cachedMasterKey = key
            return key
        } else {
            let newKey = SymmetricKey(size: .bits256)
            let rawData = newKey.withUnsafeBytes { Data($0) }

            let addQuery: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: keychainService,
                kSecAttrAccount as String: masterKeyTag,
                kSecValueData as String: rawData,
                kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            ]
            let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
            if addStatus == errSecDuplicateItem {
                SecItemDelete(addQuery as CFDictionary)
                SecItemAdd(addQuery as CFDictionary, nil)
            }
            self.cachedMasterKey = newKey
            return newKey
        }
    }

    // MARK: - AES-256-GCM Encryption / Decryption
    public func encryptString(_ plain: String) -> String? {
        // AZK-MED-08: Gate crypto operations behind authentication check
        guard isUnlocked else { return nil }
        guard let data = plain.data(using: .utf8) else { return nil }
        let key = loadOrGenerateKey()
        do {
            let sealed = try AES.GCM.seal(data, using: key)
            return sealed.combined?.base64EncodedString()
        } catch {
            return nil
        }
    }

    public func decryptString(_ cipherBase64: String) -> String? {
        // AZK-MED-08: Gate crypto operations behind authentication check
        guard isUnlocked else { return nil }
        guard let combinedData = Data(base64Encoded: cipherBase64) else { return nil }
        let key = loadOrGenerateKey()
        do {
            let sealedBox = try AES.GCM.SealedBox(combined: combinedData)
            let decryptedData = try AES.GCM.open(sealedBox, using: key)
            return String(data: decryptedData, encoding: .utf8)
        } catch {
            return nil
        }
    }

    // MARK: - Cryptographically Secure Password Generator (CSPRNG with Rejection Sampling)
    public func generateSecurePassword(length: Int = 20, includeSymbols: Bool = true) -> String {
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        let digits = "0123456789"
        let symbols = "!@#$%^&*()-_=+[]{}<>?"

        var pool = letters + digits
        if includeSymbols { pool += symbols }

        let poolArray = Array(pool)
        let poolSize = poolArray.count
        guard poolSize > 0, length > 0 else { return "" }

        // AZK-LOW-05: Rejection sampling threshold to completely eliminate modulo bias
        let maxValidByte = UInt8((256 / poolSize) * poolSize)
        var password = ""
        var byteBuffer = [UInt8](repeating: 0, count: 1)

        while password.count < length {
            let status = SecRandomCopyBytes(kSecRandomDefault, 1, &byteBuffer)
            guard status == errSecSuccess else { break }
            let b = byteBuffer[0]
            if b < maxValidByte {
                let index = Int(b) % poolSize
                password.append(poolArray[index])
            }
        }

        return password
    }

    public func calculatePasswordStrength(password: String) -> (score: Double, label: String) {
        if password.isEmpty { return (0.0, "Empty") }
        var score: Double = 0.0

        if password.count >= 8 { score += 0.25 }
        if password.count >= 14 { score += 0.25 }
        if password.rangeOfCharacter(from: .decimalDigits) != nil { score += 0.2 }
        if password.rangeOfCharacter(from: .punctuationCharacters) != nil || password.rangeOfCharacter(from: .symbols) != nil { score += 0.3 }

        let label: String
        switch score {
        case 0..<0.4: label = "Weak"
        case 0.4..<0.75: label = "Good"
        default: label = "Sovereign Strong"
        }

        return (min(1.0, score), label)
    }
}
