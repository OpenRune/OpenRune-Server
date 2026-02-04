package org.alter.skills.smithing

import dev.openrune.ServerCacheManager
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.attr.CRYSTAL_DONT_ASK_AGAIN
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.smithing.SmithingUtils.itemName
import org.generated.tables.smithing.SmithingCrystalSingingRow

class CrystalSingingEvents : PluginEvent() {

    private val crystalShardId = "items.prif_crystal_shard".asRSCM()
    private val crystalSingingItems = SmithingCrystalSingingRow.all()

    override fun init() {
        on<ObjectClickEvent> {
            where { gameObject.id == "objects.prif_singing_bowl" }
            then { singItems(player) }
        }
    }

    private fun singItems(player: Player) {
        player.queue {
            val availableOutputs = crystalSingingItems
                .filter { canPlayerUseItem(player, it) }
                .map { it.output }

            if (availableOutputs.isEmpty()) return@queue

            produceItemBox(
                player,
                *availableOutputs.toIntArray(),
                title = "What would you like to make?",
                logic = ::startSinging
            )
        }
    }

    private fun canPlayerUseItem(player: Player, item: SmithingCrystalSingingRow): Boolean {
        val smithingLevel = player.getSkills().getCurrentLevel(Skills.SMITHING)
        val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)

        return smithingLevel >= item.level &&
                craftingLevel >= item.level &&
                item.materials.zip(item.materialscount)
                    .any { (id, _) -> id != crystalShardId && player.inventory.getItemCount(id) >= 1 }
    }

    // Starts the crafting process
    private fun startSinging(player: Player, output: Int, amount: Int = 28) {
        val crystalItem = crystalSingingItems.firstOrNull { it.output == output } ?: return
        player.queue { create(this, player, crystalItem, amount) }
    }

    private suspend fun create(
        task: QueueTask,
        player: Player,
        crystalItem: SmithingCrystalSingingRow,
        amount: Int
    ) {
        val materialsText = crystalItem.materials
            .zip(crystalItem.materialscount)
            .filter { (id, _) -> id != crystalShardId }
            .joinToString(" and ") { (id, amt) ->
                val name = ServerCacheManager.getItem(id)?.name?.lowercase() ?: "materials"
                "${amt.toLiteral() ?: amt} ${name.pluralSuffix(amt)}"
            }

        val message =
            "This will consume $materialsText. Reverting the ${crystalItem.shortname} will not give you any materials back."

        task.itemMessageBox(player, message, crystalItem.output)

        if (!player.attr.getOrDefault(CRYSTAL_DONT_ASK_AGAIN, false)) {
            when (task.options(player, "Yes", "Yes, and don't ask again.", "No", title = "Are you sure you wish to proceed?")) {
                1 -> produce(player, task, crystalItem, amount)
                2 -> {
                    player.attr[CRYSTAL_DONT_ASK_AGAIN] = true
                    produce(player, task, crystalItem, amount)
                }
            }
        } else {
            produce(player, task, crystalItem, amount)
        }
    }

    private suspend fun produce(
        player: Player,
        task: QueueTask,
        item: SmithingCrystalSingingRow,
        amount: Int
    ) {
        if (!canProduce(task, player, item)) return

        var remaining = minOf(
            amount,
            item.materials.zip(item.materialscount).minOf { (id, req) ->
                player.inventory.getItemCount(id) / req
            }
        )

        task.repeatWhile(delay = 1, immediate = true, canRepeat = { remaining > 0 }) {
            player.lock()

            if (!canProduce(task, player, item)) {
                player.animate(RSCM.NONE)
                player.unlock()
                return@repeatWhile
            }

            player.animate("sequences.prif_crystal_singing")
            player.playSound(6234)

            val success = item.materials.zip(item.materialscount)
                .all { (id, req) -> player.inventory.remove(id, req).hasSucceeded() }

            if (success) {
                player.inventory.add(item.output)
                player.addXp(Skills.SMITHING, item.xp)
                player.addXp(Skills.CRAFTING, item.xp)
            }

            remaining--
            player.unlock()
        }
    }

    private suspend fun canProduce(task: QueueTask, player: Player, item: SmithingCrystalSingingRow): Boolean {
        val missing = item.materials.zip(item.materialscount)
            .mapNotNull { (id, req) ->
                val need = req - player.inventory.getItemCount(id)
                if (need > 0) itemName(id) to need else null
            }

        if (missing.isNotEmpty()) {
            val missingText = missing.joinToString(", ") { (name, amt) ->
                "${amt.toLiteral() ?: amt} ${name.lowercase()}"
            }
            task.messageBox(player, "You need $missingText to make ${itemName(item.output).prefixAn().lowercase()}.")
            return false
        }

        val smithingLevel = player.getSkills().getCurrentLevel(Skills.SMITHING)
        val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
        if (smithingLevel < item.level || craftingLevel < item.level) {
            task.messageBox(
                player,
                "You need both ${Skills.getSkillName(Skills.SMITHING)} and ${Skills.getSkillName(Skills.CRAFTING)} " +
                        "level of at least ${item.level} to create ${itemName(item.output).lowercase()}."
            )
            return false
        }

        return true
    }
}