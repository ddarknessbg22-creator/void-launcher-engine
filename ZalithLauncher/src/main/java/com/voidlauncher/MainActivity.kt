package com.voidlauncher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
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
import com.voidlauncher.ui.screens.HomeScreen
import com.voidlauncher.ui.screens.LibraryScreen
import com.voidlauncher.ui.screens.SettingsScreen
import com.voidlauncher.ui.screens.VersionsScreen
import com.voidlauncher.ui.theme.Amoled
import com.voidlauncher.ui.theme.Grey400
import com.voidlauncher.ui.theme.Hairline
import com.voidlauncher.ui.theme.VoidTheme
import com.voidlauncher.ui.components.VoidToast
import com.voidlauncher.ui.theme.White
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

        notificationPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted) vm.notify("Downloads still work, but progress notifications are hidden")
        }
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
}

private val TABS = listOf("Home", "Versions", "Library", "Settings")

@Composable
private fun VoidApp(state: UiState, vm: VoidViewModel, onMicrosoftLogin: () -> Unit) {
    var tab by remember { mutableStateOf("Home") }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        toast = message
        vm.consumeSnackbar()
    }
    // Separate effect: clearing the snackbar cancels the effect above, but not this one.
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2400)
            toast = null
        }
    }

    Box(Modifier.fillMaxSize().background(Amoled)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "Home" -> HomeScreen(state, vm) { tab = it.replaceFirstChar { c -> c.uppercase() } }
                    "Versions" -> VersionsScreen(state, vm)
                    "Library" -> LibraryScreen(state, vm)
                    "Settings" -> SettingsScreen(state, vm, onMicrosoftLogin)
                }
            }
            BottomBar(tab) { tab = it }
        }
        VoidToast(
            message = toast,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp),
        )
    }

    if (state.offlineNotice) {
        AlertDialog(
            onDismissRequest = { vm.dismissOfflineNotice() },
            containerColor = com.voidlauncher.ui.theme.Surface1,
            title = { Text("VOID", color = White, style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)) },
            text = {
                Text(
                    "Create an offline profile. VOID installs the real game files, fetches a compatible engine " +
                        "automatically and hands off the session for rendering.",
                    color = Grey400,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissOfflineNotice() }) { Text("Start", color = White) }
            },
        )
    }
}

/** Pure black bar with hairline divider, minimal label-only tabs. */
@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Row(
            Modifier.fillMaxWidth().background(Amoled).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TABS.forEach { t ->
                val selected = current == t
                Column(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .clickable { onSelect(t) }
                            .padding(horizontal = 14.dp),
                    ) {
                        Text(
                            t,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) White else Grey400,
                            ),
                        )
                    }
                    Spacer1(selected)
                }
            }
        }
    }
}

@Composable
private fun Spacer1(selected: Boolean) {
    Box(
        Modifier
            .padding(top = 5.dp)
            .size(width = 16.dp, height = 1.dp)
            .background(if (selected) White else androidx.compose.ui.graphics.Color.Transparent),
    )
}
