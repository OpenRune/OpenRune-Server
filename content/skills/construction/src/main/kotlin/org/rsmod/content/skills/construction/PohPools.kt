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
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The superior garden's pools. Each tier restores everything the one below it does and a little
 * more, so a [Pool] lists only what it adds.
 *
 * The loc names do not follow the names the game shows: `poh_pool_recovery` is the Fancy pool and
 * `poh_pool_regeneration` the Ornate one. The ids below are what decides which is which.
 */
class PohPools @Inject constructor() : PluginScript() {
    private var Player.specialEnergy by intVarp(VARP_SPECIAL_ENERGY)

    override fun ScriptContext.startup() {
        for (pool in Pool.entries) {
            val type = ServerCacheManager.getObject(pool.locId ?: continue) ?: continue
            onOpLoc1(type) { drink(pool) }
        }
    }

    private fun ProtectedAccess.drink(pool: Pool) {
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

    private enum class Pool(
        val loc: String,
        val runEnergy: Boolean = false,
        val prayer: Boolean = false,
        val stats: Boolean = false,
        val hitpoints: Boolean = false,
        val cures: Boolean = false,
        val message: String,
    ) {
        Restoration(
            loc = "loc.poh_pool_restoration",
            message = "You feel your special attack energy return.",
        ),
        Revitalisation(
            loc = "loc.poh_pool_revitalisation",
            runEnergy = true,
            message = "You feel your special attack energy and run energy return.",
        ),
        Rejuvenation(
            loc = "loc.poh_pool_rejuvenation",
            runEnergy = true,
            prayer = true,
            message = "You feel your special attack energy, run energy and prayer return.",
        ),
        Fancy(
            loc = "loc.poh_pool_recovery",
            runEnergy = true,
            prayer = true,
            stats = true,
            message = "You feel refreshed, and your stats are restored.",
        ),
        Ornate(
            loc = "loc.poh_pool_regeneration",
            runEnergy = true,
            prayer = true,
            stats = true,
            hitpoints = true,
            cures = true,
            message = "You feel like a brand new person.",
        );

        val locId: Int?
            get() {
                val id = runCatching { RSCM.getRSCM(loc) }.getOrDefault(-1)
                return id.takeIf { it > 0 }
            }
    }

    private companion object {
        const val VARP_SPECIAL_ENERGY = "varp.sa_energy"
        const val STAT_PRAYER = "stat.prayer"
        const val STAT_HITPOINTS = "stat.hitpoints"
        const val MAX_SPECIAL_ENERGY = 1000
        const val MAX_RUN_ENERGY = 10000
    }
}
