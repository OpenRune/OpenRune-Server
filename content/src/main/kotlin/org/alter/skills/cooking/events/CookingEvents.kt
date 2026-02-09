package org.alter.skills.cooking.events

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.api.ext.produceItemBox
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.pluginnew.event.impl.ItemOnItemEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.game.pluginnew.event.impl.onTimer
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.cooking.events.CookingUtils.CookStation
import org.alter.skills.cooking.events.CookingUtils.SPIT_ROAST_FIREMAKING_LEVEL
import org.alter.skills.cooking.events.CookingUtils.hasInputs
import org.alter.skills.cooking.events.CookingUtils.isSpitRoastAction
import org.alter.skills.cooking.events.CookingUtils.isStationAllowed
import org.alter.skills.cooking.events.CookingUtils.isStillAtStation
import org.alter.skills.cooking.events.CookingUtils.maxProducible
import org.alter.skills.cooking.events.CookingUtils.meetsExtraRequirements
import org.alter.skills.cooking.events.CookingUtils.rollCookSuccess
import org.alter.skills.cooking.events.CookingUtils.rollWineSuccess
import org.alter.skills.cooking.events.CookingUtils.stationFor
import org.alter.skills.cooking.runtime.CookingAction
import org.alter.skills.cooking.runtime.CookingActionRegistry
import org.alter.skills.cooking.runtime.OutcomeKind
import org.alter.skills.cooking.runtime.Trigger

/**
 * Main event handler for the Cooking skill.
 *
 * Handles:
 * - Using raw food on fires/ranges (heat-source cooking)
 * - Clicking "Cook" on fires/ranges
 * - Item-on-item preparation (e.g., combining ingredients)
 * - Camdozaal preparation table (knife-based prep)
 */
class CookingEvents : PluginEvent() {

    companion object {
        /** Timer for wine fermentation delay (~12 seconds = 20 ticks). */
        val WINE_FERMENT_TIMER = TimerKey()

        /** Number of pending wine fermentations that will succeed. */
        val WINE_SUCCESS_COUNT = AttributeKey<Int>()

        /** Number of pending wine fermentations that will fail. */
        val WINE_FAIL_COUNT = AttributeKey<Int>()

        /** Fermentation delay in game ticks (12 seconds). */
        const val WINE_FERMENT_TICKS = 20
    }

    override fun init() {
        registerCamdozaalPrepTableEvents()
        registerHeatSourceCookingEvents()
        registerItemOnItemPrepEvents()
        registerWineFermentationTimer()
    }

    // ========================================
    // Camdozaal Preparation Table
    // ========================================

    private fun registerCamdozaalPrepTableEvents() {
        on<ItemOnObject> {
            where {
                gameObject.internalID == CamdozaalPrepTable.prepTableId &&
                    CamdozaalPrepTable.isRawPrepFish(item.id)
            }
            then {
                player.queue {
                    with(CamdozaalPrepTable) {
                        prepareFish(player, gameObject, item.id)
                    }
                }
            }
        }
    }

    // ========================================
    // Heat-Source Cooking (Fire/Range)
    // ========================================

    private fun registerHeatSourceCookingEvents() {
        // Click "Cook" on range/fire -> open cooking menu
        on<ObjectClickEvent> {
            where { optionName.equals("cook", ignoreCase = true) }
            then {
                val station = stationFor(gameObject)
                openCookingMenu(player, station, gameObject)
            }
        }

        // Use raw food on range/fire -> open cooking menu for that item
        on<ItemOnObject> {
            where {
                CookingActionRegistry.byRaw.containsKey(item.id) &&
                    CookingUtils.isHeatSource(gameObject)
            }
            then {
                val actions = CookingActionRegistry.byRaw[item.id] ?: return@then
                val station = stationFor(gameObject)
                openCookingMenu(player, station, gameObject, actions)
            }
        }
    }

    // ========================================
    // Item-on-Item Preparation
    // ========================================

