package dev.openrune.tools

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.DBRowType
import dev.openrune.filesystem.Cache
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.SlayerTaskKey

object SlayerBossKeys {
    private const val SUBLIST_TABLE = "slayer_task_sublist"
    private const val BOSS_COLUMN = "task_subtable_id"
    private const val ROW_ID_MASK = 0xFFFF

    fun load(cache: Cache, revision: Int): (String) -> Int {
        val table =
            GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache = cache, revision)
                .firstNotNullOf { it.elementAs<Table>()?.takeIf { it.name == SUBLIST_TABLE } }
        val column = table.columns.first { it.name == BOSS_COLUMN }.id
        val rows = mutableMapOf<Int, DBRowType>()
        OsrsCacheProvider.DBRowDecoder().load(cache, rows)
        return { symbol ->
            val row = rows.getValue(symbol.asRSCM() and ROW_ID_MASK)
            val bossId = row.definedColumns().getValue(column).values!!.first() as Int
            SlayerTaskKey.of(SlayerTaskKey.BOSS_TASK_ID, bossId)
        }
    }
}
