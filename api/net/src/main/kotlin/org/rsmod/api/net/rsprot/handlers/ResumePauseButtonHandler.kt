package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component
import org.rsmod.game.ui.UserInterface

class ResumePauseButtonHandler @Inject constructor() : MessageHandler<ResumePauseButton> {
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

        val input = ResumePauseButtonInput(componentType, message.sub)
        val modal = player.ui.modals.getComponent(userInterface)
        if (modal != null) {
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
