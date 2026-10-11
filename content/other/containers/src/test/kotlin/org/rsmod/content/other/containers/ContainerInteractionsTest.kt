package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ContainerInteractionsTest {
    @BeforeEach fun cache() { ServerCacheManager.init(240).close() }

    @Test fun `every native form has real definitions and consumers register without duplicate ops`() {
        val f = Fixture()
        for (s in ContainerCatalog.all) {
            assertTrue(ServerCacheManager.getItem(s.id)!!.certtemplate == 0, s.symbol)
            for (id in s.accepted) assertTrue(ServerCacheManager.getItem(id)?.certtemplate == 0, "${s.symbol}: $id")
        }
    }

    @Test fun `real Fill and Empty respect herb capacity and full inventory without losing contents`() {
        val f = Fixture(); val s = f.spec("obj.slayer_herb_sack"); val herb = "obj.unidentified_ranarr".asRSCM()
        f.put(0, s.symbol)
        for (i in 1..27) f.put(i, "obj.unidentified_ranarr")
        f.store.update(f.player, s, herb, 4)
        f.operate(0, HeldOp.Op1)
        assertEquals(30, f.store.count(f.player, s, herb)); assertEquals(1, f.player.inv.physicalCount("obj.unidentified_ranarr"))
        for (i in 1..27) f.put(i, "obj.lobster")
        f.operate(0, HeldOp.Op4)
        assertEquals(30, f.store.count(f.player, s, herb))
        f.player.inv[1] = null
        f.operate(0, HeldOp.Op4)
        assertEquals(29, f.store.count(f.player, s, herb)); assertEquals(1, f.player.inv.physicalCount("obj.unidentified_ranarr"))
    }

    @Test fun `item on container works in both orders and refuses unrelated items`() {
        for (reverse in listOf(false, true)) {
            val f = Fixture(); val s = f.spec("obj.gem_bag")
            f.put(0, s.symbol); f.put(1, "obj.uncut_sapphire")
            f.use(if (reverse)1 else 0, if (reverse)0 else 1)
            assertEquals(1, f.store.count(f.player, s, "obj.uncut_sapphire".asRSCM())); assertTrue(f.player.inv[1] == null)
            f.put(1, "obj.lobster"); f.use(0, 1)
            assertEquals("obj.lobster".asRSCM(), f.player.inv[1]!!.id)
        }
    }

    @Test fun `native opening swaps the selected form and contents survive save restore`() {
        val f = Fixture(); val s = f.spec("obj.slayer_herb_sack")
        f.put(0, s.symbol); f.put(1, "obj.unidentified_ranarr"); f.operate(0, HeldOp.Op1); f.operate(0, HeldOp.Op2)
        assertEquals("obj.slayer_herb_sack_open".asRSCM(), f.player.inv[0]!!.id)
        val snapshot = f.player.attr.toPersistentMap()
        val restored = Player(); restored.attr.putAllFromPersistence(snapshot)
        assertEquals(1, f.store.count(restored, s, "obj.unidentified_ranarr".asRSCM()))
        assertTrue(snapshot.containsKey("herb_sack"))
    }

    @Test fun `open auto storage commits once and cannot intercept bank additions`() {
        val f = Fixture(); val s = f.spec("obj.slayer_herb_sack_open"); f.put(0, s.symbol)
        val dry = f.player.invAdd(f.player.inv, "obj.unidentified_ranarr", 2, autoCommit = false)
        assertTrue(dry.success); assertEquals(0, f.store.count(f.player, s, "obj.unidentified_ranarr".asRSCM()))
        dry.commitAll(); assertEquals(2, f.store.count(f.player, s, "obj.unidentified_ranarr".asRSCM()))
        val bank = Inventory.create("inv.bank")
        assertTrue(f.player.invAdd(bank, "obj.unidentified_ranarr", 2).success)
        assertEquals(2, bank.physicalCount("obj.unidentified_ranarr")); assertEquals(2, f.store.count(f.player, s, "obj.unidentified_ranarr".asRSCM()))
    }

    @Test fun `native Hunter meat and tackle whitelists accept all doses and reject ordinary meat and notes`() {
        val f = Fixture(); val meat = f.spec("obj.hg_meatpouch_large")
        assertEquals(9, meat.accepted.size)
        assertTrue("obj.huntingbeast_barbed_meat".asRSCM() in meat.accepted)
        assertFalse(ServerCacheManager.getItem("obj.raw_rabbit".asRSCM())?.id in meat.accepted)
        val tackle = f.spec("obj.tackle_box")
        for (prefix in listOf("", "2")) for (dose in 1..4) assertTrue("obj.${dose}dose${prefix}fisherspotion".asRSCM() in tackle.accepted)
        f.put(0, "obj.gem_bag")
        val note = ServerCacheManager.getItem("obj.uncut_sapphire".asRSCM())!!.certlink
        f.player.inv[1] = InvObj(ServerCacheManager.getItem(note)!!)
        assertEquals(0, f.store.store(f.player, f.spec("obj.gem_bag"), note, 1))
        assertTrue(f.player.inv[1] != null)
    }

    @Test fun `six seed types and maximum seed stacks have no overflow or seventh-type insertion`() {
        val f = Fixture(); val s = f.spec("obj.seed_box"); f.put(0, s.symbol)
        val seeds = s.accepted.sorted().take(7)
        for (i in 0..5) f.store.update(f.player, s, seeds[i], Int.MAX_VALUE)
        assertEquals(0, f.store.room(f.player, s, seeds[6]))
        assertEquals(0, f.store.room(f.player, s, seeds[0]))
        assertEquals(6, f.store.contents(f.player, s).size)
    }

    @Test fun `native baskets fill independently cannot mix fruit and partially empty without losing produce`() {
        val f = Fixture(); f.put(0, "obj.basket_empty"); f.put(1, "obj.basket_empty")
        for (i in 2..8) f.put(i, "obj.banana")
        f.operate(0, HeldOp.Op1)
        assertEquals("obj.basket_banana_5".asRSCM(), f.player.inv[0]!!.id)
        f.operate(1, HeldOp.Op1)
        assertEquals("obj.basket_banana_2".asRSCM(), f.player.inv[1]!!.id)
        f.put(2, "obj.orange"); f.use(1, 2)
        assertEquals("obj.basket_banana_2".asRSCM(), f.player.inv[1]!!.id)
        for (i in 2..27) f.put(i, "obj.lobster")
        f.operate(0, HeldOp.Op3)
        assertEquals("obj.basket_banana_5".asRSCM(), f.player.inv[0]!!.id)
        f.player.inv[2] = null; f.player.inv[3] = null
        f.operate(0, HeldOp.Op3)
        assertEquals("obj.basket_banana_3".asRSCM(), f.player.inv[0]!!.id)
        assertEquals(2, f.player.inv.physicalCount("obj.banana"))
    }

    @Test fun `native sacks prefer cabbages and remove one preserves the encoded remainder`() {
        val f = Fixture(); f.put(0, "obj.sack_empty")
        for (i in 1..8) f.put(i, "obj.cabbage")
        f.put(9, "obj.onion"); f.operate(0, HeldOp.Op1)
        assertEquals("obj.sack_cabbage_8".asRSCM(), f.player.inv[0]!!.id)
        assertEquals(1, f.player.inv.physicalCount("obj.onion"))
        f.operate(0, HeldOp.Op2)
        assertEquals("obj.sack_cabbage_7".asRSCM(), f.player.inv[0]!!.id)
        assertEquals(1, f.player.inv.physicalCount("obj.cabbage"))
        for (s in ProduceContainers.all) {
            val native = ServerCacheManager.getItem(s.id)!!
            assertEquals("Fill", native.interfaceOptions[0], s.symbol)
        }
    }

    @Test fun `native bank-side use fills tackle box and alternates plank filling with banking`() {
        val f = Fixture(); val tackle = f.spec("obj.tackle_box"); f.put(0, tackle.symbol); f.put(1, "obj.feather")
        f.bus.publish(HeldBanksideEvents.Type(f.player, 0, ServerCacheManager.getItem(tackle.id)!!))
        assertEquals(1, f.store.count(f.player, tackle, "obj.feather".asRSCM()))
        assertTrue(f.player.inv[1] == null)
        val plank = f.spec("obj.plank_sack"); f.put(0, plank.symbol); f.put(1, "obj.plank_teak")
        f.bus.publish(HeldBanksideEvents.Type(f.player, 0, ServerCacheManager.getItem(plank.id)!!))
        assertEquals(1, f.store.count(f.player, plank, "obj.plank_teak".asRSCM()))
        f.bus.publish(HeldBanksideEvents.Type(f.player, 0, ServerCacheManager.getItem(plank.id)!!))
        assertEquals(0, f.store.count(f.player, plank, "obj.plank_teak".asRSCM()))
        assertEquals(1, f.player.invMap.getOrPut("inv.bank").physicalCount("obj.plank_teak"))
    }

    private class Fixture {
        val bus = EventBus(); val store = ContainerStore()
        val player = Player(client).apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100 }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
        private val producer: HeldInteractions
        init {
            val script = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(store))))) { script.startup() }
            with(ContainerScript(store)) { script.startup() }
            with(ProduceContainerScript()) { script.startup() }
            val constructor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }
            producer = constructor.newInstance(*constructor.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray()) as HeldInteractions
        }
        fun spec(symbol: String) = ContainerCatalog.byId[symbol.asRSCM()]!!
        fun put(slot: Int, symbol: String) { player.inv[slot] = InvObj(symbol) }
        fun operate(slot: Int, op: HeldOp) = launch { producer.interact(this, player.inv, slot, op) }
        fun use(first: Int, second: Int) = launch { HeldUInteractions(bus).interact(this, player.inv, ServerCacheManager.getItem(player.inv[first]!!.id)!!, first, ServerCacheManager.getItem(player.inv[second]!!.id)!!, second) }
        fun launch(action: suspend ProtectedAccess.() -> Unit) {
            val coroutine = GameCoroutine(); player.activeCoroutine = coroutine
            var result: Result<Unit>? = null
            val block: suspend () -> Unit = { action(ProtectedAccess(player, coroutine, context)) }
            block.startCoroutine(object : Continuation<Unit> { override val context = EmptyCoroutineContext; override fun resumeWith(outcome: Result<Unit>) { result = outcome } })
            checkNotNull(result).getOrThrow(); player.activeCoroutine = null
        }
    }
    companion object {
        val client = object : Client<Any, Any> {
            override fun write(message: Any) = Unit; override fun close() = Unit; override fun read(player: Player) = Unit; override fun flush() = Unit; override fun flushHighPriority() = Unit; override fun unregister(service: Any, player: Player) = Unit
        }
    }
}
