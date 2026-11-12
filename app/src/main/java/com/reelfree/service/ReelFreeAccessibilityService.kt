package com.reelfree.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.reelfree.ReelFreeApp
import com.reelfree.data.ProtectionSettings
import com.reelfree.detection.ScreenDetector
import com.reelfree.detection.combinedVocabulary
import com.reelfree.detection.instagram.InstagramReelsDetector
import com.reelfree.detection.instagram.InstagramReelsRules
import com.reelfree.detection.model.BlockAction
import com.reelfree.detection.model.DetectionResult
import com.reelfree.diagnostics.DiagnosticsLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Thin Android shell around the detection engine.
 *
 * Threading: accessibility callbacks arrive on the main thread and only schedule work.
 * Tree walking, rule evaluation, and actions run on a dedicated background [HandlerThread],
 * so a large Instagram hierarchy never blocks the UI thread.
 */
class ReelFreeAccessibilityService : AccessibilityService() {

    private val detectors: List<ScreenDetector> = listOf(InstagramReelsDetector())
    private val vocabulary = detectors.combinedVocabulary()

    private val clock = Clock { SystemClock.uptimeMillis() }
    private val gate = ConfirmationGate(clock)
    private val controller = BlockingActionController(clock)

    private var workerThread: HandlerThread? = null
    private var worker: Handler? = null
    private var scope: CoroutineScope? = null

    @Volatile private var settings = ProtectionSettings()
    @Volatile private var evaluationScheduled = false
    @Volatile private var pendingSince = 0L

    private val evaluateRunnable = Runnable { evaluate() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Screen off / user switched: forget partial confirmations so nothing fires on unlock.
            worker?.post {
                gate.reset()
                worker?.removeCallbacks(evaluateRunnable)
                evaluationScheduled = false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val thread = HandlerThread("ReelFree-detector").also { it.start() }
        workerThread = thread
        worker = Handler(thread.looper)

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        val repository = (application as ReelFreeApp).preferences
        newScope.launch {
            repository.settings.collect { s ->
                settings = s
                if (!s.reelsBlockingActive) worker?.post { gate.reset() }
            }
        }

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ServiceStatus.setConnected(true)
        Log.i(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != InstagramReelsRules.PACKAGE) {
            // The config only subscribes to Instagram, but be defensive.
            worker?.post { gate.reset() }
            return
        }
        val s = settings
        if (!s.reelsBlockingActive && !s.debugMode) return

        val delay = if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            WINDOW_CHANGE_DELAY_MS
        } else {
            CONTENT_CHANGE_DELAY_MS
        }
        schedule(delay)
    }

    /**
     * Trailing debounce with a latency cap: rapid bursts of events collapse into one
     * evaluation, but a continuous stream (e.g. a playing video) cannot postpone it forever.
     */
    private fun schedule(delayMs: Long) {
        val handler = worker ?: return
        val now = SystemClock.uptimeMillis()
        if (evaluationScheduled && now - pendingSince >= MAX_LATENCY_MS) return
        handler.removeCallbacks(evaluateRunnable)
        if (!evaluationScheduled) {
            evaluationScheduled = true
            pendingSince = now
        }
        handler.postDelayed(evaluateRunnable, delayMs)
    }

    /** Runs on the worker thread. */
    private fun evaluate() {
        evaluationScheduled = false
        val s = settings
        if (!s.reelsBlockingActive && !s.debugMode) return

        val root = try {
            rootInActiveWindow
        } catch (e: RuntimeException) {
            Log.w(TAG, "rootInActiveWindow failed", e)
            null
        } ?: return

        try {
            if (root.packageName?.toString() != InstagramReelsRules.PACKAGE) {
                gate.reset()
                return
            }
            val snapshot = NodeSnapshotter.capture(root, vocabulary)
            if (s.debugMode) DiagnosticsLog.recordScreen(snapshot)

            // Only one detector in Phase 1; pick the most confident blocking result.
            val result = detectors
                .filter { it.packageName == snapshot.packageName }
                .map { it.detect(snapshot, DEFAULT_THRESHOLD) }
                .maxByOrNull { if (it.shouldBlock) it.confidence else -1 }
                ?: return

            val outcome = decide(result, s, root)
            if (s.debugMode && (result.confidence > 0 || result.matchedVetoes.isNotEmpty())) {
                DiagnosticsLog.record(result, outcome)
            }
        } finally {
            root.recycleCompat()
        }
    }

