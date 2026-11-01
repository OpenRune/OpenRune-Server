package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import net.rsprot.protocol.game.outgoing.zone.payload.LocAnim
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.GameCycle
import org.rsmod.api.game.process.player.PlayerInteractionProcessor
import org.rsmod.api.game.process.player.PlayerMainProcess
import org.rsmod.api.game.process.player.PlayerTimerProcessor
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.registry.zone.ZoneUpdateTransformer
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.utils.logging.GameExceptionHandler
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.ShuffledPlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class RefiningTickOrderTest {
    @Test
    fun `hit tick tool click arms and consumes sweet spot before work`() {
        val f = Fixture()
        f.player.apply {
            workStation = 3
            preformCompletion = 870
            preformTemperature = 105
            sweetSpotUntil = f.clock.cycle + 5
            sweetSpotsOffered = 1
            sweetSpotsLastSection = 3
        }
        f.cycle { f.operate("loc.giants_foundry_polishing_wheel") }
        assertEquals(930, f.player.preformCompletion)
        assertEquals(53, f.player.preformQuality)
        assertEquals(88, f.player.preformTemperature)
        assertFalse(f.player.workSweetSpotArmed)
        assertEquals(0, f.player.sweetSpotUntil)
        val output = f.output()
        assertTrue(output.contains("2192"), output)
    }

    @Test
    fun `hit tick reclick misses this action not the next one`() {
        val f = Fixture()
        f.player.apply {
            workStation = 2
            preformCompletion = 350
            preformTemperature = 450
        }
        f.cycle { f.operate("loc.giants_foundry_grindstone") }
        assertEquals(350, f.player.preformCompletion)
        assertEquals(450, f.player.preformTemperature)
        assertFalse(f.player.workOverworked)
        f.player.workNextAction = f.clock.cycle
        f.cycle()
        assertEquals(360, f.player.preformCompletion)
        assertEquals(465, f.player.preformTemperature)
    }

    @Test
    fun `reheat click resets this tick ramp and suppresses only this tick animation`() {
        for ((station, loc, sign) in listOf(
            Triple(4, "loc.giants_foundry_lava_pool", 1),
            Triple(6, "loc.giants_foundry_waterfall", -1),
        )) {
            val f = Fixture()
            f.player.apply {
                workStation = station
                workHeatTicks = 12
                preformTemperature = 650
            }
            val beforeAnimation = f.player.pendingSequence
            f.cycle { f.operate(loc) }
            assertEquals(650 + sign * 7, f.player.preformTemperature)
            assertEquals(beforeAnimation, f.player.pendingSequence)
            assertEquals(2, f.player.workHeatTicks)
            f.cycle()
            assertNotEquals(beforeAnimation, f.player.pendingSequence)
            assertEquals(650 + sign * 15, f.player.preformTemperature)
        }
    }

    @Test
    fun `new tool keeps three tick start delay and new slow heat has no immediate step`() {
        val f = Fixture(scheduleWork = false)
        f.player.workStation = 0
        f.cycle { f.operate("loc.giants_foundry_grindstone") }
        val firstAction = f.player.workNextAction
        assertEquals(103, firstAction)
        assertEquals(0, f.player.preformCompletion)
        repeat(2) { f.cycle() }
        assertEquals(0, f.player.preformCompletion)
        f.cycle()
        assertEquals(10, f.player.preformCompletion)

        val heat = Fixture(scheduleWork = false)
        heat.player.workStation = 0
        heat.cycle { heat.operate("loc.giants_foundry_lava_pool") }
        assertEquals(450, heat.player.preformTemperature)
        heat.cycle()
        assertEquals(457, heat.player.preformTemperature)
    }

    @Test
    fun `ordinary soft timers still run before the interaction and work runs after it`() {
        val f = Fixture()
        f.player.preformTemperature = 450
        f.player.softTimer(PREFORM_COOLING_TIMER, 1)
        f.cycle {
            assertEquals(449, f.player.preformTemperature)
            assertEquals(0, f.player.preformCompletion)
            f.operate("loc.giants_foundry_grindstone")
        }
        assertEquals(449, f.player.preformTemperature)
        assertEquals(0, f.player.preformCompletion)
    }

    @Test
    fun `quest completion opens the finished dialogue on the final work tick`() {
        val f = Fixture()
        f.player.apply {
            sleepingGiants = SleepingGiants.MAKING_SWORD
            sleepingGiantsTutorial = SleepingGiants.TUTORIAL_PREFORM_TAKEN
            workStation = 3
            preformCompletion = 990
            preformTemperature = 42
        }
        f.cycle()
        assertEquals(1000, f.player.preformCompletion)
        assertTrue(f.player.ui.containsModal("interface.chat_right"))
    }

    @Test
    fun `full game tick keeps hammer effects and progress on tick 100 not 99`() {
        val f = Fixture()
        f.clock.cycle = 99
        f.player.apply {
            workStation = 1
            preformTemperature = 800
            VarPlayerIntMapSetter.set(this, seedVarbit(0), 0)
        }

        f.fullCycle()
        assertEquals(100, f.clock.cycle)
        assertEquals(0, f.player.preformCompletion)
        assertTrue(f.worldEffects().isEmpty())

        f.fullCycle()
        assertEquals(101, f.clock.cycle)
        assertEquals(20, f.player.preformCompletion)
        val effects = f.worldEffects()
        val hammerSeq = "seq.giants_foundry_hammer01".asRSCM(RSCMType.SEQ)
        assertEquals(
            f.hammerLocs.map { ZoneUpdateTransformer.toLocAnim(it, hammerSeq) }.toSet(),
            effects.filterIsInstance<LocAnim>().toSet(),
        )
        assertTrue(effects.contains(
            ZoneUpdateTransformer.toSoundAreaProt(
                CoordGrid(3367, 11497, 0),
                "synth.giants_foundry_trip_hammer_loop".asRSCM(RSCMType.SYNTH),
                delay = 0, loops = 1, radius = 15, size = 0,
            )
        ))
    }

    @Test
    fun `regular commission final action replaces the tool loop with finish animation`() {
        val f = Fixture()
        f.player.apply {
            workStation = 3
            preformCompletion = 990
            preformTemperature = 42
        }

        f.fullCycle()

        assertEquals(1000, f.player.preformCompletion)
        assertEquals(
            "seq.giants_foundry_finish_sword".asRSCM(RSCMType.SEQ),
            f.player.pendingSequence.id,
        )
        assertEquals(0, f.player.workStation)
        assertTrue(f.output().contains("You have successfully finished the sword."))
        assertFalse(f.player.ui.containsModal("interface.chat_right"))
    }

    private class Fixture(scheduleWork: Boolean = true) {
        val clock = MapClock().apply { cycle = 100 }
        private val events = EventBus()
        private val client = RecordingClient()
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 890L
            slotId = 1
            assignUid()
            coords = CoordGrid(3364, 11492, 0)
            currentMapClock = 99
            processedMapClock = 99
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            worn[Wearpos.RightHand.slot] = InvObj(PREFORM, 1)
            preformQuality = 48
            preformStartQuality = 60
            preformTemperature = 450
            workStation = 2
            workOrigin = coords
            workNextAction = 100
            for (section in 0..6) VarPlayerIntMapSetter.set(this, seedVarbit(section), if (section == 2) 2 else 1)
            if (scheduleWork) softTimer("timer.giants_foundry_work", 1, afterInteractions = true)
        }
        private val timers = PlayerTimerProcessor(clock, events, mockk())
        private val interactions = mockk<PlayerInteractionProcessor>()
        private val players = PlayerList().apply { this[1] = player }
        private val exceptionHandler = mockk<GameExceptionHandler>(relaxed = true)
        private val process = PlayerMainProcess(
            clock, events, ShuffledPlayerList(players), mockk(relaxed = true), timers,
            mockk(relaxed = true), mockk(relaxed = true), interactions, exceptionHandler,
        )
        private val gameCycle = GameCycle(
            eventBus = events,
            mapClock = clock,
            worldMain = mockk(relaxed = true),
            npcPreTick = mockk(relaxed = true),
            playerShuffle = mockk(relaxed = true),
            playerInput = mockk(relaxed = true),
            playerRouteRequest = mockk(relaxed = true),
            npcMain = mockk(relaxed = true),
            controllerMain = mockk(relaxed = true),
            playerMain = process,
            playerLogout = mockk(relaxed = true),
            playerLogin = mockk(relaxed = true),
            worldPostTick = mockk(relaxed = true),
            playerPostTick = mockk(relaxed = true),
        )
        private val zoneUpdates = ZoneUpdateMap()
        val hammerLocs = listOf(CoordGrid(3362, 11495, 0), CoordGrid(3363, 11496, 0)).map {
            LocInfo(2, it, LocEntity(FoundryTool.Hammer.row.loc.id, 10, 0))
        }
        private val locRepo = mockk<LocRepository> {
            for (loc in hammerLocs) {
                every { findExact(loc.coords, LocShape.CentrepieceStraight) } returns loc
            }
        }
        private var interaction: () -> Unit = {}
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events },
            getAlignment = { TextAlignment() },
        )
        private val contextFactory = mockk<ProtectedAccessContextFactory> {
            every { create() } returns context
        }

        init {
            with(RefiningScript(DefaultGameRandom(1), clock, WorldRepository(zoneUpdates), locRepo,
                ProtectedAccessLauncher(contextFactory))) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
            every { interactions.process(player) } answers { interaction() }
        }

        fun cycle(click: () -> Unit = {}) {
            interaction = click
            player.currentMapClock = clock.cycle
            process.process()
            verify { exceptionHandler wasNot Called }
            clock.cycle++
        }

        fun fullCycle(click: () -> Unit = {}) {
            interaction = click
            zoneUpdates.clear()
            player.currentMapClock = clock.cycle
            gameCycle.tick()
            verify { exceptionHandler wasNot Called }
        }

        fun worldEffects() = zoneUpdates.updatedZones.values.flatMap { it.toList() }

        fun operate(name: String) {
            val type = checkNotNull(ServerCacheManager.getObject(name.asRSCM()))
            val loc = BoundLocInfo(player.coords, LocEntity(type.id, 10, 0), 0, 1, 1, 0)
            val coroutine = GameCoroutine("foundry-tick-test")
            val access = ProtectedAccess(player, coroutine, ProtectedAccessContextFactory.empty())
            var outcome: Result<Unit>? = null
            val action: suspend () -> Unit = { assertTrue(events.publish(access, LocEvents.Op1(loc, loc, type))) }
            action.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { outcome = result }
            })
            checkNotNull(outcome) { "Tool interaction unexpectedly suspended" }.getOrThrow()
        }

        fun output() = client.messages.joinToString("\n")
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
