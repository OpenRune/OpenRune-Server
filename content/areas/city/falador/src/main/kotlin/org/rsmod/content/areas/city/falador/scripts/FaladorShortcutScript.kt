package org.rsmod.content.areas.city.falador.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FaladorShortcutScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.falador_sc_castlewall_south") { crawlUnderWall(NORTH_TUNNEL_EXIT) }
        onOpLoc1("loc.falador_sc_castlewall_north") { crawlUnderWall(SOUTH_TUNNEL_EXIT) }
    }

    private suspend fun ProtectedAccess.crawlUnderWall(dest: CoordGrid) {
        arriveDelay()
        if (player.agilityLvl < UNDERWALL_AGILITY) {
            mes("You need an Agility level of $UNDERWALL_AGILITY to negotiate this obstacle.")
            return
        }
        anim("seq.human_longcrawl")
        delay(1)
        telejump(dest)
        spam("You climb under the wall.")
    }

    private companion object {
        const val UNDERWALL_AGILITY = 26
        val NORTH_TUNNEL_EXIT = CoordGrid(2948, 3313, 0)
        val SOUTH_TUNNEL_EXIT = CoordGrid(2948, 3309, 0)
    }
}
