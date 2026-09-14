package org.rsmod.content.areas.city.draynor

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onGameStartup
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// Clear the blank walls beside the secret gates; centrepiece swaps alone leave their wall-edge collision.
class ErnestGateClearance @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        onGameStartup { clearGateWalls() }
    }

    private fun clearGateWalls() {
        val blankwall = BlankWall.asRSCM(RSCMType.LOC)
        val missing = mutableListOf<CoordGrid>()

        for (coords in GateWalls) {
            val loc = locRepo.findAll(coords).firstOrNull { it.id == blankwall }
            if (loc == null) {
                missing += coords
                continue
            }
            locRepo.del(loc, duration = Int.MAX_VALUE)
        }

        if (missing.isNotEmpty()) {
            logger.warn {
                "Ernest gates: no '$BlankWall' at ${missing.joinToString()}; " +
                    "those gates stay one-directional."
            }
        }
    }

    private companion object {
        private const val BlankWall = "loc.blankwall_no_blockrange"

        private val GateWalls =
            listOf(
                CoordGrid(3104, 9765, 0),
                CoordGrid(3099, 9765, 0),
                CoordGrid(3102, 9762, 0),
                CoordGrid(3097, 9762, 0),
                CoordGrid(3104, 9760, 0),
                CoordGrid(3108, 9757, 0),
                CoordGrid(3099, 9760, 0),
                CoordGrid(3102, 9757, 0),
                CoordGrid(3099, 9755, 0),
            )
    }
}
