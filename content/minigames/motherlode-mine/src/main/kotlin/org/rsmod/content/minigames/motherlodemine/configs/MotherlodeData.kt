package org.rsmod.content.minigames.motherlodemine.configs

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.enum
import dev.openrune.types.dbcol.DbColumnCodec.BooleanCodec
import dev.openrune.types.dbcol.DbColumnCodec.CoordGridCodec
import dev.openrune.types.dbcol.DbColumnCodec.IntCodec
import dev.openrune.types.dbcol.DbColumnCodec.ItemServerTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.LocTypeCodec
import dev.openrune.types.dbcol.DbHelper
import dev.openrune.types.dbcol.column
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.map.CoordGrid

internal object MotherlodeData {
    val depletedVeins: Map<String, String> by lazy {
        enum("enum.motherlode_depleted_veins", LocTypeCodec, LocTypeCodec).associate {
            RSCM.getReverseMapping(RSCMType.LOC, it.key.id) to
                RSCM.getReverseMapping(RSCMType.LOC, it.value.id)
        }
    }

    val upperFloorTiles: Set<CoordGrid> by lazy {
        enum("enum.motherlode_upper_floor_tiles", CoordGridCodec, BooleanCodec)
            .filter { it.value }.map { it.key }.toSet()
    }

    val ores: List<Ore> by lazy {
        DbHelper.table("dbtable.motherlode_ores").map { row ->
            Ore(
                item = row.column("dbcol.motherlode_ores:ore", ItemServerTypeCodec),
                req = row.column("dbcol.motherlode_ores:level", IntCodec),
                low = row.column("dbcol.motherlode_ores:success_low", IntCodec),
                high = row.column("dbcol.motherlode_ores:success_high", IntCodec),
                xp = row.column("dbcol.motherlode_ores:xp", IntCodec) / PlayerStatMap.XP_FINE_PRECISION.toDouble(),
                rollOrder = row.column("dbcol.motherlode_ores:roll_order", IntCodec),
                sackOrder = row.column("dbcol.motherlode_ores:sack_order", IntCodec),
            )
        }.also { rows ->
            require(rows.isNotEmpty()) { "Motherlode ore definitions are missing; rebuild the server cache." }
            require(rows.map { it.item.id }.distinct().size == rows.size) { "Duplicate Motherlode ore." }
        }
    }

    data class Ore(
        val item: ItemServerType,
        val req: Int,
        val low: Int,
        val high: Int,
        val xp: Double,
        val rollOrder: Int,
        val sackOrder: Int,
    ) {
        val obj: String get() = RSCM.getReverseMapping(RSCMType.OBJ, item.id)
    }
}
