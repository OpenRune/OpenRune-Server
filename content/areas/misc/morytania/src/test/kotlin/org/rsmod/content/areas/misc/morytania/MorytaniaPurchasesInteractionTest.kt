package org.rsmod.content.areas.misc.morytania

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
import org.rsmod.content.areas.misc.morytania.darkmeyer.DarkmeyerShopkeepers
import org.rsmod.content.areas.misc.morytania.darkmeyer.DarkmeyerTownsfolk
import org.rsmod.content.areas.misc.morytania.meiyerditch.MeiyerditchLocals
import org.rsmod.content.areas.misc.morytania.slepe.RatAndBat
import org.rsmod.content.areas.misc.morytania.slepe.SanctuaryTravellers
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
class MorytaniaPurchasesInteractionTest {
    private lateinit var questPolicy: QuestRequirementPolicy

    @BeforeEach
    fun respectQuestProgress() {
        questPolicy = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
    }

    @AfterEach
    fun restoreQuestPolicy() {
        QuestRequirements.install(questPolicy)
    }

    @Test
    fun `Carl sells a bloody bracer for 99 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 150)
        f.talkTo(CARL)
        f.finish(listOf(1))
        assertEquals(51, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BRACER))
    }

    @Test
    fun `Carl sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 98)
        f.talkTo(CARL)
        f.finish(listOf(1))
        assertEquals(98, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BRACER))
    }

    @Test
    fun `leaving Carl thirsty costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 99)
        f.talkTo(CARL)
        f.finish(listOf(2, 3, 4))
        assertEquals(99, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BRACER))
    }

    @Test
    fun `spending the last coins frees a slot for the bracer`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 99)
        f.talkTo(CARL)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BRACER))
        assertEquals(0, f.groundCount(BRACER))
    }

    @Test
    fun `a full pack drops the bracer instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 100)
        f.talkTo(ROY)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BRACER))
        assertEquals(1, f.groundCount(BRACER))
    }

    @Test
    fun `Roy sells a bracer after explaining what happened to Carl`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 99)
        f.talkTo(ROY)
        f.finish(listOf(3, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BRACER))
    }

    @Test
    fun `Roy sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 50)
        f.talkTo(ROY)
        f.finish(listOf(1))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BRACER))
    }

    @Test
    fun `Hanchen takes 10 coins for gossip`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25)
        f.talkTo(HANCHEN)
        f.finish(listOf(2))
        assertEquals(15, f.player.inv.count(COINS))
    }

    @Test
    fun `Hanchen takes nothing from a player with under 10 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 9)
        f.talkTo(HANCHEN)
        f.finish(emptyList())
        assertEquals(9, f.player.inv.count(COINS))
    }

    @Test
    fun `refusing Hanchen keeps the coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25)
        f.talkTo(HANCHEN)
        f.finish(listOf(3))
        assertEquals(25, f.player.inv.count(COINS))
    }

    @Test
    fun `Hameln hands over one parchment`() {
        val f = Fixture()
        f.talkTo(HAMELN)
        f.finish(listOf(2, 3))
        assertEquals(1, f.player.inv.count(PARCHMENT))
        f.talkTo(HAMELN)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(PARCHMENT))
        assertEquals(0, f.groundCount(PARCHMENT))
    }

    @Test
    fun `Hameln drops the parchment for a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(HAMELN)
        f.finish(listOf(2, 3))
        assertEquals(0, f.player.inv.count(PARCHMENT))
        assertEquals(1, f.groundCount(PARCHMENT))
    }

    @Test
    fun `Hameln hands over no parchment after Sins of the Father`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = Fixture()
        f.talkTo(HAMELN)
        f.finish(listOf(1, 1))
        assertEquals(0, f.player.inv.count(PARCHMENT))
        assertEquals(0, f.groundCount(PARCHMENT))
    }

    @Test
    fun `Despoina sells a blood pint for 2 coins after The Blood Moon Rises`() {
        bloodMoonRisen()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(DESPOINA)
        f.finish(listOf(1))
        assertEquals(3, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BLOOD_PINT))
    }

    @Test
    fun `Despoina sells nothing for 1 coin`() {
        bloodMoonRisen()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1)
        f.talkTo(DESPOINA)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BLOOD_PINT))
    }

    @Test
    fun `passing on a blood pint costs nothing`() {
        bloodMoonRisen()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(DESPOINA)
        f.finish(listOf(2))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BLOOD_PINT))
    }

    @Test
    fun `Despoina sends a player back to the slave pens before The Blood Moon Rises`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(DESPOINA)
        f.finish(emptyList())
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BLOOD_PINT))
    }

    @Test
    fun `Mordan hands over a severed leg only when accepted`() {
        bloodMoonRisen()
        val f = Fixture()
        f.talkTo(MORDAN)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(SEVERED_LEG))
        f.talkTo(MORDAN)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(SEVERED_LEG))
    }

    @Test
    fun `a Meiyerditch miner lends one pickaxe and then talks about daeyalt`() {
        val f = Fixture()
        f.talkTo(MINER)
        f.finish(listOf(3, 5))
        assertEquals(1, f.player.inv.count(BRONZE_PICKAXE))
        f.talkTo(MINER)
        f.finish(listOf(4))
        assertEquals(1, f.player.inv.count(BRONZE_PICKAXE))
    }

    private fun bloodMoonRisen() {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf("quest_bloodmoonrises"),
            )
        )
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("morytania-purchases-test")
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
                uuid = 795L
                observerUUID = 795L
                slotId = 1
                assignUid()
                coords = CoordGrid(3618, 3297, 0)
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
            with(RatAndBat(objRepo)) { scripts.startup() }
            with(SanctuaryTravellers(objRepo)) { scripts.startup() }
            with(DarkmeyerShopkeepers(objRepo)) { scripts.startup() }
            with(DarkmeyerTownsfolk(objRepo)) { scripts.startup() }
            with(MeiyerditchLocals(objRepo, Shops(events))) { scripts.startup() }
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
            repeat(200) {
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
                            if (options.hasNext()) options.next() else error("Out of options"),
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
        private const val BRACER = "obj.slepe_bloody_bracer"
        private const val PARCHMENT = "obj.slepe_manuscript_buried_alive"
        private const val BLOOD_PINT = "obj.blood_pint"
        private const val SEVERED_LEG = "obj.severed_leg"
        private const val BRONZE_PICKAXE = "obj.bronze_pickaxe"
        private const val CARL = "npc.slepe_bartender_carl"
        private const val ROY = "npc.slepe_bartender_roy"
        private const val HANCHEN = "npc.slepe_thief_child"
        private const val HAMELN = "npc.slepe_jester_child"
        private const val DESPOINA = "npc.darkm_trader_pub"
        private const val MORDAN = "npc.mordan_nikazsi"
        private const val MINER = "npc.myreque_pt3_miner1_vis"
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
