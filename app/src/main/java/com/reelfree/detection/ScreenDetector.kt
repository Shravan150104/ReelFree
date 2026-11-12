package com.reelfree.detection

import com.reelfree.detection.model.BlockTarget
import com.reelfree.detection.model.DetectionResult
import com.reelfree.detection.model.LabelVocabulary
import com.reelfree.detection.model.ScreenSnapshot

/** One detector per blockable surface (Instagram Reels now; Explore / YouTube Shorts in Phase 3). */
interface ScreenDetector {
    val target: BlockTarget
    val packageName: String

    /** Labels this detector needs to see. The snapshotter discards every other label. */
    val vocabulary: Set<String>

    fun detect(screen: ScreenSnapshot, threshold: Int): DetectionResult
}

fun List<ScreenDetector>.combinedVocabulary(): LabelVocabulary =
    LabelVocabulary(flatMap { it.vocabulary })
