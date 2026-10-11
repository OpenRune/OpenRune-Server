package org.rsmod.api.player.events.interact

import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.SuspendEvent
import org.rsmod.game.inv.InvObj

/** Item-specific equipment submenu input, before the legacy component callback. */
public data class WornSubOpEvent(
    public val slot: Int,
    public val obj: InvObj,
    public val type: ItemServerType,
    public val op: IfButtonOp,
    public val subop: Int,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = type.id.toLong()
}
