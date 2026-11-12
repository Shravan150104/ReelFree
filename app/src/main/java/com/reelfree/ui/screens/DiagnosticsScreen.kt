package com.reelfree.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelfree.diagnostics.DetectionLogEntry
import com.reelfree.diagnostics.ScreenSignature
import com.reelfree.ui.components.SectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Diagnostics") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
    )
}

@Composable
fun DiagnosticsScreen(
    debugMode: Boolean,
    entries: List<DetectionLogEntry>,
    screens: List<ScreenSignature>,
    onEnableDebug: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    contentPadding: PaddingValues,
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    var expandedScreen by rememberSaveable { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = "How to calibrate") {
                val steps = listOf(
                    "Turn on Debug mode and Dry run in Settings.",
                    "In Instagram, visit: Home feed, a Story, a DM thread, a profile grid, then open a Reel (Reels tab, from the feed, from a profile, from a DM).",
                    "Return here. Each event shows which rules matched and what ReelFree would have done.",
                    "If a Reel was missed or a normal screen matched, tap Share report and adjust InstagramReelsRules.kt.",
                )
                steps.forEachIndexed { i, s ->
                    Text("${i + 1}. $s", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                }
                if (!debugMode) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onEnableDebug) { Text("Turn on Debug mode") }
                }
                Spacer(Modifier.height(8.dp))
                val hasData = entries.isNotEmpty() || screens.isNotEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onShare, enabled = hasData) { Text("Share report") }
                    OutlinedButton(onClick = onSave, enabled = hasData) { Text("Save file") }
                }
                TextButton(onClick = onClear, enabled = hasData) { Text("Clear") }
                Text(
                    "Share report attaches a .txt file (e.g. send it to yourself on Gmail). Save file stores it " +
                        "wherever you choose, e.g. Downloads. The report contains view IDs, class names, sizes and " +
                        "the fixed tab labels only; other text appears as \"text(N chars)\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item { Text("Detection events (${entries.size})", style = MaterialTheme.typography.titleMedium) }
        if (entries.isEmpty()) {
            item {
                Text(
                    if (debugMode) "Nothing yet. Open Instagram and browse around." else "Debug mode is off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(entries) { e ->
            SectionCard(title = null) {
                Text(
                    "${timeFormat.format(Date(e.timeMillis))}  •  ${e.outcome}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Score ${e.confidence} / threshold ${e.threshold}", style = MaterialTheme.typography.bodySmall)
                if (e.signals.isNotEmpty()) Text("Signals: ${e.signals.joinToString()}", style = MaterialTheme.typography.bodySmall)
                if (e.vetoes.isNotEmpty()) {
                    Text(
                        "Vetoes: ${e.vetoes.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item { Text("Screen structures (${screens.size})", style = MaterialTheme.typography.titleMedium) }
        items(screens.size) { index ->
            val s = screens[index]
            SectionCard(title = null) {
                Text(
                    "${timeFormat.format(Date(s.timeMillis))}  •  ${s.nodeCount} nodes${if (s.truncated) " (truncated)" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val ids = s.lines.mapNotNull { line -> line.substringAfter(" #", "").substringBefore(' ').ifEmpty { null } }
                    .distinct()
                Text(
                    "View IDs: ${if (ids.isEmpty()) "none exposed" else ids.take(12).joinToString()}${if (ids.size > 12) " …" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { expandedScreen = if (expandedScreen == index) -1 else index }) {
                    Text(if (expandedScreen == index) "Hide tree" else "Show tree")
                }
                if (expandedScreen == index) {
                    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        s.lines.forEach { line ->
                            Text(line, fontFamily = FontFamily.Monospace, fontSize = 10.sp, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}
