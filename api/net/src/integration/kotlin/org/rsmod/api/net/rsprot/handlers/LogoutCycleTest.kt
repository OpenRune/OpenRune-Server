package org.rsmod.api.net.rsprot.handlers

import net.rsprot.protocol.game.outgoing.logout.LogoutWithReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.player.isLogoutBlocked
import org.rsmod.api.testing.GameTestState

class LogoutCycleTest {
    @Test
    fun GameTestState.`logout inside an area closes the client on the request cycle`() =
        runGameTest {
            advanceUntil({ !player.isLogoutBlocked() })
            player.withProtectedAccess { logOut() }
            advance()
            assertTrue(player.loggingOut)
            assertEquals(1, client.outgoingMessages.count { it is LogoutWithReason })
        }
}
