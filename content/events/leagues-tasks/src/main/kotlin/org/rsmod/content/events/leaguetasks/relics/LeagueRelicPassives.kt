package org.rsmod.content.events.leaguetasks.relics

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.table.LeagueRelicPassiveRow
import org.rsmod.game.entity.Player

/**
 * A tier's passives apply once `league_points_claimed` reaches its points, whether or not its relic
 * was picked.
 */
@Singleton
public class LeagueRelicPassives @Inject constructor(relics: LeagueRelics) {
    private val reachedByLeague: Map<Int, List<Reached>> =
        LeagueRelicPassiveRow.all().groupBy { it.league }.mapValues { (league, rows) ->
            val byTier = rows.associateBy { it.tier }
            var passives = RelicPassives.NONE
            relics.tiers(league).map { tier ->
                byTier[tier.index]?.let { passives = passives.raisedBy(it) }
                Reached(tier.points, passives)
            }
        }

    public fun of(player: Player): RelicPassives {
        val reached = reachedByLeague[player.vars["varbit.league_type"]] ?: return RelicPassives.NONE
        val points = player.vars["varp.league_points_claimed"]
        return reached.lastOrNull { points >= it.points }?.passives ?: RelicPassives.NONE
    }

    private data class Reached(val points: Int, val passives: RelicPassives)
}

/** 0 and false mean the passive is off; percents are of the normal rate. */
public data class RelicPassives(
    val xpRate: Int,
    val combatXpPercent: Int,
    val dropRatePercent: Int,
    val slayerPointMultiplier: Int,
    val superiorRatePercent: Int,
    val infiniteRun: Boolean,
    val freeBiggerAndBadder: Boolean,
) {
    internal fun raisedBy(row: LeagueRelicPassiveRow): RelicPassives =
        RelicPassives(
            xpRate = row.xpRate.orElse(xpRate),
            combatXpPercent = row.combatXpPercent.orElse(combatXpPercent),
            dropRatePercent = row.dropRatePercent.orElse(dropRatePercent),
            slayerPointMultiplier = row.slayerPointMultiplier.orElse(slayerPointMultiplier),
            superiorRatePercent = row.superiorRatePercent.orElse(superiorRatePercent),
            infiniteRun = infiniteRun || row.infiniteRun,
            freeBiggerAndBadder = freeBiggerAndBadder || row.freeBiggerAndBadder,
        )

    private fun Int.orElse(current: Int): Int = if (this != 0) this else current

    public companion object {
        public val NONE: RelicPassives = RelicPassives(0, 0, 0, 0, 0, false, false)
    }
}
