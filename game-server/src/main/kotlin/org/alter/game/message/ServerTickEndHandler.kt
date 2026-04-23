package org.alter.game.message

import net.rsprot.protocol.game.outgoing.misc.client.ServerTickEnd

/**
 * A message handler for ServerTickEnd which signals the end of a server tick cycle.
 * This is used by RSProxy to properly timestamp packets for live logging.
 */
class ServerTickEndHandler {
    companion object {
        /**
         * Send a server tick end message to all connected players.
         * This should be called at the end of each game tick cycle.
         */
        fun sendToAll(world: org.alter.game.model.World) {
            world.players.forEach { player ->
                if (player.initiated) {
                    player.write(ServerTickEnd)
                }
            }
        }
    }
}
