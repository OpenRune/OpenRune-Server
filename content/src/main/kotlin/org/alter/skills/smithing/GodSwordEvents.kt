package org.alter.skills.smithing

import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.TaskPriority
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnItemEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.rscm.RSCM.asRSCM

class GodSwordEvents : PluginEvent() {

    companion object {
        private val COMPLETE_SHARDS = setOf(1, 2, 3)
    }

    private val bladeShardItems = mapOf(
        "items.godwars_godsword_blade1".asRSCM() to setOf(1),
        "items.godwars_godsword_blade2".asRSCM() to setOf(2),
        "items.godwars_godsword_blade3".asRSCM() to setOf(3),

        "items.godwars_godsword_blade1+2".asRSCM() to setOf(1, 2),
        "items.godwars_godsword_blade1+3".asRSCM() to setOf(1, 3),
        "items.godwars_godsword_blade2+3".asRSCM() to setOf(2, 3),
    )

    private val completeBlade = "items.godwars_godsword_blade1+2+3".asRSCM()

    private val allBladeParts = bladeShardItems.keys + completeBlade

    private val forgeRecipes = listOf(
        COMPLETE_SHARDS to completeBlade,
        setOf(1, 2) to "items.godwars_godsword_blade1+2".asRSCM(),
        setOf(1, 3) to "items.godwars_godsword_blade1+3".asRSCM(),
        setOf(2, 3) to "items.godwars_godsword_blade2+3".asRSCM(),
    )

    private val hiltToGodsword = mapOf(
        "items.godwars_godsword_hilt_armadyl".asRSCM() to "items.ags".asRSCM(),
        "items.godwars_godsword_hilt_ancient".asRSCM() to "items.ancient_godsword".asRSCM(),
        "items.godwars_godsword_hilt_bandos".asRSCM() to "items.bgs".asRSCM(),
        "items.godwars_godsword_hilt_zamorak".asRSCM() to "items.zgs".asRSCM(),
        "items.godwars_godsword_hilt_saradomin".asRSCM() to "items.sgs".asRSCM(),
    )

    private val hiltIds = hiltToGodsword.keys

    override fun init() {

        on<ItemOnItemEvent> {
            where { fromItem.id in allBladeParts && toItem.id in allBladeParts }
            then {
                player.message(
                    "These pieces of the godsword can't be joined together like that - try forging them on an anvil."
                )
            }
        }

        on<ItemOnItemEvent> {
            where {
                (fromItem.id == completeBlade && toItem.id in hiltIds) ||
                        (toItem.id == completeBlade && fromItem.id in hiltIds)
            }
            then {
                val hilt = if (fromItem.id in hiltIds) fromItem.id else toItem.id
                val product = hiltToGodsword[hilt] ?: return@then

                if (player.inventory.remove(hilt, 1).hasSucceeded() &&  player.inventory.remove(completeBlade, 1).hasSucceeded()) {
                    player.inventory.add(product)
                }
            }
        }


        on<ItemOnObject> {
            where {
                item.id in allBladeParts && gameObject.getDef().category == SmithingUtils.ANVIL_CATEGORY && SmithingUtils.hasHammer(player)
            }
            then { forgeBlade(player) }
        }
    }

    private fun forgeBlade(player: Player) {
        val counts = bladeShardItems.keys.associateWith {
            player.inventory.getItemCount(it)
        }

        val (toConsume, result) = forgeRecipes.firstNotNullOfOrNull { (required, product) ->
            findConsumption(required, counts)?.let { it to product }
        } ?: run {
            player.message("You need another part of the godsword blade to forge them together.")
            return
        }

        player.queue(TaskPriority.STRONG) {
            wait(3)
            player.animate("sequences.human_smithing")
            player.playSound(3771)
            wait(4)

            if (toConsume.all { player.inventory.remove(it, 1).hasSucceeded() }) {
                player.inventory.add(result)
                player.addXp(Skills.SMITHING, 100)
            }
        }
    }

    /**
     * Finds a combination of items whose shard sets exactly match [required].
     */
    private fun findConsumption(
        required: Set<Int>,
        counts: Map<Int, Int>,
    ): List<Int>? {
        if (required.isEmpty()) return emptyList()

        for ((item, shards) in bladeShardItems) {
            if (!required.containsAll(shards)) continue
            if ((counts[item] ?: 0) <= 0) continue

            val nextRequired = required - shards
            val nextCounts = counts + (item to (counts[item]!! - 1))

            findConsumption(nextRequired, nextCounts)?.let {
                return listOf(item) + it
            }
        }

        return null
    }
}