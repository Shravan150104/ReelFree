package com.reelfree.service

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.reelfree.detection.model.LabelVocabulary
import com.reelfree.detection.model.NodeSnapshot
import com.reelfree.detection.model.ScreenSnapshot

/**
 * Converts a live AccessibilityNodeInfo tree into a [ScreenSnapshot].
 *
 * Privacy guarantees enforced here:
 *  - Editable and password fields are never read (no typed messages, no passwords).
 *  - Labels are kept only if they match the fixed [LabelVocabulary]; otherwise only their length survives.
 *  - Invisible nodes and their subtrees are skipped.
 */
object NodeSnapshotter {

    const val MAX_NODES = 1_500
    const val MAX_DEPTH = 50

    fun capture(root: AccessibilityNodeInfo, vocabulary: LabelVocabulary): ScreenSnapshot {
        val screen = Rect()
        root.getBoundsInScreen(screen)
        val screenW = screen.width().coerceAtLeast(1).toFloat()
        val screenH = screen.height().coerceAtLeast(1).toFloat()

        val out = ArrayList<NodeSnapshot>(256)
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        val rect = Rect()
        var truncated = false

        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            try {
                if (out.size >= MAX_NODES) {
                    truncated = true
                    continue
                }
                if (!node.isVisibleToUser) continue
                node.getBoundsInScreen(rect)

                val sensitive = node.isEditable || node.isPassword
                val desc = if (sensitive) null else node.contentDescription
                val text = if (sensitive) null else node.text
                val label = vocabulary.match(desc) ?: vocabulary.match(text)
                val rawLength = (desc ?: text)?.length ?: 0

                out += NodeSnapshot(
                    viewId = node.viewIdResourceName?.substringAfter(":id/"),
                    className = node.className?.toString()?.substringAfterLast('.'),
                    label = label,
                    labelLength = rawLength,
                    selected = node.isSelected,
                    clickable = node.isClickable,
                    widthFraction = (rect.width() / screenW).coerceIn(0f, 1f),
                    heightFraction = (rect.height() / screenH).coerceIn(0f, 1f),
                    topFraction = ((rect.top - screen.top) / screenH).coerceIn(0f, 1f),
                    depth = depth,
                )

                if (depth >= MAX_DEPTH) {
                    if (node.childCount > 0) truncated = true
                } else {
                    for (i in 0 until node.childCount) {
                        node.getChild(i)?.let { stack.addLast(it to depth + 1) }
                    }
                }
            } finally {
                if (node !== root) node.recycleCompat()
            }
        }
        return ScreenSnapshot(root.packageName?.toString().orEmpty(), out, truncated)
    }
}

/** recycle() is required before API 33 and a deprecated no-op afterwards. */
@Suppress("DEPRECATION")
fun AccessibilityNodeInfo.recycleCompat() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        try {
            recycle()
        } catch (_: IllegalStateException) {
            // Already recycled.
        }
    }
}
