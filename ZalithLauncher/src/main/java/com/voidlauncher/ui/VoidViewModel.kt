package com.voidlauncher.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidlauncher.auth.MicrosoftAuthRepository
import com.voidlauncher.data.AccountRepository
import com.voidlauncher.data.JavaRepository
import com.voidlauncher.data.ModsRepository
import com.voidlauncher.data.SettingsRepository
import com.voidlauncher.data.VersionRepository
import com.voidlauncher.engine.EngineRepository
import com.voidlauncher.DownloadService
import com.voidlauncher.model.AccountType
import com.voidlauncher.model.AdvancedSettings
import com.voidlauncher.model.DownloadState
import com.voidlauncher.model.EngineInfo
import com.voidlauncher.model.InstalledVersion
import com.voidlauncher.model.GameFeature
import com.voidlauncher.model.JavaRuntime
import com.voidlauncher.model.LaunchProfile
import com.voidlauncher.model.LaunchSettings
import com.voidlauncher.model.ModEntry
import com.voidlauncher.model.VersionSummary
import com.voidlauncher.model.VoidAccount
import com.voidlauncher.util.VoidPaths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val accounts: List<VoidAccount> = emptyList(),
    val selectedAccountId: String? = null,
    val versions: List<VersionSummary> = emptyList(),
    val latestRelease: String = "",
    val versionLoading: Boolean = false,
    val versionError: String? = null,
    val search: String = "",
    val typeFilter: String = "All",
    val installedIds: Set<String> = emptySet(),
    val installedVersions: List<InstalledVersion> = emptyList(),
    val download: DownloadState = DownloadState(),
    val settings: LaunchSettings = LaunchSettings(),
    val advanced: AdvancedSettings = AdvancedSettings(),
    val javaDownload: DownloadState = DownloadState(),
    val profiles: List<LaunchProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val selectedVersionId: String? = null,
    val javaRuntimes: List<JavaRuntime> = emptyList(),
    val engines: List<EngineInfo> = emptyList(),
    val preferredEngine: String? = null,
    val mods: List<ModEntry> = emptyList(),
    val modsVersionId: String? = null,
    val msBusy: Boolean = false,
    val snackbar: String? = null,
    val lastLaunchMessage: String = "",
    val offlineNotice: Boolean = true,
    val loaderOptions: List<com.voidlauncher.engine.VoidLoaderOption> = emptyList(),
    val activeLoader: String = "Vanilla",
    val loaderBusy: Boolean = false,
    val loaderLoading: Boolean = false,
    val loaderTasks: List<String> = emptyList(),
    val modQuery: String = "",
    val modResults: List<com.voidlauncher.engine.VoidModResult> = emptyList(),
    val modSearching: Boolean = false,
    val modVersions: List<com.voidlauncher.engine.VoidModVersionOption> = emptyList(),
    val modVersionLoading: Boolean = false,
    val modSelected: com.voidlauncher.engine.VoidModResult? = null,
    val installedModFiles: Set<String> = emptySet(),
)

