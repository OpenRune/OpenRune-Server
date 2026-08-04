package org.rsmod.content.skills.mining.configs

import dev.openrune.types.ObjectServerType
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.game.stat.PlayerStatMap

val MiningRocksRow.miningXp: Double
    get() = xp / PlayerStatMap.XP_FINE_PRECISION.toDouble()

val MiningRocksRow.isInfinite: Boolean
    get() = depleteMechanic == 3

val MiningRocksRow.hasDepleteRange: Boolean
    get() = depleteMechanic == 2

val MiningRocksRow.isGemRock: Boolean
    get() = oreItem == null

val MiningRocksRow.depleteRange: IntRange
    get() {
        val min = depleteMinAmount ?: 1
        val max = depleteMaxAmount ?: min
        return if (max < min) min..min else min..max
    }

object MiningRocks {
    private val byLocId: Map<Int, MiningRocksRow> by lazy {
        MiningRocksRow.all().flatMap { row -> row.rockObject.map { loc -> loc.id to row } }.toMap()
    }

    fun forLoc(locId: Int): MiningRocksRow? = byLocId[locId]

    fun forLoc(type: ObjectServerType): MiningRocksRow? = forLoc(type.id)
}
