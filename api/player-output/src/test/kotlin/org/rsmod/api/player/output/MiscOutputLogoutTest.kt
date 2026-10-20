package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.logout.LogoutWithReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player

class MiscOutputLogoutTest {
    // The client reads a packet's payload after its opcode. A zero-length payload read fails once
    // it has seen the server close the socket, so a plain `Logout` sent right before the close
    // leaves the client reconnecting instead of returning to the login screen.
    @Test
    fun `manual logout sends a logout with the requested reason`() {
        val client = RecordingClient()
        val player = Player()
        player.client = client

        MiscOutput.logout(player)

        val message = client.messages.single() as LogoutWithReason
        assertEquals(0, message.reason)
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }
}
