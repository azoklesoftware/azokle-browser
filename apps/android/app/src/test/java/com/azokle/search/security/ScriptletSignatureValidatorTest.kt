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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class ScriptletSignatureValidatorTest {

    private lateinit var keyPair: KeyPair
    private lateinit var validator: ScriptletSignatureValidator

    @Before
    fun setUp() {
        val keyGen = KeyPairGenerator.getInstance("EC").apply {
            initialize(256)
        }
        keyPair = keyGen.generateKeyPair()
        validator = ScriptletSignatureValidator(keyPair.public)
    }

    private fun sign(data: ByteArray): String {
        val sig = Signature.getInstance(ScriptletSignatureValidator.SIGNATURE_ALGORITHM).apply {
            initSign(keyPair.private)
            update(data)
        }
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    @Test
    fun `valid scriptlet content and signature succeeds`() {
        val scriptContent = "console.log('Valid scriptlet');".toByteArray(Charsets.UTF_8)
        val signatureBase64 = sign(scriptContent)

        val result = validator.validate(scriptContent, signatureBase64)
        assertEquals(ScriptletValidationResult.Valid, result)
    }

    @Test
    fun `tampered scriptlet content fails verification`() {
        val originalContent = "console.log('Safe');".toByteArray(Charsets.UTF_8)
        val signatureBase64 = sign(originalContent)

        val tamperedContent = "console.log('Malicious injection');".toByteArray(Charsets.UTF_8)
        val result = validator.validate(tamperedContent, signatureBase64)

        assertEquals(ScriptletValidationResult.Invalid.SignatureVerificationFailed, result)
    }

    @Test
    fun `invalid base64 signature format returns SignatureFormat error`() {
        val content = "console.log('Test');".toByteArray(Charsets.UTF_8)
        val result = validator.validate(content, "Not-A-Valid-Base64!@#$")

        assertEquals(ScriptletValidationResult.Invalid.SignatureFormat, result)
    }

    @Test
    fun `public key base64 parse utility roundtrips accurately`() {
        val pubBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val reconstructedKey = ScriptletSignatureValidator.parsePublicKeyFromBase64(pubBase64)

        assertEquals(keyPair.public, reconstructedKey)
    }
}
