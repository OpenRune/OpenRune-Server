package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ContainerGroundPickupTest {
    @BeforeEach fun cache() { ServerCacheManager.init(240).close() }

    @Test fun `actual ground pickup fills open herb sack once even with full inventory`() {
        val f = Fixture(); f.fill(); f.take(f.loot)
        assertEquals(2, f.store.count(f.player, f.spec, f.loot.type)); verify(f.repo).del(f.loot)
        assertEquals(28, f.player.inv.occupiedSpace())
    }

    @Test fun `closed sack full capacity and already claimed ground loot cannot mutate storage`() {
        for (mode in 0..2) {
            val f = Fixture(); f.fill()
            when (mode) {
                0 -> f.player.inv[0] = InvObj("obj.slayer_herb_sack")
                1 -> f.store.update(f.player, f.spec, f.loot.type, 29)
                2 -> `when`(f.repo.del(f.loot)).thenReturn(false)
            }
            f.take(f.loot)
            assertEquals(if (mode == 1)29 else 0, f.store.count(f.player, f.spec, f.loot.type))
            if (mode != 2)verify(f.repo, never()).del(f.loot)
        }
    }

    @Test fun `late ownership denial and inventory change leave ground item intact after pickup delay`() {
        for (mode in 0..1) {
            val f = Fixture(); f.player.inv.fillNulls()
            f.player.coords = f.loot.coords.translateX(1); f.take(f.loot)
            if (mode == 0)f.denial = "That item belongs to someone else." else f.fill().also { f.player.inv[0] = InvObj("obj.lobster") }
            f.finish(); verify(f.repo, never()).del(f.loot)
            assertEquals(0, f.store.count(f.player, f.spec, f.loot.type))
        }
    }

    @Test fun `successful delayed pickup completes while death and logout cancel it`() {
        for (mode in 0..2) {
            val f = Fixture(); f.player.coords = f.loot.coords.translateX(1); f.take(f.loot)
            if (mode == 1)f.player.pendingLogout = true
            if (mode == 2)f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
            f.finish()
            if (mode == 0) {
                verify(f.repo).del(f.loot)
                assertEquals(2, f.player.inv.physicalCount("obj.unidentified_ranarr"))
            } else verify(f.repo, never()).del(f.loot)
        }
    }

    @Test fun `alternate pickup storage receives loot only after successful removal`() {
        val target = Inventory.create("inv.bank"); var commits = 0
        val hook = object : PlayerObjTakeStorageHook {
            override fun inventory(player: Player, obj: Obj, objType: ItemServerType) = target
            override fun committed(player: Player, obj: Obj, objType: ItemServerType) { commits++ }
        }
        val f = Fixture(setOf(hook)); f.fill(); f.take(f.loot)
        assertEquals(2, target.physicalCount("obj.unidentified_ranarr")); assertEquals(1, commits)
        assertEquals(0, f.store.count(f.player, f.spec, f.loot.type))
    }
    private class Fixture(storage: Set<PlayerObjTakeStorageHook> = emptySet()) {
        val player = Player(client).apply { coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val store = ContainerStore(); val spec = ContainerCatalog.byId["obj.slayer_herb_sack_open".asRSCM()]!!
        val loot = Obj.fromServer(MapClock(100), player.coords, InvObj("obj.unidentified_ranarr", 2))
        val repo = mock(ObjRepository::class.java)
        val bus = EventBus(); var denial: String? = null
        val launcher: ProtectedAccessLauncher
        private val queues: PlayerQueueProcessor
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(context) }
            launcher = ProtectedAccessLauncher(factory); queues = PlayerQueueProcessor(bus, launcher)
            `when`(repo.del(loot)).thenReturn(true)
            val ctx = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(store))))) { ctx.startup() }
            with(ObjTakePlugin(repo, PlayerObjTakeValidator(setOf(PlayerObjTakeValidateHook { _, _, _ -> denial })), storage)) { ctx.startup() }
        }
        fun fill() { player.inv[0] = InvObj(spec.symbol); for (i in 1..27)player.inv[i] = InvObj("obj.lobster") }
        fun take(obj: Obj) {
            val producer = ObjInteractions(bus); producer.interact(player, obj, InteractionOp.Op3)
            launcher.launch(player) { assertTrue(bus.publish(this, producer.opTrigger(obj, InteractionOp.Op3)!!)) }
        }
        fun finish() { repeat(3) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance(); queues.process(player) } }
    }
    companion object {
        val client = object : Client<Any, Any> {
            override fun write(message: Any) = Unit; override fun close() = Unit; override fun read(player: Player) = Unit; override fun flush() = Unit; override fun flushHighPriority() = Unit; override fun unregister(service: Any, player: Player) = Unit
        }
    }
}
