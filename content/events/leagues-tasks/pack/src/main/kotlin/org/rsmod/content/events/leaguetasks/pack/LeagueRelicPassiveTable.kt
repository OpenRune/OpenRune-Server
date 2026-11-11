package org.rsmod.content.events.leaguetasks.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * A row only sets what changes at its tier; 0 and false mean "keep the lower tier's value".
 * Passives with no game system behind them yet (farming ticks, minigame points, clue scroll boxes)
 * have no column.
 */
object LeagueRelicPassiveTable {
    const val LEAGUE = 0
    const val TIER = 1
    const val XP_RATE = 2
    const val COMBAT_XP_PERCENT = 3
    const val DROP_RATE_PERCENT = 4
    const val SLAYER_POINT_MULTIPLIER = 5
    const val SUPERIOR_RATE_PERCENT = 6
    const val INFINITE_RUN = 7
    const val FREE_BIGGER_AND_BADDER = 8

    private const val DEMONIC_PACTS = 6

    fun passives() =
        dbTable("dbtable.league_relic_passive", serverOnly = true) {
            column("league", LEAGUE, VarType.INT)
            column("tier", TIER, VarType.INT)
            column("xp_rate", XP_RATE, VarType.INT)
            column("combat_xp_percent", COMBAT_XP_PERCENT, VarType.INT)
            column("drop_rate_percent", DROP_RATE_PERCENT, VarType.INT)
            column("slayer_point_multiplier", SLAYER_POINT_MULTIPLIER, VarType.INT)
            column("superior_rate_percent", SUPERIOR_RATE_PERCENT, VarType.INT)
            column("infinite_run", INFINITE_RUN, VarType.BOOLEAN)
            column("free_bigger_and_badder", FREE_BIGGER_AND_BADDER, VarType.BOOLEAN)

            val rows = PassiveRows(this)
            rows.tier(DEMONIC_PACTS, 0, xpRate = 5, dropRatePercent = 200, infiniteRun = true)
            rows.tier(DEMONIC_PACTS, 1, xpRate = 8)
            rows.tier(
                DEMONIC_PACTS,
                2,
                combatXpPercent = 150,
                slayerPointMultiplier = 5,
                superiorRatePercent = SUPERIOR_1_IN_50,
                freeBiggerAndBadder = true,
            )
            rows.tier(DEMONIC_PACTS, 3, xpRate = 12, dropRatePercent = 500)
            rows.tier(DEMONIC_PACTS, 5, xpRate = 16)
        }

    /** The slayer module's base superior chance is 1/200, so 400% makes it 1/50. */
    private const val SUPERIOR_1_IN_50 = 400

    private class PassiveRows(private val table: DBTableBuilder) {
        private var next = FIRST_ROW

        fun tier(
            league: Int,
            tier: Int,
            xpRate: Int = 0,
            combatXpPercent: Int = 0,
            dropRatePercent: Int = 0,
            slayerPointMultiplier: Int = 0,
            superiorRatePercent: Int = 0,
            infiniteRun: Boolean = false,
            freeBiggerAndBadder: Boolean = false,
        ) {
            check(next <= LAST_ROW) { "dbtable.league_relic_passive ran out of row ids at $LAST_ROW." }
            table.row(next++) {
                column(LEAGUE, league)
                column(TIER, tier)
                column(XP_RATE, xpRate)
                column(COMBAT_XP_PERCENT, combatXpPercent)
                column(DROP_RATE_PERCENT, dropRatePercent)
                column(SLAYER_POINT_MULTIPLIER, slayerPointMultiplier)
                column(SUPERIOR_RATE_PERCENT, superiorRatePercent)
                column(INFINITE_RUN, infiniteRun)
                column(FREE_BIGGER_AND_BADDER, freeBiggerAndBadder)
            }
        }

        private companion object {
            const val FIRST_ROW = 43100
            const val LAST_ROW = 43199
        }
    }
}
