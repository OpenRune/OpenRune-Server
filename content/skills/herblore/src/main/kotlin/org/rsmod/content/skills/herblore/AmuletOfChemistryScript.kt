package org.rsmod.content.skills.herblore

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn1
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AmuletOfChemistryScript : PluginScript() {

    override fun ScriptContext.startup() {
        AmuletOfChemistry.AMULET_TYPES.forEach { amulet ->
            onOpHeld2(amulet) { openOptions(it.slot) }
            onOpHeld3(amulet) { breakAmulet(it.slot) }
            onOpWorn1(amulet) { openOptionsWorn() }
        }
    }

    private suspend fun ProtectedAccess.openOptions(slot: Int) {
        inv[slot]?.takeIf { amulet -> AmuletOfChemistry.AMULET_TYPES.any { amulet.isType(it) } } ?: return
        showOptionsMenu()
    }

    private suspend fun ProtectedAccess.openOptionsWorn() {
        if (!AmuletOfChemistry.isWearing(player)) {
            return
        }
        showOptionsMenu()
    }

    private suspend fun ProtectedAccess.showOptionsMenu() {
        val stop = player.shouldStopBrewingOnChemistryCrumble()
        val charges = player.chemistryChargeCount()
        val stopOnCrumble = choice2(
            choice1 = "Continue making potions when the amulet crumbles.",
            result1 = false,
            choice2 = "Stop making potions when the amulet crumbles.",
            result2 = true,
            title = "Amulet of chemistry ($charges charges)"
        )
        if (stopOnCrumble) {
            player.chemistryStopOnCrumble = true
            if (!stop) {
                mes("You will stop making potions when your amulet crumbles.")
            }
        } else {
            player.chemistryStopOnCrumble = false
            if (stop) {
                mes("You will continue making potions when your amulet crumbles.")
            }
        }
    }

    private suspend fun ProtectedAccess.breakAmulet(slot: Int) {
        val amulet = inv[slot]?.takeIf { item -> AmuletOfChemistry.AMULET_TYPES.any { item.isType(it) } } ?: return
        val confirmed = choice2(
            choice1 = "Break the amulet of chemistry?",
            result1 = true,
            choice2 = "Cancel.",
            result2 = false,
            title = "This will destroy the amulet."
        )
        if (!confirmed) {
            return
        }
        val typeName = AmuletOfChemistry.AMULET_TYPES.firstOrNull { amulet.isType(it) } ?: return
        if (invDel(inv, typeName, 1, slot = slot).failure) {
            return
        }
        AmuletOfChemistry.resetCharges(player)
        mes("You destroy the amulet. Your next amulet of chemistry will have 5 charges.")
    }
}
