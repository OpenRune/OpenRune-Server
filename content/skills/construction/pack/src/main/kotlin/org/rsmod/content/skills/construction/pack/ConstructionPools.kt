package org.rsmod.content.skills.construction.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

private class Pool(
    val key: String,
    val loc: String,
    val runEnergy: Boolean,
    val prayer: Boolean,
    val stats: Boolean,
    val hitpoints: Boolean,
    val cures: Boolean,
    val message: String,
)

/**
 * The superior garden's pools, each restoring everything the one below it does and a little more.
 * Special attack energy is restored by all of them, so it has no column.
 *
 * The loc names do not follow the names the game shows: `poh_pool_recovery` is the Fancy pool and
 * `poh_pool_regeneration` the Ornate one.
 */
object ConstructionPools {
    const val LOC = 0
    const val RUN_ENERGY = 1
    const val PRAYER = 2
    const val STATS = 3
    const val HITPOINTS = 4
    const val CURES = 5
    const val MESSAGE = 6

    private val POOLS =
        listOf(
            Pool(
                "restoration",
                "loc.poh_pool_restoration",
                runEnergy = false,
                prayer = false,
                stats = false,
                hitpoints = false,
                cures = false,
                message = "You feel your special attack energy return.",
            ),
            Pool(
                "revitalisation",
                "loc.poh_pool_revitalisation",
                runEnergy = true,
                prayer = false,
                stats = false,
                hitpoints = false,
                cures = false,
                message = "You feel your special attack energy and run energy return.",
            ),
            Pool(
                "rejuvenation",
                "loc.poh_pool_rejuvenation",
                runEnergy = true,
                prayer = true,
                stats = false,
                hitpoints = false,
                cures = false,
                message = "You feel your special attack energy, run energy and prayer return.",
            ),
            Pool(
                "fancy",
                "loc.poh_pool_recovery",
                runEnergy = true,
                prayer = true,
                stats = true,
                hitpoints = false,
                cures = false,
                message = "You feel refreshed, and your stats are restored.",
            ),
            Pool(
                "ornate",
                "loc.poh_pool_regeneration",
                runEnergy = true,
                prayer = true,
                stats = true,
                hitpoints = true,
                cures = true,
                message = "You feel like a brand new person.",
            ),
        )

    fun table() =
        dbTable("dbtable.construction_pool", serverOnly = true) {
            column("loc", LOC, VarType.LOC)
            column("run_energy", RUN_ENERGY, VarType.BOOLEAN)
            column("prayer", PRAYER, VarType.BOOLEAN)
            column("stats", STATS, VarType.BOOLEAN)
            column("hitpoints", HITPOINTS, VarType.BOOLEAN)
            column("cures", CURES, VarType.BOOLEAN)
            column("message", MESSAGE, VarType.STRING)

            for (pool in POOLS) {
                row("dbrow.construction_pool_${pool.key}") {
                    columnRSCM(LOC, pool.loc)
                    column(RUN_ENERGY, pool.runEnergy)
                    column(PRAYER, pool.prayer)
                    column(STATS, pool.stats)
                    column(HITPOINTS, pool.hitpoints)
                    column(CURES, pool.cures)
                    column(MESSAGE, pool.message)
                }
            }
        }
}