    private fun registerItemOnItemPrepEvents() {
        onEvent<ItemOnItemEvent> {
            val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
            val item1 = fromItem.id
            val item2 = toItem.id

            // Use pre-indexed lookup: find actions that use either item as an input
            val actionsForItem1 = CookingActionRegistry.itemOnItemByInput[item1] ?: emptyList()
            val actionsForItem2 = CookingActionRegistry.itemOnItemByInput[item2] ?: emptyList()

            // Intersect: only actions that have BOTH items as inputs
            val possibleActions = if (actionsForItem1.size <= actionsForItem2.size) {
                actionsForItem1.filter { it in actionsForItem2 }
            } else {
                actionsForItem2.filter { it in actionsForItem1 }
            }

            val candidates = possibleActions
                .filter { cookingLevel >= it.row.level && hasInputs(player, it) }

            if (candidates.isEmpty()) {
                return@onEvent
            }

            // Wine actions (stationMask == 0) use the special fermentation path
            val wineActions = candidates.filter { it.row.stationMask == 0 }
            val normalActions = candidates.filter { it.row.stationMask != 0 }

            if (wineActions.isNotEmpty() && normalActions.isEmpty()) {
                // Only wine candidates — use fermentation flow
                player.queue {
                    val action = wineActions.first()
                    val maxCount = maxProducible(player, action)
                    performWineAction(player, action, maxCount)
                }
                return@onEvent
            }

            // Normal item-on-item prep (non-wine candidates)
            val activeCandidates = normalActions.ifEmpty { candidates }
            val maxProducibleCount = activeCandidates.maxOf { action -> maxProducible(player, action) }
            val options = activeCandidates
                .sortedWith(compareBy<CookingAction> { it.row.level }.thenBy { it.primaryOutputItem() })
                .take(18)

            player.queue {
                val producedItems = options.map { it.primaryOutputItem() }.toIntArray()

                // If only one option, skip the UI and just do 1.
                if (options.size == 1) {
                    performPrepAction(player, options.first(), 1)
                    return@queue
                }

                produceItemBox(
                    player,
                    *producedItems,
                    title = "What would you like to make?",
                    maxProducable = maxProducibleCount
                ) { selectedItemId, quantity ->
                    val action = options.firstOrNull { it.primaryOutputItem() == selectedItemId }
                        ?: return@produceItemBox
                    performPrepAction(player, action, quantity)
                }
            }
        }
    }

    // ========================================
    // Cooking Menu & Cooking Logic
    // ========================================

    private fun openCookingMenu(
        player: Player,
        station: CookStation,
        gameObject: GameObject,
        only: List<CookingAction>? = null
    ) {
        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)

        val candidates = (only ?: CookingActionRegistry.allActions)
            .filter { action ->
                action.row.trigger == Trigger.HEAT_SOURCE &&
                    cookingLevel >= action.row.level &&
                    isStationAllowed(action, station) &&
                    meetsExtraRequirements(player, action) &&
                    hasInputs(player, action)
            }

        if (candidates.isEmpty()) {
            player.message("You have nothing you can cook.")
            return
        }

        val maxOptions = 18
        val menuCandidates = candidates
            .sortedWith(
                compareBy<CookingAction> { it.row.level }
                    .thenBy { getItem(it.primaryOutputItem())?.name ?: "" }
            )
            .take(maxOptions)

        val maxProducibleCount = menuCandidates.maxOf { action -> maxProducible(player, action) }

