package org.rsmod.content.skills.thieving

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.api.table.thieving.ThievingStallRow

internal data class Loot(val obj: String, val min: Int = 1, val max: Int = min)

internal class LootTable(private val entries: List<Pair<Int, Loot>>) {
    private val total = entries.sumOf { it.first }

    fun roll(random: GameRandom): Loot {
        var pick = random.of(0, total - 1)
        for ((weight, loot) in entries) {
            if (pick < weight) return loot
            pick -= weight
        }
        return entries.last().second
    }
}

internal class Stall(
    val loc: String,
    val emptyLoc: String?,
    val level: Int,
    val xp: Double,
    val restockTicks: Int,
    val owners: List<String>,
    val guards: List<String>,
    val attemptMessage: String?,
    val loot: LootTable,
)

internal class CoinPouch(val obj: String, val min: Int, val max: Int)

internal class Pickpocket(
    val name: String,
    val symbolPrefixes: List<String>,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val stunTicks: Int,
    val stunDamage: Int,
    val caughtShout: String,
    val guaranteed: List<Loot>,
    val loot: LootTable?,
    val pouch: CoinPouch?,
    val lowercaseName: Boolean,
)

internal object ThievingTables {
    val stalls: List<Stall> by lazy { ThievingStallRow.all().map(::stall) }

    val pickpockets: List<Pickpocket> by lazy { ThievingPickpocketRow.all().map(::pickpocket) }

    private fun stall(row: ThievingStallRow): Stall =
        Stall(
            loc = locSymbol(row.loc.id),
            emptyLoc = row.empty?.let { locSymbol(it.id) },
            level = row.level,
            xp = row.xp / 10.0,
            restockTicks = row.respawn,
            owners = row.owners.map { npcSymbol(it.id) },
            guards = row.guards.map { npcSymbol(it.id) },
            attemptMessage = row.attemptMessage,
            loot = LootTable(row.loot.map { it.t3 to Loot(objSymbol(it.t0.id), it.t1, it.t2) }),
        )

    private fun pickpocket(row: ThievingPickpocketRow): Pickpocket {
        val pouchObj = row.pouch?.let { objSymbol(it.id) }
        val guaranteed = row.guaranteed.map { Loot(objSymbol(it.t0.id), it.t1, it.t2) }
        val weighted = row.loot.map { it.t3 to Loot(objSymbol(it.t0.id), it.t1, it.t2) }
        val coins = (guaranteed + weighted.map { it.second }).firstOrNull { it.obj == COINS }
        val pouch =
            if (pouchObj != null && coins != null) CoinPouch(pouchObj, coins.min, coins.max) else null
        fun pouched(loot: Loot): Loot =
            if (pouch != null && loot.obj == COINS) Loot(pouch.obj) else loot
        return Pickpocket(
            name = row.name,
            symbolPrefixes = listOfNotNull(row.symbolPrefixes),
            level = row.level,
            xp = row.xp / 10.0,
            lowChance = row.low,
            highChance = row.high,
            stunTicks = row.stunTicks,
            stunDamage = row.stunDamage,
            caughtShout = row.caughtShout,
            guaranteed = guaranteed.map(::pouched),
            loot =
                weighted
                    .takeIf { it.isNotEmpty() }
                    ?.let { entries -> LootTable(entries.map { it.first to pouched(it.second) }) },
            pouch = pouch,
            lowercaseName = row.lowercaseName,
        )
    }

    private fun objSymbol(id: Int): String = RSCM.getReverseMapping(RSCMType.OBJ, id)

    private fun locSymbol(id: Int): String = RSCM.getReverseMapping(RSCMType.LOC, id)

    private fun npcSymbol(id: Int): String = RSCM.getReverseMapping(RSCMType.NPC, id)

    private const val COINS = "obj.coins"
}
