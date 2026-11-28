package com.voidlauncher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.voidlauncher.auth.MicrosoftAuthActivity
import com.voidlauncher.auth.MicrosoftAuthRepository
import com.voidlauncher.data.AccountRepository
import com.voidlauncher.data.JavaRepository
import com.voidlauncher.data.ModsRepository
import com.voidlauncher.data.SettingsRepository
import com.voidlauncher.data.VersionRepository
import com.voidlauncher.engine.EngineRepository
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.VoidToast
import com.voidlauncher.ui.screens.HomeScreen
import com.voidlauncher.ui.screens.LibraryScreen
import com.voidlauncher.ui.screens.SettingsScreen
import com.voidlauncher.ui.screens.VersionsScreen
import com.voidlauncher.ui.theme.VoidDesign
import com.voidlauncher.ui.theme.VoidTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var vm: VoidViewModel
    private lateinit var notificationPermission: ActivityResultLauncher<String>

    private val msLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val error = result.data?.getStringExtra(MicrosoftAuthActivity.EXTRA_ERROR)
        if (!error.isNullOrBlank()) {
            vm.notify("Microsoft sign-in failed: $error")
        } else if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra(MicrosoftAuthActivity.EXTRA_CODE)?.let { code ->
                lifecycleScope.launch { vm.completeMicrosoftLogin(code) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = applicationContext
                return VoidViewModel(
                    appContext = app,
                    accounts = AccountRepository(app),
                    versions = (application as VoidApplication).versions,
                    settingsRepo = (application as VoidApplication).settings,
                    javaRepo = (application as VoidApplication).javaRepo,
                    modsRepo = ModsRepository(app),
                    engines = (application as VoidApplication).engines,
                    msAuth = MicrosoftAuthRepository(app),
                ) as T
            }
        })[VoidViewModel::class.java]
        vm.attachActivity(this)

        setContent {
            val state by vm.ui.collectAsState()
            VoidTheme {
                VoidApp(
                    state = state,
                    vm = vm,
                    onMicrosoftLogin = { msLoginLauncher.launch(vm.microsoftLoginIntent()) },
                )
            }
        }
    }

    override fun onDestroy() {
        runCatching { vm.detachActivity(this) }
        super.onDestroy()
    }
}

private val NAV_ITEMS = listOf("Home", "Versions", "Library", "Settings")

@Composable
private fun VoidApp(state: UiState, vm: VoidViewModel, onMicrosoftLogin: () -> Unit) {
    var tab by remember { mutableStateOf("Home") }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        toast = message
        vm.consumeSnackbar()
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2400)
            toast = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(VoidDesign.Canvas)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "Home" -> HomeScreen(state, vm) { tab = it.replaceFirstChar { c -> c.uppercase() } }
                    "Versions" -> VersionsScreen(state, vm)
                    "Library" -> LibraryScreen(state, vm)
                    "Settings" -> SettingsScreen(state, vm, onMicrosoftLogin)
                }
            }
            VoidBottomBar(tab) { tab = it }
        }
        VoidToast(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 82.dp))
    }

    if (state.offlineNotice) {
        AlertDialog(
            onDismissRequest = { vm.dismissOfflineNotice() },
            containerColor = VoidDesign.Panel,
            title = { Text("VOID", style = VoidDesign.Headline) },
            text = {
                Text(
                    "Create an offline profile. VOID installs real game files and launches Minecraft inside this app.",
                    style = VoidDesign.Body,
                )
            },
            confirmButton = { TextButton(onClick = { vm.dismissOfflineNotice() }) { Text("START", color = VoidDesign.Ink) } },
        )
    }
}

@Composable
private fun VoidBottomBar(current: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(VoidDesign.Canvas)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(VoidDesign.Line))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 12.dp),
        ) {
            NAV_ITEMS.forEach { item ->
                val selected = item == current
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        item,
                        style = VoidDesign.Label.copy(
                            color = if (selected) VoidDesign.Ink else VoidDesign.Faint,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .height(1.dp)
                            .fillMaxWidth(if (selected) 0.22f else 0f)
                            .background(VoidDesign.Ink),
                    )
                }
            }
        }
    }
}
