package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.misc.user.ClientCheat
import org.rsmod.api.net.central.logging.CentralActivityLogWriter
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player

class ClientCheatHandler
@Inject
constructor(
    private val commands: CheatCommandMap,
    private val centralActivityLogWriter: CentralActivityLogWriter,
) : MessageHandler<ClientCheat> {
    override fun handle(player: Player, message: ClientCheat) {
        val text = message.command.lowercase()
        val split = text.split(" ")
        val command = split[0]
        val args = if (split.size > 1) split.subList(1, split.size).toList() else emptyList()
        centralActivityLogWriter.logCommand(player, command, args)
        commands.execute(player, command, args)
    }
}
