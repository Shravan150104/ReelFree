package com.reelfree.service

import com.reelfree.detection.model.BlockTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeClock(var now: Long = 1_000_000) : Clock {
    override fun nowMillis(): Long = now
}

class BlockingActionControllerTest {

    private val clock = FakeClock()
    private val controller = BlockingActionController(
        clock,
        cooldownMillis = 1_500,
        maxActionsInWindow = 4,
        loopWindowMillis = 15_000,
        lockoutMillis = 30_000,
    )

    @Test
    fun `first action is allowed`() {
        assertEquals(BlockDecision.Allowed, controller.check())
    }

    @Test
    fun `cooldown blocks repeated actions`() {
        controller.recordAction()
        clock.now += 500
        val d = controller.check()
        assertTrue(d is BlockDecision.CoolingDown)
        assertEquals(1_000, (d as BlockDecision.CoolingDown).remainingMillis)
        clock.now += 1_000
        assertEquals(BlockDecision.Allowed, controller.check())
    }

    @Test
    fun `loop guard trips after too many actions and recovers`() {
        repeat(4) {
            assertEquals(BlockDecision.Allowed, controller.check())
            controller.recordAction()
            clock.now += 2_000
        }
        assertTrue(controller.check() is BlockDecision.LoopGuardActive)
        clock.now += 30_000
        assertEquals(BlockDecision.Allowed, controller.check())
    }

    @Test
    fun `spaced out actions never trip the loop guard`() {
        repeat(10) {
            assertEquals(BlockDecision.Allowed, controller.check())
            controller.recordAction()
            clock.now += 6_000
        }
    }

    @Test
    fun `dry run actions apply cooldown but never trip the loop guard`() {
        repeat(10) {
            assertEquals(BlockDecision.Allowed, controller.check())
            controller.recordAction(countTowardsLoopGuard = false)
            assertTrue(controller.check() is BlockDecision.CoolingDown)
            clock.now += 1_600
        }
    }

    @Test
    fun `reset clears cooldown and lockout`() {
        repeat(4) { controller.recordAction(); clock.now += 100 }
        controller.reset()
        assertEquals(BlockDecision.Allowed, controller.check())
    }
}

class ConfirmationGateTest {

    private val clock = FakeClock()
    private val gate = ConfirmationGate(clock, requiredHits = 2, windowMillis = 1_500)

    @Test
    fun `needs two consecutive hits`() {
        assertFalse(gate.offer(BlockTarget.INSTAGRAM_REELS, true))
        clock.now += 250
        assertTrue(gate.offer(BlockTarget.INSTAGRAM_REELS, true))
    }

    @Test
    fun `a negative evaluation in between resets`() {
        assertFalse(gate.offer(BlockTarget.INSTAGRAM_REELS, true))
        assertFalse(gate.offer(null, false))
        assertFalse(gate.offer(BlockTarget.INSTAGRAM_REELS, true))
    }

    @Test
    fun `hits too far apart do not confirm`() {
        assertFalse(gate.offer(BlockTarget.INSTAGRAM_REELS, true))
        clock.now += 2_000
        assertFalse(gate.offer(BlockTarget.INSTAGRAM_REELS, true))
        clock.now += 200
        assertTrue(gate.offer(BlockTarget.INSTAGRAM_REELS, true))
    }
}
