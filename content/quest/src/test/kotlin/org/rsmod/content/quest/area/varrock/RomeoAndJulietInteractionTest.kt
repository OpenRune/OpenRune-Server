package org.rsmod.content.quest.area.varrock

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class RomeoAndJulietInteractionTest {
    @Test fun `accepting Romeo's request starts the quest in the native varp`() {
        val f = Fixture()
        f.talk(ROMEO)
        f.finish(listOf(3, 1, 3))
        assertEquals(10, f.stage())
        assertEquals(10, f.player.vars["varp.rjquest"])
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test fun `declining Romeo's request leaves the quest unstarted`() {
        for (first in 1..3) {
            val f = Fixture()
            f.talk(ROMEO)
            f.finish(listOf(first, 2))
            assertEquals(0, f.stage(), "first option $first")
        }
    }

    @Test fun `Juliet gives a message and a lost one is replaced exactly once`() {
        val f = Fixture(10)
        f.talk(JULIET)
        f.finish()
        assertEquals(20, f.stage())
        assertEquals(1, f.player.inv.count(MESSAGE))
        f.talk(JULIET)
        f.finish()
        assertEquals(1, f.player.inv.count(MESSAGE))
        f.player.inv[0] = null
        f.talk(JULIET)
        f.finish()
        assertEquals(1, f.player.inv.count(MESSAGE))
        assertEquals(20, f.stage())
    }

    @Test fun `a full inventory does not lose Juliet's message`() {
        val f = Fixture(10)
        for (slot in 0 until 28) f.player.inv[slot] = InvObj("obj.coins", 1)
        f.talk(JULIET)
        f.finish()
        assertEquals(20, f.stage())
        assertEquals(0, f.player.inv.count(MESSAGE))
        assertTrue(f.player.inv.count("obj.coins") <= 28)
    }

    @Test fun `Juliet has nothing to give before the quest`() {
        val f = Fixture()
        f.talk(JULIET)
        f.finish()
        assertEquals(0, f.stage())
        assertEquals(0, f.player.inv.count(MESSAGE))
    }

    @Test fun `Romeo reads the message and the stage only advances with it`() {
        val lost = Fixture(20)
        lost.talk(ROMEO)
        lost.finish()
        assertEquals(20, lost.stage())

        val f = Fixture(20)
        f.player.inv[0] = InvObj(MESSAGE, 1)
        f.talk(ROMEO)
        f.finish(listOf(4))
        assertEquals(30, f.stage())
        assertEquals(0, f.player.inv.count(MESSAGE))
    }

    @Test fun `Father Lawrence's sermon sends the player to the Apothecary`() {
        val f = Fixture(30)
        f.talk(LAWRENCE)
        f.finish()
        assertEquals(40, f.stage())
        assertTrue(f.script.questLog(f.access()).contains("Apothecary"))
    }

    @Test fun `Father Lawrence only offers the sermon after the message was delivered`() {
        val f = Fixture(20)
        f.talk(LAWRENCE)
        f.finish(listOf(4))
        assertEquals(20, f.stage())
    }

    @Test fun `the Apothecary asks for berries and resets the tailored bush`() {
        val f = Fixture(40)
        VarPlayerIntMapSetter.set(f.player, "varbit.cadavabush", 2)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1, 4))
        assertEquals(50, f.stage())
        assertEquals(0, f.player.vars["varbit.cadavabush"])
        assertEquals(0, f.player.inv.count(POTION))
    }

    @Test fun `berries are turned into a cadava potion once the Apothecary has asked for them`() {
        val f = Fixture(50)
        f.player.inv[0] = InvObj(BERRIES, 1)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1))
        assertEquals(50, f.stage())
        assertEquals(0, f.player.inv.count(BERRIES))
        assertEquals(1, f.player.inv.count(POTION))
    }

    @Test fun `bringing berries while asking in one go also brews the potion`() {
        val f = Fixture(40)
        f.player.inv[0] = InvObj(BERRIES, 1)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1))
        assertEquals(50, f.stage())
        assertEquals(0, f.player.inv.count(BERRIES))
        assertEquals(1, f.player.inv.count(POTION))
    }

    @Test fun `berries are not accepted before Father Lawrence has sent the player`() {
        val f = Fixture(30)
        f.player.inv[0] = InvObj(BERRIES, 1)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 3))
        assertEquals(30, f.stage())
        assertEquals(1, f.player.inv.count(BERRIES))
        assertEquals(0, f.player.inv.count(POTION))
    }

    @Test fun `giving Juliet the potion consumes it and plays the cutscene`() {
        val f = Fixture(50)
        f.player.inv[0] = InvObj(POTION, 1)
        f.talk(JULIET)
        f.finish()
        assertEquals(60, f.stage())
        assertEquals(0, f.player.inv.count(POTION))
        assertEquals(1, f.player.vars["varbit.romjul_juliet_visible"])
        assertEquals(0, f.player.vars["varbit.cutscene_status"])
        assertEquals(CoordGrid(3157, 3425, 1), f.player.coords)
        assertEquals(0, f.npcs.count { it.isSlotAssigned && it.id == "npc.draul_leptoc".asRSCM() })
    }

    @Test fun `Juliet without the potion does not advance the quest`() {
        val f = Fixture(50)
        f.talk(JULIET)
        f.finish()
        assertEquals(50, f.stage())
    }

    @Test fun `the crypt cutscene completes the quest once and grants five quest points`() {
        val f = Fixture(60)
        f.talk(ROMEO)
        f.finish()
        f.assertComplete()
        assertEquals(CoordGrid(3215, 3418, 0), f.player.coords)
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
        assertEquals(2, f.player.vars["varbit.romjul_juliet_visible"])
        f.talk(ROMEO)
        f.finish()
        f.assertComplete()
    }

    @Test fun `the complete quest plays through from Romeo to the crypt`() {
        val f = Fixture()
        f.talk(ROMEO)
        f.finish(listOf(3, 1, 3))
        f.talk(JULIET)
        f.finish()
        f.talk(ROMEO)
        f.finish(listOf(4))
        f.talk(LAWRENCE)
        f.finish()
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1, 4))
        f.player.inv[1] = InvObj(BERRIES, 1)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1))
        assertEquals(1, f.player.inv.count(POTION))
        f.talk(JULIET)
        f.finish()
        assertEquals(60, f.stage())
        f.talk(ROMEO)
        f.finish()
        f.assertComplete()
        assertEquals(0, f.player.inv.count(POTION))
        assertEquals(0, f.player.inv.count(MESSAGE))
    }

    @Test fun `the journal tracks each step and the completion log is written`() {
        val f = Fixture(10)
        assertTrue(f.script.questLog(f.access()).contains("<red>Juliet</red>"))
        f.setStage(20)
        assertTrue(f.script.questLog(f.access()).contains("take the message to"))
        f.setStage(50)
        assertTrue(f.script.questLog(f.access()).contains("cadava berries"))
        f.player.inv[0] = InvObj(POTION, 1)
        assertTrue(f.script.questLog(f.access()).contains("take this <red>cadava potion"))
        f.setStage(100)
        assertTrue(f.script.completedLog(f.access()).contains("Phillipa"))
    }

    @Test fun `Phillipa and Draul react to the stage without changing it`() {
        val f = Fixture()
        f.talk(PHILLIPA)
        f.finish()
        assertTrue(f.output().contains("Hello, who are you?"), f.output())
        f.talk(DRAUL)
        f.finish()
        assertTrue(f.output().contains("What are you doing in my house"), f.output())
        assertEquals(0, f.stage())
        val g = Fixture(100)
        g.talk(DRAUL)
        g.finish()
        assertTrue(g.output().contains("quite pleased with yourself"), g.output())
        assertEquals(100, g.stage())
    }

    @Test fun `walking away from the crypt cutscene returns the player and keeps the stage`() {
        val f = Fixture(60)
        f.talk(ROMEO)
        f.finishUntil { f.player.coords.x < 3000 }
        f.cancel()
        assertEquals(60, f.stage())
        assertEquals(CoordGrid(3215, 3418, 0), f.player.coords)
        assertEquals(0, f.player.vars["varbit.cutscene_status"])
        assertEquals(0, f.npcs.count { it.isSlotAssigned })
        f.talk(ROMEO)
        f.finish()
        f.assertComplete()
    }

    @Test fun `cancelling the potion cutscene keeps the hand-in and restores the interface`() {
        val f = Fixture(50)
        f.player.inv[0] = InvObj(POTION, 1)
        f.talk(JULIET)
        f.finishUntil { f.player.vars["varbit.cutscene_status"] == 1 }
        f.cancel()
        assertEquals(60, f.stage())
        assertEquals(0, f.player.inv.count(POTION))
        assertEquals(0, f.player.vars["varbit.cutscene_status"])
        assertEquals(1, f.player.vars["varbit.romjul_juliet_visible"])
        assertEquals(0, f.npcs.count { it.isSlotAssigned })
    }

    @Test fun `drinking the potion outside of the apothecary is refused`() {
        val f = Fixture(50)
        f.player.inv[0] = InvObj(POTION, 1)
        f.held(POTION, op = 2)
        assertEquals(1, f.player.inv.count(POTION))
        assertEquals(0, f.player.inv.count("obj.vial_empty"))
        assertEquals(50, f.stage())
    }

    @Test fun `drinking the potion inside the apothecary empties the vial`() {
        val f = Fixture(50)
        f.player.coords = CoordGrid(3195, 3404, 0)
        f.player.inv[0] = InvObj(POTION, 1)
        f.held(POTION, op = 2, options = listOf(1))
        assertEquals(0, f.player.inv.count(POTION))
        assertEquals(1, f.player.inv.count("obj.vial_empty"))
        assertEquals(50, f.stage())
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("romeo-and-juliet-test")
        private var result: Result<Unit>? = null
        val npcs = NpcList()
        private val collision = CollisionFlagMap()
        private lateinit var regions: RegionRegistry
        private lateinit var npcRepo: NpcRepository
        private val random =
            object : GameRandom by DefaultGameRandom(1) {
                override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive
            }
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getCollision = { collision },
                    getNpcList = { npcs },
                    getTeleportValidator = { PlayerTeleportValidator(emptySet()) },
                    getAreaChecker = { AreaChecker(regions, AreaIndex()) },
                    getRandom = { random },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 793L
                observerUUID = 793L
                slotId = 1
                assignUid()
                coords = CoordGrid(3211, 3424, 0)
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

        lateinit var script: RomeoAndJuliet

        init {
            val clock = MapClock(100)
            val updates = ZoneUpdateMap()
            val storage = LocZoneStorage()
            val normal = LocRegistryNormal(updates, collision, storage)
            val npcRegistry = NpcRegistry(npcs, collision, events)
            regions =
                RegionRegistry(
                    RegionListSmall(),
                    RegionListLarge(),
                    RegionListWorldEntity(),
                    normal,
                    collision,
                    storage,
                    npcRegistry,
                    ControllerRegistry(clock, ControllerList()),
                    ZonePlayerActivityBitSet(),
                )
            npcRepo = NpcRepository(clock, npcRegistry, npcs)
            script = RomeoAndJuliet(npcRepo, ObjRepository(clock, ObjRegistry(updates)))
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            setStage(stage)
            val zones =
                listOf(
                    CoordGrid(3157, 3425, 1),
                    CoordGrid(2333, 4646, 0),
                    CoordGrid(3215, 3418, 0),
                    CoordGrid(3195, 3404, 0),
                )
            for (zone in zones) collision.allocateIfAbsent(zone.x, zone.z, zone.level)
            for (x in 2320..2340 step 8) for (z in 4630..4660 step 8) {
                collision.allocateIfAbsent(x, z, 0)
            }
        }

        fun setStage(stage: Int) {
            VarPlayerIntMapSetter.set(player, "varbit.romeo_juliet_progress", stage)
        }

        fun stage() = script.quest.getQuestStage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun talk(npc: String, op: Int = 1) {
            val target = Npc(npc, player.coords.translateZ(1))
            start {
                val event = if (op == 3) NpcEvents.Op3(target) else NpcEvents.Op1(target)
                assertTrue(events.publish(this, event))
            }
        }

        fun held(obj: String, op: Int, options: List<Int> = emptyList()) {
            val slot = (0 until 28).first { player.inv[it]?.id == obj.asRSCM() }
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            start {
                val event =
                    if (op == 2) HeldObjEvents.Op2(slot, checkNotNull(inv[slot]), type, inv)
                    else HeldObjEvents.Op1(slot, checkNotNull(inv[slot]), type, inv)
                assertTrue(events.publish(this, event))
            }
            finish(options)
        }

        private fun start(block: suspend ProtectedAccess.() -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val body: suspend () -> Unit = { access().block() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finishUntil(condition: () -> Boolean) {
            repeat(1000) {
                if (condition()) return
                advance(emptyList<Int>().iterator())
            }
            fail<Unit>("Condition never reached: ${output()}")
        }

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(
                kotlin.coroutines.cancellation.CancellationException::class.java,
                result?.exceptionOrNull(),
            )
            result = null
            player.activeCoroutine = null
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(1000) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    when {
                        player.ui.containsModal("interface.chatmenu") ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                if (options.hasNext()) options.next() else 1,
                            )
                        player.ui.containsModal("interface.objectbox") ->
                            ResumePauseButtonInput("component.objectbox:universe", -1)
                        else -> {
                            val parent =
                                listOf("chat_left", "chat_right", "messagebox").firstOrNull {
                                    player.ui.containsModal("interface.$it")
                                } ?: error("Unknown dialogue: ${output()}")
                            ResumePauseButtonInput("component.$parent:continue", -1)
                        }
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun assertComplete() {
            assertEquals(100, script.quest.getQuestStage(player))
            assertEquals(100, player.vars["varp.rjquest"])
            assertEquals(5, player.vars["varp.qp"])
            assertEquals(0, player.vars["varbit.cutscene_status"])
        }

        fun output() = client.messages.joinToString("\n")
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

    companion object {
        private const val ROMEO = "npc.romeo"
        private const val JULIET = "npc.juliet"
        private const val PHILLIPA = "npc.phillipa"
        private const val DRAUL = "npc.draul_leptoc"
        private const val LAWRENCE = "npc.father_lawrence"
        private const val APOTHECARY = "npc.apothecary"
        private const val MESSAGE = "obj.julietmessage"
        private const val POTION = "obj.cadava"
        private const val BERRIES = "obj.cadavaberries"
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
