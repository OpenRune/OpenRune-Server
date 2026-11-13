package org.rsmod.content.areas.misc.fossilunderwater.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM.asRSCM

/**
 * What a successful search of an open chest or clam adds to its pair of tears: one roll out of
 * [ROLL], each reward taking its weight of it and the rest giving nothing more. The weights are the
 * wiki's drop rates for the underwater chest divided by its 2/17 tear chance.
 */
object FossilUnderwaterRewardsTable {
    const val ROLL = 0
    const val REWARDS = 1

    private const val ROLL_SIZE = 1750

    fun rewards() =
        dbTable("dbtable.fossil_underwater_rewards", serverOnly = true) {
            column("roll", ROLL, VarType.INT)
            column("rewards", REWARDS, VarType.OBJ, VarType.INT, VarType.INT, VarType.INT)

            row("dbrow.fossil_underwater_rewards") {
                column(ROLL, ROLL_SIZE)
                column(
                    REWARDS,
                    listOf(
                        reward("obj.fossil_numulite", min = 5, max = 24, weight = 540),
                        reward("obj.fossil_small_unid", min = 1, max = 1, weight = 30),
                        reward("obj.fossil_medium_unid", min = 1, max = 1, weight = 15),
                        reward("obj.fossil_large_unid", min = 1, max = 1, weight = 12),
                        reward("obj.fossil_rare_unid", min = 1, max = 1, weight = 3),
                    ).flatten(),
                )
            }
        }

    private fun reward(obj: String, min: Int, max: Int, weight: Int): List<Any> =
        listOf(obj.asRSCM(), min, max, weight)
}
