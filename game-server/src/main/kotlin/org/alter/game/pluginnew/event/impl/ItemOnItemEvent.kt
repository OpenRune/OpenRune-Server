package org.alter.game.pluginnew.event.impl

import net.rsprot.protocol.util.CombinedId
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType

/**
 * Event triggered when a player uses one item on another.
 */
class ItemOnItemEvent(
    val fromItem: Item,
    val toItem: Item,
    val fromSlot: Int,
    val toSlot: Int,
    val from: CombinedId,
    val to: CombinedId,
    player: Player
) : PlayerEvent(player) {
    val fromComponent: CombinedId = from
    val toComponent: CombinedId = to

    fun itemsAre(vararg ids: Int): Boolean {
        val idSet = ids.toSet()
        return fromItem.id in idSet &&
                toItem.id in idSet &&
                player.inventory.contains(fromItem) &&
                player.inventory.contains(toItem)
    }

    fun other(id: Int): Item =
        if (fromItem.id == toItem.id) Item(-1) else listOf(fromItem, toItem).first { it.id != id }

    fun bothSatisfy(condition: (Item) -> Boolean): Boolean =
        condition(fromItem) && condition(toItem)

    fun oneIs(ids: List<Int>): Boolean =
        fromItem.id in ids || toItem.id in ids

    fun oneSatisfies(condition: (Item) -> Boolean): Boolean =
        condition(fromItem) || condition(toItem)
}

/**
 * Defines how items in an [ItemOnItemEvent] should be matched.
 */
sealed class SatisfyType {


    /**
     * At least one of the items must match the specified items or IDs.
     *
     * Equivalent to requiring `ItemOnItemEvent.oneIs(listOf(...))` to return true.
     */
    object ONE : SatisfyType()

    /**
     * Alias for [BOTH], allowing more readable DSL expressions.
     * This is the default type when using [onItemOnItem] without specifying a type.
     *
     * Example:
     * ```
     * onItemOnItem("lighter", "logs").type(SatisfyType.ANY) { ... }
     * // or simply (ANY is the default):
     * onItemOnItem("lighter", "logs") { ... }
     * ```
     */
    object ANY : SatisfyType()

    /**
     * Both items must satisfy the provided predicate function.
     *
     * @param condition A function that receives an [Item] and returns true if it satisfies the condition.
     *                  Both items must return true for the event to trigger.
     *
     * Example:
     * ```
     * type(SatisfyType.BothPredicate { it.id > 1000 }) { ... }
     * ```
     */
    data class BothPredicate(val condition: (Item) -> Boolean) : SatisfyType()

    /**
     * At least one of the items must satisfy the provided predicate function.
     *
     * @param condition A function that receives an [Item] and returns true if it satisfies the condition.
     *                  The event triggers if at least one item returns true.
     *
     * Example:
     * ```
     * type(SatisfyType.OnePredicate { it.id % 2 == 0 }) { ... }
     * ```
     */
    data class OnePredicate(val condition: (Item) -> Boolean) : SatisfyType()
}

class ItemOnItemBuilder(
    private val pluginEvent: PluginEvent,
    private vararg val pairs: Pair<Any, Any>
) {
    private var satisfyType: SatisfyType = SatisfyType.ANY

    fun type(type: SatisfyType, action: suspend ItemOnItemEvent.() -> Unit) =
        apply { satisfyType = type }.also { this(action) }
    fun type(type: SatisfyType): ItemOnItemBuilder =
        apply { satisfyType = type }

    operator fun invoke(action: suspend ItemOnItemEvent.() -> Unit) {
        val currentType = satisfyType

        pluginEvent.on<ItemOnItemEvent> {
            where {
                pairs.any { (a, b) ->
                    val idA = normalizeToId(a)
                    val idB = normalizeToId(b)

                    when (currentType) {
                        SatisfyType.ANY -> itemsAre(idA, idB)
                        SatisfyType.ONE -> oneIs(listOf(idA, idB))
                        is SatisfyType.BothPredicate -> bothSatisfy(currentType.condition)
                        is SatisfyType.OnePredicate -> oneSatisfies(currentType.condition)
                    }
                }
            }
            then { action(this) }
        }
    }

    private fun normalizeToId(item: Any): Int = when (item) {
        is String -> { requireRSCM(RSCMType.OBJTYPES, item); item.asRSCM() }
        is Int -> item
        else -> error("Invalid item type: $item (must be String or Int)")
    }
}

fun PluginEvent.onItemOnItem(first: Any, second: Any) =
    ItemOnItemBuilder(this, first to second)

fun PluginEvent.onItemOnItem(
    first: Any,
    second: Any,
    action: suspend ItemOnItemEvent.() -> Unit
) = onItemOnItem(first, second).invoke(action)
