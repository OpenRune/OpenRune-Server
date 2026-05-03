package org.rsmod.api.player.vars

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpServerType
import dev.openrune.types.varp.baseVar
import org.rsmod.api.player.output.VarpSync
import org.rsmod.game.entity.Player
import org.rsmod.game.movement.MoveSpeed

internal var Player.enabledPrayers by intVarBit("varbit.prayer_allactive")
internal var Player.usingQuickPrayers by boolVarBit("varbit.quickprayer_active")
internal var Player.prayerDrainCounter by intVarBit("varbit.prayer_drain_counter")

private var Player.varSpeed: MoveSpeed by typeIntVarp("varp.option_run", ::getSpeed, ::getSpeedId)

public var Player.varMoveSpeed: MoveSpeed
    get() = varSpeed
    set(value) {
        varSpeed = value
        // Assign as `varSpeed` as it may not have
        // changed due to protected access.
        cachedMoveSpeed = varSpeed
    }

public fun Player.resyncVar(internal: String) {
    val varp = ServerCacheManager.getVarp(internal.asRSCM(RSCMType.VARP))
        ?: error("Varp '$internal' not found")

    resyncVar(varp)
}

public fun Player.resyncVar(varp: VarpServerType) {
    if (varp.transmit.never) {
        return
    }
    VarpSync.writeVarp(client, varp, vars[varp])
}

public fun Player.resyncVar(varBit: VarBitType): Unit = resyncVar(varBit.baseVar)

public fun Player.setActiveMoveSpeed(speed: MoveSpeed) {
    varMoveSpeed = speed
    moveSpeed = varMoveSpeed
    tempMoveSpeed = varMoveSpeed
}

private fun getSpeed(id: Int): MoveSpeed =
    when (id) {
        2 -> MoveSpeed.Crawl
        1 -> MoveSpeed.Run
        else -> MoveSpeed.Walk
    }

private fun getSpeedId(speed: MoveSpeed): Int =
    when (speed) {
        MoveSpeed.Crawl -> 2
        MoveSpeed.Run -> 1
        else -> 0
    }

// TODO: invert run mode setting to disable this
public fun Player.ctrlMoveSpeed(): MoveSpeed =
    if (varMoveSpeed == MoveSpeed.Run || runEnergy < 100) {
        MoveSpeed.Walk
    } else {
        MoveSpeed.Run
    }
