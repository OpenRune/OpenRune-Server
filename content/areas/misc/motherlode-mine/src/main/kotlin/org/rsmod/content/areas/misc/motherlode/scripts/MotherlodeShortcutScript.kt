package org.rsmod.content.areas.misc.motherlode.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeShortcutScript : PluginScript() {
    private val Player.faladorEasyDiaryComplete by boolVarBit("varbit.falador_diary_easy_complete")
    private val Player.faladorMediumDiaryComplete by boolVarBit("varbit.falador_diary_medium_complete")

    override fun ScriptContext.startup() {
        onOpLoc1("loc.motherlode_shortcut") { squeezeThrough(it.loc) }
    }

    private suspend fun ProtectedAccess.squeezeThrough(tunnel: BoundLocInfo) {
        val route = ROUTES[tunnel.coords] ?: return
        arriveDelay()
        if (player.agilityLvl < AGILITY_LEVEL) {
            mes("You need an Agility level of $AGILITY_LEVEL to negotiate this tunnel.")
            return
        }
        if (route.requiresDiary && !(player.faladorEasyDiaryComplete && player.faladorMediumDiaryComplete)) {
            mes("You need to complete the easy and medium Falador Diaries to use this tunnel.")
            return
        }
        anim("seq.human_longcrawl")
        soundSynth(CRAWL_SOUND, loops = 2, delay = 4)
        delay(CRAWL_CYCLES)
        telejump(route.exit)
        resetAnim()
    }

    private data class Route(val exit: CoordGrid, val requiresDiary: Boolean)

    private companion object {
        const val AGILITY_LEVEL = 54
        const val CRAWL_SOUND = 2454
        const val CRAWL_CYCLES = 2

        val ROUTES =
            mapOf(
                CoordGrid(3760, 5670, 0) to Route(CoordGrid(3765, 5671, 0), requiresDiary = true),
                CoordGrid(3764, 5671, 0) to Route(CoordGrid(3759, 5670, 0), requiresDiary = true),
                CoordGrid(3744, 5642, 0) to Route(CoordGrid(3745, 5646, 0), requiresDiary = false),
                CoordGrid(3745, 5645, 0) to Route(CoordGrid(3744, 5641, 0), requiresDiary = false),
            )
    }
}
