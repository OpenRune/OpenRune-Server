package org.alter.skills.smithing

import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.TaskPriority
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnItemEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.rscm.RSCM.asRSCM

class GodSword : PluginEvent() {

    private val blade1 = "items.godwars_godsword_blade1".asRSCM()
    private val blade2 = "items.godwars_godsword_blade2".asRSCM()
    private val blade3 = "items.godwars_godsword_blade3".asRSCM()

    private val blade1And2 = "items.godwars_godsword_blade1+2".asRSCM()
    private val blade1And3 = "items.godwars_godsword_blade1+3".asRSCM()
    private val blade2And3 = "items.godwars_godsword_blade2+3".asRSCM()
    private val blade1And2And3 = "items.godwars_godsword_blade1+2+3".asRSCM()

    private val itemShards = mapOf(
        blade1 to setOf(1),
        blade2 to setOf(2),
        blade3 to setOf(3),
        blade1And2 to setOf(1, 2),
        blade1And3 to setOf(1, 3),
        blade2And3 to setOf(2, 3),
    )

    private val recipes = listOf(
        setOf(1, 2, 3) to blade1And2And3,
        setOf(1, 2) to blade1And2,
        setOf(1, 3) to blade1And3,
        setOf(2, 3) to blade2And3,
    )

    private val bladeParts = itemShards.keys + blade1And2And3
    private val completeBlade = blade1And2And3
    private val anvilCategory = 772

    private val hiltToGodsword = listOf(
        "items.godwars_godsword_hilt_armadyl".asRSCM() to "items.ags".asRSCM(),
        "items.godwars_godsword_hilt_ancient".asRSCM() to "items.ancient_godsword".asRSCM(),
        "items.godwars_godsword_hilt_bandos".asRSCM() to "items.bgs".asRSCM(),
        "items.godwars_godsword_hilt_zamorak".asRSCM() to "items.zgs".asRSCM(),
        "items.godwars_godsword_hilt_saradomin".asRSCM() to "items.sgs".asRSCM(),
    )
    private val hiltIds = hiltToGodsword.map { it.first }.toSet()

    override fun init() {
        on<ItemOnItemEvent> {
            where { fromItem.id in bladeParts && toItem.id in bladeParts }
            then {
                player.message(
                    "These pieces of the godsword can't be joined together like that - try forging them on an anvil."
                )
            }
        }

        on<ItemOnItemEvent> {
            where {
                (fromItem.id == completeBlade && toItem.id in hiltIds) || (toItem.id == completeBlade && fromItem.id in hiltIds)
            }
            then {
                player.message("You need to use the blade and hilt on an anvil to attach them.")
            }
        }

        on<ItemOnObject> {
            where {
                item.id in bladeParts && gameObject.getDef().category == anvilCategory && SmithingUtils.hasHammer(player)
            }
            then {
                forgeGodswordBlade(player)
            }
        }

        on<ItemOnObject> {
            where {
                (item.id == completeBlade || item.id in hiltIds) &&
                    gameObject.getDef().category == anvilCategory &&
                    SmithingUtils.hasHammer(player)
            }
            then {
                attachHiltAtAnvil(player, item.id)
            }
        }
    }

    private fun attachHiltAtAnvil(player: Player, usedItem: Int) {
        val (hilt, result) = when (usedItem) {
            completeBlade -> hiltToGodsword.firstOrNull { (hiltId, _) ->
                player.inventory.contains(hiltId)
            }?.let { it.first to it.second }
            in hiltIds -> hiltToGodsword.firstOrNull { it.first == usedItem }?.let { it.first to it.second }
            else -> null
        } ?: run {
            player.message("You need a godsword blade and a godsword hilt to attach them.")
            return
        }
        if (!player.inventory.contains(completeBlade) || !player.inventory.contains(hilt)) {
            player.message("You need a godsword blade and a godsword hilt to attach them.")
            return
        }

        player.queue(TaskPriority.STRONG) {
            wait(3)
            player.animate("sequences.human_smithing")
            player.playSound(3771)
            wait(4)
            if (player.inventory.remove(completeBlade, 1).hasSucceeded() &&
                player.inventory.remove(hilt, 1).hasSucceeded()
            ) {
                player.inventory.add(result)
                player.addXp(Skills.SMITHING, 100)
            }
        }
    }

    private fun forgeGodswordBlade(player: Player) {
        val counts = itemShards.keys.associateWith { player.inventory.getItemCount(it) }

        val (toConsume, product) = recipes.firstNotNullOfOrNull { (requiredShards, product) ->
            findConsumption(requiredShards, counts)?.let { it to product }
        } ?: run {
            player.message("You need another part of the godsword blade to forge them together.")
            return
        }

        player.queue(TaskPriority.STRONG) {
            if (!SmithingUtils.requireSmithingLevel(this, player, 80, "forge the godsword blade shards together")) return@queue
            wait(3)
            player.animate("sequences.human_smithing")
            player.playSound(3771)
            wait(4)

            if (toConsume.all { player.inventory.remove(it, 1).hasSucceeded() }) {
                player.inventory.add(product)
                player.addXp(Skills.SMITHING, 100)
            }
        }
    }

    /**
     * Find a list of items to consume whose shards exactly cover [requiredShards].
     * Returns null if impossible with current [counts].
     */
    private fun findConsumption(
        requiredShards: Set<Int>,
        counts: Map<Int, Int>,
    ): List<Int>? {
        if (requiredShards.isEmpty()) return emptyList()

        for ((item, shards) in itemShards) {
            if (shards.any { it !in requiredShards }) continue
            if ((counts[item] ?: 0) < 1) continue

            val remaining = requiredShards - shards
            val nextCounts = counts.mapValues { (k, v) ->
                if (k == item) v - 1 else v
            }

            val rest = findConsumption(remaining, nextCounts)
            if (rest != null) return listOf(item) + rest
        }
        return null
    }
}