        player.queue {
            val cookedItems = menuCandidates.map { it.primaryOutputItem() }.toIntArray()

            produceItemBox(
                player,
                *cookedItems,
                title = "What would you like to cook?",
                maxProducable = maxProducibleCount
            ) { cookedItemId, qty ->
                val action = menuCandidates.firstOrNull { it.primaryOutputItem() == cookedItemId }
                    ?: return@produceItemBox

                cook(player, station, gameObject, action, qty)
            }
        }
    }

    private fun cook(
        player: Player,
        station: CookStation,
        gameObject: GameObject,
        action: CookingAction,
        quantity: Int
    ) {
        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
        if (cookingLevel < action.row.level) {
            player.filterableMessage("You need a Cooking level of ${action.row.level} to cook this.")
            return
        }

        if (!meetsExtraRequirements(player, action)) {
            if (isSpitRoastAction(action)) {
                player.filterableMessage("You need a Firemaking level of $SPIT_ROAST_FIREMAKING_LEVEL to roast this on an iron spit.")
            }
            return
        }

        val objectTile = gameObject.tile
        val objectId = gameObject.internalID

        player.queue {
            var cooked = 0

            player.filterableMessage("You begin cooking...")

            repeatWhile(delay = 4, immediate = true, canRepeat = {
                cooked < quantity &&
                    hasInputs(player, action) &&
                    isStillAtStation(player, objectTile, objectId)
            }) {
                runCatching { player.animate("sequences.human_cooking", interruptable = true) }
                    .onFailure { player.animate(RSCM.NONE) }

                // Consume inputs
                val inputs = action.inputs
                val removedInputs = mutableListOf<Pair<Int, Int>>()
                for (input in inputs) {
                    val removed = player.inventory.remove(input.item, input.count)
                    if (!removed.hasSucceeded()) {
                        removedInputs.forEach { (itemId, count) -> player.inventory.add(itemId, count) }
                        stop()
                        return@repeatWhile
                    }
                    removedInputs.add(input.item to input.count)
                }

                val success = rollCookSuccess(player, station, action)
                val outcomes = CookingOutcomes.resolveOutcomes(action, success)
                val addedOutputs = mutableListOf<Pair<Int, Int>>()

                for (outcome in outcomes) {
                    val producedCount = CookingOutcomes.rollCount(outcome)
                    val addResult = player.inventory.add(outcome.item, producedCount)
                    if (!addResult.hasSucceeded()) {
                        addedOutputs.forEach { (itemId, count) -> player.inventory.remove(itemId, count) }
                        removedInputs.forEach { (itemId, count) -> player.inventory.add(itemId, count) }
                        player.filterableMessage("You don't have enough inventory space.")
                        stop()
                        return@repeatWhile
                    }
                    addedOutputs.add(outcome.item to producedCount)

                    if (outcome.xp > 0) {
                        player.addXp(Skills.COOKING, outcome.xp.toDouble())
                    }
                }

                val primary = outcomes.firstOrNull { it.kind != OutcomeKind.ALWAYS } ?: outcomes.firstOrNull()
                if (primary != null) {
                    if (primary.kind == OutcomeKind.SUCCESS) {
                        val cookedName = getItem(primary.item)?.name?.lowercase() ?: "food"
                        player.filterableMessage("You cook the $cookedName.")
                    } else {
                        val burntName = getItem(primary.item)?.name?.lowercase() ?: "food"
                        player.filterableMessage("You accidentally burn the $burntName.")
                    }
                }

                cooked++
                wait(1)
            }

            player.animate(RSCM.NONE)
        }
    }

    private fun performPrepAction(
        player: Player,
        action: CookingAction,
        quantity: Int
    ) {
        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
        if (cookingLevel < action.row.level) {
            player.filterableMessage("You need a Cooking level of ${action.row.level} to make this.")
            return
        }

        player.queue {
            var made = 0
            repeatWhile(delay = 2, immediate = true, canRepeat = {
                made < quantity && hasInputs(player, action)
            }) {
                val removedInputs = mutableListOf<Pair<Int, Int>>()
                for (input in action.inputs) {
                    val removed = player.inventory.remove(input.item, input.count)
                    if (!removed.hasSucceeded()) {
                        removedInputs.forEach { (itemId, count) -> player.inventory.add(itemId, count) }
                        stop()
                        return@repeatWhile
                    }
                    removedInputs.add(input.item to input.count)
                }

                val outcomes = CookingOutcomes.resolveOutcomes(action, success = true)
                val addedOutputs = mutableListOf<Pair<Int, Int>>()

                for (outcome in outcomes) {
                    val producedCount = CookingOutcomes.rollCount(outcome)
                    val addResult = player.inventory.add(outcome.item, producedCount)
                    if (!addResult.hasSucceeded()) {
                        addedOutputs.forEach { (itemId, count) -> player.inventory.remove(itemId, count) }
                        removedInputs.forEach { (itemId, count) -> player.inventory.add(itemId, count) }
                        player.filterableMessage("You don't have enough inventory space.")
                        stop()
                        return@repeatWhile
                    }
                    addedOutputs.add(outcome.item to producedCount)

                    if (outcome.xp > 0) {
                        player.addXp(Skills.COOKING, outcome.xp.toDouble())
                    }
                }

                val primary = outcomes.firstOrNull { it.kind != OutcomeKind.ALWAYS } ?: outcomes.firstOrNull()
                val name = primary?.let { getItem(it.item)?.name?.lowercase() } ?: "item"
                player.filterableMessage("You make $name.")
                made++
            }
        }
    }

    // ========================================
    // Wine Fermentation
    // ========================================

    /**
     * Handles combining grapes with a jug of water to create unfermented wine.
     *
     * In OSRS, wine fermentation works as follows:
     * 1. Grapes + jug of water → unfermented wine (instant)
     * 2. Success/failure is rolled at combine time based on cooking level
     * 3. A 12-second (~20 tick) timer starts/resets for ALL pending wines
     * 4. When the timer fires, all unfermented wines convert to jug_of_wine
     *    (success) or jug_of_bad_wine (fail). XP is awarded only for successes.
     */
    private suspend fun QueueTask.performWineAction(
        player: Player,
        action: CookingAction,
        quantity: Int
    ) {
        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
        if (cookingLevel < action.row.level) {
            player.filterableMessage("You need a Cooking level of ${action.row.level} to do that.")
            return
        }

        var made = 0
        repeatWhile(delay = 2, immediate = true, canRepeat = {
            made < quantity && hasInputs(player, action)
        }) {
            // Remove inputs (grapes + jug of water)
            val removedInputs = mutableListOf<Pair<Int, Int>>()
            for (input in action.inputs) {
                val removed = player.inventory.remove(input.item, input.count)
                if (!removed.hasSucceeded()) {
                    removedInputs.forEach { (itemId, count) -> player.inventory.add(itemId, count) }
                    stop()
                    return@repeatWhile
                }
                removedInputs.add(input.item to input.count)
            }

            // Add unfermented wine
            val unfermentedWineId = action.outcomes
                .firstOrNull { it.kind == OutcomeKind.SUCCESS }?.item
                ?: action.primaryOutputItem()
            val addResult = player.inventory.add(unfermentedWineId, 1)
            if (!addResult.hasSucceeded()) {
                removedInputs.forEach { (itemId, count) -> player.inventory.add(itemId, count) }
                player.filterableMessage("You don't have enough inventory space.")
                stop()
                return@repeatWhile
            }

            // Roll success/fail at combine time — store for fermentation
            val success = rollWineSuccess(player, action)
            if (success) {
                val count = player.attr[WINE_SUCCESS_COUNT] ?: 0
                player.attr[WINE_SUCCESS_COUNT] = count + 1
            } else {
                val count = player.attr[WINE_FAIL_COUNT] ?: 0
                player.attr[WINE_FAIL_COUNT] = count + 1
            }

            // Reset fermentation timer (each new wine resets the clock for all pending wines)
            player.timers[WINE_FERMENT_TIMER] = WINE_FERMENT_TICKS

            player.filterableMessage("You squeeze the grapes into the jug of water.")
            made++
        }
    }

    /**
     * Registers the timer handler that completes wine fermentation.
     *
     * When the timer fires, all pending unfermented wines are converted:
     * - Successes → jug_of_wine (200 XP each)
     * - Failures → jug_of_bad_wine (0 XP)
     */
    private fun registerWineFermentationTimer() {
        onTimer(WINE_FERMENT_TIMER) {
            val p = player as Player
            val successes = p.attr[WINE_SUCCESS_COUNT] ?: 0
            val fails = p.attr[WINE_FAIL_COUNT] ?: 0

            val unfermentedId = runCatching { "items.jug_unfermented_wine".asRSCM() }.getOrNull() ?: return@onTimer
            val wineId = runCatching { "items.jug_wine".asRSCM() }.getOrNull() ?: return@onTimer
            val badWineId = runCatching { "items.jug_bad_wine".asRSCM() }.getOrNull() ?: return@onTimer

            // Process successes: replace unfermented wines with jug_of_wine + award XP
            var successConverted = 0
            repeat(successes) {
                if (p.inventory.contains(unfermentedId)) {
                    p.inventory.remove(unfermentedId, 1)
                    p.inventory.add(wineId, 1)
                    p.addXp(Skills.COOKING, 200.0)
                    successConverted++
                }
            }

            // Process failures: replace unfermented wines with jug_of_bad_wine
            var failConverted = 0
            repeat(fails) {
                if (p.inventory.contains(unfermentedId)) {
                    p.inventory.remove(unfermentedId, 1)
                    p.inventory.add(badWineId, 1)
                    failConverted++
                }
            }

            // Clean up attributes
            p.attr.remove(WINE_SUCCESS_COUNT)
            p.attr.remove(WINE_FAIL_COUNT)

            if (successConverted > 0 || failConverted > 0) {
                p.filterableMessage("The wine has fermented.")
            }
        }
    }
}
