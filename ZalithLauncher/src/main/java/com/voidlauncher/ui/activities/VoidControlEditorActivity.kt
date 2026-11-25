package com.voidlauncher.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.OnBackPressedCallback
import com.movtery.layer_controller.layout.loadLayoutFromFile
import com.movtery.zalithlauncher.ui.screens.main.control_editor.ControlEditor
import com.movtery.zalithlauncher.viewmodel.EditorViewModel
import com.voidlauncher.ui.theme.VoidDesign
import com.voidlauncher.ui.theme.VoidTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/** VOID wrapper around the proven Zalith control editor. */
@AndroidEntryPoint
class VoidControlEditorActivity : ComponentActivity() {
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controlFile = intent.getStringExtra(EXTRA_LAYOUT_PATH)
            ?.let(::File)
            ?.takeIf { it.isFile && it.exists() }
            ?: run { finish(); return }
        val layout = runCatching { loadLayoutFromFile(controlFile) }.getOrNull()
            ?: run { finish(); return }

        editorViewModel.initLayout(layout)
        editorViewModel.styleCabinet.load()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                editorViewModel.onBackPressed(context = this@VoidControlEditorActivity) { finish() }
            }
        })

        setContent {
            VoidTheme {
                Surface(Modifier.fillMaxSize(), color = VoidDesign.Canvas) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        ControlEditor(
                            viewModel = editorViewModel,
                            targetFile = controlFile,
                            exit = { finish() },
                            menuExit = { finish() },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_LAYOUT_PATH = "void_control_layout_path"
    }
}
