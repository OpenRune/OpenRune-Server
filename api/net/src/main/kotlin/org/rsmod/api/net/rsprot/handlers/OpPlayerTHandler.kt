package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.players.OpPlayerT
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.interact.PlayerTInteractions
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.ctrlMoveSpeed
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.interact.InteractionPlayerT
import org.rsmod.game.movement.RouteRequestPathingEntity
import org.rsmod.game.ui.Component

class OpPlayerTHandler
@Inject
constructor(
    private val eventBus: EventBus,
    private val playerList: PlayerList,
    private val playerInteractions: PlayerTInteractions,
) : MessageHandler<OpPlayerT> {
    private val logger = InlineLogger()

    private val OpPlayerT.asComponent: Component
        get() = Component(selectedInterfaceId, selectedComponentId)

    override fun handle(player: Player, message: OpPlayerT) {
        if (player.isDelayed) {
            player.clearMapFlag()
            return
        }

        val target = playerList[message.index]
        if (target == null) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

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
            InterfaceEvents.isEnabled(player.ui, componentType, comsub, IfEvent.TgtPlayer)
        if (!targetEnabled) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val speed = if (message.controlKey) player.ctrlMoveSpeed() else null
        val opTrigger = playerInteractions.hasOpTrigger(target, componentType, comsub, objType)
        val apTrigger = playerInteractions.hasApTrigger(target, componentType, comsub, objType)
        val interaction =
            InteractionPlayerT(
                target = target,
                comsub = comsub,
                objType = objType,
                component = componentType,
                hasOpTrigger = opTrigger,
                hasApTrigger = apTrigger,
            )
        val routeRequest = RouteRequestPathingEntity(target.avatar, clientRequest = true)
        player.clearPendingAction(eventBus)
        player.facePlayer(target)
        player.interaction = interaction
        player.routeRequest = routeRequest
        player.tempMoveSpeed = speed
        logger.debug {
            "OpPlayerT: target=${target.displayName}, comsub=$comsub, " +
                "component=$componentType, obj=$objType"
        }
    }
}
