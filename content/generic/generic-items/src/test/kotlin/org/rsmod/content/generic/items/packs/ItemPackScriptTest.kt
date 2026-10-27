package org.rsmod.content.generic.items.packs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
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
class ItemPackScriptTest {
    @Test
    fun `opening works on a full inventory and merges into an existing output stack`() {
        val f = Fixture(PACK)
        f.put(1, f.note, 50)
        for (slot in 2 until 28) f.put(slot, DAGGER_ID, 1)
        f.open(0)
        assertEquals(mapOf(f.note to 150, DAGGER_ID to 26), f.totals())
    }

    @Test
    fun `opening works on a full inventory without an output stack`() {
        val f = Fixture(PACK)
        for (slot in 1 until 28) f.put(slot, DAGGER_ID, 1)
        f.open(0)
        assertEquals(mapOf(f.note to 100, DAGGER_ID to 27), f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `an output stack that would overflow by one rolls back and one that fits exactly succeeds`() {
        val overflow = Fixture(PACK, DAGGER)
        overflow.put(2, overflow.note, Int.MAX_VALUE - 99)
        overflow.open(0)
        assertEquals(
            mapOf(PACK_ID to 1, DAGGER_ID to 1, overflow.note to Int.MAX_VALUE - 99),
            overflow.totals(),
        )

        val exact = Fixture(PACK, DAGGER)
        exact.put(2, exact.note, Int.MAX_VALUE - 100)
        exact.open(0)
        assertEquals(mapOf(DAGGER_ID to 1, exact.note to Int.MAX_VALUE), exact.totals())
    }

    @Test
    fun `a failed open of one pack keeps every other queued pack`() {
        val f = Fixture(PACK, PACK, PACK, DAGGER)
        f.put(4, f.note, Int.MAX_VALUE - 100)
        f.open(0)
        f.drain()
        assertEquals(mapOf(f.note to Int.MAX_VALUE, PACK_ID to 2, DAGGER_ID to 1), f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `queued packs open at most one per cycle and conserve material every cycle`() {
        val f = Fixture(PACK, PACK, PACK, DAGGER)
        f.open(0)
        f.assertConserved(3)
        f.tick()
        assertEquals(2, f.packs(), "the queued open must not fire on the cycle it was scheduled")
        repeat(10) {
            val before = f.packs()
            f.tick()
            assertTrue(before - f.packs() <= 1)
            f.assertConserved(3)
        }
        assertEquals(mapOf(f.note to 300, DAGGER_ID to 1), f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `repeated requests in the same tick never duplicate or lose a pack`() {
        val f = Fixture(PACK, PACK, PACK)
        f.open(0)
        f.open(1)
        f.open(2)
        f.assertConserved(3)
        repeat(10) {
            f.tick()
            f.assertConserved(3)
        }
        assertEquals(mapOf(f.note to 300), f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `a stale request for a pack that is gone changes nothing`() {
        val f = Fixture(PACK, DAGGER)
        val stale = checkNotNull(f.player.inv[0])
        f.open(0)
        f.open(0, stale)
        assertEquals(mapOf(f.note to 100, DAGGER_ID to 1), f.totals())
    }

    @Test
    fun `packs that vanish after queueing are never opened or replaced`() {
        val f = Fixture(PACK, PACK, DAGGER)
        f.open(0)
        f.player.inv[1] = null
        repeat(10) { f.tick() }
        assertEquals(mapOf(f.note to 100, DAGGER_ID to 1), f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `a pack swapped for another item after queueing leaves that item untouched`() {
        val f = Fixture(PACK, PACK)
        f.open(0)
        f.player.inv[1] = InvObj(DAGGER, 1)
        repeat(10) { f.tick() }
        assertEquals(mapOf(f.note to 100, DAGGER_ID to 1), f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    private class Fixture(vararg items: String) {
        val player =
            Player().apply {
                client = SilentClient()
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
        val note = checkNotNull(ServerCacheManager.getItem(PLANT_POT_COMPOST_ID)).certlink
        private val events = EventBus()
        private val launcher =
            ProtectedAccessLauncher(
                ProtectedAccessContextFactory(
                    random = mockk(),
                    eventBus = events,
                    npcList = mockk(),
                    playerList = mockk(),
                    collision = mockk(),
                    areaChecker = mockk(),
                    alignment = TextAlignment(),
                    locInteractions = mockk(),
                    npcInteractions = mockk(),
                    playerInteractions = mockk(),
                    heldInteractions = mockk(),
                    wornInteractions = mockk(),
                    musicPlayer = mockk(),
                    marketPrices = mockk(),
                    instantHitProcessor = mockk(),
                    teleportValidator = mockk(),
                    hitModifier = mockk(),
                )
            )
        private val processor = PlayerQueueProcessor(events, launcher)

        init {
            items.forEachIndexed { slot, name -> player.inv[slot] = InvObj(name, 1) }
            with(ItemPackScript()) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun put(slot: Int, id: Int, count: Int) {
            player.inv[slot] = InvObj(checkNotNull(ServerCacheManager.getItem(id)), count)
        }

        fun totals(): Map<Int, Int> {
            val totals = mutableMapOf<Int, Int>()
            for (slot in 0 until 28) {
                val obj = player.inv[slot] ?: continue
                totals.merge(obj.id, obj.count, Int::plus)
            }
            return totals
        }

        fun packs(): Int = totals()[PACK_ID] ?: 0

        fun assertConserved(initialPacks: Int) {
            val totals = totals()
            assertEquals(100 * (initialPacks - packs()), totals[note] ?: 0, totals.toString())
        }

        fun open(slot: Int, stale: InvObj? = null) {
            val obj = stale ?: checkNotNull(player.inv[slot])
            val type = checkNotNull(ServerCacheManager.getItem(obj.id))
            assertTrue(
                launcher.launch(player) {
                    check(events.publish(this, HeldObjEvents.Op1(slot, obj, type, player.inv)))
                }
            )
        }

        fun drain() = repeat(10) { tick() }

        /** One game cycle: the real queue processor runs, then the map clock advances. */
        fun tick() {
            processor.process(player)
            player.currentMapClock++
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
        private const val PACK = "obj.pack_plant_pot_compost"
        private const val DAGGER = "obj.bronze_dagger"
        private val PACK_ID by lazy { PACK.asRSCM(RSCMType.OBJ) }
        private val DAGGER_ID by lazy { DAGGER.asRSCM(RSCMType.OBJ) }
        private val PLANT_POT_COMPOST_ID by lazy { "obj.plantpot_compost".asRSCM(RSCMType.OBJ) }
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

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
