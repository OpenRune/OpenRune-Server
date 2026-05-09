package org.rsmod.content.skills.prayer.blessed

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SunfireWineEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU("obj.jug_wine","obj.sunfiresplinter") { startMakeSunfireWine() }
        onOpHeldU("obj.jug_wine_blessed","obj.sunfiresplinter") { rejectBlessedWineMix() }
        onPlayerQueue("queue.prayer_make_sunfire_wine") { processMakeSunfireWine() }
    }

    private fun ProtectedAccess.startMakeSunfireWine() {
        if (!canMakeSunfireWine()) {
            return
        }
        processMakeSunfireWine()
    }

    private fun ProtectedAccess.processMakeSunfireWine() {
        if (!canMakeSunfireWine()) {
            return
        }

        val splinter = "obj.sunfiresplinter"
        val del = invDel(
            inv = inv,
            type1 = splinter,
            count1 = 2,
            type2 = "obj.jug_wine",
            count2 = 1,
        )
        if (del.failure) {
            return
        }

        player.anim("seq.human_herbing_grind")
        invAdd(inv = inv, type = "obj.jug_sunfire_wine", count = 1)
        mes("You crush the sunfire splinters into the wine, making a jug of sunfire wine.")

        if (canMakeSunfireWine()) {
            weakQueue("queue.prayer_make_sunfire_wine", 4)
        }
    }

    private fun ProtectedAccess.canMakeSunfireWine(): Boolean {
        if (!inv.contains("obj.pestle_and_mortar")) {
            mes("You need a pestle and mortar to crush the splinters into the wine.")
            return false
        }
        val splinter = "obj.sunfiresplinter"
        val splinterCount = inv.count(splinter)
        if (splinterCount < 2) {
            return false
        }
        val wineCount = inv.count("obj.jug_wine")
        if (wineCount <= 0) {
            return false
        }
        return true
    }

    private fun ProtectedAccess.rejectBlessedWineMix() {
        mes("You can only add sunfire splinters to an unblessed jug of wine.")
    }

}
