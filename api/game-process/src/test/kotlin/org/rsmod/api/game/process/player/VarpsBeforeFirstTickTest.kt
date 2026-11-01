package org.rsmod.api.game.process.player

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.varp.VarpLarge
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import net.rsprot.protocol.game.outgoing.varp.VarpSmall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.flushVarpsSetBeforeFirstTick
import org.rsmod.api.player.vars.resyncAllVars
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class VarpsBeforeFirstTickTest {
    @Test
    fun `a varp changed before the first processed tick is sent on that tick`() {
        val (player, sent) = player()
        VarPlayerIntMapSetter.set(player, FOLLOWER_VARP, 5)
        assertTrue(sent.isEmpty())

        player.processedMapClock = 1
        player.flushVarpsSetBeforeFirstTick()

        val message = sent.single() as VarpSmall
        assertEquals(FOLLOWER_VARP_ID, message.id)
        assertEquals(5, message.value)
        assertTrue(player.varpsSetBeforeFirstTick.isEmpty())
    }

    @Test
    fun `an unchanged varp is not resent`() {
        val (player, sent) = player()
        VarPlayerIntMapSetter.set(player, FOLLOWER_VARP, 0)
        player.processedMapClock = 1
        player.flushVarpsSetBeforeFirstTick()
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `a full resync sends pending date vars once in id order`() {
        val (player, sent) = player()
        VarPlayerIntMapSetter.set(player, DATE_MINUTES, MINUTE)
        VarPlayerIntMapSetter.set(player, "varbit.date_milliseconds_past_minute", 827)
        VarPlayerIntMapSetter.set(player, "varbit.date_seconds_past_minute", 21)
        VarPlayerIntMapSetter.set(player, MAP_CLOCK, 790_475)

        player.resyncAllVars()

        assertEquals(VarpReset, sent.first())
        assertEquals(listOf(DATE_VARS, DATE_MINUTES, MAP_CLOCK).map(::varpId), sent.varpIds())

        sent.clear()
        player.processedMapClock = 1
        player.flushVarpsSetBeforeFirstTick()
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `writes after a full resync are still sent on the first tick`() {
        val (player, sent) = player()
        VarPlayerIntMapSetter.set(player, MAP_CLOCK, 790_475)
        player.resyncAllVars()
        VarPlayerIntMapSetter.set(player, MAP_CLOCK, 790_476)
        VarPlayerIntMapSetter.set(player, FOLLOWER_VARP, 5)

        sent.clear()
        player.processedMapClock = 1
        player.flushVarpsSetBeforeFirstTick()

        assertEquals(
            setOf(varpId(MAP_CLOCK) to 790_476, FOLLOWER_VARP_ID to 5),
            sent.varps().toSet(),
        )
        assertEquals(2, sent.size)
    }

    @Test
    fun `a same-minute relog still sends the saved date minute once`() {
        val (player, sent) = player()
        player.vars.backing[varpId(DATE_MINUTES)] = MINUTE
        VarPlayerIntMapSetter.set(player, MAP_CLOCK, 790_475)

        player.resyncAllVars()
        player.processedMapClock = 1
        player.flushVarpsSetBeforeFirstTick()

        assertEquals(
            listOf(varpId(DATE_MINUTES) to MINUTE, varpId(MAP_CLOCK) to 790_475),
            sent.varps(),
        )
    }

    private fun player(): Pair<Player, MutableList<Any>> {
        val client = RecordingClient()
        val player = Player()
        player.client = client
        return player to client.messages
    }

    private fun List<Any>.varps(): List<Pair<Int, Int>> = mapNotNull {
        when (it) {
            is VarpSmall -> it.id to it.value
            is VarpLarge -> it.id to it.value
            else -> null
        }
    }

    private fun List<Any>.varpIds(): List<Int> = varps().map { it.first }

    private fun varpId(internal: String): Int = internal.asRSCM(RSCMType.VARP)

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

    companion object {
        private const val FOLLOWER_VARP = "varp.follower_npc"
        private const val FOLLOWER_VARP_ID = 447
        private const val DATE_VARS = "varp.date_vars"
        private const val DATE_MINUTES = "varp.date_minutes"
        private const val MAP_CLOCK = "varp.map_clock"
        private const val MINUTE = 29_877_858

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