class VoidViewModel(
    private val appContext: Context,
    private val accounts: AccountRepository,
    private val versions: VersionRepository,
    private val settingsRepo: SettingsRepository,
    private val javaRepo: JavaRepository,
    private val modsRepo: ModsRepository,
    private val engines: EngineRepository,
    private val msAuth: MicrosoftAuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val root = VoidPaths.root(appContext)
    private var launchActivity: Activity? = null

    fun attachActivity(activity: Activity) { launchActivity = activity }
    private var pendingAutoLaunch = false
    private var pendingHandoffVersion: String?
        get() = settingsRepo.pendingHandoffVersion
        set(value) { settingsRepo.pendingHandoffVersion = value }
    private val launching = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        viewModelScope.launch {
            accounts.state.collect { store ->
                _ui.update {
                    it.copy(
                        accounts = store.accounts,
                        selectedAccountId = store.selectedId,
                    )
                }
            }
        }
        viewModelScope.launch {
            versions.manifest.collect { manifest ->
                _ui.update {
                    it.copy(
                        versions = manifest?.versions ?: emptyList(),
                        latestRelease = manifest?.latest?.release ?: "",
                    )
                }
                // First run: preselect an installed version or the latest release.
                if (settingsRepo.selectedVersionId == null) {
                    val installed = versions.installed.value.firstOrNull()?.id
                    val choice = installed ?: manifest?.latest?.release
                    choice?.let { selectVersion(it) }
                }
            }
        }
        viewModelScope.launch {
            versions.installed.collect { list ->
                _ui.update {
                    it.copy(
                        installedIds = list.map { v -> v.id }.toSet(),
                        installedVersions = list,
                    )
                }
                val current = settingsRepo.selectedVersionId
                if (list.isNotEmpty() && (current == null || list.none { it.id == current })) {
                    selectVersion(list.first().id)
                }
            }
        }
        viewModelScope.launch {
            versions.download.collect { state ->
                _ui.update { it.copy(download = state) }
                if (pendingAutoLaunch && state.success) {
                    pendingAutoLaunch = false
                    proceedToEngine()
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.settings.collect { s -> _ui.update { it.copy(settings = s) } }
        }
        viewModelScope.launch {
            settingsRepo.installedMods.collect {
                _ui.update { state ->
                    state.copy(installedModFiles = settingsRepo.installedFileNames(state.selectedVersionId.orEmpty()))
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.advanced.collect { a ->
                _ui.update { it.copy(advanced = a) }
                com.voidlauncher.util.NetConfig.autoRetry = a.autoRetry
                com.voidlauncher.util.NetConfig.maxAttempts = if (a.autoRetry) 3 else 1
                com.voidlauncher.util.NetConfig.parallelDownloads = a.downloadThreads
                com.voidlauncher.util.NetConfig.verifyDownloads = a.verifyDownloads
                    }
        }
        viewModelScope.launch {
            javaRepo.download.collect { d ->
                _ui.update { it.copy(javaDownload = d) }
                if ((d.success || d.error != null) && pendingHandoffVersion != null) {
                    pendingHandoffVersion = null
                    proceedToEngine()
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.profiles.collect { p ->
                _ui.update { it.copy(profiles = p.profiles, selectedProfileId = p.selectedId) }
            }
        }

        _ui.update {
            it.copy(
                selectedVersionId = settingsRepo.selectedVersionId,
                engines = engines.detect(),
                preferredEngine = settingsRepo.preferredEngine,
                lastLaunchMessage = settingsRepo.lastLaunchMessage,
                javaRuntimes = (javaRepo.scan() + com.voidlauncher.engine.VoidRuntimes.installed())
                    .distinctBy { it.path }
                    .sortedByDescending { it.majorVersion },
                offlineNotice = settingsRepo.onboardingDone.not(),
            )
        }
        refreshVersions()
        // A queued launch is only resumed when the user actually presses Play.
        pendingHandoffVersion = null

        // Repair version JSONs written by older VOID builds so the engine can parse them.
        viewModelScope.launch {
            val repaired = versions.repairVersionJson()
            if (repaired > 0) {
                versions.refreshInstalledFromDisk()
                notify("Repaired $repaired version file(s)")
            }
        }
    }

    // ---------------------------------------------------------------- account

    fun createOffline(username: String) {
        accounts.createOffline(username)
            .onSuccess { notify("Offline profile '$username' created") }
            .onFailure { notify(it.message ?: "Could not create profile") }
    }

    fun renameAccount(id: String, username: String) {
        accounts.rename(id, username)
            .onSuccess { notify("Profile renamed to ${it.username}") }
            .onFailure { notify(it.message ?: "Could not rename profile") }
    }

    fun deleteAccount(id: String) {
        val name = accounts.byId(id)?.username ?: "Profile"
        if (accounts.delete(id)) notify("$name removed") else notify("Could not remove profile")
    }

    fun selectAccount(id: String) {
        accounts.select(id)
    }

    fun microsoftLoginIntent() = msAuth.loginIntent()

    suspend fun completeMicrosoftLogin(code: String) {
        _ui.update { it.copy(msBusy = true) }
        val result = msAuth.completeLogin(code)
        result.onSuccess { session ->
            if (!session.uuid.isBlank()) {
                val account = accounts.addMicrosoftAccount(
                    username = session.username,
                    uuid = session.uuid,
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    xuid = session.xuid,
                    email = session.email,
                    entitlement = session.entitlement,
                )
                notify("Signed in as ${account.username}")
            } else {
                notify("Microsoft account has no Minecraft profile / entitlement")
            }
        }.onFailure { notify(it.message ?: "Microsoft sign-in failed") }
        _ui.update { it.copy(msBusy = false) }
    }

    fun dismissOfflineNotice() {
        settingsRepo.onboardingDone = true
        _ui.update { it.copy(offlineNotice = false) }
    }

    // ---------------------------------------------------------------- version

    fun refreshVersions() {
        _ui.update { it.copy(versionLoading = true, versionError = null) }
        viewModelScope.launch {
            val result = versions.refreshManifest()
            result.onFailure { notify("Offline: showing cached version list") }
            _ui.update {
                it.copy(
                    versionLoading = false,
                    versionError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun setSearch(value: String) = _ui.update { it.copy(search = value) }
    fun setTypeFilter(value: String) = _ui.update { it.copy(typeFilter = value) }

    fun selectVersion(id: String) {
        settingsRepo.selectedVersionId = id
        settingsRepo.lastLaunchMessage = ""
        _ui.update { it.copy(selectedVersionId = id, modsVersionId = id, lastLaunchMessage = "") }
        loadMods(id)
    }

    fun installVersion(id: String) {
        versions.install(id)
        notify("Installing $id — this downloads the real game files")
    }

    fun cancelInstall() {
        versions.cancelInstall()
        notify("Installation cancelled")
    }

    fun clearDownload() = versions.clearDownloadState()
    fun clearJavaDownload() = _ui.update { it.copy(javaDownload = com.voidlauncher.model.DownloadState()) }

    fun removeVersion(id: String) {
        if (versions.uninstall(id)) {
            if (_ui.value.selectedVersionId == id) {
                settingsRepo.selectedVersionId = null
                _ui.update { it.copy(selectedVersionId = null) }
            }
            notify("$id removed from library")
        }
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(transform: (LaunchSettings) -> LaunchSettings) {
        settingsRepo.updateSettings(transform)
    }

    fun updateAdvanced(transform: (AdvancedSettings) -> AdvancedSettings) {
        settingsRepo.updateAdvanced(transform)
    }

    /** Sets a renderer that actually exists in the engine registry. */
    fun setRenderer(uniqueId: String) {
        com.voidlauncher.engine.VoidRenderers.select(uniqueId)
        settingsRepo.updateAdvanced { it.copy(glImplementation = uniqueId) }
        _ui.update { it.copy(preferredEngine = uniqueId) }
        notify("Renderer changed to ${com.voidlauncher.engine.VoidRenderers.options().firstOrNull { it.uniqueId == uniqueId }?.name ?: uniqueId}")
    }

    fun createProfile(name: String) {
        val accountId = _ui.value.selectedAccountId
        val versionId = _ui.value.selectedVersionId
        if (accountId == null || versionId == null) {
            notify("Select an account and a version first")
            return
        }
        val entries = modsRepo.list(versionId)
        settingsRepo.createProfile(
            name = name,
            accountId = accountId,
            versionId = versionId,
            loader = _ui.value.activeLoader,
            mods = entries.filter { it.enabled }.map { it.fileName },
        )
            .onSuccess { notify("Profile '${it.name}' saved") }
            .onFailure { notify(it.message ?: "Could not save profile") }
    }

    fun deleteProfile(idOrName: String) {
        val profile = settingsRepo.profiles.value.profiles.firstOrNull { it.id == idOrName }
            ?: settingsRepo.profiles.value.profiles.firstOrNull { it.name.equals(idOrName, true) }
        if (profile == null) {
            notify("Profile not found")
            return
        }
        if (settingsRepo.deleteProfile(profile.id)) notify("Profile '${profile.name}' deleted")
        else notify("Could not delete profile")
    }

    fun selectProfile(id: String) {
        settingsRepo.selectProfile(id)
        settingsRepo.profiles.value.profiles.firstOrNull { it.id == id }?.let { profile ->
            settingsRepo.selectedVersionId = profile.versionId
            settingsRepo.updateSettings { it.copy(memoryMb = profile.memoryMb, jvmArgs = profile.jvmArgs) }
            accounts.select(profile.accountId)
            _ui.update {
                it.copy(
                    selectedVersionId = profile.versionId,
                    modsVersionId = profile.versionId,
                    activeLoader = profile.loader,
                )
            }
            loadMods(profile.versionId)
            applyProfileMods(profile)
        }
    }

    /** Applies a saved profile's mod selection to the version's mods folder. */
    private fun applyProfileMods(profile: LaunchProfile) {
        val entries = modsRepo.list(profile.versionId)
        entries.forEach { entry ->
            val shouldEnable = entry.fileName in profile.mods
            if (entry.enabled != shouldEnable) modsRepo.toggle(profile.versionId, entry.fileName)
        }
        loadMods(profile.versionId)
    }

    fun setLoader(name: String) {
        _ui.update {
            it.copy(
                activeLoader = name,
                loaderOptions = emptyList(),
                loaderLoading = name != "Vanilla",
            )
        }
        val version = _ui.value.selectedVersionId
        if (name == "Vanilla" || version == null) {
            _ui.update { it.copy(loaderLoading = false) }
            return
        }
        viewModelScope.launch {
            // Fetches can take a while on slow networks; keep a loading state and retry once.
            var options = com.voidlauncher.engine.VoidLoaderInstaller.list(name, version)
            if (options.isEmpty()) {
                kotlinx.coroutines.delay(1200)
                options = com.voidlauncher.engine.VoidLoaderInstaller.list(name, version)
            }
            _ui.update { it.copy(loaderOptions = options, loaderLoading = false) }
            if (options.isEmpty()) notify("No $name build listed for $version")
        }
    }

    init {
        viewModelScope.launch {
            com.voidlauncher.engine.VoidLoaderInstaller.loaderTasks.collect { tasks ->
                _ui.update { it.copy(loaderTasks = tasks) }
            }
        }
    }

    fun installLoader(loader: String, loaderVersion: String) {
        val version = _ui.value.selectedVersionId ?: return
        if (_ui.value.loaderBusy) return
        _ui.update { it.copy(loaderBusy = true) }
        viewModelScope.launch {
            runCatching {
                com.voidlauncher.engine.VoidLoaderInstaller.install(
                    context = appContext,
                    scope = viewModelScope,
                    loader = loader,
                    loaderVersion = loaderVersion,
                    minecraftVersion = version,
                )
            }.onSuccess { created ->
                versions.refreshInstalledFromDisk()
                selectVersion(created)
                _ui.update { it.copy(activeLoader = loader) }
                loadMods(created)
                notify("$loader $loaderVersion installed as $created")
            }.onFailure { notify(it.message ?: "Loader install failed") }
            _ui.update { it.copy(loaderBusy = false) }
        }
    }

    fun setModQuery(value: String) {
        _ui.update { it.copy(modQuery = value) }
    }

    fun searchMods() {
        val query = _ui.value.modQuery.trim()
        if (query.isEmpty()) {
            _ui.update { it.copy(modResults = emptyList(), modSearching = false) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(modSearching = true) }
            val results = com.voidlauncher.engine.VoidModBrowser.search(query, _ui.value.selectedVersionId)
                .map { result ->
                    val installed = result.id in settingsRepo.installedProjectIds(_ui.value.selectedVersionId.orEmpty())
                    result.copy(installed = installed)
                }
            _ui.update { it.copy(modResults = results, modSearching = false) }
        }
    }

    fun downloadMod(result: com.voidlauncher.engine.VoidModResult) {
        if (_ui.value.activeLoader == "Vanilla") {
            notify("To download mods, install a mod loader first")
            return
        }
        _ui.update {
            it.copy(
                modSelected = result,
                modVersionLoading = true,
                modVersions = emptyList(),
            )
        }
        viewModelScope.launch {
            runCatching {
                com.voidlauncher.engine.VoidModInstaller.listVersions(
                    projectId = result.id,
                    platform = result.platformKind,
                    minecraftVersion = _ui.value.selectedVersionId.orEmpty(),
                    installedFiles = settingsRepo.installedFileNames(_ui.value.selectedVersionId.orEmpty()),
                )
            }.onSuccess { versions ->
                _ui.update { it.copy(modVersions = versions, modVersionLoading = false) }
                if (versions.isEmpty()) notify("No files found for this Minecraft version")
            }.onFailure { th ->
                _ui.update { it.copy(modVersionLoading = false) }
                notify(th.message ?: "Could not load mod versions")
            }
        }
    }

    fun installModVersion(option: com.voidlauncher.engine.VoidModVersionOption) {
        val selected = _ui.value.modSelected ?: return
        val versionId = _ui.value.selectedVersionId ?: return
        if (_ui.value.modVersionLoading) return
        _ui.update { it.copy(modVersionLoading = true) }
        viewModelScope.launch {
            runCatching {
                com.voidlauncher.engine.VoidModInstaller.downloadExact(
                    context = appContext,
                    projectId = selected.id,
                    platform = selected.platformKind,
                    selectedVersionId = option.id,
                    minecraftVersion = versionId,
                    installedVersionId = versionId,
                )
            }.onSuccess { (fileName, projectId) ->
                settingsRepo.markModInstalled(versionId, projectId, fileName)
                loadMods(versionId)
                _ui.update { state ->
                    state.copy(
                        installedModFiles = settingsRepo.installedFileNames(versionId),
                        modVersions = state.modVersions.map {
                            if (it.id == option.id) it.copy(installed = true) else it
                        },
                        modResults = state.modResults.map {
                            if (it.id == projectId) it.copy(installed = true) else it
                        },
                    )
                }
                notify("$fileName installed")
            }.onFailure { th ->
                notify(th.message ?: "Mod download failed")
            }
            _ui.update { it.copy(modVersionLoading = false) }
        }
    }

    fun openMod(result: com.voidlauncher.engine.VoidModResult) {
        _ui.update { it.copy(modSelected = result) }
    }

    fun closeMod() {
        _ui.update { it.copy(modSelected = null, modVersions = emptyList()) }
    }

    private fun resolveDisplayName(uri: Uri, fallback: String): String {
        appContext.contentResolver.query(
            uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index)?.let { return it }
            }
        }
        return fallback
    }

    // ---------------------------------------------------------------- meta

    fun verifyInstalls(): Int {
        val list = versions.installed.value
        val valid = list.count { versions.verifyInstalled(it.id) }
        notify("$valid of ${list.size} installations verified")
        return valid
    }

    fun clearCaches(context: Context) {
        var removed = 0
        val candidates = listOf(
            File(com.voidlauncher.util.VoidPaths.root(context), "libraries"),
            File(com.voidlauncher.util.VoidPaths.root(context), "assets"),
        )
        candidates.forEach { dir ->
            dir.walkTopDown().filter { it.name.endsWith(".part") }.forEach { file ->
                if (file.delete()) removed++
            }
        }
        notify(if (removed == 0) "No partial downloads to clean" else "Cleaned $removed partial file(s)")
    }


    fun scanJava() {
        val found = (javaRepo.scan() + com.voidlauncher.engine.VoidRuntimes.installed())
            .distinctBy { it.path }
            .sortedByDescending { it.majorVersion }
        _ui.update { it.copy(javaRuntimes = found) }
        notify(if (found.isEmpty()) "No Java runtime unpacked yet - it unpacks on first launch" else "Found ${found.size} runtime(s)")
    }

    fun addJavaPath(path: String) {
        if (javaRepo.addCustomPath(path.trim())) notify("Java runtime added") else notify("Path does not exist")
    }

    fun removeJavaPath(path: String) {
        javaRepo.removeCustomPath(path)
        notify("Runtime removed")
    }

    // ---------------------------------------------------------------- engines

    fun refreshEngines() {
        val found = engines.detect()
        _ui.update { it.copy(engines = found) }
        clearStaleEngineMessage()
        notify("${found.count { it.installed }} compatible engine(s) found")
    }

    private fun clearStaleEngineMessage() {
        val message = settingsRepo.lastLaunchMessage
        if (message.contains("No compatible game engine", true) && engines.installedEngines().isNotEmpty()) {
            settingsRepo.lastLaunchMessage = ""
            _ui.update { it.copy(lastLaunchMessage = "") }
        }
    }

    fun setPreferredEngine(packageName: String?) {
        settingsRepo.preferredEngine = packageName
        _ui.update { it.copy(preferredEngine = packageName) }
        notify("Engine preference saved")
    }

    fun openEngineSource() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(EngineRepository.ENGINE_SOURCE_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
            .onFailure { notify("Open ${EngineRepository.ENGINE_SOURCE_URL} to download VOID Engine") }
    }

    fun downloadVoidEngine() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(EngineRepository.VOID_ENGINE_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
            .onFailure { notify("Open ${EngineRepository.VOID_ENGINE_URL} to get VOID Engine") }
    }

    /**
     * Prepares a real launch plan (paths, account, Java runtime, options, mods) and
     * hands the session to an installed engine. No success is claimed unless the
     * engine activity actually opens.
     */
    /** One tap: auto-select defaults, auto-install the game, auto-get the engine, then hand off. */
    fun launchGame() {
        if (!launching.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val state = _ui.value
                val account = accounts.byId(state.selectedAccountId) ?: accounts.accounts.firstOrNull()
                if (account == null) {
                    notify("Create an offline profile first")
                    return@launch
                }
                val versionId = state.selectedVersionId
                    ?: state.versions.firstOrNull { it.type.equals("release", true) }?.id
                if (versionId.isNullOrBlank()) {
                    notify("No Minecraft version available yet")
                    return@launch
                }
                if (state.selectedAccountId != account.id) accounts.select(account.id)
                if (state.selectedVersionId != versionId) selectVersion(versionId)

                if (!versions.verifyInstalled(versionId)) {
                    if (state.advanced.autoInstallGame) {
                        pendingAutoLaunch = true
                        DownloadService.startVersion(appContext, versionId)
                        notify("Auto-installing $versionId - Minecraft will open when it finishes")
                    } else {
                        notify("$versionId is not installed. Install it from Versions first.")
                    }
                    return@launch
                }
                proceedToEngine()
            } finally {
                launching.set(false)
            }
        }
    }

    private suspend fun proceedToEngine() {
        val state = _ui.value
        val versionId = state.selectedVersionId ?: return
        val account = accounts.byId(state.selectedAccountId) ?: accounts.accounts.firstOrNull() ?: return

        val details = runCatching { versions.detailsForInstalled(versionId) }.getOrNull()
        if (details != null) {
            val major = JavaRepository.requiredMajor(details)
            val runtime = javaRepo.selectFor(details, state.javaRuntimes, state.advanced.javaPath)
            if (runtime == null && major <= 17) {
                pendingHandoffVersion = versionId
                notify("Java $major missing - downloading ARM64 runtime")
                DownloadService.startJava(appContext, JavaRepository.componentFor(details, major), major)
                return
            }
        }

        accounts.touch(account.id)
        runCatching {
            com.voidlauncher.engine.VoidEngineBridge.launchInstalledVersion(
                context = launchActivity ?: appContext,
                versionId = versionId,
                account = account,
                settings = state.settings,
            )
        }.onSuccess {
            settingsRepo.lastLaunchMessage = "Launched $versionId in-process as ${account.username}"
            _ui.update { it.copy(lastLaunchMessage = settingsRepo.lastLaunchMessage) }
            notify("Launched $versionId")
        }.onFailure { th ->
            settingsRepo.lastLaunchMessage = "Launch failed: ${th.message ?: th::class.simpleName}"
            _ui.update { it.copy(lastLaunchMessage = settingsRepo.lastLaunchMessage) }
            notify(th.message ?: "Launch failed")
        }
    }

    private fun buildLaunchPlan(
        account: VoidAccount,
        versionId: String,
        state: UiState,
    ): Map<String, Any?> {
        val details = versions.detailsForInstalled(versionId)
        val runtime = javaRepo.selectFor(details, state.javaRuntimes)
        val mods = modsRepo.list(versionId).filter { it.enabled }.map { it.fileName }
        return mapOf(
            "format" to "void-launch-plan/1",
            "createdAt" to System.currentTimeMillis(),
            "account" to mapOf(
                "name" to account.username,
                "uuid" to account.uuid,
                "type" to account.type.name,
                "offline" to (account.type == AccountType.OFFLINE),
            ),
            "minecraft" to mapOf(
                "version" to versionId,
                "gameDirectory" to VoidPaths.versionDir(root, versionId).absolutePath,
                "librariesDirectory" to VoidPaths.libraries(root).absolutePath,
                "assetsDirectory" to VoidPaths.assets(root).absolutePath,
                "mainClass" to details.mainClass,
            ),
            "java" to mapOf(
                "path" to runtime?.path,
                "version" to runtime?.version,
                "requiredMajor" to JavaRepository.requiredMajor(details),
                "forcedPath" to state.advanced.javaPath.takeIf { state.advanced.javaMode == "Choose" },
                "selectionMode" to state.advanced.javaMode,
            ),
            "options" to mapOf(
                "memoryMb" to state.settings.memoryMb,
                "jvmArgs" to state.settings.jvmArgs,
            ),
            "advanced" to state.advanced,
            "mods" to mods,
            "note" to "Native rendering and touch input are supplied by the external engine.",
        )
    }

    private fun writeLaunchPlan(versionId: String, plan: Map<String, Any?>): File {
        val file = File(VoidPaths.exports(root), "void-session-$versionId.json")
        val json = org.json.JSONObject()
        plan.forEach { (key, value) -> json.put(key, value) }
        file.writeText(json.toString(2))
        return file
    }

    fun shareLaunchPlan(context: Context) {
        val file = File(VoidPaths.exports(root), "void-session-${_ui.value.selectedVersionId ?: "latest"}.json")
        if (!file.exists()) {
            notify("Open a session first to create a launch plan")
            return
        }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share VOID launch plan")) }
            .onFailure { notify("No app available to share the plan") }
    }

    // ---------------------------------------------------------------- mods

    fun loadMods(versionId: String) {
        _ui.update { it.copy(mods = modsRepo.list(versionId), modsVersionId = versionId) }
    }

    fun toggleMod(fileName: String) {
        val versionId = _ui.value.modsVersionId ?: return
        modsRepo.toggle(versionId, fileName)
            .onSuccess { enabled -> notify(if (enabled) "Mod enabled" else "Mod disabled") }
            .onFailure { notify(it.message ?: "Could not toggle mod") }
        loadMods(versionId)
    }

    fun deleteMod(fileName: String) {
        val versionId = _ui.value.modsVersionId ?: return
        if (modsRepo.delete(versionId, fileName)) notify("Mod deleted") else notify("Could not delete mod")
        loadMods(versionId)
    }

    fun importMod(uri: Uri, displayName: String) {
        val versionId = _ui.value.modsVersionId ?: _ui.value.selectedVersionId
        if (versionId.isNullOrBlank()) {
            notify("Select an installed version first")
            return
        }
        viewModelScope.launch {
            val realName = resolveDisplayName(uri, displayName)
            modsRepo.import(versionId, uri, realName)
                .onSuccess { notify("Added ${it.name} (${it.loader})") }
                .onFailure { notify(it.message ?: "Could not import mod") }
            loadMods(versionId)
        }
    }



    fun featureStatus(): List<GameFeature> = listOf(
        GameFeature("Offline accounts", "Multiple local profiles, persistent, last-used selection", com.voidlauncher.model.FeatureStatus.VERIFIED),
        GameFeature("Version browser", "Live Mojang manifest with search and filters", com.voidlauncher.model.FeatureStatus.VERIFIED),
        GameFeature("Game installation", "Client, libraries and assets with SHA-1 verification", com.voidlauncher.model.FeatureStatus.BUILT_NOT_DEVICE_VERIFIED),
        GameFeature("Java detection", "Reads release descriptors; multiple runtimes", com.voidlauncher.model.FeatureStatus.BUILT_NOT_DEVICE_VERIFIED),
        GameFeature("Microsoft sign-in", "OAuth -> Xbox Live -> XSTS -> Minecraft entitlement", com.voidlauncher.model.FeatureStatus.BUILT_NOT_DEVICE_VERIFIED),
        GameFeature("Mod management", "Jar detection, enable/disable, add/remove", com.voidlauncher.model.FeatureStatus.BUILT_NOT_DEVICE_VERIFIED),
        GameFeature("Game engine handoff", "Starts an already-installed compatible launcher with a prepared session", com.voidlauncher.model.FeatureStatus.BLOCKED),
        GameFeature("In-app Minecraft rendering", "Requires bundling a native JRE + LWJGL + GL translation stack", com.voidlauncher.model.FeatureStatus.BLOCKED),
        GameFeature("In-app touch controls", "Requires the native engine's input layer", com.voidlauncher.model.FeatureStatus.BLOCKED),
    )

    fun notify(message: String) {
        _ui.update { it.copy(snackbar = message) }
    }

    fun consumeSnackbar() = _ui.update { it.copy(snackbar = null) }

    fun refreshAll() {
        refreshVersions()
        refreshEngines()
        scanJava()
        _ui.value.selectedVersionId?.let { loadMods(it) }
    }

    fun refreshAccountToken(accountId: String) {
        val account = accounts.byId(accountId) ?: return
        val refresh = accounts.refreshTokenFor(account) ?: return
        viewModelScope.launch {
            msAuth.refresh(refresh)
                .onSuccess { session ->
                    accounts.updateTokens(accountId, session.accessToken, session.refreshToken)
                    accounts.setEntitlement(accountId, session.entitlement)
                    notify("Session refreshed")
                }
                .onFailure { notify(it.message ?: "Refresh failed") }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
