package org.rsmod.content.skills.cooking

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.coroutine.GameCoroutine
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
class PotatoButterTest {
    @Test
    fun `a full inventory keeps every other item and never duplicates the potato`() {
        val f = Fixture()
        for (slot in 0..25) {
            f.player.inv[slot] = InvObj("obj.spade")
        }
        f.player.inv[26] = InvObj("obj.pot_of_butter")
        f.player.inv[27] = InvObj("obj.potato_baked")
        f.butter()
        assertEquals(0, f.player.inv.count("obj.pot_of_butter"))
        assertEquals(0, f.player.inv.count("obj.potato_baked"))
        assertEquals(1, f.player.inv.count("obj.potato_butter"))
        assertEquals(26, f.player.inv.count("obj.spade"))
        assertEquals(400, f.xp())
    }

    @Test
    fun `the second use has nothing left to consume and makes nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.pot_of_butter")
        f.player.inv[1] = InvObj("obj.potato_baked")
        f.butter()
        f.butter()
        assertEquals(1, f.player.inv.count("obj.potato_butter"))
        assertEquals(400, f.xp())
    }

    @Test
    fun `a potato that vanished before the take leaves the butter untouched`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.pot_of_butter")
        f.butter()
        assertEquals(1, f.player.inv.count("obj.pot_of_butter"))
        assertEquals(0, f.player.inv.count("obj.potato_butter"))
        assertEquals(0, f.xp())
    }

    @Test
    fun `below the level neither item is touched`() {
        val f = Fixture(cookingLevel = 38)
        f.player.inv[0] = InvObj("obj.pot_of_butter")
        f.player.inv[1] = InvObj("obj.potato_baked")
        f.butter()
        assertEquals(1, f.player.inv.count("obj.pot_of_butter"))
        assertEquals(1, f.player.inv.count("obj.potato_baked"))
        assertEquals(0, f.player.inv.count("obj.potato_butter"))
        assertEquals(0, f.xp())
    }

    private class Fixture(cookingLevel: Int = 39) {
        val player =
            Player().apply {
                client = SilentClient()
                statMap.setBaseLevel("stat.cooking", cookingLevel.toByte())
                statMap.setCurrentLevel("stat.cooking", cookingLevel.toByte())
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
        private val events = EventBus()
        private val coroutine = GameCoroutine("potato-butter-test")
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(getEventBus = { events }, getAlignment = { TextAlignment() })
        private val initialXp = player.statMap.getFineXP("stat.cooking")

        init {
            with(PotatoButterEvents()) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun xp(): Int = player.statMap.getFineXP("stat.cooking") - initialXp

        fun butter() {
            val butter = checkNotNull(ServerCacheManager.getItem("obj.pot_of_butter".asRSCM()))
            val potato = checkNotNull(ServerCacheManager.getItem("obj.potato_baked".asRSCM()))
            run {
                assertTrue(
                    events.publish(this, HeldUEvents.Type(butter, 0, potato, 1)),
                    "The butter script did not run.",
                )
            }
        }

        private fun run(action: suspend ProtectedAccess.() -> Unit) {
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            var outcome: Result<Unit>? = null
            val body: suspend () -> Unit = { access.action() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        outcome = result
                    }
                },
            )
            checkNotNull(outcome).getOrThrow()
        }
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
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )
            ) {
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

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
