package com.reelfree.detection

import com.reelfree.detection.model.LabelVocabulary
import com.reelfree.detection.model.NodeSnapshot
import com.reelfree.detection.model.ScreenSnapshot
import com.reelfree.detection.rules.Condition
import com.reelfree.detection.rules.DetectionRule
import com.reelfree.detection.rules.DetectionRuleEngine
import com.reelfree.detection.rules.RuleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionRuleEngineTest {

    private val screen = ScreenSnapshot(
        "pkg",
        listOf(
            NodeSnapshot(viewId = "big_pager", widthFraction = 1f, heightFraction = 1f),
            NodeSnapshot(viewId = "small_thumb", widthFraction = 0.3f, heightFraction = 0.2f),
            NodeSnapshot(label = "reels", selected = true, topFraction = 0.95f),
        ),
    )

    @Test
    fun `scores add up and cap at 100`() {
        val engine = DetectionRuleEngine(
            listOf(
                DetectionRule("a", "", RuleKind.SIGNAL, Condition.ViewIdContains("pager"), weight = 70),
                DetectionRule("b", "", RuleKind.SIGNAL, Condition.LabelPresent("reels"), weight = 70),
                DetectionRule("c", "", RuleKind.SIGNAL, Condition.ViewIdContains("missing"), weight = 50),
            ),
        )
        val eval = engine.evaluate(screen)
        assertEquals(100, eval.score)
        assertEquals(listOf("a", "b"), eval.matchedSignals)
    }

    @Test
    fun `size limits exclude thumbnails`() {
        assertFalse(Condition.ViewIdContains("thumb", 0.9f, 0.6f).matches(screen))
        assertTrue(Condition.ViewIdContains("thumb").matches(screen))
        assertTrue(Condition.ViewIdContains("PAGER", 0.9f, 0.6f).matches(screen)) // case-insensitive
    }

    @Test
    fun `label conditions respect selection and position`() {
        assertTrue(Condition.LabelPresent("reels", requireSelected = true, minTopFraction = 0.8f).matches(screen))
        assertFalse(Condition.LabelPresent("reels", minTopFraction = 0.99f).matches(screen))
        assertFalse(Condition.LabelPresent("home").matches(screen))
    }

    @Test
    fun `combinators`() {
        val yes = Condition.ViewIdContains("pager")
        val no = Condition.ViewIdContains("missing")
        assertTrue(Condition.AnyOf(listOf(no, yes)).matches(screen))
        assertFalse(Condition.AllOf(listOf(no, yes)).matches(screen))
        assertTrue(Condition.Not(no).matches(screen))
        assertTrue(Condition.Custom("count") { it.size == 3 }.matches(screen))
    }

    @Test
    fun `vetoes are reported separately`() {
        val engine = DetectionRuleEngine(
            listOf(
                DetectionRule("s", "", RuleKind.SIGNAL, Condition.ViewIdContains("pager"), weight = 80),
                DetectionRule("v", "", RuleKind.VETO, Condition.ViewIdContains("thumb")),
            ),
        )
        val eval = engine.evaluate(screen)
        assertEquals(listOf("v"), eval.matchedVetoes)
        assertEquals(80, eval.score)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate rule ids are rejected`() {
        val r = DetectionRule("x", "", RuleKind.SIGNAL, Condition.ViewIdContains("a"), weight = 1)
        DetectionRuleEngine(listOf(r, r))
    }

    @Test
    fun `vocabulary keeps only known short labels`() {
        val vocab = LabelVocabulary(listOf("Reels", "Home"))
        assertEquals("reels", vocab.match("Reels"))
        assertEquals("reels", vocab.match("  REELS "))
        assertEquals("reels", vocab.match("Reels, tab 2 of 5"))
        assertEquals("home", vocab.match("Home tab"))
        assertNull(vocab.match("Reelsfan42"))
        assertNull(vocab.match("hey check out these reels lol"))
        assertNull(vocab.match("Reels" + "x".repeat(60)))
        assertNull(vocab.match(null))
    }
}
