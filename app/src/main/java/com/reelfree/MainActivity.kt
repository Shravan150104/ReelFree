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
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelfree.service.AccessibilityPermission
import com.reelfree.ui.MainViewModel
import com.reelfree.ui.screens.DiagnosticsScreen
import com.reelfree.ui.screens.DiagnosticsTopBar
import com.reelfree.ui.screens.HomeActions
import com.reelfree.ui.screens.HomeScreen
import com.reelfree.ui.theme.ReelFreeTheme

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
                var showDiagnostics by rememberSaveable { mutableStateOf(false) }

                BackHandler(enabled = showDiagnostics) { showDiagnostics = false }

                Scaffold(
                    topBar = { if (showDiagnostics) DiagnosticsTopBar(onBack = { showDiagnostics = false }) },
                ) { padding ->
                    AnimatedContent(
                        targetState = showDiagnostics,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen",
                    ) { diagnostics ->
                        if (diagnostics) {
                            DiagnosticsScreen(
                                debugMode = state.settings.debugMode,
                                entries = entries,
                                screens = screens,
                                onEnableDebug = { viewModel.setDebugMode(true) },
                                onShare = { shareReport() },
                                onSave = { saveReport() },
                                onClear = { viewModel.clearDiagnostics() },
                                contentPadding = padding,
                            )
                        } else {
                            HomeScreen(
                                state = state,
                                actions = HomeActions(
                                    openAccessibilitySettings = {
                                        launch(AccessibilityPermission.accessibilitySettingsIntent())
                                    },
                                    openAppInfo = { launch(AccessibilityPermission.appDetailsIntent(this@MainActivity)) },
                                    setProtectionEnabled = { viewModel.setProtectionEnabled(it) },
                                    setBlockReels = { viewModel.setBlockReels(it) },
                                    setDryRun = { viewModel.setDryRun(it) },
                                    setDebugMode = { viewModel.setDebugMode(it) },
                                    openDiagnostics = { showDiagnostics = true },
                                ),
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
