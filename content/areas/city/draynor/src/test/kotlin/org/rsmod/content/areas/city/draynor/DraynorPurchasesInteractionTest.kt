package org.rsmod.content.areas.city.draynor

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
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.shops.Shops
import org.rsmod.api.testing.random.FixedRandom
import org.rsmod.content.areas.city.draynor.npcs.DraynorVillageNpcs
import org.rsmod.content.areas.city.draynor.npcs.TreeGuard
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private var Player.bankJob by intVarBit("varbit.wom_bankjob")

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DraynorPurchasesInteractionTest {
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
    fun `Aggie makes each dye for its ingredients and 5 coins`() {
        for ((dye, recipe) in DYES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 12)
            f.give(recipe.ingredient, recipe.count, recipe.stacks)
            f.talkTo(AGGIE)
            f.finish(listOf(2, recipe.option, 1))
            assertEquals(7, f.player.inv.count(COINS), dye)
            assertEquals(0, f.player.inv.count(recipe.ingredient), dye)
            assertEquals(1, f.player.inv.count(dye), dye)
        }
    }

    @Test
    fun `Aggie keeps everything from a player short of coins`() {
        for ((dye, recipe) in DYES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 4)
            f.give(recipe.ingredient, recipe.count, recipe.stacks)
            f.talkTo(AGGIE)
            f.finish(listOf(2, recipe.option, 1))
            assertEquals(4, f.player.inv.count(COINS), dye)
            assertEquals(recipe.count, f.player.inv.count(recipe.ingredient), dye)
            assertEquals(0, f.player.inv.count(dye), dye)
        }
    }

    @Test
    fun `Aggie keeps everything from a player short of ingredients`() {
        for ((dye, recipe) in DYES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 12)
            f.give(recipe.ingredient, recipe.count - 1, recipe.stacks)
            f.talkTo(AGGIE)
            f.finish(listOf(2, recipe.option, 1))
            assertEquals(12, f.player.inv.count(COINS), dye)
            assertEquals(recipe.count - 1, f.player.inv.count(recipe.ingredient), dye)
            assertEquals(0, f.player.inv.count(dye), dye)
        }
    }

    @Test
    fun `declining Aggie costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12)
        f.give("obj.redberries", 3)
        f.talkTo(AGGIE)
        f.finish(listOf(2, 1, 3))
        assertEquals(12, f.player.inv.count(COINS))
        assertEquals(3, f.player.inv.count("obj.redberries"))
        assertEquals(0, f.player.inv.count("obj.reddye"))
    }

    @Test
    fun `Aggie fits the dye into a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 12)
        for (slot in 1..3) f.player.inv[slot] = InvObj("obj.redberries", 1)
        f.talkTo(AGGIE)
        f.finish(listOf(2, 1, 1))
        assertEquals(7, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count("obj.reddye"))
        assertEquals(0, f.groundCount("obj.reddye"))
    }

    @Test
    fun `Aggie fines a rich player 20 coins for an insult`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 150)
        f.talkTo(AGGIE)
        f.finish(listOf(3))
        assertEquals(130, f.player.inv.count(COINS))
    }

    @Test
    fun `Ned sells a rope for 15 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 20)
        f.talkTo(NED)
        f.finish(listOf(1, 1))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(ROPE))
    }

    @Test
    fun `Ned sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 14)
        f.talkTo(NED)
        f.finish(listOf(1, 1))
        assertEquals(14, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ROPE))
    }

    @Test
    fun `a full pack drops Ned's rope instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 20)
        f.talkTo(NED)
        f.finish(listOf(1, 1))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ROPE))
        assertEquals(1, f.groundCount(ROPE))
    }

    @Test
    fun `Ned makes a rope from 4 balls of wool`() {
        val f = Fixture()
        f.give(WOOL, 5)
        f.talkTo(NED)
        f.finish(listOf(1, 3))
        assertEquals(1, f.player.inv.count(WOOL))
        assertEquals(1, f.player.inv.count(ROPE))
    }

    @Test
    fun `Ned makes no rope from 3 balls of wool`() {
        val f = Fixture()
        f.give(WOOL, 3)
        f.talkTo(NED)
        f.finish(listOf(1, 3))
        assertEquals(3, f.player.inv.count(WOOL))
        assertEquals(0, f.player.inv.count(ROPE))
    }

    @Test
    fun `Ned adds a free rope after The Fremennik Isles`() {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf("quest_dragonslayer1", "quest_fremennikisles"),
            )
        )
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 20)
        f.talkTo(NED)
        f.finish(listOf(2, 1))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(ROPE))
    }

    @Test
    fun `the bank guard replays the recording for 50 coins from the pack`() {
        val f = Fixture()
        f.player.bankJob = 2
        f.player.inv[0] = InvObj(COINS, 70)
        f.talkTo(BANK_GUARD)
        f.finish(listOf(2, 1))
        assertEquals(20, f.player.inv.count(COINS))
    }

    @Test
    fun `the bank guard takes the fee from the bank when the pack is short`() {
        val f = Fixture()
        f.player.bankJob = 2
        f.player.inv[0] = InvObj(COINS, 10)
        val bank = f.player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(COINS, 120)
        f.talkTo(BANK_GUARD)
        f.finish(listOf(2, 1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(70, bank.count(COINS))
    }

    @Test
    fun `the bank guard takes nothing when both pack and bank are short`() {
        val f = Fixture()
        f.player.bankJob = 2
        f.player.inv[0] = InvObj(COINS, 10)
        val bank = f.player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(COINS, 49)
        f.talkTo(BANK_GUARD)
        f.finish(listOf(2))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(49, bank.count(COINS))
    }

    @Test
    fun `declining the replay costs nothing`() {
        val f = Fixture()
        f.player.bankJob = 2
        f.player.inv[0] = InvObj(COINS, 70)
        f.talkTo(BANK_GUARD)
        f.finish(listOf(2, 2))
        assertEquals(70, f.player.inv.count(COINS))
    }

    @Test
    fun `Martin sells a Farming cape and hood for 99000 coins`() {
        val f = Fixture()
        f.player.statMap.setBaseLevel("stat.farming", 99)
        f.player.inv[0] = InvObj(COINS, 100_000)
        f.talkTo(MARTIN)
        f.finish(listOf(1, 1, 2))
        assertEquals(1_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(FARMING_CAPE))
        assertEquals(1, f.player.inv.count(FARMING_HOOD))
    }

    @Test
    fun `Martin sells nothing to a player short of coins or space`() {
        val poor = Fixture()
        poor.player.statMap.setBaseLevel("stat.farming", 99)
        poor.player.inv[0] = InvObj(COINS, 98_999)
        poor.talkTo(MARTIN)
        poor.finish(listOf(1, 1, 2))
        assertEquals(98_999, poor.player.inv.count(COINS))
        assertEquals(0, poor.player.inv.count(FARMING_CAPE))

        val full = Fixture()
        full.player.statMap.setBaseLevel("stat.farming", 99)
        full.fillInventory()
        full.player.inv[0] = InvObj(COINS, 100_000)
        full.player.inv[1] = null
        full.talkTo(MARTIN)
        full.finish(listOf(1, 1, 2))
        assertEquals(100_000, full.player.inv.count(COINS))
        assertEquals(0, full.player.inv.count(FARMING_CAPE))
        assertEquals(0, full.player.inv.count(FARMING_HOOD))
    }

    @Test
    fun `Martin hands out a spare hood`() {
        val f = Fixture()
        f.player.statMap.setBaseLevel("stat.farming", 99)
        f.talkTo(MARTIN)
        f.finish(listOf(1, 2))
        assertEquals(1, f.player.inv.count(FARMING_HOOD))
    }

    @Test
    fun `Twiggy's chocolate goes into the pack`() {
        val f = Fixture(random = FixedRandom(0))
        f.talkTo(TWIGGY)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(CHOCOLATE))
    }

    @Test
    fun `the tree guard pays 30 coins for a stew and drops the bowl`() {
        val talk = Fixture()
        talk.player.inv[0] = InvObj(STEW, 1)
        talk.interact(LocEvents.Op2(talk.tree, talk.tree, talk.treeType))
        talk.finish(listOf(1))
        assertEquals(0, talk.player.inv.count(STEW))
        assertEquals(30, talk.player.inv.count(COINS))
        assertEquals(1, talk.groundCount(BOWL))

        val use = Fixture()
        use.player.inv[0] = InvObj(STEW, 1)
        val stew = checkNotNull(ServerCacheManager.getItem(STEW.asRSCM()))
        use.interact(LocUEvents.Op(use.tree, use.tree, use.treeType, stew, 0))
        use.finish(emptyList())
        assertEquals(0, use.player.inv.count(STEW))
        assertEquals(30, use.player.inv.count(COINS))
        assertEquals(1, use.groundCount(BOWL))
    }

    @Test
    fun `keeping the stew from the tree guard costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(STEW, 1)
        f.interact(LocEvents.Op2(f.tree, f.tree, f.treeType))
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(STEW))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(0, f.groundCount(BOWL))
    }

    private data class Recipe(
        val ingredient: String,
        val count: Int,
        val option: Int,
        val stacks: Boolean = false,
    )

    private class Fixture(random: FixedRandom? = null) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("draynor-purchases-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getRandom = { checkNotNull(random) { "No random for this test." } },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 795L
                observerUUID = 795L
                slotId = 1
                assignUid()
                coords = CoordGrid(3088, 3245, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                invMap["inv.inv"] = inv
            }

        val treeType = checkNotNull(ServerCacheManager.getObject("loc.wom_tree".asRSCM()))
        val tree =
            BoundLocInfo(
                coords = player.coords.translateX(1),
                entity = LocEntity(id = treeType.id, shape = 10, angle = 0),
                layer = 2,
                width = 2,
                length = 2,
                forceApproachFlags = 0,
            )

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(DraynorVillageNpcs(Shops(events), objRepo)) { scripts.startup() }
            with(TreeGuard(objRepo)) { scripts.startup() }
        }

        fun give(obj: String, count: Int, stacks: Boolean = false) {
            if (stacks) {
                val slot = (0 until 28).first { player.inv[it] == null }
                player.inv[slot] = InvObj(obj, count)
                return
            }
            var added = 0
            for (slot in 0 until 28) {
                if (added == count) return
                if (player.inv[slot] == null) {
                    player.inv[slot] = InvObj(obj, 1)
                    added++
                }
            }
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String) {
            interact(NpcEvents.Op1(Npc(npc, player.coords.translateZ(1))))
        }

        fun interact(event: SuspendEvent<ProtectedAccess>) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, event))
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
                            if (options.hasNext()) options.next() else 1,
                        )
                    } else {
                        val parent =
                            DIALOGUES.firstOrNull { player.ui.containsModal("interface.$it") }
                                ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                        if (parent == "objectbox") {
                            ResumePauseButtonInput("component.objectbox:universe", 0)
                        } else {
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
        private const val ROPE = "obj.rope"
        private const val WOOL = "obj.ball_of_wool"
        private const val STEW = "obj.stew"
        private const val BOWL = "obj.bowl_empty"
        private const val CHOCOLATE = "obj.chocolate_bar"
        private const val FARMING_CAPE = "obj.skillcape_farming"
        private const val FARMING_HOOD = "obj.skillcape_farming_hood"
        private const val AGGIE = "npc.aggie"
        private const val NED = "npc.ned"
        private const val BANK_GUARD = "npc.wom_bankguard"
        private const val MARTIN = "npc.martin_the_master_farmer"
        private const val TWIGGY = "npc.diary_queen"
        private val DIALOGUES = listOf("chat_left", "chat_right", "messagebox", "objectbox")
        private val DYES =
            mapOf(
                "obj.reddye" to Recipe("obj.redberries", 3, 1),
                "obj.yellowdye" to Recipe("obj.onion", 2, 2),
                "obj.bluedye" to Recipe("obj.woadleaf", 2, 3, stacks = true),
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
