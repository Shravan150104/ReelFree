package com.reelfree.detection.model

enum class BlockTarget(val displayName: String) {
    INSTAGRAM_REELS("Instagram Reels"),
}

/** How the service should get the user out of a blocked screen. */
enum class BlockAction {
    /** performGlobalAction(GLOBAL_ACTION_BACK). */
    BACK,

    /** Tap Instagram's Home tab (used on the Reels *tab*, where Back might leave Instagram). */
    CLICK_HOME_TAB,
}

data class DetectionResult(
    val target: BlockTarget,
    /** 0..100. Sum of matched signal weights, capped at 100. */
    val confidence: Int,
    val threshold: Int,
    val matchedSignals: List<String>,
    val matchedVetoes: List<String>,
    val action: BlockAction,
) {
    /** A single matched veto always wins, no matter how high the confidence is. */
    val shouldBlock: Boolean get() = matchedVetoes.isEmpty() && confidence >= threshold
}
