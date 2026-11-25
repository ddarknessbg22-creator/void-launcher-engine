package com.voidlauncher.model

import kotlinx.serialization.Serializable

enum class AccountType { OFFLINE, MICROSOFT }

/**
 * An account known to VOID. Microsoft access/refresh tokens are NEVER stored here:
 * they live in the Keystore-backed TokenVault under [tokenAlias].
 */
@Serializable
data class VoidAccount(
    val id: String,
    val type: AccountType,
    val username: String,
    val uuid: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0L,
    val tokenAlias: String? = null,
    val entitlement: Boolean = false,
    val xuid: String? = null,
    val email: String? = null,
)

@Serializable
data class AccountStore(
    val accounts: List<VoidAccount> = emptyList(),
    val selectedId: String? = null,
    val lastOfflineUsername: String? = null,
)
