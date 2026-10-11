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

package com.azokle.search.passkey

data class PasskeyItem(
    val id: String,
    val rpId: String,
    val userName: String,
    val userHandle: String = "",
    val dateCreated: String,
    val lastUsed: String = "Recently",
    val isSovereign: Boolean = false
)

sealed class PasskeyAuthResult {
    data class Success(val responseJson: String) : PasskeyAuthResult()
    data class Error(val message: String, val errorCode: String? = null) : PasskeyAuthResult()
    object Cancelled : PasskeyAuthResult()
}
