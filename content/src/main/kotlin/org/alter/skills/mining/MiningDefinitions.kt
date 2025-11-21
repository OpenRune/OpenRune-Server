package org.alter.skills.mining

import org.alter.game.util.DbHelper
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.LocType
import org.alter.game.util.vars.ObjType
import org.alter.game.util.vars.SeqType
import org.alter.game.util.vars.StringType
import org.generated.tables.mining.MiningPickaxesRow
import org.generated.tables.mining.MiningRocksRow

/**
 * Definitions for mining rocks and pickaxes.
 * Contains all mappings and data structures used by the MiningPlugin.
 * Rock data is loaded from cache tables for easy modification.
 */
object MiningDefinitions {

    /**
     * Returns true if this rock uses a countdown timer before depletion.
     */
    fun MiningRocksRow.usesCountdown(): Boolean =
        depleteMechanic == 1 && despawnTicks > 0

    /**
     * Returns true if this rock never depletes (until the inventory is full).
     */
    fun MiningRocksRow.isInfiniteResource(): Boolean = depleteMechanic == 3

    val pickaxeData: List<MiningPickaxesRow> = MiningPickaxesRow.all()

    val miningRocks: List<MiningRocksRow> = MiningRocksRow.all()
}

