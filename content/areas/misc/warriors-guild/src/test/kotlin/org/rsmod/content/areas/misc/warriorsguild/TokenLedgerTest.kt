package org.rsmod.content.areas.misc.warriorsguild

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.areas.misc.warriorsguild.GuildTestFixture.fillInv
import org.rsmod.content.areas.misc.warriorsguild.GuildTestFixture.player
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger.Claim
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class TokenLedgerTest {
    @Test
    fun `claiming with nothing owed adds nothing`() {
        val player = player()
        assertEquals(Claim.NothingOwed, TokenLedger.claim(player))
        assertEquals(0, player.tokens())
    }

    @Test
    fun `claim pays the whole ledger once even when repeated on the same tick`() {
        val player = player()
        TokenLedger.credit(player, 37)
        assertEquals(Claim.Paid(37), TokenLedger.claim(player))
        assertEquals(Claim.NothingOwed, TokenLedger.claim(player))
        assertEquals(37, player.tokens())
        assertEquals(0, player.tokensOwed)
    }

    @Test
    fun `full inventory keeps the ledger and adds nothing`() {
        val player = player()
        player.fillInv()
        TokenLedger.credit(player, 12)
        assertEquals(Claim.NoSpace, TokenLedger.claim(player))
        assertEquals(12, player.tokensOwed)
        assertEquals(0, player.tokens())
    }

    @Test
    fun `full inventory with a token stack still takes the claim`() {
        val player = player()
        player.fillInv()
        player.inv[0] = InvObj(TokenLedger.TOKEN, 100)
        TokenLedger.credit(player, 12)
        assertEquals(Claim.Paid(12), TokenLedger.claim(player))
        assertEquals(112, player.tokens())
        assertEquals(0, player.tokensOwed)
    }

    @Test
    fun `claim that would overflow the stack is refused whole`() {
        val player = player()
        player.inv[0] = InvObj(TokenLedger.TOKEN, Int.MAX_VALUE - 5)
        TokenLedger.credit(player, 10)
        assertEquals(Claim.NoSpace, TokenLedger.claim(player))
        assertEquals(Int.MAX_VALUE - 5, player.tokens())
        assertEquals(10, player.tokensOwed)

        player.inv[0] = InvObj(TokenLedger.TOKEN, Int.MAX_VALUE - 10)
        assertEquals(Claim.Paid(10), TokenLedger.claim(player))
        assertEquals(Int.MAX_VALUE, player.tokens())
        assertEquals(0, player.tokensOwed)
    }

    @Test
    fun `credit saturates instead of wrapping`() {
        val player = player()
        TokenLedger.credit(player, Int.MAX_VALUE - 1)
        TokenLedger.credit(player, 50)
        assertEquals(Int.MAX_VALUE, player.tokensOwed)
    }

    @Test
    fun `keg ledger moves to the main ledger exactly once`() {
        val player = player()
        TokenLedger.credit(player, 5)
        TokenLedger.creditKegs(player, 20)
        assertEquals(20, TokenLedger.transferKegs(player))
        assertEquals(0, TokenLedger.transferKegs(player))
        assertEquals(25, player.tokensOwed)
        assertEquals(0, player.kegTokensOwed)
    }

    private fun Player.tokens(): Int = inv.count(TokenLedger.TOKEN)

    companion object {
        @JvmStatic @BeforeAll fun cache() = GuildTestFixture.loadCache()

        @JvmStatic @AfterAll fun restore() = GuildTestFixture.restore()
    }
}
