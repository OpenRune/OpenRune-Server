package org.rsmod.content.minigames.wintertodt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class WintertodtFletchingTest {
    @Test
    fun `fletching knife speeds roots from inventory or equipment`() {
        val player = player()
        assertEquals(4, fletchingKnifeInterval(player))

        player.inv[0] = InvObj("obj.fletching_knife", 1)
        assertEquals(3, fletchingKnifeInterval(player))

        player.inv[0] = null
        player.worn[5] = InvObj("obj.fletching_knife", 1)
        assertEquals(3, fletchingKnifeInterval(player))
    }

    private fun player(): Player =
        Player().apply {
            client = SilentClient()
            uuid = 919L
            slotId = 1
            assignUid()
            inv =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                    arrayOfNulls(28),
                )
            worn =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                    arrayOfNulls(14),
                )
        }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
