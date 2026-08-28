package org.rsmod.content.skills.fishing.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FishOffcutsScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        val cookable = CookingFoodsRow.all().mapTo(HashSet()) { it.input.internalName }
        val rawFish = FishingSpotRow.all().map { it.fish.internalName }.filterTo(HashSet()) { it in cookable }

        for (fish in rawFish) {
            val offcut = if (fish in FINE_OFFCUT_FISH) FINE_FISH_OFFCUTS else FISH_OFFCUTS
            onOpHeldU(KNIFE, fish) { cutFish(fish, offcut) }
        }
    }

    private fun ProtectedAccess.cutFish(fish: String, offcut: String) {
        if (inv.isFull() && invTotal(inv, offcut) == 0) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        invDel(inv, fish, 1)
        invAdd(inv, offcut, 1)
        statAdvance("stat.cooking", CUT_XP)
        spam("You cut the fish into offcuts.")
    }

    private companion object {
        private const val KNIFE = "obj.knife"
        private const val FISH_OFFCUTS = "obj.fish_chunks"
        private const val FINE_FISH_OFFCUTS = "obj.sailing_fine_fish_offcuts"
        private const val CUT_XP = 2.0

        private val FINE_OFFCUT_FISH =
            setOf(
                "obj.raw_shark",
                "obj.raw_seaturtle",
                "obj.raw_anglerfish",
                "obj.raw_dark_crab",
                "obj.raw_mantaray",
                "obj.raw_yellowfin",
                "obj.raw_halibut",
                "obj.raw_bluefin",
                "obj.raw_jumbo_squid",
                "obj.raw_marlin",
            )
    }
}
