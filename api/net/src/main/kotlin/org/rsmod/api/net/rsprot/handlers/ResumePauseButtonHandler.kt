package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ui.IfModalPauseButton
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component
import org.rsmod.game.ui.UserInterface

class ResumePauseButtonHandler @Inject constructor(private val eventBus: EventBus) :
    MessageHandler<ResumePauseButton> {
    constructor() : this(EventBus())

    private val ResumePauseButton.asComponent: Component
        get() = Component(interfaceId, componentId)

    override fun handle(player: Player, message: ResumePauseButton) {
        val componentType = ServerCacheManager.fromComponent(message.asComponent.packed)
        val interfaceType = ServerCacheManager.fromInterface(message.asComponent.packed)
        val userInterface = UserInterface(interfaceType)

        val pauseEnabled =
            InterfaceEvents.isEnabled(player.ui, componentType, message.sub, IfEvent.PauseButton)
        if (!pauseEnabled) {
            return
        }

        val input = ResumePauseButtonInput(RSCM.getReverseMapping(RSCMType.COMPONENT, componentType.packed), message.sub)
        val modal = player.ui.modals.getComponent(userInterface)
        if (modal != null) {
            // Native browsers can submit a stable selection through a pause button. A suspended
            // dialogue retains ownership of its input; stateless handlers run only while idle.
            if (player.activeCoroutine?.isSuspended != true) {
                if (player.isDelayed) {
                    return
                }
                if (eventBus.publish(IfModalPauseButton(player, componentType, message.sub))) {
                    return
                }
            }
            player.ui.queueClose(modal)
            player.resumeActiveCoroutine(input)
            return
        }

        val overlay = player.ui.overlays.getComponent(userInterface)
        if (overlay != null) {
            player.ui.queueClose(overlay)
            player.resumeActiveCoroutine(input)
            return
        }
    }
}
