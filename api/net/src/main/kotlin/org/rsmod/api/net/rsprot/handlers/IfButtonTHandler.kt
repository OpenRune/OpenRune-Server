package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.cache.filestore.definition.InterfaceType.Companion.isType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.IfButtonT
import org.rsmod.annotations.InternalApi
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalButtonT
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.player.ui.ifCloseInputDialog
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component
import org.rsmod.game.ui.UserInterfaceMap

class IfButtonTHandler
@Inject
constructor(private val eventBus: EventBus, private val protectedAccess: ProtectedAccessLauncher) :
    MessageHandler<IfButtonT> {
    private val logger = InlineLogger()

    @OptIn(InternalApi::class)
    override fun handle(player: Player, message: IfButtonT) {
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

        // No need to verify again if both objs belong to the same interface.
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

        val targetEnabled =
            isTargetEnabled(ui, selectedComponentType, selectedSub, targetComponentType, targetSub)
        if (!targetEnabled) {
            return
        }

        val selectedItemServerType = ServerCacheManager.getItem(message.selectedObj)
        val targetItemServerType = ServerCacheManager.getItem(message.targetObj)

        val isSelectedOverlay = !isSelectedOpenedModal
        if (isSelectedOverlay) {
            val overlayButton =
                IfOverlayButtonT(
                    player = player,
                    selectedSlot = selectedSub,
                    selectedObj = selectedItemServerType,
                    targetSlot = targetSub,
                    targetObj = targetItemServerType,
                    selectedComponent = selectedComponent,
                    targetComponent = targetComponent,
                )
            logger.debug { "[Overlay] IfButtonT: $message (overlayButton=$overlayButton)" }
            eventBus.publish(overlayButton)
            return
        }

        val modalButton =
            IfModalButtonT(
                selectedSlot = selectedSub,
                selectedObj = selectedItemServerType,
                targetSlot = targetSub,
                targetObj = targetItemServerType,
                selectedComponent = selectedComponent,
                targetComponent = targetComponent,
            )
        player.ifCloseInputDialog()
        if (player.isModalButtonProtected) {
            logger.debug { "[Modal][BLOCKED] IfButtonT: $message (modalButton=$modalButton)" }
            return
        }
        logger.debug { "[Modal] IfButtonT: $message (modalButton=$modalButton)" }
        protectedAccess.launchLenient(player) { eventBus.publish(this, modalButton) }
    }

    private fun isTargetEnabled(
        ui: UserInterfaceMap,
        from: ComponentType,
        fromComsub: Int,
        target: ComponentType,
        targetComsub: Int,
    ): Boolean {
        val targetFromEnabled = InterfaceEvents.isEnabled(ui, from, fromComsub, IfEvent.TgtCom)
        if (!targetFromEnabled) {
            return false
        }
        val targetToEnabled = InterfaceEvents.isEnabled(ui, target, targetComsub, IfEvent.Target)
        return targetToEnabled
    }
}
