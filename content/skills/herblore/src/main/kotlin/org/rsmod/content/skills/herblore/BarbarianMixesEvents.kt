package org.rsmod.content.skills.herblore

import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreBarbarianMixesRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BarbarianMixesEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreDefinitions.barbarianMixes.forEach { mix ->
            onOpHeldU(mix.twoDosePotion.internalName, mix.mixIngredient.internalName) {
                handleBarbarianMix(it)
            }
        }
        onPlayerQueueWithArgs<BarbarianMixTask>("queue.herblore_barbarian_mix") {
            processBarbarianMixTick(it.args)
        }
    }

    private suspend fun ProtectedAccess.handleBarbarianMix(ev: HeldUEvents.Type) {
        val candidates = HerbloreDefinitions.barbarianMixes.filter { mix ->
            mix.twoDosePotion.internalName == ev.first.internalName && mix.mixIngredient.internalName == ev.second.internalName
        }

        if (candidates.isEmpty()) {
            return
        }

        val validCandidates = candidates.filter { mix -> player.herbloreLvl >= mix.level &&
            inv.contains(mix.twoDosePotion.internalName) &&
            inv.contains(mix.mixIngredient.internalName)
        }

        if (validCandidates.isEmpty()) {
            return
        }

        val candidatesWithMax = validCandidates.map { mix ->
            val maxProducible = minOf(
                inv.count(mix.twoDosePotion.internalName),
                inv.count(mix.mixIngredient.internalName)
            )
            mix to maxProducible
        }.filter { (_, max) -> max > 0 }

        if (candidatesWithMax.isEmpty()) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "make",
                actionType = SkillingActionType.MAKE,
                entries =
                    candidatesWithMax.map { (mix, _) ->
                        SkillMultiEntry(
                            mix.barbarianMix.internalName,
                            listOf(
                                Material(mix.twoDosePotion.internalName),
                                Material(mix.mixIngredient.internalName),
                            ),
                        )
                    },
                maxCountProvider = { inventory, entry ->
                    validCandidates
                        .firstOrNull { it.barbarianMix.internalName == entry.internal }
                        ?.let { mix ->
                            minOf(
                                inventory.count(mix.twoDosePotion.internalName),
                                inventory.count(mix.mixIngredient.internalName),
                            )
                        } ?: 0
                },
            ),
        ) { selection ->
            val mix =
                candidatesWithMax
                    .firstOrNull { (row, _) ->
                        row.barbarianMix.internalName == selection.entry.internal
                    }
                    ?.first ?: return@openSkillMulti

            startBarbarianMix(mix, selection.amount)
        }
    }

    private fun ProtectedAccess.startBarbarianMix(mix: HerbloreBarbarianMixesRow, amount: Int) {
        if (player.herbloreLvl < mix.level) {
            mes("You need a Herblore level of ${mix.level} to make this mix.")
            return
        }

        if (!inv.contains(mix.twoDosePotion.internalName) || !inv.contains(mix.mixIngredient.internalName)) {
            mes(Constants.dm_default)
            return
        }

        weakQueue("queue.herblore_barbarian_mix", 1, BarbarianMixTask(mix, amount, 0))
    }

    private suspend fun ProtectedAccess.processBarbarianMixTick(task: BarbarianMixTask) {
        val mix = task.mix

        if (player.herbloreLvl < mix.level) {
            resetAnim()
            return
        }

        if (!inv.contains(mix.twoDosePotion.internalName) ||
            !inv.contains(mix.mixIngredient.internalName) ||
            (inv.freeSpace() < 1 && !inv.contains(mix.barbarianMix.internalName))
        ) {
            resetAnim()
            return
        }

        val potionRemoved = invDel(inv, mix.twoDosePotion.internalName, 1).success
        val ingredientRemoved = invDel(inv, mix.mixIngredient.internalName, 1).success

        if (!potionRemoved || !ingredientRemoved) {
            if (potionRemoved) invAdd(inv, mix.twoDosePotion.internalName, 1)
            if (ingredientRemoved) invAdd(inv, mix.mixIngredient.internalName, 1)
            mes(Constants.dm_default)
            resetAnim()
            return
        }

        if (invAdd(inv, mix.barbarianMix.internalName, 1).failure) {
            invAdd(inv, mix.twoDosePotion.internalName, 1)
            invAdd(inv, mix.mixIngredient.internalName, 1)
            mes("You don't have enough inventory space to make this mix.")
            resetAnim()
            return
        }

        if (mix.xp > 0) {
            statAdvance("stat.herblore", mix.xp.toDouble())
        }

        val mixName = mix.barbarianMix.name
        mes("You add the ingredient to the potion.")
        mes("You make a $mixName.")

        val created = task.created + 1
        if (created < task.amount) {
            weakQueue("queue.herblore_barbarian_mix", 4, BarbarianMixTask(mix, task.amount, created))
        } else {
            resetAnim()
        }
    }

    private data class BarbarianMixTask(
        val mix: HerbloreBarbarianMixesRow,
        val amount: Int,
        val created: Int,
    )
}
