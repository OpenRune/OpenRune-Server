package org.rsmod.api.net.rsprot.handlers

import net.rsprot.protocol.game.incoming.misc.client.Idle
import net.rsprot.protocol.game.outgoing.logout.LogoutWithReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.rsmod.api.player.isLogoutBlocked
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope

class IdleLogoutTest {
    @Test
    fun GameTestState.`idle logout closes the client as kicked`() =
        runInjectedGameTest(IdleHandler::class) { idle ->
            advanceUntil({ !player.isLogoutBlocked() })
            client.queue(idle, Idle)
            assertEquals(listOf(KICKED), logoutReasons())
        }

    @Test
    fun GameTestState.`logout button closes the client as requested`() = runGameTest {
        advanceUntil({ !player.isLogoutBlocked() })
        player.withProtectedAccess { logOut() }
        assertEquals(listOf(REQUESTED), logoutReasons())
    }

    @Test
    fun GameTestState.`blocked idle logout does not turn a later logout into a kick`() =
        runInjectedGameTest(IdleHandler::class) { idle ->
            advanceUntil({ !player.isLogoutBlocked() })
            player.preventLogoutUntil = Int.MAX_VALUE
            client.queue(idle, Idle)
            advance(2)
            assertFalse(player.loggingOut)

            player.preventLogoutUntil = Int.MIN_VALUE
            player.withProtectedAccess { logOut() }
            assertEquals(listOf(REQUESTED), logoutReasons())
        }

    private fun GameTestScope.logoutReasons(): List<Int> {
        advanceUntil({ player.loggingOut }, timeoutTicks = 5)
        return client.mapOf<LogoutWithReason, Int> { it.reason }
    }

    private companion object {
        const val REQUESTED = 0
        const val KICKED = 1
    }
}
