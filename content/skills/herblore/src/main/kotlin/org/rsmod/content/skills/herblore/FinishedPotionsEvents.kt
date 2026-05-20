package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.events.interact.HeldUDefaultEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.content.skills.herblore.HerbloreDefinitions.hasRequiredMaterials
import org.rsmod.content.skills.herblore.HerbloreDefinitions.maxProducible
import org.rsmod.content.skills.herblore.HerbloreDefinitions.skillMultiMaterials
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FinishedPotionsEvents @Inject constructor(private val random: GameRandom) : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreDefinitions.itemToPotions.keys.filter { it !in heldUExclude }.forEach { item ->
            onOpHeldU(item) { handleFinishedPotionMix(it) }
        }

        onPlayerQueueWithArgs<FinishedPotionTask>("queue.herblore_finished") {
            processFinishedTick(it.args)
        }
    }

    private suspend fun ProtectedAccess.handleFinishedPotionMix(ev: HeldUDefaultEvents.Type) {
        val item1 = ev.first.internalName
        val item2 = ev.second.internalName

        if (isHerbVialMix(item1, item2) || isPestleMix(item1, item2) || isSwampTarMix(item1, item2)) {
            return
        }

        val candidates = HerbloreDefinitions.findPotionCandidates(item1, item2)
        if (candidates.isEmpty()) {
            return
        }

        val validCandidates = candidates.filter { potion ->
            player.herbloreLvl >= potion.levelRequired && potion.hasRequiredMaterials(inv)
        }

        if (validCandidates.isEmpty()) {
            val belowLevel = candidates.filter { player.herbloreLvl < it.levelRequired }
            if (belowLevel.isNotEmpty()) {
                val required = belowLevel.minOf { it.levelRequired }
                mesbox("You need a Herblore level of $required to make this potion.")
            }
            return
        }

        val candidatesWithMax = validCandidates
            .map { potion -> potion to potion.maxProducible(inv) }
            .filter { (_, max) -> max > 0 }

        if (candidatesWithMax.isEmpty()) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "make",
                entries =
                    candidatesWithMax.map { (potion, _) ->
                        SkillMultiEntry(
                            potion.outputPotion.internalName,
                            potion.skillMultiMaterials,
                        )
                    },
                maxCountProvider = { inventory, entry ->
                    validCandidates
                        .firstOrNull { it.outputPotion.internalName == entry.internal }
                        ?.maxProducible(inventory) ?: 0
                },
            ),
        ) { selection ->
            val potion = candidatesWithMax.firstOrNull { (row, _) ->
                row.outputPotion.internalName == selection.entry.internal
            }?.first ?: return@openSkillMulti

            startFinishedPotion(potion, selection.amount)
        }
    }

    private suspend fun ProtectedAccess.startFinishedPotion(potion: HerbloreFinishedRow, amount: Int) {
        if (player.herbloreLvl < potion.levelRequired) {
            mesbox("You need a Herblore level of ${potion.levelRequired} to make this potion.")
            return
        }

        if (!potion.hasRequiredMaterials(inv)) {
            mes(Constants.dm_default)
            return
        }

        weakQueue("queue.herblore_finished", 1, FinishedPotionTask(potion, amount, 0))
    }

    private suspend fun ProtectedAccess.processFinishedTick(task: FinishedPotionTask) {
        val potion = task.potion
        val brewResult = AmuletOfChemistry.rollBrewOutput(player, random, potion)
        val output = brewResult.output

        if (player.herbloreLvl < potion.levelRequired) {
            resetAnim()
            return
        }

        if (!potion.hasRequiredMaterials(inv) || (inv.freeSpace() < 1 && !inv.contains(output))) {
            resetAnim()
            return
        }

        anim("seq.human_herbing_vial")

        val savedSecondaries = HerbloreGoggles.rollSavedSecondaryCounts(player, random, potion)

        val removed = mutableListOf<Pair<String, Int>>()
        var allRemoved = true

        val unfinishedRemoved = invDel(inv, potion.unfPot.internalName, 1).success
        removed += potion.unfPot.internalName to if (unfinishedRemoved) 1 else 0
        if (!unfinishedRemoved) {
            allRemoved = false
        }

        if (potion.secondaries.size == 1) {
            val secondary = potion.secondaries.first()
            val count = potion.secondariesAmount ?: 1
            val toRemove = (count - savedSecondaries.savedAmount(secondary.internalName)).coerceAtLeast(0)
            val secondaryRemoved =
                if (toRemove == 0) {
                    true
                } else {
                    invDel(inv, secondary.internalName, toRemove).success
                }
            removed += secondary.internalName to if (secondaryRemoved) toRemove else 0
            if (!secondaryRemoved) {
                allRemoved = false
            }
        } else {
            for (secondary in potion.secondaries) {
                val saved = savedSecondaries.savedAmount(secondary.internalName)
                val toRemove = (1 - saved).coerceAtLeast(0)
                val secondaryRemoved =
                    if (toRemove == 0) {
                        true
                    } else {
                        invDel(inv, secondary.internalName, toRemove).success
                    }
                removed += secondary.internalName to if (secondaryRemoved) toRemove else 0
                if (!secondaryRemoved) {
                    allRemoved = false
                }
            }
        }

        if (!allRemoved) {
            removed.forEach { (item, amount) ->
                if (amount > 0) {
                    invAdd(inv, item, amount)
                }
            }
            mes(Constants.dm_default)
            resetAnim()
            return
        }

        if (invAdd(inv, output, 1).failure) {
            removed.forEach { (item, amount) ->
                if (amount > 0) {
                    invAdd(inv, item, amount)
                }
            }
            mes("You don't have enough inventory space to make this potion.")
            resetAnim()
            return
        }

        if (potion.xp > 0) {
            statAdvance("stat.herblore", potion.xp.toDouble())
        }

        val itemForMessage = if (potion.secondaries.size > 1) {
            potion.unfPot
        } else {
            potion.secondaries.firstOrNull() ?: potion.unfPot
        }
        mes("You mix the ${itemForMessage.name.lowercase()} into your potion.")

        if (brewResult.extraDoseApplied) {
            mes("Your amulet of chemistry helps you create an extra dose.")
        }

        if (brewResult.crumbled) {
            mes("Your amulet of chemistry crumbles to dust.")
            if (player.shouldStopBrewingOnChemistryCrumble()) {
                resetAnim()
                return
            }
        }

        val created = task.created + 1
        if (created < task.amount) {
            weakQueue("queue.herblore_finished", 4, FinishedPotionTask(potion, task.amount, created))
        } else {
            resetAnim()
        }
    }

    private fun isHerbVialMix(item1: String, item2: String): Boolean =
        (item1 == "obj.vial_water" && item2 in HerbloreDefinitions.herbItemNames) ||
            (item2 == "obj.vial_water" && item1 in HerbloreDefinitions.herbItemNames)

    private fun isPestleMix(item1: String, item2: String): Boolean =
        item1 == "obj.pestle_and_mortar" || item2 == "obj.pestle_and_mortar"

    private fun isSwampTarMix(item1: String, item2: String): Boolean =
        item1 == "obj.swamp_tar" || item2 == "obj.swamp_tar"

    private data class FinishedPotionTask(
        val potion: HerbloreFinishedRow,
        val amount: Int,
        val created: Int,
    )

    companion object {

        private val heldUExclude: Set<String> = buildSet {
            add("obj.vial_water")
            add("obj.pestle_and_mortar")
            add("obj.swamp_tar")
            addAll(HerbloreDefinitions.herbItemNames)
            HerbloreDefinitions.barbarianMixes.forEach { mix ->
                add(mix.twoDosePotion.internalName)
                add(mix.mixIngredient.internalName)
            }
        }
    }
}
