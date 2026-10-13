package org.rsmod.content.generic.locs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PickablesTailoredBushTest {
    private val events = EventBus()
    private val coroutine = GameCoroutine("pickables-test")
    private val client = RecordingClient()
    private val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events })

    @OptIn(InternalApi::class)
    private val player =
        Player().apply {
            client = this@PickablesTailoredBushTest.client
            uuid = 5L
            observerUUID = 5L
            slotId = 1
            assignUid()
            coords = BUSH_COORDS
            currentMapClock = 100
            processedMapClock = 100
        }

    private val restored = mutableListOf<() -> Unit>()

    @BeforeAll
    fun load() {
        ServerCacheManager.init(240).close()
        for ((owner, name) in
            listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
            val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
            val old = field.get(null)
            restored += { field.set(null, old) }
        }
        val oldStorage = InvVirtualStorageHolder.instance
        restored += { InvVirtualStorageHolder.instance = oldStorage }
        with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
            ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
        }
        player.inv =
            Inventory(
                checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                arrayOfNulls(28),
            )
        val clock = MapClock(100)
        val updates = ZoneUpdateMap()
        val collision = CollisionFlagMap()
        val storage = LocZoneStorage()
        val normal = LocRegistryNormal(updates, collision, storage)
        val regions =
            RegionRegistry(
                RegionListSmall(),
                RegionListLarge(),
                RegionListWorldEntity(),
                normal,
                collision,
                storage,
                NpcRegistry(NpcList(), collision, events),
                ControllerRegistry(clock, ControllerList()),
                ZonePlayerActivityBitSet(),
            )
        val locs =
            LocRepository(
                clock,
                LocRegistry(storage, normal, LocRegistryRegion(updates, collision, storage, regions)),
                regions,
            )
        val objs = ObjRepository(clock, ObjRegistry(updates))
        with(Pickables(objs, locs)) {
            ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
        }
    }

    @AfterAll
    fun reset() {
        restored.asReversed().forEach { it() }
        restored.clear()
    }

    @Test
    fun `the tailored bush gives two berries and then runs dry until it is reset`() {
        for (slot in 0 until 28) player.inv[slot] = null
        setPicks(0)
        pick()
        pick()
        assertEquals(2, player.inv.count(BERRIES))
        assertEquals(2, player.vars[PICKS])
        pick()
        assertEquals(2, player.inv.count(BERRIES))
        assertTrue(
            client.messages.any { it.toString().contains("Maybe you should try another bush") }
        )
        setPicks(0)
        pick()
        assertEquals(3, player.inv.count(BERRIES))
    }

    @Test
    fun `the tailored bush keeps its berries while the inventory is full`() {
        for (slot in 0 until 28) player.inv[slot] = InvObj("obj.coins", 1)
        setPicks(0)
        pick()
        assertEquals(0, player.inv.count(BERRIES))
        assertEquals(0, player.vars[PICKS])
    }

    private fun setPicks(value: Int) {
        VarPlayerIntMapSetter.set(player, PICKS, value)
    }

    private fun pick() {
        val base = ServerCacheManager.getObject(TAILORED.asRSCM())!!
        val visible =
            ServerCacheManager.getObject(
                when (player.vars[PICKS]) {
                    0 -> "loc.fai_varrock_cadavabush_2"
                    1 -> "loc.fai_varrock_cadavabush_1"
                    else -> "loc.fai_varrock_cadavabush_0"
                }.asRSCM()
            )!!
        val loc = BoundLocInfo(LocInfo(10, BUSH_COORDS, LocEntity(base.id, 10, 0)), base)
        val vis = BoundLocInfo(LocInfo(10, BUSH_COORDS, LocEntity(visible.id, 10, 0)), visible)
        var failure: Throwable? = null
        player.activeCoroutine = coroutine
        val body: suspend () -> Unit = {
            assertTrue(
                events.publish(
                    ProtectedAccess(player, coroutine, context),
                    LocEvents.Op1(loc, vis, visible),
                )
            )
        }
        body.startCoroutine(
            object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    failure = result.exceptionOrNull()
                }
            }
        )
        repeat(20) {
            if (coroutine.isIdle) return@repeat
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            coroutine.advance()
        }
        failure?.let { throw it }
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

    private companion object {
        const val TAILORED = "loc.fai_varrock_cadavabush_tailored"
        const val PICKS = "varbit.cadavabush"
        const val BERRIES = "obj.cadavaberries"
        val BUSH_COORDS = CoordGrid(3264, 3366, 0)
    }
}