    private fun decide(result: DetectionResult, s: ProtectionSettings, root: AccessibilityNodeInfo): String {
        if (result.matchedVetoes.isNotEmpty()) {
            gate.reset()
            return "vetoed"
        }
        if (!result.shouldBlock) {
            gate.reset()
            return "below threshold"
        }
        if (!s.reelsBlockingActive) return "protection off"

        if (!gate.offer(result.target, positive = true)) {
            // Re-check shortly even if Instagram stops sending events.
            schedule(CONFIRM_RECHECK_MS)
            return "awaiting confirmation"
        }

        when (val decision = controller.check()) {
            is BlockDecision.CoolingDown -> {
                schedule(decision.remainingMillis + 50)
                return "cooldown (${decision.remainingMillis} ms)"
            }
            is BlockDecision.LoopGuardActive -> {
                return "loop guard active (${decision.remainingMillis / 1000} s) - too many actions"
            }
            BlockDecision.Allowed -> Unit
        }

        if (s.dryRun) {
            // Cooldown only: in a dry run the Reel stays open, so counting these towards the
            // loop guard would just trip it and hide later detections.
            controller.recordAction(countTowardsLoopGuard = false)
            gate.reset()
            return "DRY RUN - would ${result.action}"
        }

        val performed = perform(result.action, root)
        gate.reset()
        if (!performed) return "action ${result.action} failed"
        controller.recordAction()
        // Verify we actually left the viewer (e.g. Back may only have closed a comment sheet).
        schedule(controller.cooldownMillis + 100)
        return "blocked via ${result.action}"
    }

    private fun perform(action: BlockAction, root: AccessibilityNodeInfo): Boolean = when (action) {
        BlockAction.CLICK_HOME_TAB -> clickHomeTab(root) || performGlobalAction(GLOBAL_ACTION_BACK)
        BlockAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Taps Instagram's bottom-bar Home tab: first by its resource ID ("feed_tab"),
     * then by a bottom-of-screen node labelled "Home".
     */
    private fun clickHomeTab(root: AccessibilityNodeInfo): Boolean {
        val screen = Rect().also { root.getBoundsInScreen(it) }
        val byId = try {
            root.findAccessibilityNodeInfosByViewId(InstagramReelsRules.HOME_TAB_VIEW_ID)
        } catch (e: RuntimeException) {
            emptyList()
        }
        var clickedById = false
        for (node in byId) {
            if (!clickedById && node.isVisibleToUser) clickedById = clickSelfOrAncestor(node)
            node.recycleCompat()
        }
        if (clickedById) return true

        val candidates = try {
            root.findAccessibilityNodeInfosByText("Home")
        } catch (e: RuntimeException) {
            return false
        }
        var clicked = false
        val rect = Rect()
        for (node in candidates) {
            if (!clicked) {
                node.getBoundsInScreen(rect)
                val isTab = vocabulary.match(node.contentDescription ?: node.text) == InstagramReelsRules.TERM_HOME &&
                    rect.top >= screen.top + screen.height() * 0.8f
                if (isTab) clicked = clickSelfOrAncestor(node)
            }
            node.recycleCompat()
        }
        return clicked
    }

    private fun clickSelfOrAncestor(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        var current: AccessibilityNodeInfo? = node.parent
        var hops = 0
        while (current != null && hops < 4) {
            if (current.isClickable) {
                val ok = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                current.recycleCompat()
                return ok
            }
            val next = current.parent
            current.recycleCompat()
            current = next
            hops++
        }
        current?.recycleCompat()
        return false
    }

    override fun onInterrupt() {
        worker?.post { gate.reset() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        if (workerThread == null) return
        ServiceStatus.setConnected(false)
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) {
            // Not registered.
        }
        scope?.cancel()
        scope = null
        worker?.removeCallbacksAndMessages(null)
        workerThread?.quitSafely()
        workerThread = null
        worker = null
        controller.reset()
        gate.reset()
        Log.i(TAG, "Service stopped")
    }

    private companion object {
        const val TAG = "ReelFree"

        /** Minimum confidence (0..100) to act. Phase 3 will expose this as "sensitivity". */
        const val DEFAULT_THRESHOLD = 60

        const val WINDOW_CHANGE_DELAY_MS = 80L
        const val CONTENT_CHANGE_DELAY_MS = 200L
        const val MAX_LATENCY_MS = 500L
        const val CONFIRM_RECHECK_MS = 250L
    }
}
