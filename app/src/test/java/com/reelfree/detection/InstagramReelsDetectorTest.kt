package com.reelfree.detection

import com.reelfree.detection.instagram.InstagramReelsDetector
import com.reelfree.detection.instagram.InstagramReelsRules
import com.reelfree.detection.model.BlockAction
import com.reelfree.detection.model.ScreenSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramReelsDetectorTest {

    private val detector = InstagramReelsDetector()
    private val threshold = 60

    @Test
    fun `home feed with embedded reel preview is not blocked`() {
        val result = detector.detect(TestScreens.homeFeed(), threshold)
        assertFalse(result.shouldBlock)
        assertEquals(0, result.confidence)
    }

    @Test
    fun `reels tab is blocked by tapping home`() {
        val result = detector.detect(TestScreens.reelsTab(), threshold)
        assertTrue(result.shouldBlock)
        assertEquals(BlockAction.CLICK_HOME_TAB, result.action)
        assertTrue(InstagramReelsRules.RULE_REELS_TAB_SELECTED in result.matchedSignals)
        assertTrue(InstagramReelsRules.RULE_CLIPS_VIEWER in result.matchedSignals)
        assertEquals(100, result.confidence) // capped
    }

    @Test
    fun `full screen viewer without tab bar is blocked with back`() {
        val result = detector.detect(TestScreens.reelViewerWithoutTabs(), threshold)
        assertTrue(result.shouldBlock)
        assertEquals(BlockAction.BACK, result.action)
    }

    @Test
    fun `stories are vetoed even when clips views are present`() {
        val result = detector.detect(TestScreens.storyViewer(), threshold)
        assertFalse(result.shouldBlock)
        assertEquals(listOf(InstagramReelsRules.VETO_STORY_VIEWER), result.matchedVetoes)
    }

    @Test
    fun `dm thread is vetoed`() {
        val result = detector.detect(TestScreens.dmThread(), threshold)
        assertFalse(result.shouldBlock)
        assertTrue(InstagramReelsRules.VETO_DM_COMPOSER in result.matchedVetoes)
    }

    @Test
    fun `other packages are ignored`() {
        val screen = TestScreens.reelsTab().copy(packageName = "com.example.other")
        assertFalse(detector.detect(screen, threshold).shouldBlock)
    }

    @Test
    fun `empty snapshot when accessibility info is missing is not blocked`() {
        assertFalse(detector.detect(ScreenSnapshot(TestScreens.IG, emptyList()), threshold).shouldBlock)
    }

    @Test
    fun `weak signal alone does not reach threshold`() {
        // A full-screen "clips" container without the stronger "clips_viewer" id or selected tab.
        val screen = ScreenSnapshot(
            TestScreens.IG,
            listOf(
                com.reelfree.detection.model.NodeSnapshot(
                    viewId = "clips_something", widthFraction = 1f, heightFraction = 1f,
                ),
            ),
        )
        val result = detector.detect(screen, threshold)
        assertEquals(30, result.confidence)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun `higher threshold makes detection stricter`() {
        val result = detector.detect(TestScreens.reelViewerWithoutTabs(), 101)
        assertFalse(result.shouldBlock)
    }
}
