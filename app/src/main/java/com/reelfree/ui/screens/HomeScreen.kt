package com.reelfree.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reelfree.ui.HomeUiState
import com.reelfree.ui.ProtectionStatus

data class HomeActions(
    val openAccessibilitySettings: () -> Unit,
    val openAppInfo: () -> Unit,
    val setProtectionEnabled: (Boolean) -> Unit,
    val setBlockReels: (Boolean) -> Unit,
    val setDryRun: (Boolean) -> Unit,
    val setDebugMode: (Boolean) -> Unit,
    val openDiagnostics: () -> Unit,
)

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header() }
        item { StatusCard(state.status, state.settings.dryRun) }
        item {
            AnimatedVisibility(
                visible = !state.permissionGranted || !state.serviceConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                OnboardingCard(state, actions)
            }
        }
        item { ControlsCard(state, actions) }
        if (state.instagramVersion == null) {
            item { InfoCard("Instagram doesn't seem to be installed. ReelFree only acts inside the Instagram app.") }
        }
        item { DeveloperCard(state, actions) }
        item { PrivacyCard() }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(horizontal = 4.dp)) {
        Text("ReelFree", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Take back control of your attention.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusCard(status: ProtectionStatus, dryRun: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val (title, detail, icon) = when (status) {
        ProtectionStatus.ACTIVE -> Triple(
            if (dryRun) "Protection Active (dry run)" else "Protection Active",
            if (dryRun) "Reels are detected and logged, but not blocked." else "Recognised Reels viewers are closed automatically.",
            Icons.Filled.CheckCircle,
        )
        ProtectionStatus.DISABLED -> Triple(
            "Protection Disabled",
            "The service is running but blocking is switched off.",
            Icons.Filled.Info,
        )
        ProtectionStatus.PERMISSION_REQUIRED -> Triple(
            "Accessibility Permission Required",
            "Enable ReelFree in Accessibility settings to start blocking.",
            Icons.Filled.Lock,
        )
        ProtectionStatus.SERVICE_NOT_RUNNING -> Triple(
            "Accessibility Permission Required",
            "ReelFree is enabled in Settings but Android isn't running it. Turn it off and on again.",
            Icons.Filled.Warning,
        )
    }
    val container by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.ACTIVE -> scheme.tertiaryContainer
            ProtectionStatus.DISABLED -> scheme.surfaceContainer
            else -> scheme.errorContainer
        },
        animationSpec = tween(400),
        label = "statusContainer",
    )
    val content by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.ACTIVE -> scheme.onTertiaryContainer
            ProtectionStatus.DISABLED -> scheme.onSurface
            else -> scheme.onErrorContainer
        },
        animationSpec = tween(400),
        label = "statusContent",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = content.copy(alpha = 0.12f), contentColor = content) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun OnboardingCard(state: HomeUiState, actions: HomeActions) {
    SectionCard(title = "Set up ReelFree") {
        val steps = listOf(
            "Tap \"Open Accessibility Settings\" below.",
            "Open \"Installed apps\" / \"Downloaded apps\" and choose \"ReelFree Reels blocker\".",
            "Turn it on and confirm Android's dialog. Android shows a strong warning for every accessibility service; ReelFree only uses it to read Instagram's screen layout and press Back.",
            "Come back here. The status card turns green.",
        )
        steps.forEachIndexed { index, step ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("${index + 1}.", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp))
                Text(step, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = actions.openAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open Accessibility Settings")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Switch greyed out or \"Restricted setting\"? Android 13+ restricts accessibility for apps " +
                "installed outside the Play Store. Open App info, tap the ⋮ menu, choose " +
                "\"Allow restricted settings\", then try again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = actions.openAppInfo, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Open ReelFree App info")
        }
        if (state.permissionGranted && !state.serviceConnected) {
            Spacer(Modifier.height(8.dp))
            Text(
                "ReelFree is switched on but not running. Toggle it off and on again in Accessibility settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ControlsCard(state: HomeUiState, actions: HomeActions) {
    val usable = state.permissionGranted
    SectionCard(title = "Protection") {
        ToggleRow(
            title = "Protection",
            subtitle = "Master switch for all blocking",
            checked = state.settings.protectionEnabled,
            enabled = usable,
            onChange = actions.setProtectionEnabled,
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        ToggleRow(
            title = "Block Instagram Reels",
            subtitle = "Leave the full-screen Reels viewer and the Reels tab",
            checked = state.settings.blockInstagramReels,
            enabled = usable && state.settings.protectionEnabled,
            onChange = actions.setBlockReels,
        )
        if (!usable) {
            Text(
                "Enable the accessibility service first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DeveloperCard(state: HomeUiState, actions: HomeActions) {
    SectionCard(title = "Calibration & diagnostics") {
        Text(
            "Instagram's layout isn't documented and changes often. Use these to check what ReelFree " +
                "detects on your phone. No message text, captions or usernames are recorded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        ToggleRow(
            title = "Dry run",
            subtitle = "Detect and log Reels, but never press Back",
            checked = state.settings.dryRun,
            enabled = true,
            onChange = actions.setDryRun,
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        ToggleRow(
            title = "Debug mode",
            subtitle = "Record matched rules and screen structure (in memory only)",
            checked = state.settings.debugMode,
            enabled = true,
            onChange = actions.setDebugMode,
        )
        TextButton(onClick = actions.openDiagnostics, modifier = Modifier.padding(top = 4.dp)) {
            Text("Open diagnostics")
        }
    }
}

@Composable
private fun PrivacyCard() {
    SectionCard(title = "Privacy") {
        val points = listOf(
            "Runs entirely on this phone. ReelFree has no internet permission.",
            "Observes only Instagram. Other apps are never inspected.",
            "Never reads text fields, passwords, or typed messages.",
            "Keeps only view IDs, layout sizes, and a fixed list of tab labels such as \"Reels\" and \"Home\".",
        )
        points.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    SectionCard(title = null) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SectionCard(title: String?, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = LocalContentColorWithAlpha(alpha))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun LocalContentColorWithAlpha(alpha: Float): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
