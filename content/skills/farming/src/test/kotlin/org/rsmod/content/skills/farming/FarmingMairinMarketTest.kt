package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.ifOpenTop
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingMairinMarketTest {
    @Test
    fun `native quantity buttons make default clicks buy exactly the selected number of spores`() {
        for (quantity in listOf(1, 5, 10, 50)) {
            val f = Fixture()
            f.player.inv[0] = InvObj(TEAR, quantity * 20)
            f.player.inv[1] = InvObj("obj.coins", 777)
            VarPlayerIntMapSetter.set(f.player, "varp.bank_closing", 0x5A0)
            f.select(quantity)
            f.buy()
            assertEquals(0, f.count(TEAR), "quantity=$quantity")
            assertEquals(quantity, f.count(SPORE), "quantity=$quantity")
            assertEquals(100 - quantity, f.stock(), "quantity=$quantity")
            assertEquals(777, f.count("obj.coins"))
            assertEquals(0x5A0, f.player.vars["varp.bank_closing"] and 7.inv())
            repeat(2) { f.buy() }
            assertEquals(quantity, f.count(SPORE))
            assertEquals(100 - quantity, f.stock())
        }
    }

    @Test
    fun `forced value never buys and explicit purchase quantities ignore the selected default`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(TEAR, 2000)
        f.select(0)
        f.buy()
        val valueMessage = f.client.messages.filterIsInstance<MessageGame>().last().message
        for (quantity in listOf(1, 5, 10, 50)) {
            f.select(quantity)
            val inventory = f.player.inv.toList()
            f.client.messages.clear()
            f.button("component.shopmain:items", f.sporeSlot + 1, SPORE, IfButtonOp.Op6)
            assertEquals(valueMessage, f.client.messages.filterIsInstance<MessageGame>().single().message)
            assertEquals(inventory, f.player.inv.toList())
            assertEquals(100, f.stock())
        }
        var purchased = 0
        for ((op, quantity) in listOf(IfButtonOp.Op2 to 1, IfButtonOp.Op3 to 5,
            IfButtonOp.Op4 to 10, IfButtonOp.Op5 to 50)) {
            f.select(if (quantity == 50) 1 else 50)
            f.button("component.shopmain:items", f.sporeSlot + 1, SPORE, op)
            purchased += quantity
            assertEquals(purchased, f.count(SPORE))
            assertEquals(2000 - purchased * 20, f.count(TEAR))
            assertEquals(100 - purchased, f.stock())
        }
        f.select(0)
        f.buy()
        assertEquals(66, f.count(SPORE))
        assertEquals(680, f.count(TEAR))
        assertEquals(34, f.stock())
    }

    @Test
    fun `coins and vanished tears cannot pay and each committed spore consumes twenty tears once`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.coins", 1000)
        f.select(1)
        f.buy()
        assertEquals(1000, f.count("obj.coins"))
        assertEquals(0, f.count(SPORE))
        assertEquals(100, f.stock())
        f.player.inv[1] = InvObj(TEAR, 20)
        f.button("component.shopmain:items", f.sporeSlot + 1, SPORE, IfButtonOp.Op6)
        f.player.inv[1] = null
        f.buy()
        assertEquals(0, f.count(SPORE))
        assertEquals(100, f.stock())
        f.player.inv[1] = InvObj(TEAR, 20)
        repeat(2) { f.buy() }
        assertEquals(0, f.count(TEAR))
        assertEquals(1, f.count(SPORE))
        assertEquals(99, f.stock())
        assertEquals(1000, f.count("obj.coins"))
    }

    @Test
    fun `full inventory and maximum spore stack roll back both tears and shop stock`() {
        val f = Fixture()
        f.select(5)
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj("obj.spade")
        f.player.inv[0] = InvObj(TEAR, 140)
        val full = f.player.inv.toList()
        f.buy()
        assertEquals(full, f.player.inv.toList())
        assertEquals(100, f.stock())
        f.player.inv[1] = InvObj(SPORE, Int.MAX_VALUE)
        val overflow = f.player.inv.toList()
        f.buy()
        assertEquals(overflow, f.player.inv.toList())
        assertEquals(100, f.stock())
        f.player.inv[1] = null
        f.player.inv[0] = InvObj(TEAR, 100)
        repeat(3) { f.buy() }
        assertEquals(0, f.count(TEAR))
        assertEquals(5, f.count(SPORE))
        assertEquals(26, f.count("obj.spade"))
        assertEquals(95, f.stock())
    }

    @Test
    fun `spending the entire tear stack frees its slot for a full inventory purchase`() {
        val f = Fixture()
        f.select(5)
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj("obj.spade")
        f.player.inv[0] = InvObj(TEAR, 100)
        repeat(2) { f.buy() }
        assertEquals(0, f.count(TEAR))
        assertEquals(5, f.count(SPORE))
        assertEquals(27, f.count("obj.spade"))
        assertEquals(95, f.stock())
    }

    @Test
    fun `spores and tridents cannot be sold back for tears through any native sell option`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(TEAR, Int.MAX_VALUE)
        f.player.inv[1] = InvObj(SPORE, 5)
        f.player.inv[2] = InvObj("obj.merfolk_trident")
        val before = f.player.inv.toList()
        val stock = checkNotNull(f.player.openedShop).inv.toList()
        for (quantity in listOf(0, 1, 5, 10, 50)) {
            f.select(quantity)
            for ((slot, obj) in listOf(1 to SPORE, 2 to "obj.merfolk_trident")) {
                for (op in listOf(IfButtonOp.Op1, IfButtonOp.Op2, IfButtonOp.Op3,
                    IfButtonOp.Op4, IfButtonOp.Op5, IfButtonOp.Op6)) {
                    f.button("component.shopside:items", slot, obj, op)
                    assertEquals(before, f.player.inv.toList())
                    assertEquals(stock, checkNotNull(f.player.openedShop).inv.toList())
                }
            }
        }
    }

    private class Fixture {
        private val events = EventBus()
        private val operations = ShopOperationMap()
        private val restock = ShopRestockProcess(MapClock())
        private val prices = object : MarketPrices {
            override fun get(type: ItemServerType): Int? = null
        }

        val client = SilentClient()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = this@Fixture.client
            uuid = 895L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            ifOpenTop("interface.toplevel_osrs_stretch")
        }
        val sporeSlot: Int

        init {
            val context = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(ShopScript(operations, restock, prices)) { context.startup() }
            with(FarmingMairinMarketScript(Shops(events), operations, restock, prices)) {
                context.startup()
            }
            publish(NpcEvents.Op3(Npc("npc.fossil_mermaid_deepwater", CoordGrid(3765, 10290, 1))))
            sporeSlot = checkNotNull(player.openedShop).inv.indexOfFirst { it?.id == SPORE.asRSCM() }
            check(sporeSlot >= 0)
        }

        fun buy() = button("component.shopmain:items", sporeSlot + 1, SPORE, IfButtonOp.Op1)
        fun select(quantity: Int) {
            val name = if (quantity == 0) "value" else "quantity$quantity"
            publish(IfModalButton(
                ServerCacheManager.fromComponent("component.shopmain:$name"), -1, null, IfButtonOp.Op1,
            ))
        }
        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun stock(): Int = checkNotNull(player.openedShop).inv.physicalCount(SPORE)
        fun button(component: String, sub: Int, obj: String, op: IfButtonOp) {
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            publish(IfModalButton(ServerCacheManager.fromComponent(component), sub, type, op))
        }

        private fun publish(event: SuspendEvent<ProtectedAccess>) {
            val coroutine = GameCoroutine("mairin-market-test")
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine,
                ProtectedAccessContextFactory.empty().copy(getEventBus = { events }))
            var result: Result<Unit>? = null
            val action: suspend () -> Unit = { check(events.publish(access, event)) }
            try {
                action.startCoroutine(object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext
                    override fun resumeWith(outcome: Result<Unit>) { result = outcome }
                })
                checkNotNull(result).getOrThrow()
            } finally {
                player.activeCoroutine = null
            }
        }
    }

    private class SilentClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val TEAR = "obj.fossil_mermaid_tear"
        private const val SPORE = "obj.seaweed_seed"
        private val RESTORED = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORED)
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
