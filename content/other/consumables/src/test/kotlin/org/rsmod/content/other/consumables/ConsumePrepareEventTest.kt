package org.rsmod.content.other.consumables

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.PlayerConsumeEvents
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onPlayerConsumePrepare
import org.rsmod.content.other.consumables.food.ConsumeFoodScript
import org.rsmod.content.other.consumables.food.FoodEffectService
import org.rsmod.content.other.consumables.food.FoodSpecialEffectService
import org.rsmod.content.other.consumables.food.kebab.KebabEffect
import org.rsmod.content.other.consumables.food.stew.SpicyStewEffect
import org.rsmod.content.other.consumables.potion.ConsumePotionScript
import org.rsmod.content.other.consumables.potion.PotionEffectService
import org.rsmod.content.other.consumables.potion.PotionSpecialEffectService
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ConsumePrepareEventTest {
    @Test
    fun `food is used up and heals its natural amount with no handler changes`() {
        val f = Fixture()
        f.player.inv[5] = InvObj(LOBSTER)
        var seen: PlayerConsumeEvents.Prepare? = null
        f.onPrepare { seen = this }

        f.click(5)

        assertNull(f.player.inv[5])
        assertEquals(START_HP + 12, f.player.hitpoints)
        assertEquals(12, checkNotNull(seen).heal)
        assertEquals(false, checkNotNull(seen).potion)
    }

    @Test
    fun `kept food stays in its slot and still heals`() {
        val f = Fixture()
        f.player.inv[5] = InvObj(LOBSTER)
        f.onPrepare { consumed = false }

        f.click(5)

        assertEquals(InvObj(LOBSTER), f.player.inv[5])
        assertEquals(1, f.player.inv.occupiedSpace())
        assertEquals(START_HP + 12, f.player.hitpoints)
    }

    @Test
    fun `kept multi-stage food does not advance a stage`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MEAT_PIE)
        f.player.inv[1] = InvObj(HALF_MEAT_PIE)
        f.onPrepare { consumed = false }

        f.click(0)

        assertEquals(InvObj(MEAT_PIE), f.player.inv[0])
        assertEquals(InvObj(HALF_MEAT_PIE), f.player.inv[1])
        assertEquals(2, f.player.inv.occupiedSpace())
    }

    @Test
    fun `unkept multi-stage food advances one stage`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MEAT_PIE)

        f.click(0)

        assertEquals(InvObj(HALF_MEAT_PIE), f.player.inv[0])
        assertEquals(1, f.player.inv.occupiedSpace())
    }

    @Test
    fun `a repeated click on kept food in one tick neither heals nor changes the item again`() {
        val f = Fixture()
        f.player.inv[5] = InvObj(LOBSTER)
        var prepares = 0
        f.onPrepare {
            consumed = false
            prepares++
        }

        f.click(5)
        f.click(5)

        assertEquals(1, prepares)
        assertEquals(InvObj(LOBSTER), f.player.inv[5])
        assertEquals(START_HP + 12, f.player.hitpoints)
    }

    @Test
    fun `food that vanished from its slot gives no heal, kept or not`() {
        for (keep in listOf(false, true)) {
            val f = Fixture()
            f.onPrepare { consumed = !keep }

            f.clickStale(slot = 5, item = LOBSTER)

            assertNull(f.player.inv[5])
            assertEquals(0, f.player.inv.occupiedSpace())
            assertEquals(START_HP, f.player.hitpoints)
        }
    }

    @Test
    fun `a different item in the slot is never replaced or healed from`() {
        for (keep in listOf(false, true)) {
            val f = Fixture()
            f.player.inv[5] = InvObj(SPADE)
            f.onPrepare { consumed = !keep }

            f.clickStale(slot = 5, item = LOBSTER)

            assertEquals(InvObj(SPADE), f.player.inv[5])
            assertEquals(START_HP, f.player.hitpoints)
        }
    }

    @Test
    fun `a handler removing the food cannot leave a free heal`() {
        val f = Fixture()
        f.player.inv[5] = InvObj(LOBSTER)
        f.onPrepare { player.inv[5] = null }

        f.click(5)

        assertNull(f.player.inv[5])
        assertEquals(START_HP, f.player.hitpoints)
    }

    @Test
    fun `heal changes what food restores but not its base cap`() {
        val raised = Fixture()
        raised.player.inv[5] = InvObj(LOBSTER)
        raised.onPrepare { heal = 20 }
        raised.click(5)
        assertEquals(START_HP + 20, raised.player.hitpoints)

        val past = Fixture()
        past.player.inv[5] = InvObj(LOBSTER)
        past.onPrepare { heal = 500 }
        past.click(5)
        assertEquals(BASE_HP, past.player.hitpoints)

        val none = Fixture()
        none.player.inv[5] = InvObj(LOBSTER)
        none.onPrepare { heal = 0 }
        none.click(5)
        assertEquals(START_HP, none.player.hitpoints)
        assertNull(none.player.inv[5])
    }

    @Test
    fun `a potion uses one dose with no handler changes`() {
        val f = Fixture()
        f.player.inv[3] = InvObj(ATTACK_MIX_2)
        var seen: PlayerConsumeEvents.Prepare? = null
        f.onPrepare { seen = this }

        f.click(3)

        assertEquals(InvObj(ATTACK_MIX_1), f.player.inv[3])
        assertEquals(1, f.player.inv.occupiedSpace())
        assertEquals(START_HP + 3, f.player.hitpoints)
        assertEquals(3, checkNotNull(seen).heal)
        assertEquals(true, checkNotNull(seen).potion)
    }

    @Test
    fun `a kept potion keeps its dose and heal changes the mix heal`() {
        val f = Fixture()
        f.player.inv[3] = InvObj(ATTACK_MIX_2)
        f.onPrepare {
            consumed = false
            heal = 7
        }

        f.click(3)

        assertEquals(InvObj(ATTACK_MIX_2), f.player.inv[3])
        assertEquals(1, f.player.inv.occupiedSpace())
        assertEquals(START_HP + 7, f.player.hitpoints)
    }

    @Test
    fun `a potion that vanished from its slot gives no effect, kept or not`() {
        for (keep in listOf(false, true)) {
            val f = Fixture()
            f.onPrepare { consumed = !keep }

            f.clickStale(slot = 3, item = ATTACK_MIX_2)

            assertNull(f.player.inv[3])
            assertEquals(START_HP, f.player.hitpoints)
        }
    }

    private class Fixture {
        val player =
            Player().apply {
                client = SilentClient()
                statMap.setBaseLevel(HITPOINTS, BASE_HP.toByte())
                statMap.setCurrentLevel(HITPOINTS, START_HP.toByte())
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

        private val events = EventBus()
        private val script = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
        private val coroutine = GameCoroutine("consume-prepare-test")
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(getEventBus = { events }, getAlignment = { TextAlignment() })

        init {
            val areas = mockk<AreaChecker>(relaxed = true)
            val special =
                mockk<PotionSpecialEffectService>(relaxed = true) {
                    every { handles(any()) } returns true
                }
            val access = ConsumableActivityAccess(emptySet())
            with(
                ConsumeFoodScript(
                    areas,
                    FoodEffectService(),
                    FoodSpecialEffectService(SpicyStewEffect(), KebabEffect()),
                    access,
                )
            ) {
                script.startup()
            }
            with(
                ConsumePotionScript(
                    areas,
                    PotionEffectService(special, MapClock()),
                    access,
                )
            ) {
                script.startup()
            }
        }

        fun onPrepare(action: PlayerConsumeEvents.Prepare.() -> Unit) {
            script.onPlayerConsumePrepare(action)
        }

        fun click(slot: Int) {
            val obj = checkNotNull(player.inv[slot])
            publish(slot, obj)
        }

        fun clickStale(slot: Int, item: String) {
            publish(slot, InvObj(item))
        }

        private fun publish(slot: Int, obj: InvObj) {
            val type = checkNotNull(ServerCacheManager.getItem(obj.id))
            run { assertTrue(events.publish(this, HeldObjEvents.Op1(slot, obj, type, player.inv))) }
        }

        private fun run(action: suspend ProtectedAccess.() -> Unit) {
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            var outcome: Result<Unit>? = null
            val body: suspend () -> Unit = { access.action() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        outcome = result
                    }
                }
            )
            checkNotNull(outcome).getOrThrow()
        }
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val HITPOINTS = "stat.hitpoints"
        private const val BASE_HP = 99
        private const val START_HP = 50
        private const val LOBSTER = "obj.lobster"
        private const val MEAT_PIE = "obj.meat_pie"
        private const val HALF_MEAT_PIE = "obj.half_a_meat_pie"
        private const val ATTACK_MIX_2 = "obj.brutal_2dose1attack"
        private const val ATTACK_MIX_1 = "obj.brutal_1dose1attack"
        private const val SPADE = "obj.spade"

        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
