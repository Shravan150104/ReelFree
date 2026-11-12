package com.reelfree.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AppSettingsAlt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reelfree.BuildConfig
import com.reelfree.ui.HomeUiState
import com.reelfree.ui.components.NavRow
import com.reelfree.ui.components.RowDivider
import com.reelfree.ui.components.RowGroup
import com.reelfree.ui.components.SectionCard
import com.reelfree.ui.components.SectionLabel
import com.reelfree.ui.components.SettingRow
import com.reelfree.ui.components.SwitchRow
import com.reelfree.ui.theme.LocalStatusColors

data class SettingsActions(
    val setDryRun: (Boolean) -> Unit,
    val setDebugMode: (Boolean) -> Unit,
    val openDiagnostics: () -> Unit,
    val openAccessibilitySettings: () -> Unit,
    val openAppInfo: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun SettingsScreen(
    state: HomeUiState,
    diagnosticEventCount: Int,
    actions: SettingsActions,
    contentPadding: PaddingValues,
) {
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                SectionLabel("Calibration")
                RowGroup {
                    SwitchRow(
                        icon = Icons.Rounded.Science,
                        tint = status.warn,
                        title = "Dry run",
                        subtitle = "Detect and log Reels, but don't close them",
                        checked = state.settings.dryRun,
                        onCheckedChange = actions.setDryRun,
                    )
                    RowDivider()
                    SwitchRow(
                        icon = Icons.Rounded.BugReport,
                        tint = scheme.primary,
                        title = "Debug mode",
                        subtitle = "Record matched rules and screen layout",
                        checked = state.settings.debugMode,
                        onCheckedChange = actions.setDebugMode,
                    )
                    RowDivider()
                    NavRow(
                        icon = Icons.AutoMirrored.Rounded.ListAlt,
                        tint = scheme.secondary,
                        title = "Diagnostics",
                        subtitle = if (diagnosticEventCount == 0) "No events recorded" else "$diagnosticEventCount events recorded",
                        onClick = actions.openDiagnostics,
                    )
                }
            }
        }
        item {
            Column {
                SectionLabel("Permissions")
                RowGroup {
                    NavRow(
                        icon = Icons.Rounded.AccessibilityNew,
                        tint = scheme.primary,
                        title = "Accessibility settings",
                        subtitle = if (state.permissionGranted) "ReelFree is enabled" else "ReelFree is not enabled",
                        onClick = actions.openAccessibilitySettings,
                    )
                    RowDivider()
                    NavRow(
                        icon = Icons.Rounded.AppSettingsAlt,
                        tint = scheme.secondary,
                        title = "App info",
                        subtitle = "Allow restricted settings, battery usage",
                        onClick = actions.openAppInfo,
                    )
                }
            }
        }
        item {
            Column {
                SectionLabel("Privacy")
                SectionCard(title = null) {
                    listOf(
                        "Runs on your phone only. ReelFree has no internet permission.",
                        "Observes Instagram only. Other apps are never inspected.",
                        "Never reads text fields, passwords or messages.",
                        "Keeps only layout IDs and the words \"Reels\" and \"Home\".",
                        "Doesn't record your screen.",
                    ).forEach { PrivacyPoint(it) }
                }
            }
        }
        item {
            Column {
                SectionLabel("About")
                RowGroup {
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        tint = scheme.primary,
                        title = "ReelFree ${BuildConfig.VERSION_NAME}",
                        subtitle = "Personal Reels blocker",
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.PhotoCamera,
                        tint = androidx.compose.ui.graphics.Color(0xFFE1306C),
                        title = "Instagram",
                        subtitle = state.instagramVersion?.let { "Version $it" } ?: "Not installed",
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyPoint(text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = LocalStatusColors.current.good,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
