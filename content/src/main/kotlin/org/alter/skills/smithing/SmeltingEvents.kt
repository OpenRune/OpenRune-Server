package org.alter.skills.smithing

import dev.openrune.ServerCacheManager
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.messageBox
import org.alter.api.ext.prefixAn
import org.alter.api.ext.produceItemBox
import org.alter.api.ext.toLiteral
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.smithing.SmithingBarsRow

class SmeltingEvents : PluginEvent() {

    private val allBars = SmithingBarsRow.all()

    private val normalBars = allBars
        .filter { it.output != "items.lovakite_bar".asRSCM() }

    private val barsByOutput = allBars
        .associateBy { it.output }

    override fun init() {

        on<ObjectClickEvent> {
            where { gameObject.getDef().category == 215 && player.inventory.containsAny(allBars.map { it.inputPrimary }.toList()) }
            then { smeltStandard(player) }
        }

        on<ObjectClickEvent> {
            where { gameObject.id == "objects.lovakengj_furnace_large_01" }
            then {
                player.queue {
                    produceItemBox(player, "items.lovakite_bar".asRSCM(),
                        title = "What would you like to smelt?",
                        logic = ::smeltItem
                    )
                }
            }
        }

    }

    fun smeltStandard(player: Player) {
        player.queue {
            produceItemBox(player, *normalBars.map { it.output }.toIntArray(),
                title = "What would you like to smelt?",
                logic = ::smeltItem
            )
        }
    }

    fun smeltItem(player: Player, output: Int, amount: Int = 28) {
        val bar = barsByOutput[output] ?: return
        player.queue { smelt(this, player,bar, amount) }
    }

    suspend fun smelt(
        task: QueueTask,
        player: Player,
        bar: SmithingBarsRow,
        amount: Int,
        isSuperHeat : Boolean = false
    ) {
        if (!canSmelt(task, player, bar)) return

        val primaryAmt = bar.inputPrimaryAmt
        val secondaryAmt = bar.inputSecondaryAmt ?: 0
        val requiresSecondary = bar.inputSecondary != null && secondaryAmt > 0

        val primaryCount = player.inventory.getItemCount(bar.inputPrimary)
        val secondaryCount = if (requiresSecondary) {
            player.inventory.getItemCount(bar.inputSecondary)
        } else {
            player.inventory.type.size
        }

        val maxByMaterials = minOf(
            primaryCount / primaryAmt,
            if (requiresSecondary) secondaryCount / secondaryAmt else Int.MAX_VALUE
        )
        var maxSmelts = minOf(amount, maxByMaterials)

        task.repeatWhile(delay = 5, immediate = true, canRepeat = {
            maxSmelts != 0
        }) {
            player.lock()

            if (!canSmelt(task, player, bar)) {
                player.animate(RSCM.NONE)
                player.unlock()
                return@repeatWhile
            }

            player.animate("sequences.human_furnace")
            player.playSound(2725)
            task.wait(2)

            val primaryRemoved = player.inventory.remove(bar.inputPrimary, primaryAmt, assureFullRemoval = true).hasSucceeded()
            val secondaryRemoved = if (requiresSecondary) {
                player.inventory.remove(bar.inputSecondary, secondaryAmt, assureFullRemoval = true).hasSucceeded()
            } else true

            if (primaryRemoved && secondaryRemoved) {
                val isIronBar = bar.output == "items.iron_bar".asRSCM()
                val ringOfForging = player.equipment.contains("items.ring_of_forging")
                val success = if (isIronBar && !ringOfForging && !isSuperHeat) {
                    (0..1).random() == 0
                } else true

                if (success) {
                    val xp: Int = when {
                        bar.smithxpalternate == null -> bar.smeltxp
                        bar.output == "items.blurite_bar".asRSCM() && isSuperHeat -> bar.smithxpalternate
                        bar.output == "items.gold_bar".asRSCM() && player.equipment.contains("items.gauntlets_of_goldsmithing") -> bar.smithxpalternate
                        else -> bar.smeltxp
                    }
                    player.inventory.add(bar.output)
                    player.addXp(Skills.SMITHING, xp)
                    player.message("You smelt the ${ServerCacheManager.getItem(bar.inputPrimary)!!.name} in the furnace.")
                } else {
                    player.message("The ore is too impure and you fail to refine it.")
                }
            }
            maxSmelts--
            player.unlock()
        }
    }

    private suspend fun canSmelt(
        task: QueueTask,
        player: Player,
        bar: SmithingBarsRow
    ): Boolean {
        val inventory = player.inventory

        val primaryItem = ServerCacheManager.getItem(bar.inputPrimary) ?: return false
        val primaryName = primaryItem.name
        val primaryAmt = bar.inputPrimaryAmt

        val secondaryId = bar.inputSecondary
        val secondaryAmt = bar.inputSecondaryAmt ?: 0

        val hasPrimary = inventory.getItemCount(bar.inputPrimary) >= primaryAmt

        val hasSecondary = secondaryId == null || inventory.getItemCount(secondaryId) >= secondaryAmt

        if (!hasPrimary || !hasSecondary) {
            val message = if (secondaryId == null || secondaryAmt == 0) {
                "You don't have ${primaryAmt.toLiteral()} $primaryName to smelt."
            } else {
                val secondaryName = ServerCacheManager.getItem(secondaryId)?.name ?: "materials"
                val barName = ServerCacheManager.getItem(bar.output)?.name ?: "bar"

                "You need ${primaryAmt.toLiteral()} $primaryName and " + "${secondaryAmt.toLiteral()} $secondaryName " + "to make ${barName.prefixAn()}."
            }

            task.messageBox(player, message)
            return false
        }

        val smithingLevel = player.getSkills().getCurrentLevel(Skills.SMITHING)
        if (smithingLevel < bar.level) {
            task.messageBox(player, "You need a ${Skills.getSkillName(Skills.SMITHING)} level of at least " + "${bar.level} to smelt $primaryName.")
            return false
        }

        return true
    }


}