package org.rsmod.content.skills.herblore

import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreUnfinishedRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class UnfinishedPotionsEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreDefinitions.herbItemNames.forEach { herb ->
            onOpHeldU("obj.vial_water", herb) { handleHerbAndVial(it) }
        }
        onPlayerQueueWithArgs<UnfinishedPotionTask>("queue.herblore_unfinished") {
            processUnfinishedTick(it.args)
        }
    }

    private suspend fun ProtectedAccess.handleHerbAndVial(ev: HeldUEvents.Type) {
        if (
            ev.first.internalName != "obj.vial_water" ||
            ev.second.internalName !in HerbloreDefinitions.herbItemNames
        ) {
            return
        }

        val herbName = ev.second.internalName
        val potionForHerb = HerbloreDefinitions.unfinishedPotions.firstOrNull {
            it.herbItem.internalName == herbName
        } ?: return

        if (player.herbloreLvl < potionForHerb.level) {
            mesbox("You need a Herblore level of ${potionForHerb.level} to make this potion.")
            return
        }

        val validCandidates =
            HerbloreDefinitions.unfinishedPotions.filter { potion ->
                inv.contains(potion.herbItem.internalName) &&
                    inv.contains("obj.vial_water") &&
                    player.herbloreLvl >= potion.level
            }

        if (validCandidates.isEmpty()) {
            return
        }

        val candidatesWithMax = validCandidates.map { potion ->
            val maxProducible = minOf(
                inv.count(potion.herbItem.internalName),
                inv.count("obj.vial_water")
            )
            potion to maxProducible
        }.filter { (_, max) -> max > 0 }

        if (candidatesWithMax.isEmpty()) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "make",
                entries =
                    candidatesWithMax.map { (potion, _) ->
                        SkillMultiEntry(
                            potion.unfinishedPotion.internalName,
                            listOf(
                                Material(potion.herbItem.internalName),
                                Material("obj.vial_water"),
                            ),
                        )
                    },
                maxCountProvider = { inventory, entry ->
                    HerbloreDefinitions.unfinishedPotions
                        .firstOrNull { it.unfinishedPotion.internalName == entry.internal }
                        ?.let { potion ->
                            minOf(
                                inventory.count(potion.herbItem.internalName),
                                inventory.count("obj.vial_water"),
                            )
                        } ?: 0
                },
            ),
        ) { selection ->
            val potion =
                candidatesWithMax
                    .firstOrNull { (row, _) ->
                        row.unfinishedPotion.internalName == selection.entry.internal
                    }
                    ?.first ?: return@openSkillMulti

            startUnfinishedPotion(potion, selection.amount)
        }
    }

    private suspend fun ProtectedAccess.startUnfinishedPotion(potion: HerbloreUnfinishedRow, amount: Int) {
        if (player.herbloreLvl < potion.level) {
            mesbox("You need a Herblore level of ${potion.level} to make this potion.")
            return
        }

        if (!inv.contains(potion.herbItem.internalName) || !inv.contains("obj.vial_water")) {
            mes("You don't have all the ingredients needed to make this potion.")
            return
        }

        weakQueue("queue.herblore_unfinished", 1, UnfinishedPotionTask(potion, amount, 0))
    }

    private suspend fun ProtectedAccess.processUnfinishedTick(task: UnfinishedPotionTask) {
        val potion = task.potion

        if (player.herbloreLvl < potion.level) {
            mesbox("You need a Herblore level of ${potion.level} to make this potion.")
            resetAnim()
            return
        }

        if (
            !inv.contains(potion.herbItem.internalName) ||
            !inv.contains("obj.vial_water") ||
            (inv.freeSpace() < 1 && !inv.contains(potion.unfinishedPotion.internalName))
        ) {
            resetAnim()
            return
        }

        anim("seq.human_herbing_vial")

        val herbRemoved = invDel(inv, potion.herbItem.internalName, 1).success
        val vialRemoved = invDel(inv, "obj.vial_water", 1).success

        if (!herbRemoved || !vialRemoved) {
            if (herbRemoved) invAdd(inv, potion.herbItem.internalName, 1)
            if (vialRemoved) invAdd(inv, "obj.vial_water", 1)
            resetAnim()
            return
        }

        if (invAdd(inv, potion.unfinishedPotion.internalName, 1).failure) {
            invAdd(inv, potion.herbItem.internalName, 1)
            invAdd(inv, "obj.vial_water", 1)
            mes("You don't have enough inventory space to make this potion.")
            resetAnim()
            return
        }

        statAdvance("stat.herblore", potion.xp.toDouble())
        val herbName = potion.herbItem.name.lowercase()
        mes("You put the $herbName into the vial of water.")

        val created = task.created + 1
        if (created < task.amount) {
            weakQueue("queue.herblore_unfinished", 4, UnfinishedPotionTask(potion, task.amount, created))
        } else {
            resetAnim()
        }
    }

    private data class UnfinishedPotionTask(
        val potion: HerbloreUnfinishedRow,
        val amount: Int,
        val created: Int,
    )

}
