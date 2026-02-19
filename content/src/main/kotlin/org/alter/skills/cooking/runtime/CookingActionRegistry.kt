package org.alter.skills.cooking.runtime

import org.alter.impl.skills.cooking.ChanceDef
import org.alter.impl.skills.cooking.CookingRecipeRegistry
import org.alter.skills.cooking.runtime.OutcomeKind
import org.generated.tables.cooking.CookingActionInputsRow
import org.generated.tables.cooking.CookingActionOutcomesRow
import org.generated.tables.cooking.CookingActionsRow
import org.alter.rscm.RSCM.asRSCM

/**
 * Runtime models for loaded cooking recipe data.
 */

/**
 * An input requirement for a cooking action.
 */
data class CookingInput(
    val item: Int,
    val count: Int
)

/**
 * A possible outcome of a cooking action.
 */
data class CookingOutcome(
    val kind: Int,
    val item: Int,
    val countMin: Int,
    val countMax: Int,
    val xp: Int,
    val weight: Int
)

/**
 * A complete cooking action with loaded data.
 */
data class CookingAction(
    val row: CookingActionsRow,
    val inputs: List<CookingInput>,
    val outcomes: List<CookingOutcome>
) {
    val key: Int get() = row.key
    val variant: Int get() = row.variant

    /**
     * Returns the primary output item (first SUCCESS outcome, else first ALWAYS, else first).
     */
    fun primaryOutputItem(): Int {
        val success = outcomes.firstOrNull { it.kind == OutcomeKind.SUCCESS }
        if (success != null) return success.item
        val always = outcomes.firstOrNull { it.kind == OutcomeKind.ALWAYS }
        if (always != null) return always.item
        return outcomes.firstOrNull()?.item ?: -1
    }
}

/**
 * Registry of all cooking actions loaded from DB tables.
 */
object CookingActionRegistry {

    /**
     * All cooking actions loaded from DB tables.
     */
    val allActions: List<CookingAction> by lazy {
        val actions = CookingActionsRow.all()
        val inputsByKey = CookingActionInputsRow.all().groupBy { it.key to it.variant }
        val outcomesByKey = CookingActionOutcomesRow.all().groupBy { it.key to it.variant }

        actions.map { actionRow ->
            val joinKey = actionRow.key to actionRow.variant
            val inputs = (inputsByKey[joinKey] ?: emptyList())
                .map { CookingInput(item = it.item, count = it.count.coerceAtLeast(1)) }
            val outcomes = (outcomesByKey[joinKey] ?: emptyList())
                .map {
                    CookingOutcome(
                        kind = it.kind,
                        item = it.item,
                        countMin = it.countMin.coerceAtLeast(1),
                        countMax = it.countMax.coerceAtLeast(1),
                        xp = it.xp,
                        weight = it.weight.coerceAtLeast(1)
                    )
                }

            CookingAction(row = actionRow, inputs = inputs, outcomes = outcomes)
        }
    }

    /**
     * Actions indexed by their raw item (key).
     */
    val byRaw: Map<Int, List<CookingAction>> by lazy {
        allActions.groupBy { it.key }
    }

    /**
     * Actions indexed by their cooked item (primary output).
     */
    val byCooked: Map<Int, CookingAction> by lazy {
        allActions
            .mapNotNull { action ->
                val cooked = action.primaryOutputItem()
                if (cooked <= 0) null else cooked to action
            }
            .toMap()
    }

    /**
     * Item-on-item actions indexed by each input item ID.
     * For any item involved in an ITEM_ON_ITEM action, returns
     * the list of actions that use it as an input.
     */
    val itemOnItemByInput: Map<Int, List<CookingAction>> by lazy {
        allActions
            .filter { it.row.trigger == Trigger.ITEM_ON_ITEM }
            .flatMap { action -> action.inputs.map { it.item to action } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, actions) -> actions.distinct() }
    }

    /**
     * Chance profiles indexed by (key, variant).
     *
     * Built directly from [CookingRecipeRegistry.allRecipes] at startup.
     * This avoids needing a separate DB table for chance data.
     */
    val chancesByAction: Map<Pair<Int, Int>, List<ChanceDef>> by lazy {
        CookingRecipeRegistry.allRecipes
            .filter { it.chances.isNotEmpty() }
            .mapNotNull { action ->
                val id = runCatching { action.key.asRSCM() }.getOrNull() ?: return@mapNotNull null
                (id to action.variant) to action.chances
            }
            .toMap()
    }
}
