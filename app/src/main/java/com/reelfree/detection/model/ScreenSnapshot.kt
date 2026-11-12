package com.reelfree.detection.model

/**
 * A privacy-preserving, platform-independent copy of one accessibility node.
 *
 * It deliberately does NOT contain arbitrary on-screen text. [label] is only set when the
 * node's content description / text matches a term from a fixed [LabelVocabulary]
 * (e.g. "reels", "home"). Everything else is reduced to [labelLength] so that structure
 * can be inspected without exposing messages, captions, usernames, or comments.
 */
data class NodeSnapshot(
    /** Resource entry name without the package, e.g. "clips_viewer_view_pager". Null if not exposed. */
    val viewId: String? = null,
    /** Simple class name, e.g. "RecyclerView". */
    val className: String? = null,
    /** Normalised vocabulary term, or null if the label is absent or not in the vocabulary. */
    val label: String? = null,
    /** Length of the raw label (0 if none). Used only for structural diagnostics. */
    val labelLength: Int = 0,
    val selected: Boolean = false,
    val clickable: Boolean = false,
    /** Node width / screen width, 0..1. */
    val widthFraction: Float = 0f,
    /** Node height / screen height, 0..1. */
    val heightFraction: Float = 0f,
    /** Node top / screen height, 0..1. */
    val topFraction: Float = 0f,
    val depth: Int = 0,
)

data class ScreenSnapshot(
    val packageName: String,
    val nodes: List<NodeSnapshot>,
    /** True if the walk stopped early because of node/depth limits. */
    val truncated: Boolean = false,
) {
    val isEmpty: Boolean get() = nodes.isEmpty()

    /** Stable fingerprint of the screen's *structure* (view IDs + classes), used to de-duplicate diagnostics. */
    fun structureHash(): Int = nodes.map { "${it.viewId}|${it.className}" }.toSortedSet().hashCode()
}
