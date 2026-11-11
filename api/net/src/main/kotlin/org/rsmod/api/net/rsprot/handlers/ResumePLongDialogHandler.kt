package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePLongDialog
import org.rsmod.api.player.input.ResumePLongDialogInput
import org.rsmod.game.entity.Player

class ResumePLongDialogHandler @Inject constructor() : MessageHandler<ResumePLongDialog> {
    /**
     * Passes the submitted long to the player's active dialog coroutine.
     *
     * The coroutine must be waiting for [ResumePLongDialogInput]. Replies to a closed
     * dialog or sent while another type of input is pending are ignored.
     */
    override fun handle(player: Player, message: ResumePLongDialog) {
        if (player.activeCoroutine?.isAwaiting(ResumePLongDialogInput::class) == true) {
            player.resumeActiveCoroutine(ResumePLongDialogInput(message.count))
        }
    }
}
