package com.reelfree.detection.rules

import com.reelfree.detection.model.ScreenSnapshot

data class RuleEvaluation(
    val score: Int,
    val matchedSignals: List<String>,
    val matchedVetoes: List<String>,
)

/** Evaluates a list of [DetectionRule]s against a snapshot. Pure and side-effect free. */
class DetectionRuleEngine(val rules: List<DetectionRule>) {

    init {
        val duplicates = rules.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate rule ids: $duplicates" }
    }

    fun evaluate(screen: ScreenSnapshot): RuleEvaluation {
        if (screen.isEmpty) return RuleEvaluation(0, emptyList(), emptyList())

        val vetoes = rules.filter { it.kind == RuleKind.VETO && it.condition.matches(screen) }.map { it.id }
        val signals = rules.filter { it.kind == RuleKind.SIGNAL && it.condition.matches(screen) }
        val score = signals.sumOf { it.weight }.coerceIn(0, 100)
        return RuleEvaluation(score, signals.map { it.id }, vetoes)
    }
}
