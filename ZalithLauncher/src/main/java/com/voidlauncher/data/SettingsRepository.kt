package com.voidlauncher.data

import android.content.Context
import com.voidlauncher.model.AdvancedSettings
import com.voidlauncher.model.LaunchProfile
import com.voidlauncher.model.LaunchSettings
import com.voidlauncher.model.ProfileStore
import com.voidlauncher.util.newId
import com.voidlauncher.util.voidJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("void_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LaunchSettings> = _settings.asStateFlow()

    private val _advanced = MutableStateFlow(loadAdvanced())
    val advanced: StateFlow<AdvancedSettings> = _advanced.asStateFlow()

    private val _profiles = MutableStateFlow(loadProfiles())
    val profiles: StateFlow<ProfileStore> = _profiles.asStateFlow()

    var selectedVersionId: String?
        get() = prefs.getString("selected_version", null)
        set(value) = prefs.edit().putString("selected_version", value).apply()

    var preferredEngine: String?
        get() = prefs.getString("preferred_engine", null)
        set(value) = prefs.edit().putString("preferred_engine", value).apply()

    var lastLaunchMessage: String
        get() = prefs.getString("last_launch_message", "") ?: ""
        set(value) = prefs.edit().putString("last_launch_message", value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()

    var autoSelectDefaults: Boolean
        get() = prefs.getBoolean("auto_select_defaults", true)
        set(value) = prefs.edit().putBoolean("auto_select_defaults", value).apply()

    var pendingHandoffVersion: String?
        get() = prefs.getString("pending_handoff_version", null)
        set(value) = prefs.edit().putString("pending_handoff_version", value).apply()

    private fun loadSettings(): LaunchSettings = try {
        val raw = prefs.getString("launch_settings", null)
        if (raw.isNullOrBlank()) LaunchSettings() else voidJson.decodeFromString(raw)
    } catch (e: Exception) {
        LaunchSettings()
    }

    fun updateSettings(transform: (LaunchSettings) -> LaunchSettings) {
        _settings.value = transform(_settings.value)
        prefs.edit().putString("launch_settings", voidJson.encodeToString(_settings.value)).apply()
    }

    private fun loadAdvanced(): AdvancedSettings = try {
        val raw = prefs.getString("advanced_settings", null)
        if (raw.isNullOrBlank()) AdvancedSettings() else voidJson.decodeFromString(raw)
    } catch (e: Exception) {
        AdvancedSettings()
    }

    fun updateAdvanced(transform: (AdvancedSettings) -> AdvancedSettings) {
        _advanced.value = transform(_advanced.value)
        prefs.edit().putString("advanced_settings", voidJson.encodeToString(_advanced.value)).apply()
    }

    private fun loadProfiles(): ProfileStore = try {
        val raw = prefs.getString("profiles", null)
        if (raw.isNullOrBlank()) ProfileStore() else voidJson.decodeFromString(raw)
    } catch (e: Exception) {
        ProfileStore()
    }

    fun createProfile(
        name: String,
        accountId: String,
        versionId: String,
        loader: String = "vanilla",
    ): Result<LaunchProfile> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Give the profile a name"))
        if (_profiles.value.profiles.any { it.name.equals(trimmed, true) }) {
            return Result.failure(IllegalArgumentException("A profile with that name already exists"))
        }
        val s = _settings.value
        val profile = LaunchProfile(
            id = newId(),
            name = trimmed,
            accountId = accountId,
            versionId = versionId,
            memoryMb = s.memoryMb,
            jvmArgs = s.jvmArgs,
        )
        _profiles.value = _profiles.value.copy(
            profiles = _profiles.value.profiles + profile,
            selectedId = profile.id,
        )
        persistProfiles()
        return Result.success(profile)
    }

    fun deleteProfile(id: String): Boolean {
        val remaining = _profiles.value.profiles.filterNot { it.id == id }
        if (remaining.size == _profiles.value.profiles.size) return false
        _profiles.value = _profiles.value.copy(
            profiles = remaining,
            selectedId = if (_profiles.value.selectedId == id) remaining.firstOrNull()?.id else _profiles.value.selectedId,
        )
        persistProfiles()
        return true
    }

    fun selectProfile(id: String): Boolean {
        if (_profiles.value.profiles.none { it.id == id }) return false
        _profiles.value = _profiles.value.copy(selectedId = id)
        persistProfiles()
        return true
    }

    fun selectedProfile(): LaunchProfile? = _profiles.value.profiles
        .firstOrNull { it.id == _profiles.value.selectedId }

    private fun persistProfiles() {
        prefs.edit().putString("profiles", voidJson.encodeToString(_profiles.value)).apply()
    }
}
