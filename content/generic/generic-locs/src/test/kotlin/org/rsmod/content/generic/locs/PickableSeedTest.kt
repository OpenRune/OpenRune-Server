package org.rsmod.content.generic.locs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.table.PickableObjectsRow
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PickableSeedTest {
    @Test
    fun `a seed roll replaces produce even when both could fit`() {
        val row = PickableObjectsRow.all().first { it.seed != null }
        val player = player()
        val seed = requireNotNull(row.seed)
        assertTrue(player.collectPickable(row, seed = true))
        assertEquals(1, player.inv.physicalCount(seed.internalName))
        assertEquals(0, player.inv.physicalCount(row.itemgiven.internalName))
        assertTrue(player.collectPickable(row, seed = false))
        assertEquals(1, player.inv.physicalCount(seed.internalName))
        assertEquals(row.itemamount, player.inv.physicalCount(row.itemgiven.internalName))
    }

    @Test
    fun `full inventory and seed stack overflow leave every item unchanged`() {
        val row = PickableObjectsRow.all().first { it.seed != null }
        val player = player()
        for (slot in player.inv.indices) player.inv[slot] = InvObj("obj.spade")
        var before = player.inv.indices.map { player.inv[it] }
        assertFalse(player.collectPickable(row, seed = true))
        assertFalse(player.collectPickable(row, seed = false))
        assertEquals(before, player.inv.indices.map { player.inv[it] })
        player.inv[0] = InvObj(requireNotNull(row.seed), Int.MAX_VALUE - 1)
        assertTrue(player.collectPickable(row, seed = true))
        assertEquals(Int.MAX_VALUE, player.inv.physicalCount(requireNotNull(row.seed).internalName))
        before = player.inv.indices.map { player.inv[it] }
        assertFalse(player.collectPickable(row, seed = true))
        assertEquals(before, player.inv.indices.map { player.inv[it] })
        assertEquals(0, player.inv.physicalCount(row.itemgiven.internalName))
    }

    @OptIn(InternalApi::class)
    private fun player() = Player().apply {
        client = SilentClient()
        uuid = 893L
        slotId = 1
        assignUid()
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
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
        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
