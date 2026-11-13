package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.mechanics.toxins.Toxin.cureAllToxins
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statRestoreAll
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.table.construction.ConstructionPoolRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The superior garden's pools, out of `dbtable.construction_pool`. */
class PohPools @Inject constructor() : PluginScript() {
    private var Player.specialEnergy by intVarp(VARP_SPECIAL_ENERGY)

    override fun ScriptContext.startup() {
        for (pool in ConstructionPoolRow.all()) {
            onOpLoc1(pool.loc) { drink(pool) }
        }
    }

    private fun ProtectedAccess.drink(pool: ConstructionPoolRow) {
        player.specialEnergy = MAX_SPECIAL_ENERGY
        if (pool.runEnergy) {
            player.runEnergy = MAX_RUN_ENERGY
        }
        if (pool.prayer) {
            statRestore(STAT_PRAYER)
        }
        if (pool.stats) {
            player.statRestoreAll(restorableStats())
        }
        if (pool.hitpoints) {
            statRestore(STAT_HITPOINTS)
        }
        if (pool.cures) {
            player.cureAllToxins()
        }
        spam(pool.message)
    }

    /** Every stat but Hitpoints, which only the Ornate pool brings back. */
    private fun restorableStats(): List<String> =
        ServerCacheManager.getStats().values
            .map { RSCM.getReverseMapping(RSCMType.STAT, it.id) }
            .filter { it != STAT_HITPOINTS }

    private companion object {
        const val VARP_SPECIAL_ENERGY = "varp.sa_energy"
        const val STAT_PRAYER = "stat.prayer"
        const val STAT_HITPOINTS = "stat.hitpoints"
        const val MAX_SPECIAL_ENERGY = 1000
        const val MAX_RUN_ENERGY = 10000
    }
}
