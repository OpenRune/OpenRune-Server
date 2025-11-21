package org.alter.items.essencepouch

import dev.openrune.ServerCacheManager
import dev.openrune.ServerCacheManager.getItemOrDefault
import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.container.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.plugin.*
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOption
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.getRSCM

class EssencePouchPlugin : PluginEvent() {

    data class EssencePouch(
        val id: String,
        val levelReq: Int,
        var capacity: Int,
        val varbitId: String,
        val degradable: Boolean
    )

    private val pouches = listOf(
        EssencePouch("items.rcu_pouch_small", 1, 3, "varbits.small_essence_pouch", degradable = false),
        EssencePouch("items.rcu_pouch_medium", 25, 6, "varbits.medium_essence_pouch", degradable = true),
        EssencePouch("items.rcu_pouch_large", 50, 9, "varbits.large_essence_pouch", degradable = true),
        EssencePouch("items.rcu_pouch_giant", 75, 12, "varbits.giant_essence_pouch", degradable = true),
        EssencePouch("items.rcu_pouch_colossal", 85, 40, "varbits.colossal_essence_pouch", degradable = true)
    )

    override fun init() {
        pouches.forEach { pouch ->
            onItemOption(item = pouch.id, op = MenuOption.OP2) {
                if (pouchHasEssence(player, pouch)) {
                    emptyPouch(player, pouch)
                } else {
                    fillPouch(player, pouch, slot)
                }
            }

            onItemOption(item = pouch.id, op = MenuOption.OP3) {
                if (pouchHasEssence(player, pouch)) {
                    fillPouch(player, pouch,slot)
                } else {
                    emptyPouch(player, pouch)
                }
            }

            onItemOption(item = pouch.id, option = "check") { checkPouch(player, pouch) }
        }
    }

    private fun pouchHasEssence(player: Player, pouch: EssencePouch): Boolean {
        return player.getVarbit(pouch.varbitId) > 0
    }

    private fun fillPouch(player: Player, pouchDef: EssencePouch, slot : Int) {
        if (player.getSkills().getBaseLevel(Skills.RUNECRAFTING) < pouchDef.levelReq) {
            player.message("This pouch requires level ${pouchDef.levelReq} ${Skills.getSkillName(Skills.RUNECRAFTING)} to use.")
            return
        }

        val inventory = player.inventory
        val essenceType = when {
            inventory.contains(getRSCM("items.blankrune_high")) -> getRSCM("items.blankrune_high")
            inventory.contains(getRSCM("items.blankrune")) -> getRSCM("items.blankrune")
            else -> {
                player.message("You do not have any essence to fill your pouch with.")
                return
            }
        }

        val pouchItem = player.inventory[slot]?: return
        var currentAmount = player.getVarbit(pouchDef.varbitId)
        val freeSpace = pouchDef.capacity - currentAmount
        if (freeSpace <= 0) {
            player.message("You cannot add any more essence to the pouch.")
            return
        }

        val amountToDeposit = inventory.getItemCount(essenceType).coerceAtMost(freeSpace)
        if (amountToDeposit <= 0) return

        inventory.remove(essenceType, amountToDeposit)
        currentAmount += amountToDeposit
        player.setVarbit(pouchDef.varbitId, currentAmount)

        // Handle degradation
        if (pouchDef.degradable && !playerHasDegradeProtection(player)) {
            val threshold = pouchItem.getAttr(ItemAttribute.ESSENCE_POUCH_THRESHOLD) ?: 0
            val increment = (0..3).random() * amountToDeposit
            val newThreshold = threshold + increment
            pouchItem.putAttr(ItemAttribute.ESSENCE_POUCH_THRESHOLD, newThreshold)

            // Determine new capacity
            val newCapacity = getDegradedCapacity(pouchDef, newThreshold)
            pouchDef.capacity = newCapacity

            if (newCapacity <= 0) {
                player.message("Your ${pouchDef.id.asRSCM().toItem().getDef().name.lowercase()} has been destroyed due to overuse!")
                player.inventory.remove(pouchItem)
                return
            }
        }

        player.message("You fill the pouch with $amountToDeposit ${getItemOrDefault(essenceType).name.lowercase()}.")
    }

    private fun emptyPouch(player: Player, pouchDef: EssencePouch) {
        val amount = player.getVarbit(pouchDef.varbitId)
        if (amount <= 0) {
            player.message("There are no essences in this pouch.")
            return
        }

        val inventory = player.inventory
        val essenceType = if (inventory.contains(getRSCM("items.blankrune_high"))) getRSCM("items.blankrune_high") else getRSCM("items.blankrune")
        val removeCount = inventory.freeSlotCount.coerceAtMost(amount)

        if (removeCount <= 0) {
            player.message("You do not have any free space in your inventory.")
            return
        }

        inventory.add(essenceType, removeCount)
        player.setVarbit(pouchDef.varbitId, amount - removeCount)
        player.message("You empty $removeCount essence${if (removeCount > 1) "s" else ""} from the pouch.")
    }

    private fun checkPouch(player: Player, pouchDef: EssencePouch) {
        val amount = player.getVarbit(pouchDef.varbitId)
        if (amount <= 0) {
            player.message("There are no essences in this pouch.")
            return
        }

        val essenceType = if (player.inventory.contains(getRSCM("items.blankrune_high"))) getRSCM("items.blankrune_high") else getRSCM("items.blankrune")
        val name = getItemOrDefault(essenceType).name.lowercase()
        player.message("There ${amount.toLiteral()?.pluralPrefix(amount)} ${name.pluralSuffix(amount)} in this pouch.")
    }

    private fun playerHasDegradeProtection(player: Player): Boolean {
        return player.equipment.contains("items.skillcape_runecrafting") || player.equipment.contains("items.skillcape_runecrafting_trimmed")
    }

    // Determine capacity based on thresholds
    private fun getDegradedCapacity(pouchDef: EssencePouch, threshold: Int): Int {
        return when (pouchDef.id) {
            "items.rcu_pouch_medium" -> when {
                threshold < 400 -> 6
                threshold in 400..799 -> 3
                else -> 0
            }
            "items.rcu_pouch_large" -> when {
                threshold < 400 -> 9
                threshold in 400..599 -> 7
                threshold in 600..799 -> 5
                threshold in 800..999 -> 3
                else -> 0
            }
            "items.rcu_pouch_giant" -> when {
                threshold < 200 -> 12
                threshold in 200..299 -> 9
                threshold in 300..399 -> 8
                threshold in 400..599 -> 7
                threshold in 600..799 -> 6
                threshold in 800..999 -> 5
                threshold in 1000..1199 -> 3
                else -> 0
            }
            "items.rcu_pouch_colossal" -> when {
                threshold < 320 -> 40
                threshold in 320..564 -> 35
                threshold in 565..744 -> 30
                threshold in 745..869 -> 25
                threshold in 870..949 -> 20
                threshold in 950..994 -> 15
                threshold in 995..1014 -> 10
                threshold in 1015..1020 -> 5
                else -> 0
            }
            else -> pouchDef.capacity
        }
    }
}