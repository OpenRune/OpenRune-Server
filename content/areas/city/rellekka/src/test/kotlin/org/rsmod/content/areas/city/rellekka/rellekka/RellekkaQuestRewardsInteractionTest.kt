package org.rsmod.content.areas.city.rellekka.rellekka

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.areas.city.rellekka.rellekka.npcs.FremennikCouncil
import org.rsmod.content.areas.city.rellekka.rellekka.npcs.FremennikProvinceGapNpcs
import org.rsmod.content.areas.city.rellekka.rellekka.npcs.LarryRellekka
import org.rsmod.content.areas.city.rellekka.rellekka.npcs.RellekkaTownsfolk
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
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
class RellekkaQuestRewardsInteractionTest {
    private lateinit var questPolicy: QuestRequirementPolicy

    @BeforeEach
    fun assumeQuestsCompleted() {
        questPolicy = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
    }

    @AfterEach
    fun restoreQuestPolicy() {
        QuestRequirements.install(questPolicy)
    }

    @Test fun `Larravak hands over one pet rock`() {
        val f = Fixture()
        f.talkTo(ASKELADDEN)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(PET_ROCK))
    }

    @Test fun `Larravak hands over another pet rock to a player who has one`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(PET_ROCK, 1)
        f.talkTo(ASKELADDEN)
        f.finish(listOf(2))
        assertEquals(2, f.player.inv.count(PET_ROCK))
    }

    @Test fun `Larravak keeps the pet rock from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ASKELADDEN)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(PET_ROCK))
        assertEquals(0, f.groundCount(PET_ROCK))
    }

    @Test fun `Askeladden gives an outerlander no pet rock`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = Fixture()
        f.talkTo(ASKELADDEN)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(PET_ROCK))
    }

    @Test fun `Manni buys a Fremennik one beer`() {
        val f = Fixture()
        f.talkTo(MANNI)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(BEER))
    }

    @Test fun `a full pack drops Manni's beer instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(MANNI)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(BEER))
        assertEquals(1, f.groundCount(BEER))
    }

    @Test fun `Manni buys an outerlander nothing`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = Fixture()
        f.talkTo(MANNI)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(BEER))
    }

    @Test fun `Larry replaces a lost clockwork book once`() {
        val f = Fixture()
        f.talkTo(LARRY)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(CLOCKWORK_BOOK))
    }

    @Test fun `Larry has no second clockwork book for a player who has one`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(CLOCKWORK_BOOK, 1)
        f.talkTo(LARRY)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(CLOCKWORK_BOOK))
    }

    @Test fun `a full pack drops Larry's clockwork book instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(LARRY)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(CLOCKWORK_BOOK))
        assertEquals(1, f.groundCount(CLOCKWORK_BOOK))
    }

    @Test fun `Lalli sells golden wool for 1000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1500)
        f.talkTo(LALLI)
        f.finish(listOf(1))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(GOLDEN_FLEECE))
    }

    @Test fun `Lalli sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, GOLDEN_WOOL_PRICE - 1)
        f.talkTo(LALLI)
        f.finish(listOf(1))
        assertEquals(GOLDEN_WOOL_PRICE - 1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GOLDEN_FLEECE))
    }

    @Test fun `not paying Lalli costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1500)
        f.talkTo(LALLI)
        f.finish(listOf(2))
        assertEquals(1500, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GOLDEN_FLEECE))
    }

    @Test fun `spending the last coins frees a slot for the golden wool`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, GOLDEN_WOOL_PRICE)
        f.talkTo(LALLI)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(GOLDEN_FLEECE))
        assertEquals(0, f.groundCount(GOLDEN_FLEECE))
    }

    @Test fun `a full pack drops the golden wool instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 1500)
        f.talkTo(LALLI)
        f.finish(listOf(1))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GOLDEN_FLEECE))
        assertEquals(1, f.groundCount(GOLDEN_FLEECE))
    }

    @Test fun `Lalli sells no golden wool to an outerlander`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1500)
        f.talkTo(LALLI)
        f.finish(emptyList())
        assertEquals(1500, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GOLDEN_FLEECE))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("rellekka-quest-rewards-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
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
                uuid = 794L
                observerUUID = 794L
                slotId = 1
                assignUid()
                coords = CoordGrid(3213, 3428, 0)
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
                invMap["inv.inv"] = inv
                invMap["inv.worn"] = worn
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(FremennikCouncil(objRepo)) { scripts.startup() }
            with(FremennikProvinceGapNpcs(objRepo)) { scripts.startup() }
            with(LarryRellekka(objRepo)) { scripts.startup() }
            with(RellekkaTownsfolk()) { scripts.startup() }
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, NpcEvents.Op1(target)))
            }
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

        fun finish(options: List<Int>) {
            val selections = options.iterator()
            repeat(100) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${client.messages.joinToString("\n")}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    if (player.ui.containsModal("interface.chatmenu")) {
                        ResumePauseButtonInput(
                            "component.chatmenu:options",
                            if (options.hasNext()) options.next() else 1,
                        )
                    } else {
                        val component =
                            CONTINUE_COMPONENTS.entries
                                .firstOrNull { player.ui.containsModal(it.key) }
                                ?.value
                                ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                        ResumePauseButtonInput(component, -1)
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
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private const val COINS = "obj.coins"
        private const val ASKELADDEN = "npc.viking_askelapen"
        private const val PET_ROCK = "obj.vt_useless_rock"
        private const val MANNI = "npc.viking_reveller_3"
        private const val BEER = "obj.beer"
        private const val LARRY = "npc.peng_larry_rell"
        private const val CLOCKWORK_BOOK = "obj.peng_book"
        private const val LALLI = "npc.viking_lalli_troll"
        private const val GOLDEN_FLEECE = "obj.viking_golden_fleece"
        private const val GOLDEN_WOOL_PRICE = 1000
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
