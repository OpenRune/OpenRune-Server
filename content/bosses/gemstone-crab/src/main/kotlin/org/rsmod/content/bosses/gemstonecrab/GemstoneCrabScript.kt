package org.rsmod.content.bosses.gemstonecrab

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GemstoneCrabScript
@Inject
constructor(
    private val crab: GemstoneCrabManager,
    private val mining: GemstoneCrabMiningScript,
    private val cave: GemstoneCrabCaveScript,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { crab.tick() }

        SPOT_AREAS.forEachIndexed { index, area ->
            onArea(area) { crab.enterSpotArea(player, index) }
            onAreaExit(area) { crab.exitSpotArea(player, index) }
        }

        onOpNpc1("npc.gemstone_crab_remains") { with(mining) { attempt(it.npc) } }
        onOpLoc1("loc.cave_rock02_entrance01_gemstone") { with(cave) { crawl(it.loc) } }

        onCommand("gemstonecrab") {
            desc = "Force the active gemstone crab to burrow immediately"
            cheat { crab.forceBurrow() }
        }

        crab.start()
    }

    private fun Cheat.forceBurrow() {
        crab.forceBurrow()
        player.mes("Forced the gemstone crab to burrow.")
    }

    private companion object {
        private val SPOT_AREAS =
            listOf(
                "area.gemstone_crab_spot_1",
                "area.gemstone_crab_spot_2",
                "area.gemstone_crab_spot_3",
            )
    }
}
