package com.reelfree

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

    private fun shareReport() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ReelFree diagnostics")
            putExtra(Intent.EXTRA_TEXT, viewModel.diagnosticsReport())
        }
        launch(Intent.createChooser(send, "Share diagnostics"))
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "This screen isn't available on your device.", Toast.LENGTH_LONG).show()
        }
    }
}
