package org.alter.skills.smithing

import dev.openrune.ServerCacheManager
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.messageBox
import org.alter.api.ext.produceItemBox
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.api.ext.prefixAn
import org.alter.api.ext.toLiteral
import org.alter.skills.smithing.SmithingData.allBars
import org.generated.tables.smithing.SmithingBarsRow

class SmeltingEvents : PluginEvent() {

    val normalBars: List<SmithingBarsRow> = allBars.filter { it.output != "items.lovakite_bar".asRSCM() }

    override fun init() {
        on<ObjectClickEvent> {
            where {
                gameObject.getDef().category == SmithingData.FURNACE_CATEGORY &&
                    player.inventory.containsAny(allBars.map { it.inputPrimary }.toList())
            }
            then { smeltStandard(player) }
        }

        on<ObjectClickEvent> {
            where { gameObject.id == "objects.lovakengj_furnace_large_01" }
            then {
                player.queue {
                    produceItemBox(
                        player,
                        "items.lovakite_bar".asRSCM(),
                        title = "What would you like to smelt?",
                        logic = ::smeltItem
                    )
                }
            }
        }
    }

    fun smeltStandard(player: Player) {
        player.queue {
            val smeltableBars = normalBars.filter { hasItemsForBar(player, it) }
            if (smeltableBars.isEmpty()) return@queue
            produceItemBox(
                player,
                *smeltableBars.map { it.output }.toIntArray(),
                title = "What would you like to smelt?",
                logic = ::smeltItem
            )
        }
    }

    fun smeltItem(player: Player, output: Int, amount: Int = 28) {
        val bar = SmithingData.barsByOutput[output] ?: return
        player.queue { smelt(this, player, bar, amount) }
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
        val hasCatalyst = player.inventory.contains("items.smithing_catalyst".asRSCM())
        val isCoalBar = bar.inputSecondary == "items.coal".asRSCM()
        val effectiveSecondaryAmt = if (hasCatalyst && isCoalBar) (secondaryAmt / 2).coerceAtLeast(1) else secondaryAmt

        val primaryCount = player.inventory.getItemCount(bar.inputPrimary)
        val secondaryCount = if (requiresSecondary) {
            if (isCoalBar) CoalBagEvents.getEffectiveCoalCount(player)
            else player.inventory.getItemCount(bar.inputSecondary)
        } else {
            player.inventory.type.size
        }

        val maxByMaterials = minOf(
            primaryCount / primaryAmt,
            if (requiresSecondary) secondaryCount / effectiveSecondaryAmt else Int.MAX_VALUE
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

            player.animate(SmithingData.FURNACE_ANIMATION)
            player.playSound(SmithingData.FURNACE_SOUND)
            task.wait(2)

            val primaryRemoved = player.inventory.remove(bar.inputPrimary, primaryAmt, assureFullRemoval = true).hasSucceeded()
            val secondaryRemoved = if (requiresSecondary) {
                if (isCoalBar) CoalBagEvents.consumeCoal(player, effectiveSecondaryAmt)
                else player.inventory.remove(bar.inputSecondary, effectiveSecondaryAmt, assureFullRemoval = true).hasSucceeded()
            } else true

            if (primaryRemoved && secondaryRemoved) {
                val isIronBar = bar.output == "items.iron_bar".asRSCM()
                val ringOfForging = player.equipment.contains("items.ring_of_forging")
                val success = if (isIronBar && !ringOfForging && !isSuperHeat) {
                    (0..1).random() == 0
                } else true

                if (success) {
                    var xp: Int = when {
                        bar.smithxpalternate == null -> bar.smeltxp
                        bar.output == "items.blurite_bar".asRSCM() && isSuperHeat -> bar.smithxpalternate
                        bar.output == "items.gold_bar".asRSCM() && player.equipment.containsAny(
                            "items.gauntlets_of_goldsmithing",
                            "items.skillcape_smithing",
                            "items.skillcape_smithing_trimmed",
                            "items.skillcape_max"
                        ) -> bar.smithxpalternate
                        else -> bar.smeltxp
                    }
                    if (hasCatalyst && isCoalBar) xp *= 2
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

    private suspend fun canSmelt(task: QueueTask, player: Player, bar: SmithingBarsRow): Boolean {
        val primaryItem = ServerCacheManager.getItem(bar.inputPrimary) ?: return false
        val primaryName = primaryItem.name
        val primaryAmt = bar.inputPrimaryAmt
        val secondaryId = bar.inputSecondary
        val secondaryAmt = bar.inputSecondaryAmt ?: 0

        val hasPrimary = player.inventory.getItemCount(bar.inputPrimary) >= primaryAmt
        val effectiveSecondary = if (secondaryId == "items.coal".asRSCM()) {
            CoalBagEvents.getEffectiveCoalCount(player)
        } else {
            if (secondaryId != null) player.inventory.getItemCount(secondaryId) else Int.MAX_VALUE
        }
        val hasSecondary = secondaryId == null || effectiveSecondary >= secondaryAmt

        if (!hasPrimary || !hasSecondary) {
            val message = if (secondaryId == null || secondaryAmt == 0) {
                "You don't have ${primaryAmt.toLiteral()} $primaryName to smelt."
            } else {
                val secondaryName = ServerCacheManager.getItem(secondaryId)?.name ?: "materials"
                val barName = ServerCacheManager.getItem(bar.output)?.name ?: "bar"
                "You need ${primaryAmt.toLiteral()} $primaryName and ${secondaryAmt.toLiteral()} $secondaryName to make ${barName.prefixAn()}."
            }
            task.messageBox(player, message)
            return false
        }

        return SmithingUtils.requireSmithingLevel(task, player, bar.level, "smelt $primaryName")
    }


    private fun hasItemsForBar(player: Player, bar: SmithingBarsRow): Boolean {
        val primaryAmt = bar.inputPrimaryAmt
        val secondaryAmt = bar.inputSecondaryAmt ?: 0
        val hasPrimary = player.inventory.getItemCount(bar.inputPrimary) >= primaryAmt
        val effectiveSecondary = if (bar.inputSecondary == "items.coal".asRSCM()) {
            CoalBagEvents.getEffectiveCoalCount(player)
        } else {
            if (bar.inputSecondary != null) player.inventory.getItemCount(bar.inputSecondary) else Int.MAX_VALUE
        }
        val hasSecondary = bar.inputSecondary == null || secondaryAmt == 0 || effectiveSecondary >= secondaryAmt
        return hasPrimary && hasSecondary
    }



}