package org.rsmod.content.bosses.whisperer

import jakarta.inject.Inject
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Temporary entrance. The real fight is entered by running across z=6384, which the instance
 * system can't express yet, so a statue in the lobby stands in for it.
 */
class WhispererEntrance @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        locRepo.add(
            CoordGrid(2656, 6390, 0),
            ENTRANCE_LOC,
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
    }

    private companion object {
        private const val ENTRANCE_LOC = "loc.dt2_vault_whisperer_statue_normal"
    }
}
