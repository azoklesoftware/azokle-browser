package com.azokle.search.security.passkey

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
