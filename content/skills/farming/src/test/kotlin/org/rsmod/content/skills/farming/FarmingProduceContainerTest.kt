package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingProduceContainerTest {
    @Test
    fun `empty sack fills cabbages first then onions and skips Draynor and noted cabbages`() {
        val f = Fixture()
        val sack = f.give("obj.sack_empty")
        f.give("obj.magic_cabbage", 2)
        f.give("obj.cert_cabbage", 40)
        f.give("obj.potato", 3)
        f.give("obj.onion", 12)
        f.op1(sack)
        assertEquals("obj.sack_onion_10", f.at(sack))
        assertEquals(2, f.count("obj.onion"))
        assertEquals(3, f.count("obj.potato"))
        assertEquals(2, f.count("obj.magic_cabbage"))
        assertEquals(40, f.count("obj.cert_cabbage"))

        val second = f.give("obj.sack_empty")
        f.give("obj.cabbage", 4)
        f.op1(second)
        assertEquals("obj.sack_cabbage_4", f.at(second))
        assertEquals(0, f.count("obj.cabbage"))
        assertEquals(2, f.count("obj.onion"))
    }

    @Test
    fun `fill tops a partial container up to its capacity and a full one takes nothing`() {
        val f = Fixture()
        val sack = f.give("obj.sack_potato_7")
        f.give("obj.potato", 5)
        f.give("obj.onion", 5)
        f.op1(sack)
        assertEquals("obj.sack_potato_10", f.at(sack))
        assertEquals(2, f.count("obj.potato"))
        assertEquals(5, f.count("obj.onion"))
        f.op1(sack)
        assertEquals("obj.sack_potato_10", f.at(sack))
        assertEquals(2, f.count("obj.potato"))

        val basket = f.give("obj.basket_tomato_2")
        f.give("obj.tomato", 9)
        f.op1(basket)
        assertEquals("obj.basket_tomato_5", f.at(basket))
        assertEquals(6, f.count("obj.tomato"))
        f.op1(basket)
        assertEquals("obj.basket_tomato_5", f.at(basket))
        assertEquals(6, f.count("obj.tomato"))
    }

    @Test
    fun `using produce adds exactly one and only the used item`() {
        val f = Fixture()
        val basket = f.give("obj.basket_empty")
        val first = f.give("obj.strawberry")
        val second = f.give("obj.strawberry")
        f.use(basket, first)
        assertEquals("obj.basket_strawberry_1", f.at(basket))
        assertEquals(null, f.at(first))
        assertEquals("obj.strawberry", f.at(second))
        f.use(basket, second)
        assertEquals("obj.basket_strawberry_2", f.at(basket))
        assertEquals(0, f.count("obj.strawberry"))
    }

    @Test
    fun `full mismatched Draynor and noted item uses leave every item unchanged`() {
        val f = Fixture()
        val full = f.give("obj.basket_apple_5")
        val apple = f.give("obj.cooking_apple")
        val sack = f.give("obj.sack_empty")
        val draynor = f.give("obj.magic_cabbage")
        val onions = f.give("obj.sack_onion_3")
        val potato = f.give("obj.potato")
        val notedSack = f.give("obj.cert_sack_empty")
        val notedPotato = f.give("obj.cert_potato", 10)
        f.use(full, apple)
        f.use(sack, draynor)
        f.use(onions, potato)
        f.use(notedSack, potato)
        f.use(sack, notedPotato)
        assertEquals("obj.basket_apple_5", f.at(full))
        assertEquals("obj.sack_empty", f.at(sack))
        assertEquals("obj.sack_onion_3", f.at(onions))
        assertEquals("obj.cooking_apple", f.at(apple))
        assertEquals("obj.magic_cabbage", f.at(draynor))
        assertEquals("obj.potato", f.at(potato))
        assertEquals("obj.cert_sack_empty", f.at(notedSack))
        assertEquals(10, f.count("obj.cert_potato"))
    }

    @Test
    fun `a vanished selected produce item cannot consume another slot`() {
        val f = Fixture()
        val basket = f.give("obj.basket_empty")
        val selected = f.give("obj.strawberry")
        val other = f.give("obj.strawberry")
        val click = HeldUEvents.Type(f.type(f.held(basket)), basket, f.type(f.held(selected)), selected)
        f.player.inv[selected] = InvObj("obj.spade")
        f.publish(click)
        assertEquals("obj.basket_empty", f.at(basket))
        assertEquals("obj.spade", f.at(selected))
        assertEquals("obj.strawberry", f.at(other))
        assertEquals(1, f.count("obj.strawberry"))
    }

    @Test
    fun `remove-one with a full inventory keeps the container and its contents`() {
        val f = Fixture()
        val sack = f.give("obj.sack_potato_5")
        f.fill()
        f.op2(sack)
        assertEquals("obj.sack_potato_5", f.at(sack))
        assertEquals(0, f.count("obj.potato"))
        f.player.inv[27] = null
        f.op2(sack)
        assertEquals("obj.sack_potato_4", f.at(sack))
        assertEquals(1, f.count("obj.potato"))
    }

    @Test
    fun `emptying moves only what fits and leaves the rest in the container`() {
        val f = Fixture()
        val basket = f.give("obj.basket_apple_5")
        f.fill()
        f.op3(basket)
        assertEquals("obj.basket_apple_5", f.at(basket))
        assertEquals(0, f.count("obj.cooking_apple"))
        f.player.inv[26] = null
        f.player.inv[27] = null
        f.op3(basket)
        assertEquals("obj.basket_apple_3", f.at(basket))
        assertEquals(2, f.count("obj.cooking_apple"))
        for (slot in 1 until 26) f.player.inv[slot] = null
        f.op3(basket)
        assertEquals("obj.basket_empty", f.at(basket))
        assertEquals(5, f.count("obj.cooking_apple"))
    }

    @Test
    fun `a repeated click for a container that already changed does nothing`() {
        val f = Fixture()
        val sack = f.give("obj.sack_cabbage_2")
        val stale = f.held(sack)
        f.op2(sack)
        assertEquals("obj.sack_cabbage_1", f.at(sack))
        f.publish(HeldObjEvents.Op2(sack, stale, f.type(stale), f.player.inv))
        assertEquals("obj.sack_cabbage_1", f.at(sack))
        assertEquals(1, f.count("obj.cabbage"))
    }

    private class Fixture {
        private val coroutine = GameCoroutine("produce-container-test")
        private val events = EventBus()
        private val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events })

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 799L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        }

        init {
            with(ProduceContainersScript()) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun type(obj: InvObj) = checkNotNull(ServerCacheManager.getItem(obj.id))
        fun held(slot: Int) = checkNotNull(player.inv[slot])
        fun at(slot: Int): String? = player.inv[slot]?.let { type(it).internalName }
        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun fill() { for (slot in 0 until 28) if (player.inv[slot] == null) player.inv[slot] = InvObj("obj.spade") }

        fun give(obj: String, count: Int = 1): Int {
            val stackable = checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).isStackable
            var first = -1
            repeat(if (stackable) 1 else count) {
                val slot = (0 until 28).first { player.inv[it] == null }
                player.inv[slot] = InvObj(obj, if (stackable) count else 1)
                if (first == -1) first = slot
            }
            return first
        }

        fun op1(slot: Int) = publish(HeldObjEvents.Op1(slot, held(slot), type(held(slot)), player.inv))
        fun op2(slot: Int) = publish(HeldObjEvents.Op2(slot, held(slot), type(held(slot)), player.inv))
        fun op3(slot: Int) = publish(HeldObjEvents.Op3(slot, held(slot), type(held(slot)), player.inv))
        fun use(container: Int, produce: Int) =
            publish(
                HeldUEvents.Type(type(held(container)), container, type(held(produce)), produce),
            )

        fun publish(event: SuspendEvent<ProtectedAccess>) {
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { events.publish(access, event) }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { result.getOrThrow() }
            })
            assertTrue(coroutine.isIdle)
        }
    }

    companion object {
        private val restored = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(restored)
        }

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
