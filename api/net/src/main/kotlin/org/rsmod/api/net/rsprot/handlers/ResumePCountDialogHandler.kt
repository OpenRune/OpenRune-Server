package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePCountDialog
import org.rsmod.api.player.input.DialogInput
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent

class ResumePCountDialogHandler@Inject constructor(
    private val eventBus: EventBus,
)  : MessageHandler<ResumePCountDialog> {
    override fun handle(player: Player, message: ResumePCountDialog) {
        val input = ResumePCountDialogInput(message.count)
        eventBus.publish(DialogInput(player,message.count))
        player.resumeActiveCoroutine(input)
    }
}
