package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePStringDialog
import org.rsmod.api.player.input.ResumePStringDialogInput
import org.rsmod.api.player.input.StringDialogInput
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

class ResumePStringDialogHandler
@Inject
constructor(private val eventBus: EventBus) : MessageHandler<ResumePStringDialog> {
    override fun handle(player: Player, message: ResumePStringDialog) {
        val input = ResumePStringDialogInput(message.string)
        eventBus.publish(StringDialogInput(player, message.string))
        player.resumeActiveCoroutine(input)
    }
}
