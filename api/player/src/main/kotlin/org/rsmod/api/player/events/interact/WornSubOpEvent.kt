package org.rsmod.api.player.events.interact

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.SuspendEvent
import org.rsmod.game.inv.InvObj

/** [op] is the worn op number as used by `onOpWornN`; [subop] is the sub-menu index from 0. */
public data class WornSubOpEvent(
    val slot: Int,
    val obj: InvObj,
    val type: ItemServerType,
    val op: Int,
    val subop: Int,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = type.id.toLong()
}
