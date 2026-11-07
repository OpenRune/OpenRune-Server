package org.rsmod.content.minigames.wintertodt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.invtx.InvTransactions
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class WintertodtCrateTransactionTest {
    @Test
    fun `full inventory rolls back the crate and every reward`() {
        val player = player()
        for (slot in 0 until 27) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        player.inv[27] = InvObj(EXTRA_CRATE, 1)
        val rewards =
            listOf(
                WintertodtReward("obj.magic_logs", 10, noted = true),
                WintertodtReward("obj.dragon_axe", 1),
            )
        val before = player.inv.objs.toList()

        assertFalse(openExtraCrateTransaction(player, 27, rewards))

        assertEquals(before, player.inv.objs.toList())
        assertEquals(1, player.inv.count(EXTRA_CRATE))
        assertEquals(0, player.inv.count("obj.dragon_axe"))
    }

    @Test
    fun `successful open replaces the crate with its rewards`() {
        val player = player()
        for (slot in 0 until 26) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        player.inv[27] = InvObj(EXTRA_CRATE, 1)

        assertTrue(openExtraCrateTransaction(player, 27, listOf(WintertodtReward("obj.dragon_axe", 1))))

        assertEquals(0, player.inv.count(EXTRA_CRATE))
        assertEquals(1, player.inv.count("obj.dragon_axe"))
    }

    private fun player(): Player =
        Player().apply {
            client = SilentClient()
            uuid = 918L
            slotId = 1
            assignUid()
            currentMapClock = 100
            processedMapClock = 100
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
            val field =
                Class.forName("org.rsmod.api.invtx.InvTransactionsScriptKt")
                    .getDeclaredField("cachedInventoryTransactions")
                    .apply { isAccessible = true }
            field.set(null, InvTransactions.from())
        }
    }
}
