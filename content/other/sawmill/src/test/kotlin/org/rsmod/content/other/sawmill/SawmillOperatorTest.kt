package org.rsmod.content.other.sawmill

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.shops.Shops
import org.rsmod.content.skills.validButtons
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
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
class SawmillOperatorTest {

    @Test
    fun `every operator has Talk-to, Buy-plank and Trade on the expected ops`() {
        for (operator in SawmillOperator.entries) {
            val type = checkNotNull(ServerCacheManager.getNpc(operator.npc.asRSCM(RSCMType.NPC)))
            assertEquals("Talk-to", type.actions.getOpOrNull(0), operator.npc)
            assertEquals("Buy-plank", type.actions.getOpOrNull(2), operator.npc)
            assertEquals("Trade", type.actions.getOpOrNull(3), operator.npc)
        }
    }

    @Test
    fun `the Lumber Yard operator walks every talk branch`() {
        val op = SawmillOperator.LumberYard

        val decline = Fixture().apply { talk(op) }
        decline.finish(listOf(3))
        assertTrue(decline.output().contains("Hello there. Do you want me to make some planks"))
        assertTrue(decline.output().contains("I'm good, thanks."))
        assertTrue(decline.output().contains("You'll struggle to find quality planks anywhere but here!"))

        val supplies = Fixture().apply { talk(op) }
        supplies.finish(listOf(2))
        assertTrue(supplies.output().contains("Can I buy some housing supplies?"))
        assertTrue(supplies.output().contains("Of course!"))
        assertNotNull(supplies.player.openedShop)

        val planks = Fixture(coins = 1000, "obj.logs" to 5).apply { talk(op) }
        planks.finish(listOf(1), Plank.Wood to 5)
        assertTrue(planks.output().contains("Yes, please make me some planks."))
        assertEquals(5, planks.player.inv.count("obj.woodplank"))
        assertEquals(500, planks.player.inv.count("obj.coins"))
    }

    @Test
    fun `the Prifddinas operator explains the planks and does not say of course`() {
        val op = SawmillOperator.Prifddinas

        val kinds = Fixture().apply { talk(op) }
        kinds.finish(listOf(2))
        assertTrue(kinds.output().contains("Do you want me to make some planks"))
        assertFalse(kinds.output().contains("Hello there"))
        assertTrue(kinds.output().contains("What kind of planks can you make?"))
        assertTrue(kinds.output().contains("I don't make planks from other woods"))
        assertTrue(kinds.output().contains("teak and mahogany can only be found in a few places like Karamja and Etceteria."))

        val supplies = Fixture().apply { talk(op) }
        supplies.finish(listOf(3))
        assertFalse(supplies.output().contains("Of course!"))
        assertNotNull(supplies.player.openedShop)

        val decline = Fixture().apply { talk(op) }
        decline.finish(listOf(4))
        assertTrue(decline.output().contains("Nothing, thanks"))
        assertTrue(decline.output().contains("You can't get good quality planks anywhere but here!"))

        val planks = Fixture(coins = 250, "obj.oak_logs" to 1).apply { talk(op) }
        planks.finish(listOf(1), Plank.Oak to 1)
        assertEquals(1, planks.player.inv.count("obj.plank_oak"))
    }

    @Test
    fun `the Auburnvale operator greets in Varlamorian and walks every branch`() {
        val op = SawmillOperator.Auburnvale

        val decline = Fixture().apply { talk(op) }
        decline.finish(listOf(3))
        assertTrue(decline.output().contains("Nilsal. Do you want me to make some planks"))
        assertTrue(decline.output().contains("I'm good, thanks"))
        assertTrue(decline.output().contains("You'll struggle to find quality planks anywhere but here!"))

        val supplies = Fixture().apply { talk(op) }
        supplies.finish(listOf(2))
        assertTrue(supplies.output().contains("Of course!"))
        assertNotNull(supplies.player.openedShop)

        val planks = Fixture(coins = 500, "obj.teak_logs" to 1).apply { talk(op) }
        planks.finish(listOf(1), Plank.Teak to 1)
        assertEquals(1, planks.player.inv.count("obj.plank_teak"))
    }

    @Test
    fun `Buy-plank turns each log type into its plank at the listed price`() {
        for (operator in SawmillOperator.entries) {
            for (plank in Plank.entries) {
                val f = Fixture(coins = plank.price * 3 + 7, plank.logs to 3)
                f.buyPlank(operator)
                f.finish(plank = plank to 3)
                assertEquals(0, f.player.inv.count(plank.logs), "$operator $plank")
                assertEquals(3, f.player.inv.count(plank.plank), "$operator $plank")
                assertEquals(7, f.player.inv.count("obj.coins"), "$operator $plank")
            }
        }
    }

    @Test
    fun `the plank prices match the wiki`() {
        assertEquals(
            listOf(100, 250, 500, 1500, 2500, 5000, 7500),
            Plank.entries.map { it.price },
        )
    }

