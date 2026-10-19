package org.rsmod.api.game.process.player

import dev.openrune.ServerCacheManager
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

    private fun player(): Pair<Player, List<Any>> {
        val client = RecordingClient()
        val player = Player()
        player.client = client
        return player to client.messages
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

    companion object {
        private const val FOLLOWER_VARP = "varp.follower_npc"
        private const val FOLLOWER_VARP_ID = 447

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
