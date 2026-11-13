package org.rsmod.api.player.ui

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.aconverted.interf.IfSubType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.events.KeyedEvent
import org.rsmod.events.SuspendEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.ui.Component
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

public class IfMoveTop(override val player: Player, interf: InterfaceType) :
    KeyedEvent, PlayerEvent {
    override val id: Long = interf.id.toLong()
}

public data class IfOpenSub(
    override val player: Player,
    val interf: UserInterface,
    val target: Component,
    val subType: IfSubType,
) : KeyedEvent, PlayerEvent {
    override val id: Long = interf.id.toLong()
}

public data class IfCloseSub(
    override val player: Player,
    val interf: UserInterface,
    val from: Component,
) : KeyedEvent, PlayerEvent {
    override val id: Long = interf.id.toLong()
}

public class IfMoveSub(override val player: Player, destComponent: Int) : KeyedEvent, PlayerEvent {
    override val id: Long = destComponent.toLong()
}

/**
 * Any modal or overlay opening or closing, for observers. [IfOpenSub] and [IfCloseSub] allow one
 * subscriber per interface, so content that owns an interface keeps those. [Closed] also fires when
 * another interface replaces this one.
 */
public class PlayerInterfaceEvents {
    public data class Opened(
        override val player: Player,
        val interf: UserInterface,
        val target: Component,
        val subType: IfSubType,
    ) : UnboundEvent, PlayerEvent

    public data class Closed(
        override val player: Player,
        val interf: UserInterface,
        val from: Component,
    ) : UnboundEvent, PlayerEvent
}

public data class IfModalButton(
    val component: ComponentType,
    val comsub: Int,
    val obj: ItemServerType?,
    val op: IfButtonOp,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = component.packed.toLong()
}

public data class IfOverlayScriptTrigger(
    val component: ComponentType,
    val comsub: Int,
    val obj: ItemServerType?,
    val crc: Int,
    val args: List<Any>,
) : SuspendEvent<ProtectedAccess> {
    override val id: Long = component.packed.toLong()
}

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
    override val player: Player,
    public val selectedSlot: Int,
    public val selectedObj: ItemServerType?,
    public val targetSlot: Int,
    public val targetObj: ItemServerType?,
    selectedComponent: Component,
    targetComponent: Component,
) : KeyedEvent, PlayerEvent {
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
     * Stable event-bus key for [org.rsmod.api.script.onWorldMapClick]. Scripts gate by
     * [dev.or2.central.account.Rights]; this must not vary per player or the subscription key
     * would not match the published event.
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
    override val player: Player,
    public val selectedSlot: Int?,
    public val selectedObj: Int?,
    public val targetSlot: Int?,
    public val targetObj: Int?,
    selectedComponent: Component,
    targetComponent: Component,
) : KeyedEvent, PlayerEvent {
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
