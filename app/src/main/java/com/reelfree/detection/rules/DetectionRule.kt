package com.reelfree.detection.rules

import com.reelfree.detection.model.NodeSnapshot
import com.reelfree.detection.model.ScreenSnapshot

/** A predicate over a whole screen snapshot. */
sealed interface Condition {
    fun matches(screen: ScreenSnapshot): Boolean

    /**
     * Some node's resource name contains [fragment] (case-insensitive) and is at least the
     * given size relative to the screen. Size limits distinguish a full-screen viewer from a
     * thumbnail embedded in the feed.
     */
    data class ViewIdContains(
        val fragment: String,
        val minWidthFraction: Float = 0f,
        val minHeightFraction: Float = 0f,
    ) : Condition {
        private val needle = fragment.lowercase()
        override fun matches(screen: ScreenSnapshot): Boolean = screen.nodes.any { node ->
            val id = node.viewId
            id != null &&
                id.lowercase().contains(needle) &&
                node.widthFraction >= minWidthFraction &&
                node.heightFraction >= minHeightFraction
        }
    }

    /**
     * Some node carries vocabulary label [term]. Optionally it must be selected and/or be
     * located in the lower part of the screen (where Instagram's tab bar lives).
     */
    data class LabelPresent(
        val term: String,
        val requireSelected: Boolean = false,
        val minTopFraction: Float = 0f,
    ) : Condition {
        override fun matches(screen: ScreenSnapshot): Boolean = screen.nodes.any { node ->
            node.label == term &&
                (!requireSelected || node.selected) &&
                node.topFraction >= minTopFraction
        }
    }

    data class AllOf(val conditions: List<Condition>) : Condition {
        override fun matches(screen: ScreenSnapshot): Boolean = conditions.all { it.matches(screen) }
    }

    data class AnyOf(val conditions: List<Condition>) : Condition {
        override fun matches(screen: ScreenSnapshot): Boolean = conditions.any { it.matches(screen) }
    }

    data class Not(val condition: Condition) : Condition {
        override fun matches(screen: ScreenSnapshot): Boolean = !condition.matches(screen)
    }

    /** Escape hatch for structural checks that don't fit the declarative conditions. */
    class Custom(val name: String, private val predicate: (List<NodeSnapshot>) -> Boolean) : Condition {
        override fun matches(screen: ScreenSnapshot): Boolean = predicate(screen.nodes)
        override fun toString(): String = "Custom($name)"
    }
}

enum class RuleKind {
    /** Adds [DetectionRule.weight] to the confidence score. */
    SIGNAL,

    /** If it matches, the screen is never blocked (false-positive guard). */
    VETO,
}

data class DetectionRule(
    /** Stable identifier shown in diagnostics, e.g. "ig.reels.clips_viewer". */
    val id: String,
    val description: String,
    val kind: RuleKind,
    val condition: Condition,
    val weight: Int = 0,
    /** False until the rule has been confirmed against a real Instagram build. */
    val verified: Boolean = false,
)
