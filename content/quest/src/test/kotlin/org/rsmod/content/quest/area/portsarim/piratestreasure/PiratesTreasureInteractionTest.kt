package org.rsmod.content.quest.area.portsarim.piratestreasure

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.BANANA_RUM
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.CASKET
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.CHEST_KEY
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.COINS
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.CRATE_BANANAS
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.CRATE_RUM
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.LUTHAS_JOB
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.MESSAGE
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.RUM
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.RUM_RETRIEVED
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.WYDIN_JOB
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.WYDIN_RUM
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureSmuggling.Companion.BANANA
import org.rsmod.content.quest.manager.journalLines
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PiratesTreasureInteractionTest {
    @Test
    fun `the quest ends at stage four of the hunt varp`() {
        val f = Fixture(stage = 0)
        assertEquals(PiratesTreasureQuest.STAGE_COMPLETE, f.quest.quest.maxSteps)
    }

    @Test
    fun `each rum smuggling state shows the journal line Quest Helper syncs from`() {
        assertJournalHas("I need to go to") {}
        assertJournalHas("I have taken employment") { employ() }
        assertJournalHas("I have hidden my") {
            employ()
            set(CRATE_RUM, 1)
        }
        assertJournalHas("I have hidden my", "in the crate and filled it with") {
            employ()
            set(CRATE_RUM, 1)
            set(CRATE_BANANAS, 10)
        }
        assertJournalHas("I have hidden my", "the crate has been shipped") { set(WYDIN_RUM, 1) }
        assertJournalHas("the crate has been shipped", "I have taken a job at") {
            set(WYDIN_RUM, 1)
            set(WYDIN_JOB, 1)
        }
        assertJournalHas("I should take it to") { give(RUM to 1) }
        assertJournalHas("I seem to have lost it.", "I need to go to") { set(RUM_RETRIEVED, 1) }
    }

    @Test
    fun `taking the rum from Wydin's crate marks it retrieved until new rum is stashed`() {
        val f = Fixture(stage = 1)
        f.set(WYDIN_RUM, 1)
        assertTrue(f.smuggling.takeRumFromGroceryCrate(f.access()))
        assertEquals(1, f.player.vars[RUM_RETRIEVED])
        f.employ()
        assertTrue(f.smuggling.stashRum(f.access()))
        assertEquals(0, f.player.vars[RUM_RETRIEVED])
    }

    @Test
    fun `Frank starts the quest from his treasure talk`() {
        val f = Fixture(stage = 0)
        f.talk(PiratesTreasureQuest.FRANK)
        f.finish(listOf(1, 1))
        assertEquals(PiratesTreasureQuest.STAGE_STARTED, f.stage())
    }

    @Test
    fun `Frank takes exactly one rum for exactly one key`() {
        val f = Fixture(stage = 1)
        f.give(RUM to 2)
        assertTrue(f.quest.tradeRumForKey(f.access()))
        assertEquals(1, f.player.inv.count(RUM))
        assertEquals(1, f.player.inv.count(CHEST_KEY))
        assertEquals(PiratesTreasureQuest.STAGE_KEY, f.stage())
        assertFalse(f.quest.tradeRumForKey(f.access()))
        assertEquals(1, f.player.inv.count(RUM))
        assertEquals(1, f.player.inv.count(CHEST_KEY))
    }

    @Test
    fun `talking to Frank with rum swaps it for the key in a full inventory`() {
        val f = Fixture(stage = 1)
        f.fill(27)
        f.give(RUM to 1)
        f.talk(PiratesTreasureQuest.FRANK)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(RUM))
        assertEquals(1, f.player.inv.count(CHEST_KEY))
        assertEquals(27, f.player.inv.count(FILLER))
        assertEquals(PiratesTreasureQuest.STAGE_KEY, f.stage())
    }

    @Test
    fun `Frank keeps refusing banana rum and never takes it`() {
        val f = Fixture(stage = 1)
        f.give(BANANA_RUM to 1)
        f.talk(PiratesTreasureQuest.FRANK)
        f.finish()
        assertEquals(1, f.player.inv.count(BANANA_RUM))
        assertEquals(0, f.player.inv.count(CHEST_KEY))
        assertEquals(PiratesTreasureQuest.STAGE_STARTED, f.stage())
    }

    @Test
    fun `no rum means no key`() {
        val f = Fixture(stage = 1)
        assertFalse(f.quest.tradeRumForKey(f.access()))
        assertEquals(0, f.player.inv.count(CHEST_KEY))
        assertEquals(PiratesTreasureQuest.STAGE_STARTED, f.stage())
    }

    @Test
    fun `a lost key is replaced once and never alongside the message`() {
        val f = Fixture(stage = 2)
        assertTrue(f.quest.giveReplacementKey(f.access()))
        assertFalse(f.quest.giveReplacementKey(f.access()))
        assertEquals(1, f.player.inv.count(CHEST_KEY))
        f.player.inv[0] = InvObj(MESSAGE, 1)
        assertFalse(f.quest.giveReplacementKey(f.access()))
        assertEquals(0, f.player.inv.count(CHEST_KEY))
    }

    @Test
    fun `a banked key is not replaced`() {
        val f = Fixture(stage = 2)
        f.player.invMap.getOrPut("inv.bank")[0] = InvObj(CHEST_KEY, 1)
        assertFalse(f.quest.giveReplacementKey(f.access()))
        assertEquals(0, f.player.inv.count(CHEST_KEY))
    }

    @Test
    fun `the chest turns one key into one message`() {
        val f = Fixture(stage = 2)
        f.fill(27)
        f.give(CHEST_KEY to 1)
        assertTrue(f.quest.swapKeyForMessage(f.access()))
        assertEquals(0, f.player.inv.count(CHEST_KEY))
        assertEquals(1, f.player.inv.count(MESSAGE))
        assertFalse(f.quest.swapKeyForMessage(f.access()))
        assertEquals(1, f.player.inv.count(MESSAGE))
    }

    @Test
    fun `the chest gives nothing once the message has been read`() {
        val f = Fixture(stage = 3)
        f.give(CHEST_KEY to 1)
        assertFalse(f.quest.swapKeyForMessage(f.access()))
        assertEquals(1, f.player.inv.count(CHEST_KEY))
        assertEquals(0, f.player.inv.count(MESSAGE))
    }

    @Test
    fun `the casket opens into Hector's treasure exactly once`() {
        val f = Fixture(stage = 4)
        f.give(CASKET to 1, COINS to 10)
        assertTrue(f.quest.openCasket(f.access()))
        assertEquals(0, f.player.inv.count(CASKET))
        assertEquals(460, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(GOLD_RING))
        assertEquals(1, f.player.inv.count(EMERALD))
        assertFalse(f.quest.openCasket(f.access()))
        assertEquals(460, f.player.inv.count(COINS))
    }

    @Test
    fun `a casket without room stays shut`() {
        val f = Fixture(stage = 4)
        f.fill(27)
        f.give(CASKET to 1)
        assertFalse(f.quest.openCasket(f.access()))
        assertEquals(1, f.player.inv.count(CASKET))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GOLD_RING))
        assertEquals(0, f.player.inv.count(EMERALD))
    }

    @Test
    fun `the casket keeps its coins when the stack would overflow`() {
        val f = Fixture(stage = 4)
        f.give(CASKET to 1, COINS to Int.MAX_VALUE - 100)
        assertFalse(f.quest.openCasket(f.access()))
        assertEquals(1, f.player.inv.count(CASKET))
        assertEquals(Int.MAX_VALUE - 100, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GOLD_RING))
    }

    @Test
    fun `digging up the treasure gives one casket and completes the quest`() {
        val f = Fixture(stage = 3)
        assertTrue(f.quest.unearthTreasure(f.access()))
        assertEquals(1, f.player.inv.count(CASKET))
        assertEquals(PiratesTreasureQuest.STAGE_COMPLETE, f.stage())
        assertFalse(f.quest.unearthTreasure(f.access()))
        assertEquals(1, f.player.inv.count(CASKET))
    }

    @Test
    fun `a full inventory leaves the treasure buried`() {
        val f = Fixture(stage = 3)
        f.fill(28)
        assertFalse(f.quest.unearthTreasure(f.access()))
        assertEquals(0, f.player.inv.count(CASKET))
        assertEquals(PiratesTreasureQuest.STAGE_MESSAGE_READ, f.stage())
    }

    @Test
    fun `bananas fill the crate up to ten and the rest stay in the pack`() {
        val f = Fixture(stage = 1)
        f.employ()
        f.give(BANANA to 7)
        assertEquals(7, f.smuggling.packBananas(f.access(), 10))
        f.give(BANANA to 7)
        assertEquals(3, f.smuggling.packBananas(f.access(), 10))
        assertEquals(10, f.player.vars[CRATE_BANANAS])
        assertEquals(4, f.player.inv.count(BANANA))
        assertEquals(0, f.smuggling.packBananas(f.access(), 10))
        assertEquals(4, f.player.inv.count(BANANA))
    }

    @Test
    fun `one banana at a time packs only one`() {
        val f = Fixture(stage = 0)
        f.employ()
        f.give(BANANA to 3)
        assertEquals(1, f.smuggling.packBananas(f.access(), 1))
        assertEquals(2, f.player.inv.count(BANANA))
        assertEquals(1, f.player.vars[CRATE_BANANAS])
    }

    @Test
    fun `rum is stashed once and only while working the quest`() {
        val f = Fixture(stage = 1)
        f.give(RUM to 2)
        assertFalse(f.smuggling.stashRum(f.access()))
        assertEquals(2, f.player.inv.count(RUM))
        f.employ()
        assertTrue(f.smuggling.stashRum(f.access()))
        assertFalse(f.smuggling.stashRum(f.access()))
        assertEquals(1, f.player.inv.count(RUM))
        assertEquals(1, f.player.vars[CRATE_RUM])
    }

    @Test
    fun `no rum is stashed after the quest`() {
        val f = Fixture(stage = 4)
        f.employ()
        f.give(RUM to 1)
        assertFalse(f.smuggling.stashRum(f.access()))
        assertEquals(1, f.player.inv.count(RUM))
        assertEquals(0, f.player.vars[CRATE_RUM])
    }

    @Test
    fun `a second rum cannot be stashed while the first waits at Wydin's`() {
        val f = Fixture(stage = 1)
        f.employ()
        VarPlayerIntMapSetter.set(f.player, WYDIN_RUM, 1)
        f.give(RUM to 1)
        assertFalse(f.smuggling.stashRum(f.access()))
        assertEquals(1, f.player.inv.count(RUM))
    }

    @Test
    fun `Luthas pays thirty coins once and ships the rum to Wydin`() {
        val f = Fixture(stage = 1)
        f.employ()
        VarPlayerIntMapSetter.set(f.player, CRATE_BANANAS, 10)
        VarPlayerIntMapSetter.set(f.player, CRATE_RUM, 1)
        assertTrue(f.smuggling.payForCrate(f.access()))
        assertEquals(30, f.player.inv.count(COINS))
        assertEquals(0, f.player.vars[CRATE_BANANAS])
        assertEquals(0, f.player.vars[CRATE_RUM])
        assertEquals(1, f.player.vars[WYDIN_RUM])
        assertEquals(0, f.player.vars[LUTHAS_JOB])
        assertFalse(f.smuggling.payForCrate(f.access()))
        assertEquals(30, f.player.inv.count(COINS))
    }

    @Test
    fun `a crate that is not full earns nothing`() {
        val f = Fixture(stage = 1)
        f.employ()
        VarPlayerIntMapSetter.set(f.player, CRATE_BANANAS, 9)
        assertFalse(f.smuggling.payForCrate(f.access()))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(9, f.player.vars[CRATE_BANANAS])
    }

    @Test
    fun `Luthas drops his pay at your feet when the pack is full`() {
        val f = Fixture(stage = 1)
        f.employ()
        f.fill(28)
        VarPlayerIntMapSetter.set(f.player, CRATE_BANANAS, 10)
        assertTrue(f.smuggling.payForCrate(f.access()))
        assertEquals(0, f.player.inv.count(COINS))
        val coins = COINS.asRSCM(RSCMType.OBJ)
        val dropped = f.objRepo.findAll(f.player.coords).filter { it.type == coins }.toList()
        assertEquals(1, dropped.size)
        assertEquals(30, dropped.single().count)
    }

    @Test
    fun `the whole Luthas conversation pays and ships the rum`() {
        val f = Fixture(stage = 1)
        f.employ()
        VarPlayerIntMapSetter.set(f.player, CRATE_BANANAS, 10)
        VarPlayerIntMapSetter.set(f.player, CRATE_RUM, 1)
        f.talk(PiratesTreasureSmuggling.LUTHAS)
        f.finish(listOf(2))
        assertEquals(30, f.player.inv.count(COINS))
        assertEquals(1, f.player.vars[WYDIN_RUM])
    }

    @Test
    fun `Wydin's crate gives the smuggled rum back once`() {
        val f = Fixture(stage = 1)
        VarPlayerIntMapSetter.set(f.player, WYDIN_RUM, 1)
        assertTrue(f.smuggling.takeRumFromGroceryCrate(f.access()))
        assertFalse(f.smuggling.takeRumFromGroceryCrate(f.access()))
        assertEquals(1, f.player.inv.count(RUM))
        assertEquals(0, f.player.vars[WYDIN_RUM])
    }

    @Test
    fun `the rum waits in Wydin's crate while the pack is full`() {
        val f = Fixture(stage = 1)
        VarPlayerIntMapSetter.set(f.player, WYDIN_RUM, 1)
        f.fill(28)
        assertFalse(f.smuggling.takeRumFromGroceryCrate(f.access()))
        assertEquals(0, f.player.inv.count(RUM))
        assertEquals(1, f.player.vars[WYDIN_RUM])
        f.player.inv[0] = null
        assertTrue(f.smuggling.takeRumFromGroceryCrate(f.access()))
        assertEquals(1, f.player.inv.count(RUM))
    }

    @Test
    fun `customs take every bottle of rum and nothing else`() {
        val f = Fixture(stage = 1)
        f.give(RUM to 3, BANANA to 2, COINS to 50)
        assertEquals(3, confiscateRum(f.access()))
        assertEquals(0, f.player.inv.count(RUM))
        assertEquals(2, f.player.inv.count(BANANA))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(0, confiscateRum(f.access()))
    }

    @Test
    fun `customs leave banana rum alone`() {
        val f = Fixture(stage = 1)
        f.give(BANANA_RUM to 1)
        assertEquals(0, confiscateRum(f.access()))
        assertEquals(1, f.player.inv.count(BANANA_RUM))
    }

    private fun assertJournalHas(vararg phrases: String, setup: Fixture.() -> Unit) {
        val f = Fixture(stage = 1)
        f.setup()
        val lines = journalLines(f.quest.questLog(f.access()))
        for (phrase in phrases) {
            assertTrue(lines.any { phrase in it }, "No journal line holds '$phrase': $lines")
        }
    }

    private class Fixture(stage: Int) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("pirates-treasure-test")
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
                uuid = 791L
                observerUUID = 791L
                slotId = 1
                assignUid()
                coords = CoordGrid(3052, 3252, 0)
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
                invMap.getOrPut("inv.bank")
            }

        val objRepo = ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap()))
        val quest = PiratesTreasureQuest()
        val smuggling = PiratesTreasureSmuggling(quest, objRepo)

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(quest) { scripts.startup() }
            with(smuggling) { scripts.startup() }
            VarPlayerIntMapSetter.set(player, "varbit.pirates_treasure_progress", stage)
        }

        fun stage() = quest.stage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun employ() = VarPlayerIntMapSetter.set(player, LUTHAS_JOB, 1)

        fun set(varbit: String, value: Int) = VarPlayerIntMapSetter.set(player, varbit, value)

        fun give(vararg objs: Pair<String, Int>) {
            for ((obj, count) in objs) {
                val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
                val stacks = if (type.stackable) listOf(count) else List(count) { 1 }
                for (amount in stacks) {
                    val slot = (0 until 28).first { player.inv[it] == null }
                    player.inv[slot] = InvObj(obj, amount)
                }
            }
        }

        fun fill(slots: Int) {
            for (slot in 0 until slots) player.inv[slot] = InvObj(FILLER, 1)
        }

        fun talk(npc: String) =
            start { assertTrue(events.publish(this, NpcEvents.Op1(Npc(npc, coords.translateZ(1))))) }

        private fun start(block: suspend ProtectedAccess.() -> Unit) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val access = access()
            val run: suspend () -> Unit = { access.block() }
            run.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(300) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${client.messages.joinToString("\n")}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent =
                    listOf("chat_left", "chat_right", "messagebox", "objectbox", "chatmenu")
                        .firstOrNull { player.ui.containsModal("interface.$it") }
                        ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
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
        private const val FILLER = "obj.bronze_dagger"
        private const val GOLD_RING = "obj.gold_ring"
        private const val EMERALD = "obj.emerald"

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
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
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
