package org.rsmod.api.inv

import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.ui.IfOverlayDrag
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onIfOverlayDrag
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HeldOpScript
@Inject
private constructor(
    private val eventBus: EventBus,
    private val interactions: HeldInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.inventory:items") { opHeldButton(it) }
        onIfOverlayDrag("component.inventory:items") { dragHeldButton() }
    }

    private suspend fun ProtectedAccess.opHeldButton(event: IfOverlayButton) {
        if (event.op == IfButtonOp.Op10) {
            interactions.examine(player, player.inv, event.comsub)
            return
        }
        val heldOp = event.op.toHeldOp() ?: throw IllegalStateException("Op not supported: $event")
        player.opHeld(event.comsub, heldOp)
    }

    private fun Player.opHeld(invSlot: Int, op: HeldOp) {
        ifClose(eventBus)
        if (isAccessProtected) {
            resendSlot(inv, 0)
            return
        }
        protectedAccess.launch(this) {
            clearPendingAction()
            interactions.interact(this, inv, invSlot, op)
        }
    }

    private fun IfOverlayDrag.dragHeldButton() {
        val fromSlot = selectedSlot ?: return
        val intoSlot = targetSlot ?: return
        player.dragHeld(fromSlot, intoSlot)
    }

    private fun Player.dragHeld(fromSlot: Int, intoSlot: Int) {
        ifClose(eventBus)
        if (isAccessProtected) {
            resendSlot(inv, 0)
            return
        }
        protectedAccess.launch(this) { invMoveToSlot(inv, inv, fromSlot, intoSlot) }
    }

    private fun IfButtonOp.toHeldOp(): HeldOp? =
        when (this) {
            IfButtonOp.Op2 -> HeldOp.Op1
            IfButtonOp.Op3 -> HeldOp.Op2
            IfButtonOp.Op4 -> HeldOp.Op3
            IfButtonOp.Op6 -> HeldOp.Op4
            IfButtonOp.Op7 -> HeldOp.Op5
            else -> null
        }
}
