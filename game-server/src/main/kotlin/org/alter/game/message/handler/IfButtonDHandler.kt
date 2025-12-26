package org.alter.game.message.handler

import dev.openrune.ServerCacheManager
import dev.openrune.cache.filestore.definition.InterfaceType.Companion.isType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.IfEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import net.rsprot.protocol.game.incoming.buttons.IfButtonD
import net.rsprot.protocol.util.CombinedId
import org.alter.game.message.MessageHandler
import org.alter.game.model.attr.INTERACTING_COMPONENT_CHILD
import org.alter.game.model.attr.INTERACTING_ITEM_SLOT
import org.alter.game.model.attr.OTHER_ITEM_SLOT_ATTR
import org.alter.game.model.entity.Client
import org.alter.game.pluginnew.event.impl.IfModalDrag
import org.alter.game.pluginnew.event.impl.IfOverlayDrag
import org.alter.game.ui.InterfaceEvents
import org.alter.game.ui.UserInterfaceMap
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType

/**
 * @author Tom <rspsmods@gmail.com>
 */
class IfButtonDHandler : MessageHandler<IfButtonD> {

    private val logger = KotlinLogging.logger {}

    override fun consume(client: Client, message: IfButtonD) {

        val fromSlot = message.selectedSub
        val fromItemId = message.selectedObj

        val toSlot = message.targetSub
        val toItemId = message.targetObj

        val fromInterfaceId = message.selectedInterfaceId
        val fromComponent = message.selectedComponentId
        val toInterfaceId = message.targetInterfaceId
        val toComponent = message.targetComponentId

        log(
            client,
            "Swap component to component item: src_component=[%d:%d], dst_component=[%d:%d], src_item=%d, dst_item=%d",
            fromInterfaceId,
            fromComponent,
            toInterfaceId,
            toComponent,
            fromItemId,
            toItemId,
        )

        if (!client.lock.canItemInteract()) {
            return
        }

        client.attr[INTERACTING_ITEM_SLOT] = fromSlot
        client.attr[OTHER_ITEM_SLOT_ATTR] = toSlot
        client.attr[INTERACTING_COMPONENT_CHILD] = fromComponent

        val selectedComponent = CombinedId(message.selectedInterfaceId, message.selectedComponentId)
        val selectedComponentType = ServerCacheManager.fromComponent(selectedComponent.combinedId)
        val selectedInterface = ServerCacheManager.fromInterface(message.selectedInterfaceId)
        val targetComponent = CombinedId(message.targetInterfaceId, message.targetComponentId)
        val targetComponentType = ServerCacheManager.fromComponent(targetComponent.combinedId)
        val targetInterface = ServerCacheManager.fromInterface(message.targetInterfaceId)
        val ui = client.ui

        val isSelectedOpenedModal = ui.containsModal(selectedInterface)
        val isSelectedOpened = isSelectedOpenedModal || ui.containsOverlay(selectedInterface)
        if (!isSelectedOpened) {
            logger.debug { "Selected interface is not open: message=$message, player=$client" }
            return
        }

        val skipTargetVerification = selectedInterface.isType(targetInterface)
        if (!skipTargetVerification) {
            val isTargetOpenedModal = ui.containsModal(targetInterface)
            val targetOpened = isTargetOpenedModal || ui.containsOverlay(targetInterface)
            if (!targetOpened) {
                logger.debug { "Target interface is not open: message=$message, player=$client" }
                return
            }
        }

        val selectedSub = message.selectedSub
        val targetSub = message.targetSub

        val dragEnabled = isDragEnabled(
            ui,
            selectedComponentType,
            selectedSub,
            targetComponentType,
            targetSub
        )
        if (!dragEnabled) {
            return
        }

        // Client replaces empty obj ids with `6512`. To make life easier, we simply replace those
        // with null obj types as that's what associated scripts should treat them as.
        val selectedObjType = convertNullReplacement(message.selectedObj)
        val targetObjType = convertNullReplacement(message.targetObj)

        val isSelectedOverlay = !isSelectedOpenedModal
        if (isSelectedOverlay) {
            val overlayDrag = IfOverlayDrag(
                player = client,
                selectedSlot = selectedSub,
                selectedObj = selectedObjType,
                targetSlot = targetSub,
                targetObj = targetObjType,
                selectedComponent = selectedComponent.combinedId,
                targetComponent = targetComponent.combinedId,
            ).post()
            logger.debug { "[Overlay] IfButtonD: $message (overlayDrag=$overlayDrag)" }
            return
        }

        client.ifCloseInputDialog()
        val modalDrag = IfModalDrag(
            player = client,
            selectedSlot = selectedSub,
            selectedObj = selectedObjType,
            targetSlot = targetSub,
            targetObj = targetObjType,
            selectedComponent = selectedComponent.combinedId,
            targetComponent = targetComponent.combinedId,
        ).post()
        logger.debug { "[Modal] IfButtonD: $message (modalDrag=$modalDrag)" }

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
        return if (type == "objects.deserttreasure_sarcophigi_door".asRSCM()) {
            null
        } else {
            type
        }
    }

}
