package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocCategory2
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.smithing.hasCannonballFurnaceMould
import org.rsmod.content.skills.smithing.openCannonballFurnaceMenu
import org.rsmod.content.skills.smithing.util.SmithingBonuses
import org.rsmod.content.skills.smithing.util.SmithingBonuses.effectiveCoalAmount
import org.rsmod.content.skills.smithing.util.SmithingBonuses.isBarEligibleForVarrockArmour
import org.rsmod.content.skills.smithing.util.SmithingBonuses.isEdgevilleFurnace
import org.rsmod.content.skills.smithing.util.SmithingBonuses.rollVarrockDoubleBar
import org.rsmod.content.skills.smithing.util.SmithingBonuses.shouldConsumeSmithingCatalyst
import org.rsmod.content.skills.smithing.util.SmithingBonuses.varrockArmourTier
import org.rsmod.content.skills.smithing.util.SmithingUtils
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import kotlin.random.Random

class SmeltingScript @Inject constructor(private val xpMods: XpModifiers, ) : PluginScript() {

    private val allBars = SmithingBarsRow.Companion.all()
    private val normalBars = allBars.filter { it.output.internalName != "obj.lovakite_bar" }
    private val barsByOutput = allBars.associateBy { it.output.internalName }

    override fun ScriptContext.startup() {
        onOpLocCategory2("category.furnace") {
            val locInternal = it.type.internalName
            val coords = it.loc.coords
            if (hasBarSmeltMaterials()) {
                openStandardSmeltMenu(locInternal, coords)
            } else if (hasCannonballFurnaceMould()) {
                openCannonballFurnaceMenu(locInternal)
            }
        }

        onOpLoc1("loc.lovakengj_furnace_large_01") { openLovakiteSmeltMenu() }

        onPlayerQueueWithArgs<SmeltTask>("queue.smithing_bar_smelt") {
            processSmeltTask(it.args)
        }
    }

    private fun ProtectedAccess.hasBarSmeltMaterials(): Boolean =
        normalBars.any { hasItemsForBar(it) }

