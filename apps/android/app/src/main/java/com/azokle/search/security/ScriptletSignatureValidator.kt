/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.security

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

sealed interface ScriptletValidationResult {
    data object Valid : ScriptletValidationResult
    sealed interface Invalid : ScriptletValidationResult {
        data object Encoding : Invalid
        data object SignatureFormat : Invalid
        data object SignatureVerificationFailed : Invalid
    }
}

/**
 * Enterprise Cryptographic Validator for Ad-blocking & Anti-Tracking Scriptlets.
 * Implements ECDSA (SHA256withECDSA) public-key signature verification modeled after
 * DuckDuckGo's browser defense architecture. Guarantees that only cryptographically
 * signed, tamper-free scriptlets are permitted to run in the engine context.
 */
class ScriptletSignatureValidator(
    private val publicKey: PublicKey
) {
    companion object {
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val KEY_ALGORITHM = "EC"

        /**
         * Utility to construct an ECDSA PublicKey from an X.509 Base64-encoded string.
         */
        fun parsePublicKeyFromBase64(base64PublicKey: String): PublicKey {
            val keyBytes = Base64.getDecoder().decode(base64PublicKey.trim())
            val spec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            return keyFactory.generatePublic(spec)
        }
    }

    /**
     * Verifies that the given scriptlet content is valid UTF-8 and signed by the trusted authority.
     */
    fun validate(content: ByteArray, signatureBase64: String): ScriptletValidationResult {
        // 1. Verify strict UTF-8 text encoding
        try {
            StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(content))
        } catch (e: CharacterCodingException) {
            return ScriptletValidationResult.Invalid.Encoding
        }

        // 2. Decode Base64 signature
        val signatureBytes = try {
            Base64.getDecoder().decode(signatureBase64.trim())
        } catch (e: IllegalArgumentException) {
            return ScriptletValidationResult.Invalid.SignatureFormat
        }

        // 3. Cryptographically verify signature
        return try {
            val signatureInstance = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
                initVerify(publicKey)
                update(content)
            }
            if (signatureInstance.verify(signatureBytes)) {
                ScriptletValidationResult.Valid
            } else {
                ScriptletValidationResult.Invalid.SignatureVerificationFailed
            }
        } catch (t: Throwable) {
            ScriptletValidationResult.Invalid.SignatureVerificationFailed
        }
    }
}
