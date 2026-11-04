package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.InventoryServerType
import dev.openrune.types.NpcMode
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import net.rsprot.protocol.game.outgoing.zone.payload.MapAnim
import net.rsprot.protocol.game.outgoing.zone.payload.MapProjAnimV2
import net.rsprot.protocol.game.outgoing.zone.payload.SoundArea
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.delivery.DeliveryClock
import org.rsmod.api.delivery.DeliveryLineRegistry
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeMinerScript
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeRockfallScript
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeVeinScript
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class MotherlodeMiningTest {
    @Test fun `timed vein depletion emits the empty ore sound once at the vein`() {
        val f = Fixture()
        val vein = f.loc(MotherlodeConstants.VEIN_SINGLE)
        val state = MotherlodeVeinState(f.clock, f.random)
        val machine = MotherlodeMachine(f.locs, f.npcs, f.random, f.collision)
        val script = MotherlodeVeinScript(f.locs, XpModifiers(emptySet()), InvisibleLevels(emptySet()),
            f.clock, DeliveryManager(DeliveryLineRegistry(), DeliveryClock()), state, machine, f.world, PlayerList())
        with(script) { f.context.startup() }
        state.startIfAbsent(vein, upperFloor = true)
        f.clock.cycle += MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS.first
        f.events.publish(GameLifecycle.LateCycle)
        val sound = f.updates.updatedZones.values.flatMap { it.toList() }.filterIsInstance<SoundArea>().single()
        assertEquals(2661, sound.id)
        assertEquals(10, sound.delay)
        assertEquals(1, sound.range)
        assertNotEquals(vein.id, f.locs.findExact(vein.coords, vein.shape)?.id)
        f.events.publish(GameLifecycle.LateCycle)
        assertEquals(1, f.updates.updatedZones.values.flatMap { it.toList() }.filterIsInstance<SoundArea>().size)
    }

    @Test fun `rockfall respawns with its effect only after the full wait`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1)
        assertTrue(f.rockfalls.clear(rockfall))
        assertFalse(f.rockfalls.clear(rockfall))
        assertNull(f.locs.findExact(rockfall.coords, rockfall.shape))
        f.clock.cycle += MotherlodeConstants.ROCKFALL_RESPAWN_TICKS - 1
        f.rockfalls.tick()
        assertNull(f.locs.findExact(rockfall.coords, rockfall.shape))
        val falling = f.updates.updatedZones.values.flatMap { it.toList() }
        val projectiles = falling.filterIsInstance<MapProjAnimV2>()
        assertEquals(2, projectiles.size)
        assertTrue(projectiles.all { it.id == 645 && it.endTime == 30 && it.endHeight == 25 })
        assertEquals(listOf(1000, 900), projectiles.map { it.startHeight })
        assertEquals(listOf(0, 10), projectiles.map { it.startTime })
        val impact = falling.filterIsInstance<MapAnim>().single()
        assertEquals(305, impact.id)
        assertEquals(30, impact.delay)
        val sound = falling.filterIsInstance<SoundArea>().single()
        assertEquals(360, sound.id)
        assertEquals(30, sound.delay)
        f.clock.tick()
        f.rockfalls.tick()
        assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
        val effects = f.updates.updatedZones.values.flatMap { it.toList() }.filterIsInstance<MapAnim>()
        assertEquals(1, effects.size)
        f.rockfalls.tick()
        assertEquals(1, f.updates.updatedZones.values.flatMap { it.toList() }.filterIsInstance<MapAnim>().size)
    }

    @Test fun `player must swing for a full tick before clearing and stop on completion`() {
        for (inputPhase in listOf(false, true)) {
            val f = Fixture()
            val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1)
            val messages = mutableListOf<Any>()
            val player = minerPlayer(f.clock.cycle, inputPhase, messages)
            val script = MotherlodeRockfallScript(f.rockfalls, XpModifiers(emptySet()))
            val coroutine = player.launch {
                with(script) { ProtectedAccess(player, this@launch, unusedContext()).mine(rockfall) }
            }
            assertTrue(coroutine.isSuspended)
            assertTrue(player.pendingSequence.id > 0)
            assertTrue(messages.filterIsInstance<SynthSound>().isEmpty())
            assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
            player.processedMapClock = f.clock.cycle
            coroutine.advance()
            assertTrue(coroutine.isSuspended)
            assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
            f.clock.tick()
            player.currentMapClock = f.clock.cycle
            player.processedMapClock = f.clock.cycle
            coroutine.advance()
            assertTrue(coroutine.isIdle)
            assertNull(f.locs.findExact(rockfall.coords, rockfall.shape))
            assertEquals(EntitySeq.ZERO, player.pendingSequence)
            assertTrue(player.statMap.getFineXP("stat.mining") > 0)
            assertEquals("synth.mine_ore".asRSCM(), messages.filterIsInstance<SynthSound>().single().id)
        }
    }

    @Test fun `arrival and competing miners cannot clear a rockfall instantly or leave a swing running`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1)
        val messages = mutableListOf<Any>()
        val player = minerPlayer(f.clock.cycle, true, messages).apply {
            lastMovement = currentMapClock - 1
            processedMapClock = currentMapClock
        }
        val script = MotherlodeRockfallScript(f.rockfalls, XpModifiers(emptySet()))
        val coroutine = player.launch {
            with(script) { ProtectedAccess(player, this@launch, unusedContext()).mine(rockfall) }
        }
        assertEquals(EntitySeq.NULL, player.pendingSequence)
        f.clock.tick()
        player.currentMapClock = f.clock.cycle
        player.processedMapClock = f.clock.cycle
        coroutine.advance()
        assertTrue(player.pendingSequence.id > 0)
        assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
        assertTrue(f.rockfalls.clear(rockfall))
        f.clock.tick()
        player.currentMapClock = f.clock.cycle
        player.processedMapClock = f.clock.cycle
        coroutine.advance()
        assertEquals(EntitySeq.ZERO, player.pendingSequence)
        assertEquals(0, player.statMap.getFineXP("stat.mining"))
        assertTrue(messages.filterIsInstance<SynthSound>().isEmpty())
    }

    @Test fun `cancelling a swing stops the animation and leaves the rockfall intact`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1)
        val player = minerPlayer(f.clock.cycle, false)
        val script = MotherlodeRockfallScript(f.rockfalls, XpModifiers(emptySet()))
        player.launch {
            with(script) { ProtectedAccess(player, this@launch, unusedContext()).mine(rockfall) }
        }
        player.cancelActiveCoroutine()
        assertEquals(EntitySeq.ZERO, player.pendingSequence)
        assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
    }

    @Test fun `ambient miners animate while leaving the vein available`() {
        val f = Fixture()
        val vein = f.loc(MotherlodeConstants.VEIN_SINGLE)
        val npc = f.miner()
        repeat(80) { f.miners.mine(npc); f.clock.tick() }
        assertTrue(f.info(npc).sequences.any { it == "seq.royal_dwarf_mining".asRSCM() })
        assertEquals(vein.id, f.locs.findExact(vein.coords, vein.shape)?.id)
        assertEquals(vein.coords, npc.pendingFaceSquare)
        assertEquals(NpcMode.None, npc.mode)
        assertEquals(1, npc.aiTimer)
    }

    @Test fun `ambient miners wait a tick after swinging before clearing a rockfall`() {
        val f = Fixture(clearRockfalls = true)
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1)
        val npc = f.miner()
        f.miners.mine(npc)
        f.clock.tick()
        f.miners.mine(npc)
        assertTrue(f.info(npc).sequences.isNotEmpty())
        assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
        f.clock.tick()
        f.miners.mine(npc)
        assertNull(f.locs.findExact(rockfall.coords, rockfall.shape))
    }

    @Test fun `Runite Minor uses a human pickaxe animation`() {
        val f = Fixture()
        f.loc(MotherlodeConstants.VEIN_SINGLE)
        val npc = f.miner("npc.motherlode_miner_2")
        repeat(3) { f.miners.mine(npc); f.clock.tick() }
        assertTrue(f.info(npc).sequences.contains("seq.human_mining_rune_pickaxe_wall".asRSCM()))
        assertFalse(f.info(npc).sequences.contains("seq.royal_dwarf_mining".asRSCM()))
    }

    @Test fun `ambient mining shouts remain infrequent`() {
        val f = Fixture()
        f.loc(MotherlodeConstants.VEIN_SINGLE)
        val npc = f.miner()
        repeat(400) { f.miners.mine(npc); f.clock.tick() }
        assertTrue(f.info(npc).messages.size in 1..4)
        assertTrue(f.info(npc).messages.all { it == "Pay-dirt!" })
    }

    internal class Fixture(clearRockfalls: Boolean = false, randomSource: GameRandom? = null) {
        val clock = MapClock(100)
        val events = EventBus()
        val context = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
        val storage = LocZoneStorage()
        val updates = ZoneUpdateMap()
        val collision = CollisionFlagMap()
        val npcList = NpcList()
        val playerList = PlayerList()
        val npcRegistry = NpcRegistry(npcList, collision, events)
        val npcs = NpcRepository(clock, npcRegistry, npcList)
        private val normal = LocRegistryNormal(updates, collision, storage)
        private val regions = RegionRegistry(RegionListSmall(), RegionListLarge(), RegionListWorldEntity(), normal, collision, storage,
            npcRegistry, ControllerRegistry(clock, ControllerList()), ZonePlayerActivityBitSet())
        val locs = LocRepository(clock, LocRegistry(storage, normal, LocRegistryRegion(updates, collision, storage, regions)), regions)
        val world = WorldRepository(updates)
        val rockfalls = MotherlodeRockfalls(clock, locs, world, playerList, PlayerHitModifier {}, npcList, StepFactory(collision), events)
        val random = randomSource ?: object : GameRandom {
            override fun of(maxExclusive: Int): Int = if (maxExclusive == 8 && !clearRockfalls) 1 else 0
            override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
            override fun randomDouble(): Double = 0.0
        }
        val miners = MotherlodeMinerScript(clock, random, npcList, locs, RouteFactory(collision), rockfalls)

        init {
            for (x in 3740..3770) for (z in 5670..5690) collision.allocateIfAbsent(x, z, 0)
            with(miners) { context.startup() }
        }

        fun miner(symbol: String = "npc.motherlode_miner_1"): Npc = Npc(symbol, CoordGrid(3755, 5680)).apply {
            infoProtocol = RecordingInfo()
            npcs.add(this, Int.MAX_VALUE)
        }

        fun info(npc: Npc) = npc.infoProtocol as RecordingInfo

        fun loc(symbol: String, coords: CoordGrid = CoordGrid(3756, 5680)): BoundLocInfo {
            val loc = LocInfo(2, coords, LocEntity(symbol.asRSCM(), 10, 0))
            storage.mapLocs[ZoneKey.from(loc.coords), LocZoneKey(ZoneGrid.from(loc.coords), loc.layer)] = loc.entity
            return BoundLocInfo(loc, checkNotNull(ServerCacheManager.getObject(loc.id)))
        }
    }

    internal class RecordingInfo : NpcInfoProtocol by NoopNpcInfo {
        val messages = mutableListOf<String>()
        val sequences = mutableListOf<Int>()
        override fun setSay(text: String) { messages += text }
        override fun setSequence(seq: Int, delay: Int) { sequences += seq }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }

        private fun minerPlayer(cycle: Int, inputPhase: Boolean, messages: MutableList<Any> = mutableListOf()) = Player().apply {
            pendingSequence = EntitySeq.NULL
            currentMapClock = cycle
            processedMapClock = cycle - if (inputPhase) 1 else 0
            coords = CoordGrid(3755, 5680)
            inv = Inventory(InventoryServerType(size = 28), arrayOfNulls(28))
            worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14))
            inv[0] = InvObj("obj.bronze_pickaxe")
            client = object : Client<Any, Any> {
                override fun write(message: Any) { messages += message }
                override fun close() {}
                override fun read(player: Player) {}
                override fun flush() {}
                override fun flushHighPriority() {}
                override fun unregister(service: Any, player: Player) {}
            }
        }

        private fun unusedContext() = ProtectedAccessContext(
            getRandom = { error("Unexpected random access") },
            getEventBus = { error("Unexpected event access") },
            getNpcList = { error("Unexpected NPC access") },
            getPlayerList = { error("Unexpected player-list access") },
            getCollision = { error("Unexpected collision access") },
            getAreaChecker = { error("Unexpected area access") },
            getAlignment = { error("Unexpected dialogue access") },
            getLocInteractions = { error("Unexpected loc access") },
            getNpcInteractions = { error("Unexpected NPC interaction") },
            getPlayerInteractions = { error("Unexpected player interaction") },
            getHeldInteractions = { error("Unexpected held interaction") },
            getWornInteractions = { error("Unexpected worn interaction") },
            getMusicPlayer = { error("Unexpected music access") },
            getMarketPrices = { error("Unexpected market access") },
            getInstantHitProcessor = { error("Unexpected damage access") },
            getTeleportValidator = { error("Unexpected teleport access") },
            getHitModifier = { error("Unexpected hit modifier") },
        )
    }
}