    private suspend fun ProtectedAccess.openStandardSmeltMenu(
        furnaceLocInternal: String,
        furnaceCoords: org.rsmod.map.CoordGrid,
    ) {
        val smeltable = normalBars.filter { hasItemsForBar(it) }
        if (smeltable.isEmpty()) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "smelt",
                actionType = SkillingActionType.SMELT,
                entries = smeltable.map { smeltEntry(it) },
                maxCountProvider = { inventory, entry ->
                    barsByOutput[entry.internal]?.let { maxSmeltCount(inventory, player, it) } ?: 0
                },
            ),
        ) { selection ->
            val bar = barsByOutput[selection.entry.internal] ?: return@openSkillMulti
            startSmelting(bar, selection.amount, furnaceLocInternal, furnaceCoords)
        }
    }

    private suspend fun ProtectedAccess.openLovakiteSmeltMenu() {
        val bar = barsByOutput["obj.lovakite_bar"] ?: return
        if (!hasItemsForBar(bar, regularFurnace = false)) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "smelt",
                actionType = SkillingActionType.SMELT,
                entries = listOf(smeltEntry(bar)),
                maxCountProvider = { inventory, _ ->
                    maxSmeltCount(inventory, player, bar, regularFurnace = false)
                },
            ),
        ) { selection ->
            startSmelting(
                bar,
                selection.amount,
                furnaceLocInternal = "",
                furnaceCoords = player.coords,
                regularFurnace = false,
            )
        }
    }

    private fun ProtectedAccess.smeltEntry(bar: SmithingBarsRow): SkillMultiEntry {
        val materials = buildList {
            add(Material(bar.inputPrimary.internalName, bar.inputPrimaryAmt))
            val secondary = bar.inputSecondary
            val secondaryAmt = bar.inputSecondaryAmt
            if (secondary != null && secondaryAmt != null && secondaryAmt > 0) {
                add(Material(secondary.internalName, effectiveCoalAmount(player, inv, bar, regularFurnace = true)))
            }
        }
        return SkillMultiEntry(bar.output.internalName, materials)
    }

    private fun ProtectedAccess.startSmelting(
        bar: SmithingBarsRow,
        requestedAmount: Int,
        furnaceLocInternal: String,
        furnaceCoords: org.rsmod.map.CoordGrid,
        regularFurnace: Boolean = true,
    ) {
        val amount = minOf(requestedAmount, maxSmeltCount(inv, player, bar, regularFurnace))
        if (amount <= 0) {
            return
        }

        queueNext(
            bar,
            amount,
            completed = 0,
            furnaceLocInternal = furnaceLocInternal,
            furnaceCoords = furnaceCoords,
            regularFurnace = regularFurnace,
        )
    }

    private suspend fun ProtectedAccess.processSmeltTask(task: SmeltTask) {
        if (!canSmelt(task.bar, task.regularFurnace)) {
            resetAnim()
            return
        }

        performSmelt(
            task.bar,
            task.isSuperHeat,
            task.furnaceLocInternal,
            task.furnaceCoords,
            task.regularFurnace,
        )

        val completed = task.completed + 1
        if (completed >= task.amount || !canSmelt(task.bar, task.regularFurnace)) {
            return
        }

        queueNext(
            task.bar,
            task.amount,
            completed,
            task.isSuperHeat,
            task.furnaceLocInternal,
            task.furnaceCoords,
            task.regularFurnace,
        )
    }

    private fun ProtectedAccess.queueNext(
        bar: SmithingBarsRow,
        amount: Int,
        completed: Int,
        isSuperHeat: Boolean = false,
        furnaceLocInternal: String = "",
        furnaceCoords: org.rsmod.map.CoordGrid = player.coords,
        regularFurnace: Boolean = true,
    ) {
        weakQueue(
            "queue.smithing_bar_smelt",
            if (completed == 0) SMELT_INITIAL_DELAY else SMELT_CYCLE_DELAY,
            SmeltTask(bar, amount, completed, isSuperHeat, furnaceLocInternal, furnaceCoords, regularFurnace),
        )
    }

    private suspend fun ProtectedAccess.performSmelt(
        bar: SmithingBarsRow,
        isSuperHeat: Boolean,
        furnaceLocInternal: String,
        furnaceCoords: org.rsmod.map.CoordGrid,
        regularFurnace: Boolean,
    ) {
        anim("seq.human_furnace")
        soundSynth("synth.furnace")
        delay(2)

        val primaryAmt = bar.inputPrimaryAmt
        val secondary = bar.inputSecondary
        val requiresSecondary = secondary != null && (bar.inputSecondaryAmt ?: 0) > 0
        val effectiveSecondaryAmt =
            if (requiresSecondary) {
                effectiveCoalAmount(player, inv, bar, regularFurnace)
            } else {
                0
            }

        val primaryRemoved = invDel(inv, bar.inputPrimary.internalName, primaryAmt).success
        val secondaryRemoved =
            if (!requiresSecondary) {
                true
            } else {
                invDel(inv, secondary.internalName, effectiveSecondaryAmt).success
            }

        if (!primaryRemoved || !secondaryRemoved) {
            return
        }

        if (shouldConsumeSmithingCatalyst(player, inv, bar, regularFurnace)) {
            invDel(inv, SmithingBonuses.SMITHING_CATALYST, 1)
        }

        val isIronBar = bar.output.internalName == "obj.iron_bar"
        val hasRingOfForging = "obj.ring_of_forging" in player.worn
        val success = if (isIronBar && !hasRingOfForging && !isSuperHeat) {
            Random.nextBoolean()
        } else {
            true
        }

        if (success) {
            val outputCount = outputBarCount(bar, furnaceLocInternal, furnaceCoords)
            if (invAdd(inv, bar.output.internalName, outputCount).success) {
                val xp = SmithingSmeltXp.resolve(player, inv, bar, isSuperHeat, xpMods, regularFurnace)
                statAdvance("stat.smithing", xp)
                val oreName = SmithingUtils.itemName(bar.inputPrimary, "ore")
                mes("You smelt the $oreName in the furnace.")
                if (outputCount > 1) {
                    val barName = SmithingUtils.itemName(bar.output, "bar")
                    mes("Your Varrock armour helps you smelt an extra $barName.")
                }
            }
        } else {
            mes("The ore is too impure and you fail to refine it.")
        }
    }

    private fun ProtectedAccess.outputBarCount(
        bar: SmithingBarsRow,
        furnaceLocInternal: String,
        furnaceCoords: org.rsmod.map.CoordGrid,
    ): Int {
        if (!isEdgevilleFurnace(furnaceLocInternal, furnaceCoords)) {
            return 1
        }
        val tier = player.varrockArmourTier() ?: return 1
        if (!isBarEligibleForVarrockArmour(bar, tier)) {
            return 1
        }
        return if (rollVarrockDoubleBar()) 2 else 1
    }

    private suspend fun ProtectedAccess.canSmelt(bar: SmithingBarsRow, regularFurnace: Boolean = true): Boolean {
        val primaryAmt = bar.inputPrimaryAmt
        val secondary = bar.inputSecondary
        val secondaryAmt = effectiveCoalAmount(player, inv, bar, regularFurnace)
        val primaryName = SmithingUtils.itemName(bar.inputPrimary, "ore")

        val hasPrimary = inv.count(bar.inputPrimary.internalName) >= primaryAmt
        val requiresSecondary = secondary != null && (bar.inputSecondaryAmt ?: 0) > 0
        val hasSecondary = !requiresSecondary || inv.count(secondary.internalName) >= secondaryAmt

        if (!hasPrimary || !hasSecondary) {
            val message =
                if (!requiresSecondary) {
                    "You don't have ${SmithingUtils.countLiteral(primaryAmt)} $primaryName to smelt."
                } else {
                    val secondaryName = SmithingUtils.itemName(secondary, "materials")
                    val barName = SmithingUtils.itemName(bar.output, "bar")
                    "You need ${SmithingUtils.countLiteral(primaryAmt)} $primaryName and " +
                        "${SmithingUtils.countLiteral(secondaryAmt)} $secondaryName to make " +
                        "${SmithingUtils.prefixAn(barName)}."
                }
            mesbox(message)
            return false
        }

        return SmithingUtils.requireSmithingLevel(
            this,
            bar.level,
            "smelt $primaryName",
        )
    }

    private fun ProtectedAccess.hasItemsForBar(bar: SmithingBarsRow, regularFurnace: Boolean = true): Boolean {
        val primaryAmt = bar.inputPrimaryAmt
        val secondaryAmt = effectiveCoalAmount(player, inv, bar, regularFurnace)
        val secondary = bar.inputSecondary
        val hasPrimary = inv.count(bar.inputPrimary.internalName) >= primaryAmt
        val requiresSecondary = secondary != null && (bar.inputSecondaryAmt ?: 0) > 0
        val hasSecondary = !requiresSecondary || inv.count(secondary.internalName) >= secondaryAmt
        return hasPrimary && hasSecondary
    }

    private fun maxSmeltCount(
        inventory: Inventory,
        player: Player,
        bar: SmithingBarsRow,
        regularFurnace: Boolean = true,
    ): Int {
        val primaryAmt = bar.inputPrimaryAmt
        val effectiveSecondaryAmt = effectiveCoalAmount(player, inventory, bar, regularFurnace)
        val secondary = bar.inputSecondary
        val requiresSecondary = secondary != null && (bar.inputSecondaryAmt ?: 0) > 0

        val primaryCount = inventory.count(bar.inputPrimary.internalName)
        val secondaryCount =
            if (requiresSecondary) {
                inventory.count(secondary.internalName)
            } else {
                Int.MAX_VALUE
            }

        return minOf(
            primaryCount / primaryAmt,
            if (requiresSecondary) secondaryCount / effectiveSecondaryAmt else Int.MAX_VALUE,
        )
    }

    private data class SmeltTask(
        val bar: SmithingBarsRow,
        val amount: Int,
        val completed: Int,
        val isSuperHeat: Boolean,
        val furnaceLocInternal: String,
        val furnaceCoords: org.rsmod.map.CoordGrid,
        val regularFurnace: Boolean,
    )

    private companion object {
        private const val SMELT_INITIAL_DELAY = 1
        private const val SMELT_CYCLE_DELAY = 5
    }
}
