package com.voidlauncher.data

import android.content.Context
import com.voidlauncher.model.AccountStore
import com.voidlauncher.model.AccountType
import com.voidlauncher.model.VoidAccount
import com.voidlauncher.util.TokenVault
import com.voidlauncher.util.newId
import com.voidlauncher.util.offlineUuid
import com.voidlauncher.util.voidJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("void_accounts", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AccountStore> = _state.asStateFlow()

    val accounts: List<VoidAccount> get() = _state.value.accounts

    fun selected(): VoidAccount? = _state.value.accounts
        .firstOrNull { it.id == _state.value.selectedId }

    fun byId(id: String?): VoidAccount? = _state.value.accounts.firstOrNull { it.id == id }

    private fun load(): AccountStore = try {
        val raw = prefs.getString("store", null)
        if (raw.isNullOrBlank()) AccountStore() else voidJson.decodeFromString<AccountStore>(raw)
    } catch (e: Exception) {
        AccountStore()
    }

    private fun persist() {
        prefs.edit().putString("store", voidJson.encodeToString(_state.value)).apply()
    }

    fun createOffline(usernameRaw: String): Result<VoidAccount> {
        val username = usernameRaw.trim()
        val problem = validateUsername(username)
        if (problem != null) return Result.failure(IllegalArgumentException(problem))
        if (accounts.any { it.type == AccountType.OFFLINE && it.username.equals(username, true) }) {
            return Result.failure(IllegalArgumentException("An offline profile with that name already exists"))
        }
        val now = System.currentTimeMillis()
        val account = VoidAccount(
            id = newId(),
            type = AccountType.OFFLINE,
            username = username,
            uuid = offlineUuid(username),
            createdAt = now,
            lastUsedAt = now,
        )
        _state.value = _state.value.copy(
            accounts = accounts + account,
            selectedId = account.id,
            lastOfflineUsername = username,
        )
        persist()
        return Result.success(account)
    }

    fun rename(accountId: String, newUsernameRaw: String): Result<VoidAccount> {
        val account = byId(accountId)
            ?: return Result.failure(IllegalArgumentException("Profile not found"))
        if (account.type != AccountType.OFFLINE) {
            return Result.failure(IllegalArgumentException("Microsoft profiles cannot be renamed here"))
        }
        val username = newUsernameRaw.trim()
        val problem = validateUsername(username)
        if (problem != null) return Result.failure(IllegalArgumentException(problem))
        if (accounts.any { it.id != accountId && it.username.equals(username, true) }) {
            return Result.failure(IllegalArgumentException("Another profile already uses that name"))
        }
        val updated = account.copy(username = username, uuid = offlineUuid(username))
        _state.value = _state.value.copy(
            accounts = accounts.map { if (it.id == accountId) updated else it },
            lastOfflineUsername = if (_state.value.selectedId == accountId) username else _state.value.lastOfflineUsername,
        )
        persist()
        return Result.success(updated)
    }

    fun delete(accountId: String): Boolean {
        val account = byId(accountId) ?: return false
        account.tokenAlias?.let { alias ->
            TokenVault.delete(alias)
            TokenVault.delete("${alias}_refresh")
        }
        val remaining = accounts.filter { it.id != accountId }
        val newSelection = when {
            _state.value.selectedId != accountId -> _state.value.selectedId
            else -> remaining.firstOrNull { it.type == AccountType.OFFLINE }?.id
                ?: remaining.firstOrNull()?.id
        }
        _state.value = _state.value.copy(accounts = remaining, selectedId = newSelection)
        persist()
        return true
    }

    fun select(accountId: String): Boolean {
        if (byId(accountId) == null) return false
        _state.value = _state.value.copy(selectedId = accountId)
        persist()
        return true
    }

    /** Records that an account was actually used for a launch, for last-used selection. */
    fun touch(accountId: String) {
        _state.value = _state.value.copy(
            accounts = accounts.map {
                if (it.id == accountId) it.copy(lastUsedAt = System.currentTimeMillis()) else it
            },
        )
        persist()
    }

    fun addMicrosoftAccount(
        username: String,
        uuid: String,
        accessToken: String,
        refreshToken: String,
        xuid: String?,
        email: String?,
        entitlement: Boolean,
    ): VoidAccount {
        val alias = "ms_${newId()}"
        TokenVault.store(alias, accessToken)
        TokenVault.store("${alias}_refresh", refreshToken)
        val account = VoidAccount(
            id = newId(),
            type = AccountType.MICROSOFT,
            username = username,
            uuid = uuid,
            tokenAlias = alias,
            xuid = xuid,
            email = email,
            entitlement = entitlement,
            lastUsedAt = System.currentTimeMillis(),
        )
        _state.value = _state.value.copy(
            accounts = accounts.filterNot {
                it.type == AccountType.MICROSOFT && it.username.equals(username, true)
            } + account,
            selectedId = account.id,
        )
        persist()
        return account
    }

    fun refreshTokenFor(account: VoidAccount): String? =
        account.tokenAlias?.let { TokenVault.read("${it}_refresh") }

    fun accessTokenFor(account: VoidAccount): String? =
        account.tokenAlias?.let { TokenVault.read(it) }

    fun updateTokens(accountId: String, accessToken: String, refreshToken: String? = null) {
        val account = byId(accountId) ?: return
        val alias = account.tokenAlias ?: return
        TokenVault.store(alias, accessToken)
        if (refreshToken != null) TokenVault.store("${alias}_refresh", refreshToken)
    }

    fun setEntitlement(accountId: String, entitled: Boolean) {
        _state.value = _state.value.copy(
            accounts = accounts.map { if (it.id == accountId) it.copy(entitlement = entitled) else it },
        )
        persist()
    }

    companion object {
        fun validateUsername(username: String): String? = when {
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 16 -> "Username must be at most 16 characters"
            !username.matches(Regex("[A-Za-z0-9_]+")) -> "Use letters, numbers and underscores only"
            else -> null
        }
    }
}
