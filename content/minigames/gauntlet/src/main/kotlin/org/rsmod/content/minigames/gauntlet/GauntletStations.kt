package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletStations @Inject constructor(private val runs: GauntletRuns) : PluginScript() {
    override fun ScriptContext.startup() {
        for (suffix in listOf("", "_hm")) {
            onOpLoc1("loc.gauntlet_singing_bowl$suffix") { singCrystals() }
            onOpLoc1("loc.gauntlet_tools$suffix") { takeTools() }
            onOpLoc1("loc.gauntlet_book$suffix") { ifOpenMainModal("interface.gauntlet_recipes") }
            onOpLoc1("loc.gauntlet_book_2$suffix") { readEgniolScroll() }
            onOpLoc1("loc.gauntlet_exit$suffix") { confirmLeave() }
            onOpLoc2("loc.gauntlet_exit$suffix") { with(runs) { leave() } }
            val vial = "obj.gauntlet_vial_empty$suffix"
            onOpLocU("loc.gauntlet_sink$suffix", vial) { fillVials(vial) }
            onOpLoc1("loc.gauntlet_sink$suffix") { fillVials(vial) }
            onOpLocU("loc.gauntlet_pond$suffix", vial) { fillVialsAtPond(vial) }
        }
    }

    private fun ProtectedAccess.fillVialsAtPond(vial: String) {
        val count = inv.count(vial)
        if (count == 0) return
        invDel(inv, vial, count)
        invAdd(inv, "obj.gauntlet_vial_water", count)
        spam("You fill the crystal vials with water.")
    }

    private suspend fun ProtectedAccess.confirmLeave() {
        val leave =
            choice2("Leave the Gauntlet and forfeit this run.", true, "Stay.", false, title = "Leave the Gauntlet?")
        if (leave) with(runs) { leave() }
    }

    private fun ProtectedAccess.takeTools() {
        val corrupted = player.gauntletCorrupted
        val missing =
            listOf("axe", "pickaxe", "harpoon", "pestle", "sceptre")
                .map { gauntletObj(it, corrupted) }
                .filter { it !in inv && it !in player.worn }
        if (missing.isEmpty()) {
            mes("You already have all the tools you need.")
            return
        }
        for (tool in missing) {
            if (!invAdd(inv, tool, 1).success) {
                mes("You don't have enough inventory space.")
                return
            }
        }
        mes("You take the missing tools from the storage.")
    }

    private suspend fun ProtectedAccess.readEgniolScroll() {
        mesbox(
            "Egniol potion: fill a crystal vial at the water pump, add a grym leaf, " +
                "then mix in crystal dust made by crushing shards with the pestle."
        )
    }

    private suspend fun ProtectedAccess.fillVials(vial: String) {
        if (vial !in inv) {
            mes("You need an empty crystal vial to use the pump.")
            return
        }
        while (vial in inv) {
            invDel(inv, vial, 1)
            invAdd(inv, "obj.gauntlet_vial_water", 1)
            spam("You fill a crystal vial with water.")
            delay(2)
        }
    }
}
