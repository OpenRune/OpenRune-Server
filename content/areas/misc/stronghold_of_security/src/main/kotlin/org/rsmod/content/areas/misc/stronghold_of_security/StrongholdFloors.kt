package org.rsmod.content.areas.misc.stronghold_of_security

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.StrongholdFloorsRow
import org.rsmod.game.entity.Player

internal object StrongholdFloors {
    val all: List<StrongholdFloorsRow>
        get() = StrongholdFloorsRow.all()

    val war: StrongholdFloorsRow
        get() = StrongholdFloorsRow.getRow("dbrow.stronghold_floor_war")

    val famine: StrongholdFloorsRow
        get() = StrongholdFloorsRow.getRow("dbrow.stronghold_floor_famine")

    val pestilence: StrongholdFloorsRow
        get() = StrongholdFloorsRow.getRow("dbrow.stronghold_floor_pestilence")

    val death: StrongholdFloorsRow
        get() = StrongholdFloorsRow.getRow("dbrow.stronghold_floor_death")
}

internal val StrongholdFloorsRow.doorNpcName: String
    get() = RSCM.getReverseMapping(RSCMType.NPC, doorNpc.id)

internal fun StrongholdFloorsRow.isSameFloor(other: StrongholdFloorsRow): Boolean =
    rowId == other.rowId

private fun varbit(id: Int): VarBitType =
    checkNotNull(ServerCacheManager.getVarbit(id)) { "Missing stronghold varbit: $id" }

internal fun Player.hasClaimed(floor: StrongholdFloorsRow): Boolean =
    vars[varbit(floor.claimedVarbit)] != 0

internal fun Player.markClaimed(floor: StrongholdFloorsRow) {
    VarPlayerIntMapSetter.set(this, varbit(floor.claimedVarbit), 1)
}

internal fun Player.unlockEmote(floor: StrongholdFloorsRow) {
    VarPlayerIntMapSetter.set(this, varbit(floor.emoteVarbit), 1)
}

internal fun Player.completedStronghold(): Boolean = StrongholdFloors.all.all { hasClaimed(it) }

internal fun Player.doorsStayQuiet(floor: StrongholdFloorsRow): Boolean =
    hasClaimed(floor) || completedStronghold()
