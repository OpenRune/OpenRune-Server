package org.rsmod.api.player.ui

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.aconverted.interf.IfSubType
import dev.openrune.util.Coord
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.events.KeyedEvent
import org.rsmod.events.SuspendEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

public class IfMoveTop(public val player: Player, interf: InterfaceType) : KeyedEvent {
    override val id: Long = interf.id.toLong()
}

public data class IfOpenSub(
    val player: Player,
    val interf: UserInterface,
    val target: Component,
    val subType: IfSubType,
) : KeyedEvent {
    override val id: Long = interf.id.toLong()
}

public data class IfCloseSub(val player: Player, val interf: UserInterface, val from: Component) :
    KeyedEvent {
    override val id: Long = interf.id.toLong()
}

public class IfMoveSub(public val player: Player, destComponent: Int) : KeyedEvent {
    override val id: Long = destComponent.toLong()
}

public data class IfModalButton(
    val component: ComponentType,
    val comsub: Int,
    val obj: ItemServerType?,
    val op: IfButtonOp,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = component.packed.toLong()
}

/**
 * Overlay interface button press, delivered on [ProtectedAccess] like [IfModalButton].
 *
 * [player] is available as [ProtectedAccess.player] on the script receiver.
 */
public data class IfOverlayButton(
    val component: ComponentType,
    val comsub: Int,
    val obj: ItemServerType?,
    val op: IfButtonOp,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = component.packed.toLong()
}

public data class IfModalSubOpMenu(
    val component: ComponentType,
    val comsub: Int,
    val obj: ItemServerType?,
    val op: IfButtonOp,
    val subop: Int,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = component.packed.toLong()
}

public data class IfOverlaySubOpMenu(
    val component: ComponentType,
    val comsub: Int,
    val obj: ItemServerType?,
    val op: IfButtonOp,
    val subop: Int,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = component.packed.toLong()
}

public class IfModalButtonT(
    public val selectedSlot: Int,
    public val selectedObj: ItemServerType?,
    public val targetSlot: Int,
    public val targetObj: ItemServerType?,
    selectedComponent: Component,
    targetComponent: Component,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long =
        EventBus.composeLongKey(selectedComponent.packed, targetComponent.packed)

    override fun toString(): String =
        "IfModalButtonT(" +
            "selectedSlot=$selectedSlot, " +
            "targetSlot=$targetSlot, " +
            "selectedObj=$selectedObj, " +
            "targetObj=$targetObj" +
            ")"
}

public class IfOverlayButtonT(
    public val player: Player,
    public val selectedSlot: Int,
    public val selectedObj: ItemServerType?,
    public val targetSlot: Int,
    public val targetObj: ItemServerType?,
    selectedComponent: Component,
    targetComponent: Component,
) : KeyedEvent {
    override val id: Long =
        EventBus.composeLongKey(selectedComponent.packed, targetComponent.packed)

    override fun toString(): String =
        "IfOverlayButtonT(" +
            "selectedSlot=$selectedSlot, " +
            "targetSlot=$targetSlot, " +
            "selectedObj=$selectedObj, " +
            "targetObj=$targetObj, " +
            "player=$player" +
            ")"
}

public class WorldMapClick(
    public val player: Player,
    public val coord: CoordGrid
) : SuspendEvent<ProtectedAccess> {
    /**
     * Stable event-bus key for [org.rsmod.api.script.onWorldMapClick]. Scripts gate by mod level
     * with [dev.openrune.types.ModLevelType.hasAccessTo]; this must not vary per player or the
     * subscription key would not match the published event.
     */
    override val id: Long = BUS_ID

    public companion object {
        public const val BUS_ID: Long = 0L
    }
}

public class IfModalDrag(
    public val selectedSlot: Int?,
    public val selectedObj: Int?,
    public val targetSlot: Int?,
    public val targetObj: Int?,
    selectedComponent: Component,
    targetComponent: Component,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long =
        EventBus.composeLongKey(selectedComponent.packed, targetComponent.packed)

    override fun toString(): String =
        "IfModalDrag(" +
            "selectedSlot=$selectedSlot, " +
            "targetSlot=$targetSlot, " +
            "selectedObj=$selectedObj, " +
            "targetObj=$targetObj" +
            ")"
}

public class IfOverlayDrag(
    public val player: Player,
    public val selectedSlot: Int?,
    public val selectedObj: Int?,
    public val targetSlot: Int?,
    public val targetObj: Int?,
    selectedComponent: Component,
    targetComponent: Component,
) : KeyedEvent {
    override val id: Long =
        EventBus.composeLongKey(selectedComponent.packed, targetComponent.packed)

    override fun toString(): String =
        "IfOverlayDrag(" +
            "selectedSlot=$selectedSlot, " +
            "targetSlot=$targetSlot, " +
            "selectedObj=$selectedObj, " +
            "targetObj=$targetObj, " +
            "player=$player" +
            ")"
}
