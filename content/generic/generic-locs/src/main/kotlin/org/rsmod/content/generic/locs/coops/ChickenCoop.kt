package org.rsmod.content.generic.locs.coops

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ChickenCoop : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.chicken_coop") { searchCoop() }
    }

    private suspend fun ProtectedAccess.searchCoop() {
        arriveDelay()
        val add = invAdd(inv, "obj.egg")
        if (add.failure) {
            mes("You search the coop and find an egg but you don't have room to take it.")
            return
        }
        mes("You search the coop and find an egg.")
        anim("seq.human_pickuptable")
        soundSynth("synth.pick")
    }
}
