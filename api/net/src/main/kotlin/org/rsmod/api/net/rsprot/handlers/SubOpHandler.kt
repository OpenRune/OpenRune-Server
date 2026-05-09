package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.IfSubOp
import org.rsmod.annotations.InternalApi
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.events.interact.HeldSubOpEvent
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalSubOpMenu
import org.rsmod.api.player.ui.IfOverlaySubOpMenu
import org.rsmod.api.player.ui.ifCloseInputDialog
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.hasInvOp
import org.rsmod.game.ui.Component

@OptIn(InternalApi::class)
class IfSubOpHandler
@Inject constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
) : MessageHandler<IfSubOp> {

    private val IfSubOp.asComponent: Component
        get() = Component(interfaceId, componentId)

    private val IfSubOp.buttonOp: IfButtonOp
        get() = when (op) {
            1 -> IfButtonOp.Op1
            2 -> IfButtonOp.Op2
            3 -> IfButtonOp.Op3
            4 -> IfButtonOp.Op4
            5 -> IfButtonOp.Op5
            6 -> IfButtonOp.Op6
            7 -> IfButtonOp.Op7
            8 -> IfButtonOp.Op8
            9 -> IfButtonOp.Op9
            10 -> IfButtonOp.Op10
            else -> throw NotImplementedError("Unhandled IfSubOp op: $this")
        }

    override fun handle(player: Player, message: IfSubOp) {
        val componentType = ServerCacheManager.fromComponent(message.asComponent.packed)
        val interfaceType = ServerCacheManager.fromInterface(message.asComponent.packed)
        val comsub = message.sub
        val buttonOp = message.buttonOp
        val subop = message.subop

        val opEnabled = InterfaceEvents.isEnabled(player.ui, componentType, comsub, buttonOp.toIfEvent())
        if (!opEnabled) {
            return
        }

        val packetObjType = ServerCacheManager.getItem(message.obj)
        val heldOp = opToHeldOp(message.op)
        val tryHeld = packetObjType != null && heldOp != null
        val inventory = player.inv
        val invObj = if (tryHeld) inventory[comsub] else null

        if (player.ui.containsOverlay(interfaceType) || player.ui.containsTopLevel(interfaceType)) {
            if (tryHeld && invObj != null) {
                if (!verifyHeldSubOp(player, inventory, invObj, packetObjType, heldOp)) {
                    return
                }
                val launched = protectedAccess.launch(player) {
                    clearPendingAction()
                    val type = getInvObj(invObj)
                    val event = HeldSubOpEvent(comsub, invObj, type, inventory, heldOp, subop)
                    eventBus.publish(this, event)
                }
                if (launched) {
                    return
                }
            }
            val event = IfOverlaySubOpMenu(
                componentType,
                comsub,
                packetObjType,
                buttonOp,
                subop,
            )
            protectedAccess.launchLenient(player) { eventBus.publish(this, event) }
            return
        }

        if (player.ui.containsModal(interfaceType)) {
            player.ifCloseInputDialog()
            if (player.isModalButtonProtected) {
                return
            }
            if (tryHeld && invObj != null) {
                if (!verifyHeldSubOp(player, inventory, invObj, packetObjType, heldOp)) {
                    return
                }
                protectedAccess.launchLenient(player) {
                    val type = getInvObj(invObj)
                    val event = HeldSubOpEvent(comsub, invObj, type, inventory, heldOp, subop)
                    eventBus.publish(this, event)
                }
            } else {
                val event = IfModalSubOpMenu(componentType, comsub, packetObjType, buttonOp, subop,)
                protectedAccess.launchLenient(player) { eventBus.publish(this, event) }
            }
        }
    }

    private fun verifyHeldSubOp(
        player: Player,
        inventory: Inventory,
        invObj: InvObj,
        packetObjType: ItemServerType,
        heldOp: HeldOp,
    ): Boolean {
        if (player.isDelayed) {
            resendSlot(inventory, 0)
            return false
        }
        if (!invObj.isType(packetObjType)) {
            resendSlot(inventory, 0)
            return false
        }
        val type = getInvObj(invObj)
        if (!type.hasInvOp(heldOp) && heldOp != HeldOp.Op5) {
            resendSlot(inventory, 0)
            return false
        }
        return true
    }

    private fun opToHeldOp(op: Int): HeldOp? = when (op) {
        2 -> HeldOp.Op1
        3 -> HeldOp.Op2
        4 -> HeldOp.Op3
        6 -> HeldOp.Op4
        7 -> HeldOp.Op5
        else -> null
    }

    private fun IfButtonOp.toIfEvent(): IfEvent = when (this) {
        IfButtonOp.Op1 -> IfEvent.Op1
        IfButtonOp.Op2 -> IfEvent.Op2
        IfButtonOp.Op3 -> IfEvent.Op3
        IfButtonOp.Op4 -> IfEvent.Op4
        IfButtonOp.Op5 -> IfEvent.Op5
        IfButtonOp.Op6 -> IfEvent.Op6
        IfButtonOp.Op7 -> IfEvent.Op7
        IfButtonOp.Op8 -> IfEvent.Op8
        IfButtonOp.Op9 -> IfEvent.Op9
        IfButtonOp.Op10 -> IfEvent.Op10
        IfButtonOp.Op11 -> IfEvent.Op11
        IfButtonOp.Op12 -> IfEvent.Op12
        IfButtonOp.Op13 -> IfEvent.Op13
        IfButtonOp.Op14 -> IfEvent.Op14
        IfButtonOp.Op15 -> IfEvent.Op15
        IfButtonOp.Op16 -> IfEvent.Op16
        IfButtonOp.Op17 -> IfEvent.Op17
        IfButtonOp.Op18 -> IfEvent.Op18
        IfButtonOp.Op19 -> IfEvent.Op19
        IfButtonOp.Op20 -> IfEvent.Op20
        IfButtonOp.Op21 -> IfEvent.Op21
        IfButtonOp.Op22 -> IfEvent.Op22
        IfButtonOp.Op23 -> IfEvent.Op23
        IfButtonOp.Op24 -> IfEvent.Op24
        IfButtonOp.Op25 -> IfEvent.Op25
        IfButtonOp.Op26 -> IfEvent.Op26
        IfButtonOp.Op27 -> IfEvent.Op27
        IfButtonOp.Op28 -> IfEvent.Op28
        IfButtonOp.Op29 -> IfEvent.Op29
        IfButtonOp.Op30 -> IfEvent.Op30
        IfButtonOp.Op31 -> IfEvent.Op31
        IfButtonOp.Op32 -> IfEvent.Op32
    }
}
