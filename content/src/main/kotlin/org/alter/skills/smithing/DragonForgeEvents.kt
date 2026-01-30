package org.alter.skills.smithing

import dev.openrune.ServerCacheManager
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.rscm.RSCM
import org.generated.tables.smithing.SmithingDragonForgeRow

class DragonForgeEvents : PluginEvent() {

    private val dragonForge = SmithingDragonForgeRow.all()

    override fun init() {
        on<ObjectClickEvent> {
            where { gameObject.id == "objects.ds2_ac_forge_anvil" }
            then { smeltDragonItems(player) }
        }
    }

    private fun smeltDragonItems(player: Player) {
        player.queue {
            val availableOutputs = dragonForge
                .filter { player.getSkills().getCurrentLevel(Skills.SMITHING) >= it.level }
                .map { it.output }

            if (availableOutputs.isEmpty()) return@queue

            produceItemBox(
                player,
                *availableOutputs.toIntArray(),
                title = "How Many do you wish to make?",
                logic = ::startSmelting
            )
        }
    }

    private fun startSmelting(player: Player, output: Int, amount: Int = 28) {
        val bar = dragonForge.firstOrNull { it.output == output } ?: return
        player.queue { smelt(this, player, bar, amount) }
    }

    private suspend fun smelt(
        task: QueueTask,
        player: Player,
        bar: SmithingDragonForgeRow,
        amount: Int
    ) {
        if (!canSmelt(task, player, bar)) return

        val items = bar.inputPrimary
        val amtPerItem = bar.inputPrimaryAmt

        var remaining = minOf(
            amount,
            items.minOf { player.inventory.getItemCount(it) / amtPerItem }
        )

        task.repeatWhile(delay = 5, immediate = true, canRepeat = { remaining > 0 }) {
            player.lock()

            if (!canSmelt(task, player, bar)) {
                player.animate(RSCM.NONE)
                player.unlock()
                return@repeatWhile
            }

            player.animate(SmithingData.FURNACE_ANIMATION)
            player.playSound(SmithingData.FURNACE_SOUND)
            task.wait(2)

            if (player.inventory.removeAll(items, amtPerItem)) {
                player.inventory.add(bar.output)
                player.addXp(Skills.SMITHING, bar.xp)

                player.message(
                    "You smelt the ${itemName(items.first(), "ore")} in the furnace."
                )
            }

            remaining--
            player.unlock()
        }
    }

    private suspend fun canSmelt(
        task: QueueTask,
        player: Player,
        bar: SmithingDragonForgeRow
    ): Boolean {
        val missing = bar.inputPrimary.mapNotNull { itemId ->
            val have = player.inventory.getItemCount(itemId)
            val need = bar.inputPrimaryAmt - have

            if (need > 0) itemName(itemId)?.let { it to need } else null
        }

        if (missing.isNotEmpty()) {
            val missingText = missing.joinToString(", ") { (name, amt) ->
                "${amt.toLiteral()} $name"
            }

            task.messageBox(
                player,
                "You need $missingText to make ${itemName(bar.output, "bar").prefixAn()}."
            )
            return false
        }

        return SmithingUtils.requireSmithingLevel(
            task,
            player,
            bar.level,
            "smelt ${itemName(bar.inputPrimary.first(), "ore")}"
        )
    }

    private fun itemName(id: Int, fallback: String? = null): String =
        ServerCacheManager.getItem(id)?.name ?: fallback ?: id.toString()
}