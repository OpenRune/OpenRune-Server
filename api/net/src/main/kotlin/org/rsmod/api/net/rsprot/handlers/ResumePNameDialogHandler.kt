package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePNameDialog
import org.rsmod.api.player.input.ResumePNameDialogInput
import org.rsmod.api.player.input.StringDialogInput
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class ResumePNameDialogHandler
@Inject
constructor(private val playerList: PlayerList, private val eventBus: EventBus) :
    MessageHandler<ResumePNameDialog> {
    override fun handle(player: Player, message: ResumePNameDialog) {
        val result = find(message.name)
        val input = ResumePNameDialogInput(result)
        eventBus.publish(StringDialogInput(player, message.name))
        player.resumeActiveCoroutine(input)
    }

    private fun find(name: String): ResumePNameDialogInput.Result {
        val player = playerList.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
        return if (player != null) {
            ResumePNameDialogInput.Result.SameWorld(player.uid)
        } else {
            ResumePNameDialogInput.Result.NotFound
        }
    }
}
