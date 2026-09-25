package org.rsmod.content.other.pets

import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.PetSkillingRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Rolls every skilling pet from `dbtable.pet_skilling`. Any skill that publishes a
 * [SkillingProductPrepareEvent] is covered simply by adding a row for it.
 */
class PetSkillingScript @Inject constructor(private val rewards: PetRewards) : PluginScript() {
    private class Entry(row: PetSkillingRow) {
        val pet: String = row.pet.internalName
        val skill: String = row.skill.internalName
        val base: Int = row.base
        val perResource: Map<String, Int> =
            row.chances.backing.mapNotNull { (item, base) -> base?.let { item.internalName to it } }.toMap()
    }

    private val bySkill: Map<String, Entry> = PetSkillingRow.all().associate { it.skill.internalName to Entry(it) }

    override fun ScriptContext.startup() {
        onEvent<SkillingProductPrepareEvent> { roll(product) }
    }

    private fun roll(product: SkillingProduct) {
        if (product.cancelled) {
            return
        }
        val entry = bySkill[product.skill] ?: return
        val base = entry.perResource[product.item] ?: entry.base
        if (base <= 0) {
            return
        }
        rewards.rollSkillingPet(product.player, entry.pet, entry.skill, base, rarity(product))
    }

    /**
     * Shark lures are the one resource that changes its own pet rate: the wiki states they make the
     * heron rarer, scaling with how many are used per catch.
     */
    private fun rarity(product: SkillingProduct): Int {
        if (product.item != RAW_SHARK) {
            return 1
        }
        val player = product.player
        val selected = SHARK_LURE_QUANTITIES.getOrElse(player.vars["varbit.shark_lure_use_quantity"]) { 1 }
        if (player.inv.count(SHARK_LURE) < selected) {
            return 1
        }
        return when (selected) {
            5 -> 6
            3 -> 5
            else -> 4
        }
    }

    private companion object {
        const val RAW_SHARK = "obj.raw_shark"
        const val SHARK_LURE = "obj.shark_lure"
        val SHARK_LURE_QUANTITIES = listOf(1, 3, 5)
    }
}
