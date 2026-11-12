package com.reelfree.detection.instagram

import com.reelfree.detection.rules.Condition
import com.reelfree.detection.rules.DetectionRule
import com.reelfree.detection.rules.RuleKind

/**
 * Default rule set for the Instagram Reels viewer.
 *
 * IMPORTANT: none of these identifiers come from documentation. Instagram does not publish
 * its view hierarchy and changes it without notice. The resource-name fragments below are
 * community-reported names for Instagram's Reels ("clips") UI and MUST be confirmed with
 * ReelFree's Diagnostics screen on your own device. Every rule is therefore `verified = false`
 * until you confirm it; edit this file to add, re-weight, or remove rules.
 *
 * Naming trap: inside Instagram's code, *Stories* are historically called "reels"
 * (e.g. "reel_viewer"), while *Reels* are called "clips". Matching the substring "reel" in a
 * view ID would therefore block Stories. The rules below match "clips" for Reels and use
 * "reel_viewer" only as a VETO.
 */
object InstagramReelsRules {

    const val PACKAGE = "com.instagram.android"

    const val TERM_REELS = "reels"
    const val TERM_HOME = "home"

    const val RULE_CLIPS_VIEWER = "ig.reels.clips_viewer_id"
    const val RULE_CLIPS_FULLSCREEN = "ig.reels.clips_fullscreen_container"
    const val RULE_REELS_TAB_SELECTED = "ig.reels.tab_selected"
    const val VETO_STORY_VIEWER = "ig.veto.story_viewer"
    const val VETO_DM_COMPOSER = "ig.veto.dm_composer"

    /** Fraction of the screen a node must cover to count as "full screen". */
    private const val FULL_W = 0.9f
    private const val FULL_H = 0.6f

    val vocabulary: Set<String> = setOf(TERM_REELS, TERM_HOME)

    val rules: List<DetectionRule> = listOf(
        DetectionRule(
            id = RULE_CLIPS_VIEWER,
            description = "A large view whose resource name contains \"clips_viewer\" (full-screen Reels pager).",
            kind = RuleKind.SIGNAL,
            weight = 70,
            condition = Condition.ViewIdContains("clips_viewer", FULL_W, FULL_H),
        ),
        DetectionRule(
            id = RULE_CLIPS_FULLSCREEN,
            description = "Any view with \"clips\" in its resource name covering most of the screen. " +
                "Weak on its own; feed thumbnails are excluded by the size requirement.",
            kind = RuleKind.SIGNAL,
            weight = 30,
            condition = Condition.ViewIdContains("clips", FULL_W, FULL_H),
        ),
        DetectionRule(
            id = RULE_REELS_TAB_SELECTED,
            description = "The bottom-bar tab labelled \"Reels\" is selected.",
            kind = RuleKind.SIGNAL,
            weight = 70,
            condition = Condition.LabelPresent(TERM_REELS, requireSelected = true, minTopFraction = 0.8f),
        ),
        DetectionRule(
            id = VETO_STORY_VIEWER,
            description = "Stories viewer (Instagram internally calls Stories \"reel\"). Never block Stories.",
            kind = RuleKind.VETO,
            condition = Condition.ViewIdContains("reel_viewer"),
        ),
        DetectionRule(
            id = VETO_DM_COMPOSER,
            description = "A DM thread message composer is visible. Never block inside a conversation.",
            kind = RuleKind.VETO,
            condition = Condition.ViewIdContains("row_thread_composer"),
        ),
    )
}
