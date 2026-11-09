package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.locs.OpLocTV1
import net.rsprot.protocol.game.incoming.locs.OpLocTV2
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.interact.LocTInteractions
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.ctrlMoveSpeed
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLocT
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.movement.RouteRequestLoc
import org.rsmod.game.ui.Component
import org.rsmod.map.CoordGrid

class OpLocTHandler
@Inject
constructor(
    private val eventBus: EventBus,
    private val locRegistry: LocRegistry,
    private val locInteractions: LocTInteractions,
) {
    private val logger = InlineLogger()

    val v1: MessageHandler<OpLocTV1> = MessageHandler { player, message ->
        handle(
            player = player,
            locId = message.id,
            x = message.x,
            z = message.z,
            controlKey = message.controlKey,
            component = Component(message.selectedInterfaceId, message.selectedComponentId),
            comsub = message.selectedSub,
            selectedObj = message.selectedObj,
        )
    }

    val v2: MessageHandler<OpLocTV2> = MessageHandler { player, message ->
        handle(
            player = player,
            locId = message.id,
            x = message.x,
            z = message.z,
            controlKey = message.controlKey,
            component = Component(message.selectedInterfaceId, message.selectedComponentId),
            comsub = message.selectedSub,
            selectedObj = message.selectedObj,
        )
    }

    private fun handle(
        player: Player,
        locId: Int,
        x: Int,
        z: Int,
        controlKey: Boolean,
        component: Component,
        comsub: Int,
        selectedObj: Int,
    ) {
        if (player.isDelayed) {
            return
        }

        val coords = CoordGrid(x, z, player.level)
        val loc = locRegistry.findType(coords, locId)
        if (loc == null) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }
        val type = ServerCacheManager.getObject(locId) ?: return
        val interfaceType = ServerCacheManager.fromInterface(component.packed)
        val componentType = ServerCacheManager.fromComponent(component.packed)
        val objType =
            selectedObj
                .takeIf { it != -1 }
                ?.let { id -> ServerCacheManager.getItems().values.firstOrNull { it.id == id } }

        val isValidInterface =
            player.ui.containsOverlay(interfaceType) || player.ui.containsModal(interfaceType)
        if (!isValidInterface) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val targetEnabled =
            InterfaceEvents.isEnabled(player.ui, componentType, comsub, IfEvent.TgtLoc)
        if (!targetEnabled) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val speed = if (controlKey) player.ctrlMoveSpeed() else null
        val boundLoc = BoundLocInfo(loc, type)
        val opTrigger =
            locInteractions.hasOpTrigger(player, boundLoc, type, objType, componentType, comsub)
        val apTrigger =
            locInteractions.hasApTrigger(player, boundLoc, type, objType, componentType, comsub)
        val interaction =
            InteractionLocT(
                target = boundLoc,
                comsub = comsub,
                objType = objType,
                component = componentType,
                hasOpTrigger = opTrigger,
                hasApTrigger = apTrigger,
            )
        val routeRequest =
            RouteRequestLoc(
                destination = coords,
                width = type.width,
                length = type.length,
                shape = loc.entity.shape,
                angle = loc.entity.angle,
                forceApproachFlags = type.forceApproachFlags,
                clientRequest = true,
            )
        player.clearPendingAction(eventBus)
        player.resetFaceEntity()
        player.faceLoc(loc, type.width, type.length)
        player.interaction = interaction
        player.routeRequest = routeRequest
        player.tempMoveSpeed = speed
        logger.debug {
            "OpLocT: loc=$boundLoc, type=$type, comsub=$comsub, " +
                "component=$componentType, obj=$objType"
        }
    }
}
