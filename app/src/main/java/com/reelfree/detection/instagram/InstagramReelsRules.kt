package com.reelfree.detection.instagram

import com.reelfree.detection.rules.Condition
import com.reelfree.detection.rules.DetectionRule
import com.reelfree.detection.rules.RuleKind

/**
 * Default rule set for the Instagram Reels viewer.
 *
 * Instagram does not publish its view hierarchy and changes it without notice.
 * `verified = true` means the rule was confirmed against a real diagnostics report:
 *   Instagram 455.0.0.51.72, Android SDK 37, 2026-11-12.
 * Observed in that report:
 *   - Reels tab:        tab_bar > clips_tab (label "Reels", selected), feed_tab (label "Home"),
 *                       clips_viewer_view_pager 100% x 84%.
 *   - Reel from feed/profile: clips_viewer_view_pager full screen, no tab bar.
 *   - Reel opened from a DM: same pager plus sender_profile_pic and
 *                       reel_viewer_message_composer (a "reply to sender" box).
 *   - DM thread:        row_thread_composer_* views.
 *
 * Naming trap: inside Instagram's code *Stories* are historically called "reel"
 * (reel_viewer_*), while *Reels* are called "clips". But the Reels viewer opened from a DM
 * ALSO contains "reel_viewer_message_composer", so "reel_viewer" alone is not proof of a Story.
 */
object InstagramReelsRules {

    const val PACKAGE = "com.instagram.android"

    const val TERM_REELS = "reels"
    const val TERM_HOME = "home"

    /** Resource name of the bottom-bar Home button (observed). */
    const val HOME_TAB_VIEW_ID = "$PACKAGE:id/feed_tab"

    const val RULE_CLIPS_VIEWER = "ig.reels.clips_viewer_id"
    const val RULE_CLIPS_FULLSCREEN = "ig.reels.clips_fullscreen_container"
    const val RULE_REELS_TAB_SELECTED = "ig.reels.tab_selected"
    const val VETO_STORY_VIEWER = "ig.veto.story_viewer"
    const val VETO_DM_COMPOSER = "ig.veto.dm_composer"

    /** Fraction of the screen a node must cover to count as "full screen". */
    private const val FULL_W = 0.9f
    private const val FULL_H = 0.6f

    private val reelsPager = Condition.ViewIdContains("clips_viewer_view_pager", FULL_W, FULL_H)

    val vocabulary: Set<String> = setOf(TERM_REELS, TERM_HOME)

    val rules: List<DetectionRule> = listOf(
        DetectionRule(
            id = RULE_CLIPS_VIEWER,
            description = "A large view whose resource name contains \"clips_viewer\" (full-screen Reels pager).",
            kind = RuleKind.SIGNAL,
            weight = 70,
            condition = Condition.ViewIdContains("clips_viewer", FULL_W, FULL_H),
            verified = true,
        ),
        DetectionRule(
            id = RULE_CLIPS_FULLSCREEN,
            description = "Any view with \"clips\" in its resource name covering most of the screen. " +
                "Weak on its own; feed thumbnails are excluded by the size requirement.",
            kind = RuleKind.SIGNAL,
            weight = 30,
            condition = Condition.ViewIdContains("clips", FULL_W, FULL_H),
            verified = true,
        ),
        DetectionRule(
            id = RULE_REELS_TAB_SELECTED,
            description = "The bottom-bar tab labelled \"Reels\" is selected.",
            kind = RuleKind.SIGNAL,
            weight = 70,
            condition = Condition.LabelPresent(TERM_REELS, requireSelected = true, minTopFraction = 0.8f),
            verified = true,
        ),
        DetectionRule(
            id = VETO_STORY_VIEWER,
            description = "Stories viewer (\"reel_viewer*\" views) without the full-screen Reels pager. " +
                "Never block Stories.",
            kind = RuleKind.VETO,
            condition = Condition.AllOf(
                listOf(Condition.ViewIdContains("reel_viewer"), Condition.Not(reelsPager)),
            ),
            verified = false, // No Story was captured yet.
        ),
        DetectionRule(
            id = VETO_DM_COMPOSER,
            description = "A DM thread message composer is visible. Never block inside a conversation.",
            kind = RuleKind.VETO,
            condition = Condition.ViewIdContains("row_thread_composer"),
            verified = true,
        ),
    )
}