    @Test
    fun `Buy-plank offers all seven planks even without logs`() {
        val f = Fixture(coins = 0)
        f.buyPlank(SawmillOperator.LumberYard)
        assertTrue(f.player.ui.containsModal("interface.skillmulti"))
        f.finish(plank = Plank.Oak to 1)
        assertTrue(f.output().contains("You'll need to bring me some more logs."), f.output())
    }

    @Test
    fun `having only a different log type is the same as having none`() {
        val f = Fixture(coins = 100000, "obj.logs" to 10)
        f.buyPlank(SawmillOperator.LumberYard)
        f.finish(plank = Plank.Mahogany to 5)
        assertTrue(f.output().contains("You'll need to bring me some more logs."), f.output())
        assertEquals(10, f.player.inv.count("obj.logs"))
        assertEquals(100000, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `noted logs are not accepted`() {
        val f = Fixture(coins = 100000, "obj.cert_logs" to 1)
        f.buyPlank(SawmillOperator.LumberYard)
        f.finish(plank = Plank.Wood to 1)
        assertTrue(f.output().contains("You'll need to bring me some more logs."), f.output())
        assertEquals(1, f.player.inv.count("obj.cert_logs"))
        assertEquals(0, f.player.inv.count("obj.woodplank"))
    }

    @Test
    fun `not enough coins reports the full price and changes nothing`() {
        val f = Fixture(coins = 1499, "obj.mahogany_logs" to 1)
        f.buyPlank(SawmillOperator.LumberYard)
        f.finish(plank = Plank.Mahogany to 1)
        assertTrue(
            f.output().contains("Those planks cost 1,500 coins. You don't have enough money for all of them."),
            f.output(),
        )
        assertEquals(1, f.player.inv.count("obj.mahogany_logs"))
        assertEquals(1499, f.player.inv.count("obj.coins"))
        assertEquals(0, f.player.inv.count("obj.plank_mahogany"))
    }

    @Test
    fun `not enough coins for the whole quantity prices the whole quantity`() {
        val f = Fixture(coins = 799, "obj.oak_logs" to 4, "obj.teak_logs" to 2)
        f.buyPlank(SawmillOperator.Prifddinas)
        f.finish(plank = Plank.Oak to 4)
        assertTrue(f.output().contains("Those planks cost 1,000 coins."), f.output())
        assertEquals(4, f.player.inv.count("obj.oak_logs"))
        assertEquals(799, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `asking for more planks than logs only converts the logs held`() {
        val f = Fixture(coins = 10000, "obj.oak_logs" to 3)
        f.buyPlank(SawmillOperator.Auburnvale)
        f.finish(plank = Plank.Oak to 28)
        assertEquals(0, f.player.inv.count("obj.oak_logs"))
        assertEquals(3, f.player.inv.count("obj.plank_oak"))
        assertEquals(9250, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `quantities one five and ten convert exactly that many`() {
        for (quantity in listOf(1, 5, 10)) {
            val f = Fixture(coins = 100000, "obj.logs" to 20)
            f.buyPlank(SawmillOperator.LumberYard)
            f.finish(plank = Plank.Wood to quantity)
            assertEquals(20 - quantity, f.player.inv.count("obj.logs"), "quantity $quantity")
            assertEquals(quantity, f.player.inv.count("obj.woodplank"), "quantity $quantity")
            assertEquals(100000 - quantity * 100, f.player.inv.count("obj.coins"), "quantity $quantity")
        }
    }

    @Test
    fun `mixed log types only convert the chosen one`() {
        val f = Fixture(coins = 10000, "obj.logs" to 4, "obj.oak_logs" to 4, "obj.teak_logs" to 4)
        f.buyPlank(SawmillOperator.LumberYard)
        f.finish(plank = Plank.Oak to 4)
        assertEquals(4, f.player.inv.count("obj.logs"))
        assertEquals(0, f.player.inv.count("obj.oak_logs"))
        assertEquals(4, f.player.inv.count("obj.teak_logs"))
        assertEquals(4, f.player.inv.count("obj.plank_oak"))
        assertEquals(9000, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `paying with the last coins in a full inventory still works`() {
        val f = Fixture(coins = 250, "obj.oak_logs" to 1)
        for (slot in 0 until 28) if (f.player.inv[slot] == null) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.buyPlank(SawmillOperator.LumberYard)
        f.finish(plank = Plank.Oak to 1)
        assertEquals(1, f.player.inv.count("obj.plank_oak"))
        assertEquals(0, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `Trade opens the construction supplies shop with the wiki stock`() {
        for (operator in SawmillOperator.entries) {
            val f = Fixture()
            f.trade(operator)
            f.finish()
            val shop = checkNotNull(f.player.openedShop)
            assertEquals(130.0, shop.sellPercentage, operator.npc)
            assertEquals(50.0, shop.buyPercentage, operator.npc)
            assertEquals(0.0, shop.changePercentage, operator.npc)
            val stock = checkNotNull(shop.inv.type.stock).filterNotNull().associate { it.obj to it.count }
            val expected =
                mapOf(
                    "obj.poh_saw" to 1000,
                    "obj.cloth" to 1000,
                    "obj.nails_bronze" to 1000,
                    "obj.nails_iron" to 1000,
                    "obj.nails" to 1000,
                )
            assertEquals(expected.mapKeys { it.key.asRSCM(RSCMType.OBJ) }, stock, operator.npc)
            assertEquals("inv.poh_sawmill_shop".asRSCM(RSCMType.INV), shop.inv.type.id)
        }
    }

    @Test
    fun `every operator shares the same stock`() {
        val a = Fixture().apply { trade(SawmillOperator.LumberYard) }
        a.finish()
        val b = Fixture(shops = a.shops).apply { trade(SawmillOperator.Prifddinas) }
        b.finish()
        assertTrue(checkNotNull(a.player.openedShop).inv === checkNotNull(b.player.openedShop).inv)
    }

    @Test
    fun `registered hooks add options to the talk menu and run when chosen`() {
        var chosen: SawmillOperator? = null
        val hook =
            object : SawmillTalkHook {
                override fun option(player: Player, operator: SawmillOperator) =
                    if (operator == SawmillOperator.Prifddinas) null else "Extra option"

                override suspend fun choose(dialogue: Dialogue, operator: SawmillOperator) {
                    chosen = operator
                    dialogue.chatNpc(dialogue.neutral, "Hook ran.")
                }
            }
        val f = Fixture().apply { hooks.register(hook) }
        f.talk(SawmillOperator.LumberYard)
        f.finish(listOf(3))
        assertEquals(SawmillOperator.LumberYard, chosen)
        assertTrue(f.output().contains("Hook ran."))

        chosen = null
        val prif = Fixture().apply { hooks.register(hook) }
        prif.talk(SawmillOperator.Prifddinas)
        prif.finish(listOf(3))
        assertNull(chosen)
        assertFalse(prif.output().contains("Extra option"))
    }

    @Test
    fun `approach triggers serve a player standing across the counter`() {
        for (operator in SawmillOperator.entries) {
            val talk = Fixture().apply { approachTalk(operator) }
            talk.finish(listOf(3))
            assertTrue(talk.output().contains("Do you want me to make some planks"), operator.npc)

            val buy = Fixture(coins = 100, "obj.logs" to 1).apply { approachBuyPlank(operator) }
            buy.finish(plank = Plank.Wood to 1)
            assertEquals(1, buy.player.inv.count("obj.woodplank"), operator.npc)

            val trade = Fixture().apply { approachTrade(operator) }
            trade.finish()
            assertNotNull(trade.player.openedShop, operator.npc)
        }
    }

    @Test
    fun `approach triggers wait until the player is within two tiles`() {
        val f = Fixture(far = true).apply { approachTalk(SawmillOperator.LumberYard) }
        f.finish()
        assertFalse(f.output().contains("Do you want me to make some planks"), f.output())
        assertNull(f.player.openedShop)
    }

    private class Fixture(
        coins: Int = 0,
        vararg items: Pair<String, Int>,
        val shops: Shops = Shops(EventBus()),
        val far: Boolean = false,
    ) {
        val events = EventBus()
        val hooks = SawmillHooks()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("sawmill-test")
        private var result: Result<Unit>? = null
        private var plank: Pair<Plank, Int>? = null
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
                uuid = 793L
                observerUUID = 793L
                slotId = 1
                assignUid()
                coords = if (far) CoordGrid(3302, 3480, 0) else CoordGrid(3302, 3490, 0)
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

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(SawmillOperatorScript(shops, hooks)) { scripts.startup() }
            if (coins > 0) give("obj.coins" to coins)
            give(*items)
        }

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

        fun access() = ProtectedAccess(player, coroutine, context)

        fun talk(operator: SawmillOperator) = op(operator) { NpcEvents.Op1(it) }

        fun buyPlank(operator: SawmillOperator) = op(operator) { NpcEvents.Op3(it) }

        fun trade(operator: SawmillOperator) = op(operator) { NpcEvents.Op4(it) }

        fun approachTalk(operator: SawmillOperator) = op(operator) { NpcEvents.Ap1(it) }

        fun approachBuyPlank(operator: SawmillOperator) = op(operator) { NpcEvents.Ap3(it) }

        fun approachTrade(operator: SawmillOperator) = op(operator) { NpcEvents.Ap4(it) }

        private fun op(
            operator: SawmillOperator,
            event: (Npc) -> SuspendEvent<ProtectedAccess>,
        ) = start {
            assertTrue(events.publish(this, event(Npc(operator.npc, coords.translateZ(if (far) 6 else 1)))))
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

        fun finish(options: List<Int> = emptyList(), plank: Pair<Plank, Int>? = null) {
            this.plank = plank
            val selections = options.iterator()
            repeat(400) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    when {
                        player.ui.containsModal("interface.skillmulti") -> {
                            val (selected, quantity) = checkNotNull(plank) { "no plank selection" }
                            ResumePauseButtonInput(validButtons[selected.ordinal], quantity)
                        }
                        player.ui.containsModal("interface.chatmenu") ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                if (options.hasNext()) options.next() else 1,
                            )
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

        fun output() = client.messages.joinToString("\n").replace("<br>", " ")
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
