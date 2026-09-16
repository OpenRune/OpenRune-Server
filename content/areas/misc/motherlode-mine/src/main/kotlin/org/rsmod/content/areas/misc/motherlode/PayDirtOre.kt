package org.rsmod.content.areas.misc.motherlode

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.MotherlodePaydirtRow
import org.rsmod.api.utils.skills.SkillingSuccessRate

/**
 * Ore tiers in roll order. Level, experience, obj and success rates all come from
 * `dbtable.motherlode_paydirt`; only the player state varbits are declared here.
 */
internal enum class PayDirtOre(
    private val dbRow: String,
    val sackVarbit: String,
    val heldVarbit: String,
    val cleaningVarbit: String,
) {
    Nugget(
        "dbrow.motherlode_paydirt_nugget",
        "varbit.motherlode_sack_nugget",
        "varbit.motherlode_held_nugget",
        "varbit.motherlode_cleaning_nugget",
    ),
    Runite(
        "dbrow.motherlode_paydirt_runite",
        "varbit.motherlode_sack_runite",
        "varbit.motherlode_held_runite",
        "varbit.motherlode_cleaning_runite",
    ),
    Adamantite(
        "dbrow.motherlode_paydirt_adamantite",
        "varbit.motherlode_sack_adamantite",
        "varbit.motherlode_held_adamantite",
        "varbit.motherlode_cleaning_adamantite",
    ),
    Mithril(
        "dbrow.motherlode_paydirt_mithril",
        "varbit.motherlode_sack_mithril",
        "varbit.motherlode_held_mithril",
        "varbit.motherlode_cleaning_mithril",
    ),
    Gold(
        "dbrow.motherlode_paydirt_gold",
        "varbit.motherlode_sack_gold",
        "varbit.motherlode_held_gold",
        "varbit.motherlode_cleaning_gold",
    ),
    Coal(
        "dbrow.motherlode_paydirt_coal",
        "varbit.motherlode_sack_coal",
        "varbit.motherlode_held_coal",
        "varbit.motherlode_cleaning_coal",
    );

    private val row: MotherlodePaydirtRow by lazy { MotherlodePaydirtRow.getRow(dbRow) }

    val obj: String
        get() = RSCM.getReverseMapping(RSCMType.OBJ, row.oreItem.id)

    val level: Int
        get() = row.level

    val xp: Double
        get() = row.xp / XP_MULTIPLIER

    companion object {
        private const val MAX_LEVEL = 99
        private const val XP_MULTIPLIER = 10.0

        /** Rolls each tier from the top down; [Coal] is the fallback when every roll fails. */
        fun roll(miningLevel: Int, random: GameRandom): PayDirtOre {
            val level = miningLevel.coerceIn(1, MAX_LEVEL)
            for (ore in entries) {
                if (ore == Coal) {
                    break
                }
                if (level < ore.level) {
                    continue
                }
                val rate =
                    SkillingSuccessRate.successRate(
                        ore.row.successRateLow,
                        ore.row.successRateHigh,
                        level,
                        MAX_LEVEL,
                    )
                if (rate > random.randomDouble()) {
                    return ore
                }
            }
            return Coal
        }
    }
}
