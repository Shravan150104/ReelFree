package com.reelfree.diagnostics

import com.reelfree.detection.model.DetectionResult
import com.reelfree.detection.model.ScreenSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetectionLogEntry(
    val timeMillis: Long,
    val target: String,
    val confidence: Int,
    val threshold: Int,
    val signals: List<String>,
    val vetoes: List<String>,
    val outcome: String,
)

/**
 * Structural signature of one Instagram screen: view IDs, class names, size and position,
 * and vocabulary labels only. Free text is represented as "text(N chars)".
 */
data class ScreenSignature(
    val timeMillis: Long,
    val structureHash: Int,
    val nodeCount: Int,
    val truncated: Boolean,
    val lines: List<String>,
)

/**
 * In-memory, debug-only log shared by the service and the UI (same process).
 * Nothing is written to disk or sent anywhere; it is cleared when the process dies or
 * when the user taps Clear. Exporting is always an explicit user action.
 */
object DiagnosticsLog {

    private const val MAX_ENTRIES = 200
    private const val MAX_SCREENS = 25
    private const val MAX_LINES_PER_SCREEN = 400

    private val _entries = MutableStateFlow<List<DetectionLogEntry>>(emptyList())
    val entries: StateFlow<List<DetectionLogEntry>> = _entries.asStateFlow()

    private val _screens = MutableStateFlow<List<ScreenSignature>>(emptyList())
    val screens: StateFlow<List<ScreenSignature>> = _screens.asStateFlow()

    fun record(result: DetectionResult, outcome: String, nowMillis: Long = System.currentTimeMillis()) {
        val entry = DetectionLogEntry(
            timeMillis = nowMillis,
            target = result.target.displayName,
            confidence = result.confidence,
            threshold = result.threshold,
            signals = result.matchedSignals,
            vetoes = result.matchedVetoes,
            outcome = outcome,
        )
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }

    /** Stores the screen's structure if it differs from every signature already stored. */
    fun recordScreen(screen: ScreenSnapshot, nowMillis: Long = System.currentTimeMillis()) {
        val hash = screen.structureHash()
        if (_screens.value.any { it.structureHash == hash }) return
        val lines = screen.nodes.take(MAX_LINES_PER_SCREEN).map { n ->
            buildString {
                append("  ".repeat(n.depth.coerceAtMost(20)))
                append(n.className ?: "?")
                n.viewId?.let { append(" #").append(it) }
                when {
                    n.label != null -> append(" label=\"").append(n.label).append('"')
                    n.labelLength > 0 -> append(" text(").append(n.labelLength).append(" chars)")
                }
                if (n.selected) append(" [selected]")
                if (n.clickable) append(" [clickable]")
                append(" w=").append(pct(n.widthFraction))
                append(" h=").append(pct(n.heightFraction))
                append(" top=").append(pct(n.topFraction))
            }
        }
        val signature = ScreenSignature(nowMillis, hash, screen.nodes.size, screen.truncated, lines)
        _screens.update { (listOf(signature) + it).take(MAX_SCREENS) }
    }

    fun clear() {
        _entries.value = emptyList()
        _screens.value = emptyList()
    }

    /** Plain-text report for the share sheet. Contains no free text from Instagram. */
    fun exportText(appVersion: String, instagramVersion: String?): String = buildString {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        appendLine("ReelFree diagnostics")
        appendLine("ReelFree version: $appVersion")
        appendLine("Instagram version: ${instagramVersion ?: "not installed / unknown"}")
        appendLine("Android SDK: ${android.os.Build.VERSION.SDK_INT}")
        appendLine("Exported: ${fmt.format(Date())}")
        appendLine()
        appendLine("== Detection events (newest first) ==")
        entries.value.forEach { e ->
            appendLine(
                "${fmt.format(Date(e.timeMillis))}  ${e.target}  score=${e.confidence}/${e.threshold}  " +
                    "signals=${e.signals}  vetoes=${e.vetoes}  -> ${e.outcome}",
            )
        }
        appendLine()
        appendLine("== Distinct Instagram screen structures (newest first) ==")
        screens.value.forEachIndexed { i, s ->
            appendLine("--- Screen #${i + 1} at ${fmt.format(Date(s.timeMillis))}, ${s.nodeCount} nodes${if (s.truncated) " (truncated)" else ""}")
            s.lines.forEach { appendLine(it) }
        }
    }

    private fun pct(f: Float): String = "${(f * 100).toInt()}%"
}
