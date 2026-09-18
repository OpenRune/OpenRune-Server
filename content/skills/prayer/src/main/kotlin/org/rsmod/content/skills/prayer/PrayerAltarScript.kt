package org.rsmod.content.skills.prayer

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PrayerAltarScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (altar in ALTARS) {
            onOpLoc1(altar) { prayAt() }
        }
    }

    private suspend fun ProtectedAccess.prayAt() {
        arriveDelay()
        if (player.prayerLvl >= player.basePrayerLvl) {
            mes("You already have full Prayer Points.")
            return
        }
        anim("seq.human_pray")
        soundSynth("synth.prayer_altar_recharge")
        statRestore("stat.prayer")
        spam("You recharge your Prayer Points.")
    }

    private companion object {
        val ALTARS = listOf("loc.altar", "loc.altarv2", "loc.fai_varrock_church_altar")
    }
}
