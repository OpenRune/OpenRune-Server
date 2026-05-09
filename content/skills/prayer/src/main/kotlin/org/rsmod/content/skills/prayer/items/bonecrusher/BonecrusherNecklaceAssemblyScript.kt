package org.rsmod.content.skills.prayer.items.bonecrusher

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BonecrusherNecklaceAssemblyScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU("obj.bonecrusher", "obj.hydra_tail") { tryAssemble(it) }
        onOpHeldU("obj.bonecrusher", "obj.dragonbone_necklace") { tryAssemble(it) }
        onOpHeldU("obj.hydra_tail", "obj.dragonbone_necklace") { tryAssemble(it) }
    }

    private suspend fun ProtectedAccess.tryAssemble(ev: HeldUEvents.Type) {
        if (player.basePrayerLvl < 70) {
            mes("You need a Prayer level of at least 70 to wear the bonecrusher necklace.")
            return
        }

        if (inv.contains("obj.bonecrusher_necklace")) {
            mes("You already have a bonecrusher necklace.")
            return
        }

        val reagents = inv.findBonecrusherCraftReagents() ?: run {
            mes(
                "You need a bonecrusher, hydra tail and dragonbone necklace to make the bonecrusher necklace.",
            )
            return
        }

        val usedSlots = setOf(ev.firstSlot, ev.secondSlot)
        if (!usedSlots.all(reagents.values::contains)) {
            mes("You need a bonecrusher, hydra tail and dragonbone necklace to make the bonecrusher necklace.")
            return
        }

        val proceed =
            choice2(
                "Proceed with the combination.",
                true,
                "Cancel.",
                false,
                title =
                    "Are you sure you wish to combine the Hydra Tail, Dragonbone, and the Bonecrusher to create the Necklace.",
            )
        if (!proceed) {
            return
        }

        val crusherSlot = reagents["obj.bonecrusher"] ?: return
        val crusherObj = inv[crusherSlot] ?: return
        val crusherVars = crusherObj.vars

        val deleteOrder = listOf("obj.bonecrusher", "obj.hydra_tail", "obj.dragonbone_necklace").map {
            type -> type to reagents.getValue(type)
        }.sortedByDescending { (_, slot) -> slot }

        for ((type, slot) in deleteOrder) {
            if (invDel(inv, type, count = 1, slot = slot).failure) {
                return
            }
        }

        if (invAdd(inv, "obj.bonecrusher_necklace", count = 1, vars = crusherVars).failure) {
            return
        }

        objbox(
            "obj.bonecrusher_necklace",
            400,
            "You successfully combine the Hydra Tail, Dragonbone Necklace, and the Bonecrusher into the Bonecrusher Necklace.",
        )
    }

    private fun Inventory.findBonecrusherCraftReagents(): Map<String, Int>? {
        val required = setOf("obj.bonecrusher", "obj.hydra_tail", "obj.dragonbone_necklace")
        val found = mutableMapOf<String, Int>()
        for (slot in indices) {
            val obj = this[slot] ?: continue
            val internal = RSCM.getReverseMapping(RSCMType.OBJ, obj.id)
            if (internal !in required) {
                continue
            }
            if (found.put(internal, slot) != null) {
                return null
            }
        }
        return found.takeIf { it.size == required.size }
    }

}
