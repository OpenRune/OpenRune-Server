package org.alter.skills.cooking.events

import org.alter.skills.cooking.runtime.CookingAction
import org.alter.skills.cooking.runtime.OutcomeKind
import org.alter.skills.cooking.runtime.CookingOutcome
import kotlin.random.Random

/**
 * Outcome selection and resolution for cooking actions.
 */
object CookingOutcomes {

    /**
     * Selects the primary outcome based on success/failure.
     */
    fun selectOutcome(action: CookingAction, success: Boolean): CookingOutcome {
        val outcomes = action.outcomes
        val preferredKind = if (success) OutcomeKind.SUCCESS else OutcomeKind.FAIL
        val candidates = outcomes.filter { it.kind == preferredKind }
        if (candidates.isNotEmpty()) {
            return weightedPick(candidates)
        }

        // Fallback (misconfigured data): allow ANY non-ALWAYS outcome.
        val any = outcomes.filter { it.kind != OutcomeKind.ALWAYS }
        if (any.isNotEmpty()) {
            return weightedPick(any)
        }

        val alwaysOnly = outcomes.filter { it.kind == OutcomeKind.ALWAYS }
        require(alwaysOnly.isNotEmpty()) {
            "Cooking action has no outcomes for key=${action.key} variant=${action.variant}"
        }
        return weightedPick(alwaysOnly)
    }

    /**
     * Resolves all outcomes for an action (ALWAYS outcomes plus the main outcome).
     */
    fun resolveOutcomes(action: CookingAction, success: Boolean): List<CookingOutcome> {
        val always = action.outcomes.filter { it.kind == OutcomeKind.ALWAYS }
        val main = selectOutcome(action, success)
        return if (main.kind == OutcomeKind.ALWAYS) always else always + main
    }

    /**
     * Picks an outcome from candidates based on weights.
     */
    fun weightedPick(candidates: List<CookingOutcome>): CookingOutcome {
        if (candidates.size == 1) return candidates.first()
        val total = candidates.sumOf { it.weight.coerceAtLeast(1) }
        var roll = Random.nextInt(total)
        for (candidate in candidates) {
            roll -= candidate.weight.coerceAtLeast(1)
            if (roll < 0) return candidate
        }
        return candidates.last()
    }

    /**
     * Rolls the production count for an outcome.
     */
    fun rollCount(outcome: CookingOutcome): Int {
        val min = outcome.countMin.coerceAtLeast(1)
        val max = outcome.countMax.coerceAtLeast(min)
        return if (min == max) min else Random.nextInt(min, max + 1)
    }
}
