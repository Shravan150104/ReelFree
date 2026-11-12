package com.reelfree.service

import com.reelfree.detection.model.BlockTarget

fun interface Clock {
    fun nowMillis(): Long
}

/**
 * Requires a positive detection to be seen [requiredHits] times in a row (within
 * [windowMillis]) before it is acted on. This filters out transient frames, e.g. while a
 * Reel thumbnail animates or a screen is half-way through a transition.
 */
class ConfirmationGate(
    private val clock: Clock,
    private val requiredHits: Int = 2,
    private val windowMillis: Long = 1_500,
) {
    private var target: BlockTarget? = null
    private var hits = 0
    private var firstHitAt = 0L

    /** Feed one evaluation. Returns true when the detection is confirmed. */
    fun offer(target: BlockTarget?, positive: Boolean): Boolean {
        if (!positive || target == null) {
            reset()
            return false
        }
        val now = clock.nowMillis()
        if (this.target != target || now - firstHitAt > windowMillis) {
            this.target = target
            hits = 0
            firstHitAt = now
        }
        hits++
        return hits >= requiredHits
    }

    fun reset() {
        target = null
        hits = 0
        firstHitAt = 0L
    }
}

sealed interface BlockDecision {
    data object Allowed : BlockDecision
    data class CoolingDown(val remainingMillis: Long) : BlockDecision
    data class LoopGuardActive(val remainingMillis: Long) : BlockDecision
}

/**
 * Rate-limits blocking actions so ReelFree can never get stuck pressing Back repeatedly
 * (which would eventually close Instagram or fight the user).
 *
 * - After each action, further actions are refused for [cooldownMillis].
 * - If [maxActionsInWindow] actions happen within [loopWindowMillis], blocking is suspended
 *   for [lockoutMillis]. This is the "something is wrong, stop" circuit breaker.
 */
class BlockingActionController(
    private val clock: Clock,
    @Volatile var cooldownMillis: Long = 1_500,
    private val maxActionsInWindow: Int = 4,
    private val loopWindowMillis: Long = 15_000,
    private val lockoutMillis: Long = 30_000,
) {
    private val recentActions = ArrayDeque<Long>()
    private var lastActionAt: Long? = null
    private var lockedUntil = 0L

    @Synchronized
    fun check(): BlockDecision {
        val now = clock.nowMillis()
        if (now < lockedUntil) return BlockDecision.LoopGuardActive(lockedUntil - now)
        val last = lastActionAt
        if (last != null && now - last < cooldownMillis) {
            return BlockDecision.CoolingDown(cooldownMillis - (now - last))
        }
        return BlockDecision.Allowed
    }

    /** Call after an action was actually performed. */
    @Synchronized
    fun recordAction() {
        val now = clock.nowMillis()
        lastActionAt = now
        recentActions.addLast(now)
        while (recentActions.isNotEmpty() && now - recentActions.first() > loopWindowMillis) {
            recentActions.removeFirst()
        }
        if (recentActions.size >= maxActionsInWindow) {
            lockedUntil = now + lockoutMillis
            recentActions.clear()
        }
    }

    @Synchronized
    fun reset() {
        recentActions.clear()
        lastActionAt = null
        lockedUntil = 0L
    }
}
