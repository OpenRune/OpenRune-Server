package org.rsmod.content.skills.thieving

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.api.table.thieving.ThievingStallRow

internal const val STAT_THIEVING: String = "stat.thieving"
internal const val ANIM_PICKPOCKET: String = "seq.human_pickpocket"
internal const val ANIM_STEAL_STALL: String = "seq.human_pickuptable"
internal const val ANIM_STUNNED: String = "seq.stunned_thieving"
internal const val SPOTANIM_STUNNED: String = "spotanim.stunned_thieving"

data class Loot(val obj: String, val amount: IntRange = 1..1, val weight: Int = 1)

data class PickpocketTarget(
    val displayName: String,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val stunTicks: Int,
    val stunDamage: Int,
    val guaranteed: List<Loot> = emptyList(),
    val loot: List<Loot> = emptyList(),
    val symbolPrefixes: List<String> = emptyList(),
)

data class StallTarget(
    val loc: String,
    val level: Int,
    val xp: Double,
    val loot: List<Loot>,
    val empty: String? = null,
    val respawn: Int = 20,
)

fun List<Loot>.roll(random: GameRandom): Pair<String, Int> {
    val total = sumOf(Loot::weight)
    var roll = random.of(1, total)
    for (entry in this) {
        roll -= entry.weight
        if (roll <= 0) {
            return entry.obj to random.of(entry.amount.first, entry.amount.last)
        }
    }
    val fallback = last()
    return fallback.obj to random.of(fallback.amount.first, fallback.amount.last)
}

private fun objSymbol(id: Int): String = RSCM.getReverseMapping(RSCMType.OBJ, id)

private fun locSymbol(id: Int): String = RSCM.getReverseMapping(RSCMType.LOC, id)

private fun ThievingPickpocketRow.toTarget(): PickpocketTarget =
    PickpocketTarget(
        displayName = name,
        level = level,
        xp = xp / 10.0,
        low = low,
        high = high,
        stunTicks = stunTicks,
        stunDamage = stunDamage,
        guaranteed = guaranteed.map { Loot(objSymbol(it.t0.id), it.t1..it.t2) },
        loot = loot.map { Loot(objSymbol(it.t0.id), it.t1..it.t2, it.t3) },
        symbolPrefixes = listOfNotNull(symbolPrefixes),
    )

private fun ThievingStallRow.toTarget(): StallTarget =
    StallTarget(
        loc = locSymbol(loc.id),
        level = level,
        xp = xp / 10.0,
        loot = loot.map { Loot(objSymbol(it.t0.id), it.t1..it.t2, it.t3) },
        empty = empty?.let { locSymbol(it.id) },
        respawn = respawn,
    )

object ThievingData {
    val pickpocketTargets: List<PickpocketTarget> by lazy {
        ThievingPickpocketRow.all().map(ThievingPickpocketRow::toTarget)
    }

    val stalls: List<StallTarget> by lazy { ThievingStallRow.all().map(ThievingStallRow::toTarget) }
}
