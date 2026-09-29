package org.rsmod.content.bosses.callisto

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.content.areas.wilderness.checkWildernessBossFee
import org.rsmod.content.areas.wilderness.tryPayWildernessBossFee
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CallistoDen
@Inject
constructor(private val playerList: PlayerList, private val random: GameRandom) : PluginScript() {

    private data class Entrance(
        val loc: String,
        val arrival: CoordGrid,
        val exits: List<CoordGrid>,
        val den: BearDen,
    )

    private val entrances =
        listOf(
            Entrance(
                "loc.wild_callisto_singles_entrance01",
                CoordGrid(1758, 11552, 0),
                listOf(CoordGrid(3115, 3675, 0)),
                ARTIO_DEN,
            ),
            Entrance(
                "loc.wild_callisto_entrance01",
                CoordGrid(3360, 10336, 0),
                listOf(
                    CoordGrid(3360, 10295, 0),
                    CoordGrid(3341, 10255, 0),
                    CoordGrid(3376, 10255, 0),
                ),
                CALLISTO_DEN,
            ),
        )

    override fun ScriptContext.startup() {
        for (entrance in entrances) {
            onOpLoc1(entrance.loc) { enterDen(entrance) }
            onOpLoc2(entrance.loc) { peekDen(entrance) }
            onOpLoc3(entrance.loc) { checkFee() }
        }
        onOpLoc1(EXIT_LOC) { leaveDen() }
    }

    private suspend fun ProtectedAccess.enterDen(entrance: Entrance) {
        arriveDelay()
        tryPayWildernessBossFee(ENTRY_FEE) { source ->
            mes("<col=ef1020>You enter the cave and $FEE_TEXT is taken from $source to pay the entry fee.")
            anim(CRAWL_SEQ)
            delay(1)
            telejump(entrance.arrival, TeleportType.Exempt)
            anim(LAND_SEQ)
        }
    }

    private fun ProtectedAccess.peekDen(entrance: Entrance) {
        val occupied = playerList.any { entrance.den.contains(it.coords) }
        if (occupied) {
            mes(
                "You peek into the darkness and can make out some movement. " +
                    "There is activity inside."
            )
        } else {
            mes(
                "You peek into the darkness and can make out no movement. " +
                    "There is no activity inside."
            )
        }
    }

    private suspend fun ProtectedAccess.checkFee() {
        checkWildernessBossFee(ENTRY_FEE)
    }

    private suspend fun ProtectedAccess.leaveDen() {
        arriveDelay()
        val entrance = entrances.firstOrNull { it.den.contains(player.coords) } ?: return
        telejump(entrance.exits[random.of(entrance.exits.size)], TeleportType.Exempt)
    }

    private companion object {
        private const val EXIT_LOC = "loc.wild_callisto_exit01"
        private const val ENTRY_FEE = 50_000
        private const val FEE_TEXT = "50,000 coins"
        private const val CRAWL_SEQ = "seq.godwars_human_crawling"
        private const val LAND_SEQ = "seq.human_falling_end"
    }
}
