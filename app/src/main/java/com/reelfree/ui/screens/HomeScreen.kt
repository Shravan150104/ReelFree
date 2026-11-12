package com.reelfree.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reelfree.ui.HomeUiState
import com.reelfree.ui.ProtectionStatus
import com.reelfree.ui.components.Pill
import com.reelfree.ui.components.RowDivider
import com.reelfree.ui.components.RowGroup
import com.reelfree.ui.components.SectionLabel
import com.reelfree.ui.components.SettingRow
import com.reelfree.ui.components.Step
import com.reelfree.ui.components.SwitchRow
import com.reelfree.ui.theme.LocalStatusColors

data class HomeActions(
    val openAccessibilitySettings: () -> Unit,
    val openAppInfo: () -> Unit,
    val setProtectionEnabled: (Boolean) -> Unit,
    val setBlockReels: (Boolean) -> Unit,
    val openSettings: () -> Unit,
)

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TopRow(actions.openSettings) }
        item { StatusHero(state, actions) }
        item {
            AnimatedVisibility(
                visible = !state.permissionGranted || !state.serviceConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SetupCard(state, actions)
            }
        }
        item { BlockersSection(state, actions) }
        item { Footer(state.instagramVersion) }
    }
}

@Composable
private fun TopRow(openSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("ReelFree", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Take back control of your attention.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = openSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class HeroContent(val title: String, val subtitle: String, val icon: ImageVector, val accent: Color)

@Composable
private fun StatusHero(state: HomeUiState, actions: HomeActions) {
    val status = LocalStatusColors.current
    val content = when (state.status) {
        ProtectionStatus.ACTIVE -> HeroContent(
            "Protection active",
            if (state.settings.dryRun) "Reels are detected but not closed." else "Reels close as soon as they open.",
            Icons.Rounded.Shield,
            if (state.settings.dryRun) status.warn else status.good,
        )
        ProtectionStatus.DISABLED -> HeroContent(
            "Protection paused",
            "Reels can be watched until you turn it back on.",
            Icons.Rounded.Pause,
            status.idle,
        )
        ProtectionStatus.PERMISSION_REQUIRED -> HeroContent(
            "Permission needed",
            "Turn on ReelFree in Accessibility settings to start.",
            Icons.Rounded.Lock,
            status.bad,
        )
        ProtectionStatus.SERVICE_NOT_RUNNING -> HeroContent(
            "Not running",
            "ReelFree is enabled but Android stopped it. Turn it off and on again.",
            Icons.Rounded.Warning,
            status.warn,
        )
    }
    val accent by animateColorAsState(content.accent, tween(500), label = "heroAccent")

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusOrb(content.icon, accent, pulsing = state.status == ProtectionStatus.ACTIVE)
            Spacer(Modifier.height(20.dp))
            Text(content.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                content.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (state.status == ProtectionStatus.ACTIVE && state.settings.dryRun) {
                Spacer(Modifier.height(12.dp))
                Pill("DRY RUN", status.warn)
            }
            Spacer(Modifier.height(24.dp))

            when (state.status) {
                ProtectionStatus.PERMISSION_REQUIRED, ProtectionStatus.SERVICE_NOT_RUNNING -> {
                    Button(
                        onClick = actions.openAccessibilitySettings,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape,
                    ) {
                        Text("Open Accessibility settings", style = MaterialTheme.typography.labelLarge)
                    }
                }
                ProtectionStatus.ACTIVE, ProtectionStatus.DISABLED -> {
                    MasterToggle(
                        checked = state.settings.protectionEnabled,
                        onCheckedChange = actions.setProtectionEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusOrb(icon: ImageVector, accent: Color, pulsing: Boolean) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(136.dp)) {
        Box(Modifier.size(136.dp).scale(pulse).clip(CircleShape).background(accent.copy(alpha = 0.08f)))
        Box(Modifier.size(104.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun MasterToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable { onCheckedChange(!checked) },
    ) {
        Row(
            Modifier.padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (checked) "Protection on" else "Protection off",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SetupCard(state: HomeUiState, actions: HomeActions) {
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Set up in 3 steps", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Step(1, "Open Accessibility settings", "Use the button above")
            Step(2, "Find ReelFree Reels blocker", "Under Installed apps or Downloaded apps")
            Step(3, "Turn it on and tap Allow", "Android shows the same warning for every accessibility app", done = state.permissionGranted)

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { helpOpen = !helpOpen }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Switch greyed out or “Restricted setting”?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (helpOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            AnimatedVisibility(helpOpen) {
                Column {
                    Text(
                        "Android 13+ restricts apps installed from a browser. Open App info, tap ⋮ in the corner, " +
                            "choose “Allow restricted settings”, then try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = actions.openAppInfo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(),
                    ) { Text("Open App info") }
                }
            }
        }
    }
}

@Composable
private fun BlockersSection(state: HomeUiState, actions: HomeActions) {
    val usable = state.permissionGranted && state.settings.protectionEnabled
    val scheme = MaterialTheme.colorScheme
    Column {
        SectionLabel("Blocking")
        RowGroup {
            SwitchRow(
                icon = Icons.Rounded.VideoLibrary,
                tint = Color(0xFFE1306C),
                title = "Instagram Reels",
                subtitle = "Reels tab and full-screen Reels",
                checked = state.settings.blockInstagramReels,
                enabled = usable,
                onCheckedChange = actions.setBlockReels,
            )
            RowDivider()
            SettingRow(
                icon = Icons.Rounded.Explore,
                tint = scheme.primary,
                title = "Instagram Explore",
                subtitle = "Optional, off by default",
                enabled = false,
            ) { Pill("SOON", scheme.onSurfaceVariant) }
            RowDivider()
            SettingRow(
                icon = Icons.Rounded.SmartDisplay,
                tint = Color(0xFFE53935),
                title = "YouTube Shorts",
                subtitle = "Optional, off by default",
                enabled = false,
            ) { Pill("SOON", scheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Footer(instagramVersion: String?) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Runs on your phone only · no internet access",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (instagramVersion == null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Instagram isn't installed",
                style = MaterialTheme.typography.bodySmall,
                color = LocalStatusColors.current.warn,
            )
        }
    }
}
