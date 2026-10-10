package org.rsmod.content.other.sawmill

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.rsmod.api.shops.Shops
import org.rsmod.api.table.SawmillOperatorsRow
import org.rsmod.api.table.SawmillPlanksRow
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
    fun `Buy-plank turns each log type into its plank at the listed price`() {
        for (operator in SawmillOperators.all) {
            for (plank in SawmillPlanks.all) {
                val f = Fixture(coins = plank.price * 3 + 7, plank.logs.internalName to 3)
                f.buyPlank(operator)
                f.finish(plank = plank to 3)
                assertEquals(0, f.player.inv.count(plank.logs.internalName), "${operator.npc.internalName} ${plank.plank.internalName}")
                assertEquals(3, f.player.inv.count(plank.plank.internalName), "${operator.npc.internalName} ${plank.plank.internalName}")
                assertEquals(7, f.player.inv.count("obj.coins"), "${operator.npc.internalName} ${plank.plank.internalName}")
            }
        }
    }

    @Test
    fun `having only a different log type is the same as having none`() {
        val f = Fixture(coins = 100000, "obj.logs" to 10)
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("mahogany") to 5)
        assertTrue(f.output().contains("You'll need to bring me some more logs."), f.output())
        assertEquals(10, f.player.inv.count("obj.logs"))
        assertEquals(100000, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `noted logs are not accepted`() {
        val f = Fixture(coins = 100000, "obj.cert_logs" to 1)
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("wood") to 1)
        assertTrue(f.output().contains("You'll need to bring me some more logs."), f.output())
        assertEquals(1, f.player.inv.count("obj.cert_logs"))
        assertEquals(0, f.player.inv.count("obj.woodplank"))
    }

    @Test
    fun `not enough coins reports the full price and changes nothing`() {
        val f = Fixture(coins = 1499, "obj.mahogany_logs" to 1)
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("mahogany") to 1)
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
        f.buyPlank(SawmillOperators.prifddinas)
        f.finish(plank = plank("oak") to 4)
        assertTrue(f.output().contains("Those planks cost 1,000 coins."), f.output())
        assertEquals(4, f.player.inv.count("obj.oak_logs"))
        assertEquals(799, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `asking for more planks than logs only converts the logs held`() {
        val f = Fixture(coins = 10000, "obj.oak_logs" to 3)
        f.buyPlank(SawmillOperators.auburnvale)
        f.finish(plank = plank("oak") to 28)
        assertEquals(0, f.player.inv.count("obj.oak_logs"))
        assertEquals(3, f.player.inv.count("obj.plank_oak"))
        assertEquals(9250, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `quantities one five and ten convert exactly that many`() {
        for (quantity in listOf(1, 5, 10)) {
            val f = Fixture(coins = 100000, "obj.logs" to 20)
            f.buyPlank(SawmillOperators.lumberYard)
            f.finish(plank = plank("wood") to quantity)
            assertEquals(20 - quantity, f.player.inv.count("obj.logs"), "quantity $quantity")
            assertEquals(quantity, f.player.inv.count("obj.woodplank"), "quantity $quantity")
            assertEquals(100000 - quantity * 100, f.player.inv.count("obj.coins"), "quantity $quantity")
        }
    }

    @Test
    fun `mixed log types only convert the chosen one`() {
        val f = Fixture(coins = 10000, "obj.logs" to 4, "obj.oak_logs" to 4, "obj.teak_logs" to 4)
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("oak") to 4)
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
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("oak") to 1)
        assertEquals(1, f.player.inv.count("obj.plank_oak"))
        assertEquals(0, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `a purchase accounts for every item it touches`() {
        val f = Fixture(coins = 2_000, "obj.oak_logs" to 4, "obj.teak_logs" to 2, "obj.bronze_dagger" to 1)
        val before = f.totals()
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("oak") to 3)
        val after = f.totals()
        assertEquals(before.getValue("obj.coins") - 750, after.getValue("obj.coins"))
        assertEquals(before.getValue("obj.oak_logs") - 3, after.getValue("obj.oak_logs"))
        assertEquals(3, after.getValue("obj.plank_oak"))
        assertEquals(before.getValue("obj.teak_logs"), after.getValue("obj.teak_logs"))
        assertEquals(before.getValue("obj.bronze_dagger"), after.getValue("obj.bronze_dagger"))
        assertEquals(before.keys + "obj.plank_oak", after.keys)
    }

    @Test
    fun `buying again after the logs are gone converts nothing and charges nothing`() {
        val f = Fixture(coins = 1_000, "obj.oak_logs" to 2)
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("oak") to 2)
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("oak") to 2)
        assertEquals(2, f.player.inv.count("obj.plank_oak"))
        assertEquals(0, f.player.inv.count("obj.oak_logs"))
        assertEquals(500, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `logs dropped while the quantity menu is open are neither converted nor charged`() {
        val f = Fixture(coins = 1_000, "obj.oak_logs" to 3)
        f.buyPlank(SawmillOperators.lumberYard)
        for (slot in 0 until 28) if (f.player.inv[slot]?.id == "obj.oak_logs".asRSCM(RSCMType.OBJ)) f.player.inv[slot] = null
        f.finish(plank = plank("oak") to 3)
        assertEquals(0, f.player.inv.count("obj.plank_oak"))
        assertEquals(1_000, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `paying from a maximum coin stack takes exactly the price`() {
        val f = Fixture(coins = Int.MAX_VALUE, "obj.logs" to 5)
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("wood") to 5)
        assertEquals(Int.MAX_VALUE - 500, f.player.inv.count("obj.coins"))
        assertEquals(5, f.player.inv.count("obj.woodplank"))
    }

    @Test
    fun `a full inventory of logs converts every log without losing one`() {
        val f = Fixture(coins = 100_000, *Array(27) { "obj.oak_logs" to 1 })
        f.buyPlank(SawmillOperators.lumberYard)
        f.finish(plank = plank("oak") to 27)
        assertEquals(0, f.player.inv.count("obj.oak_logs"))
        assertEquals(27, f.player.inv.count("obj.plank_oak"))
        assertEquals(100_000 - 27 * 250, f.player.inv.count("obj.coins"))
    }

    private fun plank(name: String): SawmillPlanksRow =
        SawmillPlanksRow.getRow("dbrow.sawmill_plank_$name")

    private class Fixture(
        coins: Int = 0,
        vararg items: Pair<String, Int>,
        val shops: Shops = Shops(EventBus()),
    ) {
        val events = EventBus()
        val hooks = SawmillHooks()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("sawmill-test")
        private var result: Result<Unit>? = null
        private var plank: Pair<SawmillPlanksRow, Int>? = null
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
                coords = CoordGrid(3302, 3490, 0)
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

        fun buyPlank(operator: SawmillOperatorsRow) = op(operator) { NpcEvents.Op3(it) }

        fun totals(): Map<String, Int> {
            val totals = mutableMapOf<String, Int>()
            for (slot in 0 until 28) {
                val obj = player.inv[slot] ?: continue
                val name = checkNotNull(ServerCacheManager.getItem(obj.id)).internalName
                totals.merge(name, obj.count, Int::plus)
            }
            return totals
        }

        private fun op(
            operator: SawmillOperatorsRow,
            event: (Npc) -> SuspendEvent<ProtectedAccess>,
        ) = start {
            assertTrue(events.publish(this, event(Npc(operator.npc.internalName, coords.translateZ(1)))))
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

        fun finish(options: List<Int> = emptyList(), plank: Pair<SawmillPlanksRow, Int>? = null) {
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
                            ResumePauseButtonInput(validButtons[SawmillPlanks.all.indexOfFirst { it.rowId == selected.rowId }], quantity)
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
