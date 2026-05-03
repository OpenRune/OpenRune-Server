package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.cache.filestore.definition.InterfaceType.Companion.isType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.IfButtonD
import org.rsmod.annotations.InternalApi
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalDrag
import org.rsmod.api.player.ui.IfOverlayDrag
import org.rsmod.api.player.ui.ifCloseInputDialog
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component
import org.rsmod.game.ui.UserInterfaceMap

class IfButtonDHandler
@Inject
constructor(private val eventBus: EventBus, private val protectedAccess: ProtectedAccessLauncher) :
    MessageHandler<IfButtonD> {
    private val logger = InlineLogger()

    @OptIn(InternalApi::class)
    override fun handle(player: Player, message: IfButtonD) {
        val selectedComponent = Component(message.selectedInterfaceId, message.selectedComponentId)
        val selectedComponentType = ServerCacheManager.fromComponent(selectedComponent.packed)
        val selectedInterface = ServerCacheManager.fromInterface(selectedComponent.packed)

        val targetComponent = Component(message.targetInterfaceId, message.targetComponentId)
        val targetComponentType = ServerCacheManager.fromComponent(targetComponent.packed)
        val targetInterface = ServerCacheManager.fromInterface(targetComponent.packed)
        val ui = player.ui

        val isSelectedOpenedModal = ui.containsModal(selectedInterface)
        val isSelectedOpened = isSelectedOpenedModal || ui.containsOverlay(selectedInterface)
        if (!isSelectedOpened) {
            logger.debug { "Selected interface is not open: message=$message, player=$player" }
            return
        }

        // No need to verify again if dragging objs on the same interface.
        val skipTargetVerification = selectedInterface.isType(targetInterface)
        if (!skipTargetVerification) {
            val isTargetOpenedModal = ui.containsModal(targetInterface)
            val targetOpened = isTargetOpenedModal || ui.containsOverlay(targetInterface)
            if (!targetOpened) {
                logger.debug { "Target interface is not open: message=$message, player=$player" }
                return
            }
        }

        val selectedSub = message.selectedSub
        val targetSub = message.targetSub

        val dragEnabled =
            isDragEnabled(ui, selectedComponentType, selectedSub, targetComponentType, targetSub)
        if (!dragEnabled) {
            return
        }

        // Client replaces empty obj ids with `6512`. To make life easier, we simply replace those
        // with null obj types as that's what associated scripts should treat them as.
        val selectedItemServerType = convertNullReplacement(message.selectedObj)
        val targetItemServerType = convertNullReplacement(message.targetObj)

        val isSelectedOverlay = !isSelectedOpenedModal
        if (isSelectedOverlay) {
            val overlayDrag =
                IfOverlayDrag(
                    player = player,
                    selectedSlot = selectedSub,
                    selectedObj = selectedItemServerType,
                    targetSlot = targetSub,
                    targetObj = targetItemServerType,
                    selectedComponent = selectedComponent,
                    targetComponent = targetComponent,
                )
            logger.debug { "[Overlay] IfButtonD: $message (overlayDrag=$overlayDrag)" }
            eventBus.publish(overlayDrag)
            return
        }

        val modalDrag =
            IfModalDrag(
                selectedSlot = selectedSub,
                selectedObj = selectedItemServerType,
                targetSlot = targetSub,
                targetObj = targetItemServerType,
                selectedComponent = selectedComponent,
                targetComponent = targetComponent,
            )
        player.ifCloseInputDialog()
        if (player.isModalButtonProtected) {
            logger.debug { "[Modal][BLOCKED] IfButtonD: $message (modalDrag=$modalDrag)" }
            return
        }
        logger.debug { "[Modal] IfButtonD: $message (modalDrag=$modalDrag)" }
        protectedAccess.launchLenient(player) { eventBus.publish(this, modalDrag) }
    }

    private fun isDragEnabled(
        ui: UserInterfaceMap,
        from: ComponentType,
        fromSlot: Int,
        target: ComponentType,
        targetSlot: Int,
    ): Boolean {
        val dragFromEnabled = InterfaceEvents.isEnabled(ui, from, fromSlot, IfEvent.DragTarget)
        if (!dragFromEnabled) {
            return false
        }
        val dragToEnabled = InterfaceEvents.isEnabled(ui, target, targetSlot, IfEvent.DragTarget)
        return dragToEnabled
    }

    private fun convertNullReplacement(type: Int?): Int? {
        return if (type == "obj.blankobject".asRSCM(RSCMType.OBJ)) {
            null
        } else {
            type
        }
    }
}
