package com.reelfree.detection.instagram

import com.reelfree.detection.ScreenDetector
import com.reelfree.detection.model.BlockAction
import com.reelfree.detection.model.BlockTarget
import com.reelfree.detection.model.DetectionResult
import com.reelfree.detection.model.ScreenSnapshot
import com.reelfree.detection.rules.DetectionRule
import com.reelfree.detection.rules.DetectionRuleEngine

class InstagramReelsDetector(
    rules: List<DetectionRule> = InstagramReelsRules.rules,
) : ScreenDetector {

    private val engine = DetectionRuleEngine(rules)

    override val target: BlockTarget = BlockTarget.INSTAGRAM_REELS
    override val packageName: String = InstagramReelsRules.PACKAGE
    override val vocabulary: Set<String> = InstagramReelsRules.vocabulary

    override fun detect(screen: ScreenSnapshot, threshold: Int): DetectionResult {
        val evaluation = if (screen.packageName == packageName) {
            engine.evaluate(screen)
        } else {
            null
        }
        val signals = evaluation?.matchedSignals.orEmpty()
        val homeTabVisible = screen.nodes.any {
            it.label == InstagramReelsRules.TERM_HOME && it.topFraction >= 0.8f
        }
        // On the Reels *tab* there may be nothing behind it in Instagram's back stack, so
        // pressing Back could close Instagram. Tapping the Home tab is safer there.
        val action = if (InstagramReelsRules.RULE_REELS_TAB_SELECTED in signals && homeTabVisible) {
            BlockAction.CLICK_HOME_TAB
        } else {
            BlockAction.BACK
        }
        return DetectionResult(
            target = target,
            confidence = evaluation?.score ?: 0,
            threshold = threshold,
            matchedSignals = signals,
            matchedVetoes = evaluation?.matchedVetoes.orEmpty(),
            action = action,
        )
    }
}
