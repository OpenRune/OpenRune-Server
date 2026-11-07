package org.rsmod.content.skills.crafting

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.skills.Material
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
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
class CraftingTransactionTest {
    @Test
    fun `valid knife craft replaces its input exactly once even in a full inventory`() {
        val f = Fixture()
        f.fill(except = listOf(KNIFE, BRANCH))
        val before = f.snapshot()
        assertTrue(f.craft(product()))
        assertEquals(before.minus(BRANCH).plus(STAFF), f.snapshot())
        assertEquals(0, f.count(BRANCH))
        assertEquals(1, f.count(STAFF))
        assertEquals(1, f.count(KNIFE))
        assertEquals(CRAFT_XP, f.xp(CRAFTING))
        assertEquals(FLETCH_XP, f.xp(FLETCHING))
    }

    @Test
    fun `repeated crafts pay and convert once per success then stop without inputs`() {
        val f = Fixture()
        f.give(KNIFE)
        f.give(BRANCH, 3)
        repeat(3) { assertTrue(f.craft(product())) }
        assertEquals(0, f.count(BRANCH))
        assertEquals(3, f.count(STAFF))
        assertEquals(3 * CRAFT_XP, f.xp(CRAFTING))
        assertEquals(3 * FLETCH_XP, f.xp(FLETCHING))
        val after = f.snapshot()
        assertFalse(f.craft(product()))
        assertEquals(after, f.snapshot())
        assertEquals(3 * CRAFT_XP, f.xp(CRAFTING))
    }

    @Test
    fun `knife removed after validation fails the commit and preserves items and XP`() {
        val f = Fixture()
        f.give(KNIFE)
        f.give(BRANCH, 2)
        assertTrue(f.can(product()))
        f.remove(KNIFE)
        val before = f.snapshot()
        assertFalse(f.craft(product()))
        assertEquals(before, f.snapshot())
        assertEquals(0, f.xp(CRAFTING))
        assertEquals(0, f.xp(FLETCHING))
    }

    @Test
    fun `crafting level dropping after validation fails the commit and preserves items and XP`() {
        val f = Fixture()
        f.give(KNIFE)
        f.give(BRANCH)
        assertTrue(f.can(product()))
        f.player.statMap.setCurrentLevel(CRAFTING, (LEVEL - 1).toByte())
        val before = f.snapshot()
        assertFalse(f.craft(product()))
        assertEquals(before, f.snapshot())
        assertEquals(0, f.xp(CRAFTING))
        assertEquals(0, f.xp(FLETCHING))
    }

    @Test
    fun `fletching level dropping after validation fails the commit and preserves items and XP`() {
        val f = Fixture()
        f.give(KNIFE)
        f.give(BRANCH)
        assertTrue(f.can(product()))
        f.player.statMap.setCurrentLevel(FLETCHING, (FLETCH_LEVEL - 1).toByte())
        val before = f.snapshot()
        assertFalse(f.craft(product()))
        assertEquals(before, f.snapshot())
        assertEquals(0, f.xp(CRAFTING))
        assertEquals(0, f.xp(FLETCHING))
    }

    @Test
    fun `missing output space rolls back the consumed input without XP`() {
        val f = Fixture()
        f.fill(except = listOf(KNIFE, BRANCH))
        val before = f.snapshot()
        assertFalse(f.craft(product().copy(outputCount = 2)))
        assertEquals(before, f.snapshot())
        assertEquals(0, f.xp(CRAFTING))
        assertEquals(0, f.xp(FLETCHING))
    }

    private fun product() =
        CraftingProduct(
            section = CraftingSection.KNIFE,
            output = STAFF,
            inputs = listOf(Material(BRANCH)),
            level = LEVEL,
            xp = CRAFT_XP.toDouble(),
            extraReqs = listOf(CraftingStatReq(FLETCHING, "Fletching", FLETCH_LEVEL)),
            extraXp = listOf(CraftingStatXp(FLETCHING, FLETCH_XP.toDouble())),
            ticks = listOf(1),
            tools = listOf("obj.knife"),
            actionName = "make a dramen staff",
        )

    @OptIn(InternalApi::class)
    private class Fixture {
        private val coroutine = GameCoroutine("crafting-transaction-test")
        private val context =
            ProtectedAccessContextFactory.empty().copy(getEventBus = { EventBus() })
        val player =
            Player().apply {
                client = SilentClient()
                uuid = 799L
                slotId = 1
                assignUid()
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
                for (stat in listOf(CRAFTING, FLETCHING)) {
                    statMap.setXP(stat, 13_034_431)
                    statMap.setBaseLevel(stat, 99)
                    statMap.setCurrentLevel(stat, 99)
                }
            }
        private val initialXp = mapOf(
            CRAFTING to player.statMap.getFineXP(CRAFTING),
            FLETCHING to player.statMap.getFineXP(FLETCHING),
        )

        fun xp(stat: String): Int = player.statMap.getFineXP(stat) - initialXp.getValue(stat)

        fun count(obj: String): Int = player.inv.count(obj)

        fun give(obj: String, count: Int = 1) {
            repeat(count) { player.inv[player.inv.indices.first { player.inv[it] == null }] = InvObj(obj) }
        }

        fun remove(obj: String) {
            player.inv[player.inv.indices.first { player.inv[it]?.id == obj.asRSCM() }] = null
        }

        /** Fills every slot with a spade, then places [except] items in the first slots. */
        fun fill(except: List<String>) {
            for (slot in player.inv.indices) player.inv[slot] = InvObj(SPADE)
            except.forEachIndexed { slot, obj -> player.inv[slot] = InvObj(obj) }
        }

        fun snapshot(): List<Pair<Int, Int>> =
            player.inv.indices.mapNotNull { player.inv[it]?.let { obj -> obj.id to obj.count } }.sortedBy { it.first }

        fun can(product: CraftingProduct): Boolean = exec { canCraft(product, verbose = false) }

        fun craft(product: CraftingProduct): Boolean = exec { craftOnce(product) }

        private fun exec(action: suspend ProtectedAccess.() -> Boolean): Boolean {
            var result: Result<Boolean>? = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Boolean = { access.action() }
            body.startCoroutine(
                object : Continuation<Boolean> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result2: Result<Boolean>) {
                        result = result2
                    }
                }
            )
            return checkNotNull(result) { "Crafting action suspended" }.getOrThrow()
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
        private const val CRAFTING = "stat.crafting"
        private const val FLETCHING = "stat.fletching"
        private const val KNIFE = "obj.knife"
        private const val SPADE = "obj.spade"
        private const val BRANCH = "obj.dramen_branch"
        private const val STAFF = "obj.dramen_staff"
        private const val LEVEL = 31
        private const val FLETCH_LEVEL = 40
        private const val CRAFT_XP = 250
        private const val FLETCH_XP = 800
        private val restored = mutableListOf<() -> Unit>()

        private fun List<Pair<Int, Int>>.minus(obj: String) =
            toMutableList().apply { remove(obj.asRSCM() to 1) }

        private fun List<Pair<Int, Int>>.plus(obj: String) =
            (this + (obj.asRSCM() to 1)).sortedBy { it.first }

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
