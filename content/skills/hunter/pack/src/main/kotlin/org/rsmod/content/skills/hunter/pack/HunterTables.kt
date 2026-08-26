package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.util.VarType

/**
 * The hunter creature dbtables. Every `npc` and `obj` is a cache symbol confirmed against the
 * cache, never a wiki name transcribed directly. XP is stored x10 so fractional wiki values
 * survive an int column. See docs/hunter.md.
 */
object HunterTables {
    // Column ids must form a dense 0..n-1 set per table: the encoder writes columns sorted by id
    // without the id itself, so a gap silently shifts every later column and drops the last, with
    // no pack-time diagnostic (docs/hunter.md). Ids 0-7 are shared; per-technique columns start
    // at 8, nested per table so one table's column cannot be typed into another's builder.
    const val COL_NPC = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_SUCCESS_LOW = 3
    const val COL_SUCCESS_HIGH = 4
    const val COL_CAUGHT_ITEMS = 5
    const val COL_CAUGHT_MIN = 6
    const val COL_CAUGHT_MAX = 7

    /** Columns 0-7, shared verbatim by every creature table. */
    private fun DBTableBuilder.creatureColumns() {
        column("npc", COL_NPC, VarType.NPC)
        column("level", COL_LEVEL, VarType.INT)
        // Stored x10.
        column("xp", COL_XP, VarType.INT)
        column("success_low", COL_SUCCESS_LOW, VarType.INT)
        column("success_high", COL_SUCCESS_HIGH, VarType.INT)
        column("caught_items", COL_CAUGHT_ITEMS, VarType.OBJ)
        column("caught_min", COL_CAUGHT_MIN, VarType.INT)
        column("caught_max", COL_CAUGHT_MAX, VarType.INT)
    }
}
