package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.locs.OpLocT
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
) : MessageHandler<OpLocT> {
    private val logger = InlineLogger()

    private val OpLocT.asComponent: Component
        get() = Component(selectedInterfaceId, selectedComponentId)

    override fun handle(player: Player, message: OpLocT) {
        if (player.isDelayed) {
            return
        }

        val coords = CoordGrid(message.x, message.z, player.level)
        val loc = locRegistry.findType(coords, message.id)
        if (loc == null) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }
        val type = ServerCacheManager.getObject(message.id) ?: return
        val interfaceType = ServerCacheManager.fromInterface(message.asComponent.packed)
        val componentType = ServerCacheManager.fromComponent(message.asComponent.packed)
        val objType =
            message.selectedObj
                .takeIf { it != -1 }
                ?.let { id -> ServerCacheManager.getItems().values.firstOrNull { it.id == id } }

        val isValidInterface =
            player.ui.containsOverlay(interfaceType) || player.ui.containsModal(interfaceType)
        if (!isValidInterface) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val comsub = message.selectedSub

        val targetEnabled =
            InterfaceEvents.isEnabled(player.ui, componentType, comsub, IfEvent.TgtLoc)
        if (!targetEnabled) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val speed = if (message.controlKey) player.ctrlMoveSpeed() else null
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
