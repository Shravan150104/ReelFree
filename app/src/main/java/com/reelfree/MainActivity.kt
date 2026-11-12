package com.reelfree

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelfree.service.AccessibilityPermission
import com.reelfree.ui.MainViewModel
import com.reelfree.ui.screens.BackTopBar
import com.reelfree.ui.screens.DiagnosticsScreen
import com.reelfree.ui.screens.HomeActions
import com.reelfree.ui.screens.HomeScreen
import com.reelfree.ui.screens.SettingsActions
import com.reelfree.ui.screens.SettingsScreen
import com.reelfree.ui.theme.ReelFreeTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Screen(val depth: Int) { HOME(0), SETTINGS(1), DIAGNOSTICS(2) }

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelFreeTheme {
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val entries by viewModel.diagnosticEntries.collectAsStateWithLifecycle()
                val screens by viewModel.diagnosticScreens.collectAsStateWithLifecycle()
                var screenName by rememberSaveable { mutableStateOf(Screen.HOME.name) }
                val screen = Screen.valueOf(screenName)
                fun go(to: Screen) { screenName = to.name }

                BackHandler(enabled = screen != Screen.HOME) {
                    go(if (screen == Screen.DIAGNOSTICS) Screen.SETTINGS else Screen.HOME)
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        when (screen) {
                            Screen.HOME -> Unit
                            Screen.SETTINGS -> BackTopBar("Settings") { go(Screen.HOME) }
                            Screen.DIAGNOSTICS -> BackTopBar("Diagnostics") { go(Screen.SETTINGS) }
                        }
                    },
                ) { padding ->
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            val forward = targetState.depth > initialState.depth
                            (slideInHorizontally { w -> if (forward) w / 6 else -w / 6 } + fadeIn()) togetherWith
                                (slideOutHorizontally { w -> if (forward) -w / 6 else w / 6 } + fadeOut())
                        },
                        label = "screen",
                    ) { current ->
                        when (current) {
                            Screen.HOME -> HomeScreen(
                                state = state,
                                actions = HomeActions(
                                    openAccessibilitySettings = { launch(AccessibilityPermission.accessibilitySettingsIntent()) },
                                    openAppInfo = { launch(AccessibilityPermission.appDetailsIntent(this@MainActivity)) },
                                    setProtectionEnabled = { viewModel.setProtectionEnabled(it) },
                                    setBlockReels = { viewModel.setBlockReels(it) },
                                    openSettings = { go(Screen.SETTINGS) },
                                ),
                                contentPadding = padding,
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                state = state,
                                diagnosticEventCount = entries.size,
                                actions = SettingsActions(
                                    setDryRun = { viewModel.setDryRun(it) },
                                    setDebugMode = { viewModel.setDebugMode(it) },
                                    openDiagnostics = { go(Screen.DIAGNOSTICS) },
                                    openAccessibilitySettings = { launch(AccessibilityPermission.accessibilitySettingsIntent()) },
                                    openAppInfo = { launch(AccessibilityPermission.appDetailsIntent(this@MainActivity)) },
                                ),
                                contentPadding = padding,
                            )
                            Screen.DIAGNOSTICS -> DiagnosticsScreen(
                                debugMode = state.settings.debugMode,
                                entries = entries,
                                screens = screens,
                                onEnableDebug = { viewModel.setDebugMode(true) },
                                onShare = { shareReport() },
                                onSave = { saveReport() },
                                onClear = { viewModel.clearDiagnostics() },
                                contentPadding = padding,
                            )
                        }
                    }
                }
            }
        }
    }

    /** "Save file": lets the user pick a location (e.g. Downloads) via the system file picker. */
    private val saveReportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(viewModel.diagnosticsReport().toByteArray()) }
                    ?: error("Could not open the file")
                toast("Report saved")
            } catch (e: Exception) {
                toast("Couldn't save the report: ${e.message}")
            }
        }

    private fun saveReport() {
        saveReportLauncher.launch(reportFileName())
    }

    /**
     * Shares the report as a file attachment. Putting the whole report into EXTRA_TEXT
     * can exceed Android's ~1 MB Binder limit, in which case the share silently fails.
     */
    private fun shareReport() {
        try {
            val dir = File(cacheDir, "reports").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // keep only the latest report
            val file = File(dir, reportFileName())
            file.writeText(viewModel.diagnosticsReport())
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "ReelFree diagnostics")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            launch(Intent.createChooser(send, "Share diagnostics"))
        } catch (e: Exception) {
            toast("Couldn't share the report: ${e.message}")
        }
    }

    private fun reportFileName(): String =
        "reelfree-diagnostics-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast("This screen isn't available on your device.")
        } catch (e: RuntimeException) {
            toast("Couldn't open: ${e.message}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
