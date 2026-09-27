package org.rsmod.content.bosses.gemstonecrab

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GemstoneCrabScript
@Inject
constructor(
    private val crab: GemstoneCrabManager,
    private val mining: GemstoneCrabMiningScript,
    private val cave: GemstoneCrabCaveScript,
    private val playerList: PlayerList,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { crab.tick() }

        val liveType =
            ServerCacheManager.getNpc("npc.gemstone_crab".asRSCM(RSCMType.NPC))
                ?: error("Missing npc.gemstone_crab")
        onNpcHit(liveType) {
            if (!hit.isFromPlayer) return@onNpcHit
            val source = hit.resolvePlayerSource(playerList) ?: return@onNpcHit
            crab.openBarFor(source, npc)
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
}
