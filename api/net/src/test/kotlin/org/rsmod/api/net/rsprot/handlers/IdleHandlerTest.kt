package org.rsmod.api.net.rsprot.handlers

import net.rsprot.protocol.game.incoming.misc.client.Idle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Player

class IdleHandlerTest {
    @Test
    fun `idle requests the same logout as the logout button`() {
        val player = Player()
        IdleHandler().handle(player, Idle)
        assertTrue(player.manualLogout)
    }

    @Test
    fun `idle during a logout already under way does nothing`() {
        val player = Player()
        player.pendingLogout = true
        IdleHandler().handle(player, Idle)
        assertFalse(player.manualLogout)
    }
}
