package org.rsmod.content.quest.area.falador.doricsquest

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DoricsQuestInteractionTest {
    @Test
    fun `accepting starts the quest and hands over a bronze pickaxe`() {
        val f = Fixture()
        f.talk()
        f.finish(listOf(AnvilsOption, Yes, Certainly))
        assertEquals(StageStarted, f.stage())
        assertEquals(1, f.player.inv.count("obj.bronze_pickaxe"))
        assertTrue(f.output().contains("You've started a new quest"), f.output())
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test
    fun `declining leaves the quest unstarted without a pickaxe`() {
        val f = Fixture()
        f.talk()
        f.finish(listOf(AnvilsOption, No))
        assertEquals(0, f.stage())
        assertEquals(0, f.player.inv.count("obj.bronze_pickaxe"))
    }

    @Test
    fun `low mining shows the recommended skills warning before the start prompt`() {
        val recommended = QuestRow.getRow("dbrow.quest_dorics".asRSCM()).recommendedStats
        val mining = recommended.single { it.t0.isType("stat.mining") }.t1

        val low = Fixture(mining = mining - 1)
        low.talk()
        low.finish(listOf(AnvilsOption, No))
        assertTrue(low.output().contains(SkillWarning), low.output())

        val enough = Fixture(mining = mining)
        enough.talk()
        enough.finish(listOf(AnvilsOption, No))
        assertFalse(enough.output().contains(SkillWarning), enough.output())
    }

    @Test
    fun `carrying the exact materials when accepting completes the quest at once`() {
        val f = Fixture()
        f.materials()
        f.talk()
        f.finish(listOf(AnvilsOption, Yes))
        assertTrue(f.output().contains("in the exact quantities too"), f.output())
        f.assertReward()
        assertEquals(1, f.player.inv.count("obj.bronze_pickaxe"))
    }

    @Test
    fun `surplus materials skip the exact quantities line and keep the surplus`() {
        val f = Fixture()
        f.materials(clay = 7)
        f.talk()
        f.finish(listOf(AnvilsOption, Yes))
        assertFalse(f.output().contains("in the exact quantities too"), f.output())
        f.assertReward(clayLeft = 1)
    }

    @Test
    fun `an incomplete set of materials is not taken`() {
        val f = Fixture(stage = StageStarted)
        f.materials(clay = 5)
        f.talk()
        f.finish(listOf(Certainly))
        assertEquals(StageStarted, f.stage())
        assertEquals(5, f.player.inv.count("obj.clay"))
        assertEquals(4, f.player.inv.count("obj.copper_ore"))
        assertEquals(2, f.player.inv.count("obj.iron_ore"))
        assertEquals(0, f.player.statMap.getXP("stat.mining"))
    }

    @Test
    fun `asking where to find the ores warns a miner below level 15 only`() {
        val low = Fixture(stage = StageStarted, mining = 14)
        low.talk()
        low.finish(listOf(WhereToFind))
        assertTrue(low.output().contains("not a good enough miner"), low.output())

        val able = Fixture(stage = StageStarted, mining = 15)
        able.talk()
        able.finish(listOf(WhereToFind))
        assertFalse(able.output().contains("not a good enough miner"), able.output())
    }

    @Test
    fun `handing in grants the reward once`() {
        val f = Fixture(stage = StageStarted)
        f.materials()
        f.talk()
        f.finish()
        f.assertReward()
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
        f.talk()
        f.finish()
        f.assertReward()
        assertTrue(f.output().contains("how is your metalworking coming along"), f.output())
    }

    @Test
    fun `cancelling on the hand-over box keeps the materials and progress`() {
        val f = Fixture(stage = StageStarted)
        f.materials()
        f.talk()
        f.until { f.output().contains(HandOver) }
        f.cancel()
        assertEquals(StageStarted, f.stage())
        assertEquals(6, f.player.inv.count("obj.clay"))
        assertEquals(0, f.player.statMap.getXP("stat.mining"))
    }

    @Test
    fun `losing a material during the hand-over box takes nothing`() {
        val f = Fixture(stage = StageStarted)
        f.materials()
        f.talk()
        f.until { f.output().contains(HandOver) }
        f.player.inv[CopperSlot] = null
        f.finish()
        assertEquals(StageStarted, f.stage())
        assertEquals(6, f.player.inv.count("obj.clay"))
        assertEquals(3, f.player.inv.count("obj.copper_ore"))
        assertEquals(0, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `coin overflow preserves materials and allows a later hand-in`() {
        val f = Fixture(stage = StageStarted)
        f.materials()
        f.player.inv[27] = InvObj("obj.coins", Int.MAX_VALUE - 179)
        f.talk()
        f.finish()
        assertEquals(StageStarted, f.stage())
        assertEquals(6, f.player.inv.count("obj.clay"))
        assertEquals(4, f.player.inv.count("obj.copper_ore"))
        assertEquals(2, f.player.inv.count("obj.iron_ore"))
        assertEquals(Int.MAX_VALUE - 179, f.player.inv.count("obj.coins"))
        assertEquals(0, f.player.vars["varp.qp"])
        assertEquals(0, f.player.statMap.getXP("stat.mining"))

        f.player.inv[27] = InvObj("obj.coins", Int.MAX_VALUE - 180)
        f.talk()
        f.finish()
        assertEquals(StageComplete, f.stage())
        assertEquals(0, f.player.inv.count("obj.clay"))
        assertEquals(0, f.player.inv.count("obj.copper_ore"))
        assertEquals(0, f.player.inv.count("obj.iron_ore"))
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins"))
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1300, f.player.statMap.getXP("stat.mining"))
    }

    @Test
    fun `the journal strikes a carried material and says how many more of the rest`() {
        val f = Fixture(stage = StageStarted)
        f.materials(clay = 0, iron = 0)
        val journal = f.script.questLog(f.access())
        assertTrue(journal.contains("<red>6 Clay</red> - I need 6 more."), journal)
        assertTrue(journal.contains("<str>4 Copper Ore.</str>"), journal)
        assertTrue(journal.contains("<red>2 Iron Ore</red> - I need 2 more."), journal)
    }

    @Test
    fun `Doric stops an unfinished player at his anvil only under real progress`() {
        withPolicy(QuestRequirementMode.RespectProgress) {
            val blocked = Fixture()
            assertFalse(blocked.anvil(DoricsAnvil, listOf(SorryOption, No)))
            assertTrue(blocked.output().contains("Hey, who said you could use that?"), blocked.output())
            assertEquals(0, blocked.stage())

            assertTrue(Fixture().anvil("loc.anvil"))
            assertTrue(Fixture(stage = StageComplete).anvil(DoricsAnvil))
        }
        withPolicy(QuestRequirementMode.AssumeCompleted) {
            assertTrue(Fixture().anvil(DoricsAnvil))
        }
    }

    @Test
    fun `the quest can be started from the anvil`() {
        withPolicy(QuestRequirementMode.RespectProgress) {
            val f = Fixture()
            assertFalse(f.anvil(DoricsAnvil, listOf(SorryOption, Yes, Certainly)))
            assertEquals(StageStarted, f.stage())
            assertEquals(1, f.player.inv.count("obj.bronze_pickaxe"))
        }
    }

    @Test
    fun `the anvil mid-quest asks for the materials and Doric hands over`() {
        withPolicy(QuestRequirementMode.RespectProgress) {
            val f = Fixture(stage = StageStarted)
            val doric = f.spawnDoric()
            f.materials()
            assertFalse(f.anvil(DoricsAnvil))
            assertFalse(f.output().contains("Hey, who said you could use that?"), f.output())
            assertTrue(f.output().contains("Have you got my materials yet"), f.output())
            assertEquals("seq.dwarf_hand_over".asRSCM(), doric.pendingSequence.id)
            f.assertReward()
        }
    }

    @Test
    fun `the whetstone follows quest completion`() {
        withPolicy(QuestRequirementMode.RespectProgress) {
            val before = Fixture()
            before.whetstone()
            before.finish()
            assertTrue(before.output().contains("You should probably ask"), before.output())

            val after = Fixture(stage = StageComplete)
            after.whetstone()
            after.finish()
            assertTrue(after.output().contains("anything suitable"), after.output())
        }
    }

    private fun withPolicy(mode: QuestRequirementMode, block: () -> Unit) {
        val previous = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(mode = mode))
        try {
            block()
        } finally {
            QuestRequirements.install(previous)
        }
    }

    private class Fixture(stage: Int = 0, mining: Int = 1) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("dorics-quest-test")
        private var result: Result<Unit>? = null
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 790L
                slotId = 1
                assignUid()
                coords = CoordGrid(2951, 3450, 0)
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
                statMap.setBaseLevel("stat.mining", mining.toByte())
                statMap.setCurrentLevel("stat.mining", mining.toByte())
            }

        private val collision = CollisionFlagMap()
        private val npcs = NpcList()
        private val npcRepo = NpcRepository(MapClock(100), NpcRegistry(npcs, collision, events), npcs)

        val script =
            DoricsQuest(ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap())), npcRepo)

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varp.doricquest", stage)
        }

        fun access() = ProtectedAccess(player, coroutine, context)

        fun stage(): Int = script.quest.getQuestStage(player)

        fun materials(clay: Int = 6, copper: Int = 4, iron: Int = 2) {
            var slot = 0
            repeat(clay) { player.inv[slot++] = InvObj("obj.clay", 1) }
            repeat(copper) { player.inv[slot++] = InvObj("obj.copper_ore", 1) }
            repeat(iron) { player.inv[slot++] = InvObj("obj.iron_ore", 1) }
        }

        fun talk() {
            val npc = Npc("npc.doric", player.coords.translateZ(1))
            run { assertTrue(events.publish(access(), NpcEvents.Op1(npc))) }
        }

        fun spawnDoric(): Npc {
            val coords = player.coords.translateZ(1)
            collision.allocateIfAbsent(coords.x, coords.z, coords.level)
            return Npc("npc.doric", coords).also { npcRepo.add(it, Int.MAX_VALUE) }
        }

        fun whetstone() {
            val loc = loc("loc.devious_whetstone")
            val type = checkNotNull(ServerCacheManager.getObject(loc.id))
            run { assertTrue(events.publish(access(), LocEvents.Op1(loc, loc, type))) }
        }

        fun anvil(name: String, options: List<Int> = emptyList()): Boolean {
            val type = checkNotNull(ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC)))
            var allowed: Boolean? = null
            run { allowed = with(script) { access().mayUseAnvil(type) } }
            finish(options)
            return checkNotNull(allowed)
        }

        private fun loc(name: String): BoundLocInfo {
            val type = checkNotNull(ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC)))
            val entity = LocEntity(type.id, CentrepieceShape, 0)
            return BoundLocInfo(LocInfo(CentrepieceLayer, CoordGrid(2953, 3451, 0), entity), type)
        }

        private fun run(action: suspend () -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            action.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun until(predicate: () -> Boolean) {
            repeat(150) {
                if (predicate()) return
                advance()
            }
            fail<Unit>("Condition was not reached: ${output()}")
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(150) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int> = emptyList<Int>().iterator()) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent =
                    listOf("chat_left", "chat_right", "messagebox", "objectbox", "chatmenu")
                        .firstOrNull { player.ui.containsModal("interface.$it") }
                        ?: error("Unknown dialogue: ${output()}")
                val input =
                    when (parent) {
                        "chatmenu" ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                if (options.hasNext()) options.next() else 1,
                            )
                        "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", 0)
                        else -> ResumePauseButtonInput("component.$parent:continue", -1)
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(CancellationException::class.java, result?.exceptionOrNull())
            result = null
            player.activeCoroutine = null
        }

        fun assertReward(clayLeft: Int = 0) {
            assertEquals(StageComplete, stage())
            assertEquals(StageComplete, player.vars["varp.doricquest"])
            assertEquals(1, player.vars["varp.qp"])
            assertEquals(1300, player.statMap.getXP("stat.mining"))
            assertEquals(180, player.inv.count("obj.coins"))
            assertEquals(clayLeft, player.inv.count("obj.clay"))
            assertEquals(0, player.inv.count("obj.copper_ore"))
            assertEquals(0, player.inv.count("obj.iron_ore"))
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
        private const val StageStarted = 10
        private const val StageComplete = 100

        private const val AnvilsOption = 1
        private const val SorryOption = 1
        private const val Yes = 1
        private const val No = 2
        private const val WhereToFind = 1
        private const val Certainly = 2

        private const val CopperSlot = 6
        private const val DoricsAnvil = "loc.dorics_anvil"
        private const val HandOver = "You hand the clay, copper, and iron to Doric."
        private const val CentrepieceShape = 10
        private const val CentrepieceLayer = 2
        private const val SkillWarning = "lower than recommended"

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
