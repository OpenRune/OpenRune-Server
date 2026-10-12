package org.rsmod.content.areas.city.ardougne

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
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.ardougne.npcs.ArdougneZoo
import org.rsmod.content.areas.city.ardougne.npcs.EastArdougneTownsfolk
import org.rsmod.content.areas.city.ardougne.npcs.shops.ArdougneMarket
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
class ArdougneQuestRewardsInteractionTest {
    private lateinit var questPolicy: QuestRequirementPolicy

    @BeforeEach
    fun saveQuestPolicy() {
        questPolicy = QuestRequirements.activePolicy()
    }

    @AfterEach
    fun restoreQuestPolicy() {
        QuestRequirements.install(questPolicy)
    }

    @Test fun `the silver merchant sells a lost enchanted key for 500 coins`() {
        completed("quest_makinghistory")
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 600)
        f.talkTo(SILVER_MERCHANT)
        f.finish(listOf(1))
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(ENCHANTED_KEY))
    }

    @Test fun `the silver merchant keeps the key from a player short of coins`() {
        completed("quest_makinghistory")
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 499)
        f.talkTo(SILVER_MERCHANT)
        f.finish(listOf(1))
        assertEquals(499, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ENCHANTED_KEY))
    }

    @Test fun `the silver merchant takes nothing from a player with a full pack`() {
        completed("quest_makinghistory")
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 600)
        f.talkTo(SILVER_MERCHANT)
        f.finish(listOf(1))
        assertEquals(600, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ENCHANTED_KEY))
        assertEquals(0, f.groundCount(ENCHANTED_KEY))
    }

    @Test fun `the silver merchant sells no second key to a player with one banked`() {
        completed("quest_makinghistory")
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 600)
        f.bank[0] = InvObj(ENCHANTED_KEY, 1)
        f.talkTo(SILVER_MERCHANT)
        f.finish(emptyList())
        assertEquals(600, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ENCHANTED_KEY))
    }

    @Test fun `declining the silver merchant's key costs nothing`() {
        completed("quest_makinghistory")
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 600)
        f.talkTo(SILVER_MERCHANT)
        f.finish(listOf(2))
        assertEquals(600, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ENCHANTED_KEY))
    }

    @Test fun `Larry replaces a lost clockwork book after Cold War`() {
        completed("quest_coldwar")
        val f = Fixture()
        f.talkTo(LARRY)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(CLOCKWORK_BOOK))
    }

    @Test fun `Larry offers no book to a player who still has one`() {
        completed("quest_coldwar")
        val f = Fixture()
        f.bank[0] = InvObj(CLOCKWORK_BOOK, 1)
        f.talkTo(LARRY)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(CLOCKWORK_BOOK))
        assertEquals(1, f.bank.count(CLOCKWORK_BOOK))
    }

    @Test fun `Alrena replaces a lost tabard after Song of the Elves`() {
        completed("quest_songoftheelves")
        val f = Fixture()
        f.talkTo(ALRENA)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(TABARD))
    }

    @Test fun `a full pack drops Alrena's tabard instead of losing it`() {
        completed("quest_songoftheelves")
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ALRENA)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(TABARD))
        assertEquals(1, f.groundCount(TABARD))
    }

    @Test fun `Alrena gives no second tabard to a player wearing one`() {
        completed("quest_songoftheelves")
        val f = Fixture()
        f.player.worn[4] = InvObj(TABARD, 1)
        f.talkTo(ALRENA)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(TABARD))
    }

    @Test fun `Alrena gives no tabard before Song of the Elves`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = Fixture()
        f.talkTo(ALRENA)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(TABARD))
    }

    @Test fun `Elena hands out a large sieve after Mourning's End Part I`() {
        completed("quest_mourningsendpart1")
        val f = Fixture()
        f.talkTo(ELENA)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(SIEVE))
    }

    @Test fun `Jimmy hands over the rat pits manual after Ratcatchers`() {
        completed("quest_ratcatchers")
        val f = Fixture()
        f.talkTo(JIMMY)
        f.finish(listOf(2, 2, 2))
        assertEquals(1, f.player.inv.count(MANUAL))
    }

    @Test fun `Jimmy gives no second rat pits manual`() {
        completed("quest_ratcatchers")
        val f = Fixture()
        f.player.inv[0] = InvObj(MANUAL, 1)
        f.talkTo(JIMMY)
        f.finish(listOf(2, 2, 2))
        assertEquals(1, f.player.inv.count(MANUAL))
        assertEquals(0, f.groundCount(MANUAL))
    }

    private fun completed(quest: String) {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf(quest),
            )
        )
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("ardougne-quest-rewards-test")
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
                coords = CoordGrid(2662, 3305, 0)
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
                invMap["inv.bank"] = Inventory.create("inv.bank")
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(ArdougneMarket(Shops(events), objRepo)) { scripts.startup() }
            with(ArdougneZoo(objRepo)) { scripts.startup() }
            with(EastArdougneTownsfolk(objRepo)) { scripts.startup() }
        }

        val bank: Inventory
            get() = checkNotNull(player.invMap["inv.bank"])

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String, op: (Npc) -> NpcEvents.Op = { NpcEvents.Op1(it) }) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, op(target)))
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
        private const val COINS = "obj.coins"
        private const val SILVER_MERCHANT = "npc.silver_merchant_ardougne"
        private const val ENCHANTED_KEY = "obj.makinghistory_key"
        private const val LARRY = "npc.peng_larry_zoo"
        private const val CLOCKWORK_BOOK = "obj.peng_book"
        private const val ALRENA = "npc.alrena"
        private const val TABARD = "obj.ardougne_knight_tabard"
        private const val ELENA = "npc.elena2"
        private const val SIEVE = "obj.mourning_sieve"
        private const val JIMMY = "npc.vc_jimmy_dazzler"
        private const val MANUAL = "obj.ratpits_manual"
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
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
