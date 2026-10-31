package org.rsmod.content.skills.crafting

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.PlayerQueueEvents
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.skills.crafting.scripts.HeldCraftingScript
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.validButtons
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.queue.PlayerQueueList
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Drives the real [HeldCraftingScript] (native Fletch Op1 and knife-use [HeldUEvents]) and the real
 * [CraftingWorkerScript] queue handler against the packed cache recipes.
 * The only event clock used is `currentMapClock == processedMapClock`, so the first queue delay is
 * the raw recipe tick and later delays include the worker's fixed handler compensation.
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BarkFletchingTest {
    @Test
    fun `fletching level 39 is denied and a boosted level 40 crafts exact fletching xp only`() {
        val denied = Fixture(fletching = 39)
        denied.give(0, KNIFE)
        denied.give(1, WOOD)
        denied.fletch(1)
        assertTrue(denied.pending().isEmpty())
        assertEquals(1, denied.count(WOOD))
        assertEquals(0, denied.count(STAFF))
        assertEquals(0, denied.xp(FLETCHING))

        val f = Fixture(fletching = 40)
        f.give(0, KNIFE)
        f.give(1, WOOD)
        f.fletch(1)
        f.drain()
        assertEquals(0, f.count(WOOD))
        assertEquals(1, f.count(STAFF))
        assertEquals(1, f.count(KNIFE))
        assertEquals(FLETCH_FINE_XP, f.xp(FLETCHING))
        assertEquals(0, f.xp(CRAFTING))
    }

    @Test
    fun `native fletch and knife use both replace the input in a full inventory`() {
        for (mode in listOf("fletch", "knife on bark")) {
            val f = Fixture()
            f.fillWithSpades()
            f.give(0, KNIFE)
            f.give(1, WOOD)
            when (mode) {
                "fletch" -> f.fletch(1)
                else -> f.useKnife(0, 1)
            }
            f.drain()
            assertEquals(26, f.count(SPADE))
            assertEquals(1, f.count(KNIFE))
            assertEquals(0, f.count(WOOD))
            assertEquals(1, f.count(STAFF))
            assertEquals(FLETCH_FINE_XP, f.xp(FLETCHING))
        }
    }

    @Test
    fun `special knife shortens only later cycles whether carried or worn`() {
        val (ordinaryFirst, ordinaryLater) = delays { give(0, KNIFE) }
        val (carriedFirst, carriedLater) = delays { give(0, SPECIAL) }
        val (wornFirst, wornLater) = delays { give(0, KNIFE); player.worn[SHIELD_SLOT] = InvObj(SPECIAL) }
        // Public cadence: first cycle 4 ticks for everyone; later cycles 4 ordinary, 3 special.
        assertEquals(4, ordinaryFirst)
        assertEquals(4, carriedFirst)
        assertEquals(4, wornFirst)
        assertEquals(ordinaryLater - 1, carriedLater)
        assertEquals(carriedLater, wornLater)

        // The special knife leaves while an ordinary knife stays: the next cycle is ordinary.
        val (_, fallback) = delays(removeSpecialBeforeFirstCraft = true) { give(0, KNIFE); give(10, SPECIAL) }
        assertEquals(ordinaryLater, fallback)
    }

    @Test
    fun `worn special knife alone cannot start fletching`() {
        val f = Fixture()
        f.player.worn[SHIELD_SLOT] = InvObj(SPECIAL)
        f.give(1, WOOD)
        f.fletch(1)
        assertTrue(f.pending().isEmpty())
        assertEquals(1, f.count(WOOD))
        assertEquals(0, f.count(STAFF))
    }

    @Test
    fun `losing the only special knife aborts the next cycle without cost`() {
        val before = Fixture()
        before.give(0, SPECIAL)
        before.give(1, WOOD, 3)
        before.fletch(1)
        before.remove(SPECIAL)
        before.step()
        assertTrue(before.pending().isEmpty())
        assertEquals(3, before.count(WOOD))
        assertEquals(0, before.count(STAFF))
        assertEquals(0, before.xp(FLETCHING))

        val mid = Fixture()
        mid.give(0, SPECIAL)
        mid.give(1, WOOD, 3)
        mid.fletch(1)
        mid.step()
        mid.remove(SPECIAL)
        mid.step()
        assertTrue(mid.pending().isEmpty())
        assertEquals(2, mid.count(WOOD))
        assertEquals(1, mid.count(STAFF))
        assertEquals(FLETCH_FINE_XP, mid.xp(FLETCHING))
    }

    @Test
    fun `tool bark or level vanishing before commit pays nothing`() {
        val vanish =
            listOf<Fixture.() -> Unit>(
                { remove(KNIFE) },
                {
                    remove(WOOD)
                    remove(WOOD)
                },
                { player.statMap.setCurrentLevel(FLETCHING, 39) },
            )
        for (change in vanish) {
            val f = Fixture()
            f.give(0, KNIFE)
            f.give(1, WOOD, 2)
            f.fletch(1)
            assertEquals(1, f.pending().size)
            f.change()
            val items = f.snapshot()
            f.step()
            assertTrue(f.pending().isEmpty())
            assertEquals(items, f.snapshot())
            assertEquals(0, f.xp(FLETCHING))
            assertEquals(0, f.xp(CRAFTING))
        }
    }

    @Test
    fun `stale clicked slots in either item use direction or native op do nothing`() {
        val f = Fixture()
        f.give(0, KNIFE)
        f.give(1, WOOD)
        f.give(2, SPADE)
        val items = f.snapshot()

        f.fletch(2) // slot now holds a spade, not the clicked bark
        f.fletch(5) // empty slot
        f.useKnife(0, 1, slotOfA = 1, slotOfB = 0) // swapped
        f.useKnife(1, 0, slotOfA = 4) // bark moved away from the clicked slot
        f.useKnife(0, 1, slotOfB = 2) // clicked slot now holds a spade
        assertTrue(f.pending().isEmpty())
        assertEquals(items, f.snapshot())
        assertEquals(0, f.xp(FLETCHING))
    }

    @Test
    fun `quantity cancel and exhausted source cannot duplicate output`() {
        val cancelled = Fixture()
        cancelled.give(0, KNIFE)
        cancelled.give(1, WOOD, 3)
        cancelled.fletch(1, cancel = true)
        assertTrue(cancelled.pending().isEmpty())
        assertEquals(3, cancelled.count(WOOD))

        val partial = Fixture()
        partial.give(0, KNIFE)
        partial.give(1, WOOD, 3)
        partial.fletch(1, quantity = 2)
        partial.drain()
        assertEquals(1, partial.count(WOOD))
        assertEquals(2, partial.count(STAFF))
        assertEquals(2 * FLETCH_FINE_XP, partial.xp(FLETCHING))

        val exhausted = Fixture()
        exhausted.give(0, KNIFE)
        exhausted.give(1, WOOD, 2)
        exhausted.fletch(1, quantity = 99)
        exhausted.drain()
        assertEquals(0, exhausted.count(WOOD))
        assertEquals(2, exhausted.count(STAFF))
        assertEquals(2 * FLETCH_FINE_XP, exhausted.xp(FLETCHING))
    }

    @Test
    fun `repeated operations over one bark produce one output`() {
        val f = Fixture()
        f.give(0, KNIFE)
        f.give(1, WOOD)
        f.fletch(1)
        f.fletch(1)
        f.useKnife(0, 1)
        f.drain()
        assertEquals(0, f.count(WOOD))
        assertEquals(1, f.count(STAFF))
        assertEquals(1, f.count(KNIFE))
        assertEquals(FLETCH_FINE_XP, f.xp(FLETCHING))
    }

    /** First queued delay and the delay queued after the first successful craft. */
    private fun delays(
        removeSpecialBeforeFirstCraft: Boolean = false,
        setup: Fixture.() -> Unit,
    ): Pair<Int, Int> {
        val f = Fixture()
        f.setup()
        f.give(1, WOOD, 3)
        f.fletch(1)
        val first = f.pending().single().remainingCycles
        if (removeSpecialBeforeFirstCraft) f.remove(SPECIAL)
        f.step()
        assertEquals(1, f.count(STAFF))
        return first to f.pending().single().remainingCycles
    }

    @OptIn(InternalApi::class)
    private class Fixture(fletching: Int = 40) {
        private val events = EventBus()
        private val coroutine = GameCoroutine("bark-fletching-test")
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(getEventBus = { events }, getAlignment = { TextAlignment() })
        private var result: Result<Unit>? = null
        val player =
            Player().apply {
                client = SilentClient()
                uuid = 801L
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
                statMap.setXP(CRAFTING, 0)
                statMap.setBaseLevel(CRAFTING, 1)
                statMap.setCurrentLevel(CRAFTING, 1)
                // Base 30 keeps the recipe's 40 requirement reachable only through current level.
                statMap.setXP(FLETCHING, 13_363)
                statMap.setBaseLevel(FLETCHING, 30)
                statMap.setCurrentLevel(FLETCHING, fletching.toByte())
            }
        private val initialXp =
            mapOf(
                CRAFTING to player.statMap.getFineXP(CRAFTING),
                FLETCHING to player.statMap.getFineXP(FLETCHING),
            )

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(HeldCraftingScript()) { scripts.startup() }
            with(CraftingWorkerScript(WorldRepository(ZoneUpdateMap()))) { scripts.startup() }
        }

        fun xp(stat: String) = player.statMap.getFineXP(stat) - initialXp.getValue(stat)

        fun count(obj: String) = player.inv.count(obj)

        fun give(slot: Int, obj: String, count: Int = 1) {
            if (count == 1) {
                player.inv[slot] = InvObj(obj)
            } else {
                repeat(count) { player.inv[slot + it] = InvObj(obj) }
            }
        }

        fun remove(obj: String) {
            player.inv[player.inv.indices.first { player.inv[it]?.id == obj.asRSCM() }] = null
        }

        fun fillWithSpades() {
            for (slot in player.inv.indices) player.inv[slot] = InvObj(SPADE)
        }

        fun snapshot() =
            player.inv.indices
                .mapNotNull { slot -> player.inv[slot]?.let { slot to it.id to it.count } }

        fun pending(): List<PlayerQueueList.Queue> {
            val iterator = player.weakQueueList.iterator() ?: return emptyList()
            return buildList { while (iterator.hasNext()) add(iterator.next()) }
        }

        /** Native Fletch (Op1) on the bark in [slot], then answers the real skill-multi prompt. */
        fun fletch(slot: Int, quantity: Int = Int.MAX_VALUE, cancel: Boolean = false) {
            val obj = InvObj(WOOD)
            val type = checkNotNull(ServerCacheManager.getItem(obj.id))
            start { events.publish(this, HeldObjEvents.Op1(slot, obj, type, player.inv)) }
            answer(quantity, cancel)
        }

        /**
         * Item-use of the obj at slot [a] on the obj at slot [b], delivered in whichever order the
         * script registered (as the engine's opHeldU does). [slotOfA]/[slotOfB] override the slot
         * carried by the event, to model a stale click.
         */
        fun useKnife(a: Int, b: Int, slotOfA: Int = a, slotOfB: Int = b) {
            val first = checkNotNull(player.inv[a]).id
            val second = checkNotNull(player.inv[b]).id
            val key = EventBus.composeLongKey(first, second)
            val forward = events.contains(HeldUEvents.Type::class.java, key)
            val (x, xSlot, y, ySlot) =
                if (forward) listOf(first, slotOfA, second, slotOfB)
                else listOf(second, slotOfB, first, slotOfA)
            val xType = checkNotNull(ServerCacheManager.getItem(x))
            val yType = checkNotNull(ServerCacheManager.getItem(y))
            start { events.publish(this, HeldUEvents.Type(xType, xSlot, yType, ySlot)) }
            answer(Int.MAX_VALUE, cancel = false)
        }

        /** Processes the oldest weak queue entry through the real worker handler. */
        fun step(): Boolean {
            val iterator = player.weakQueueList.iterator() ?: return false
            val queue = iterator.next()
            iterator.remove()
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            start { events.publish(this, PlayerQueueEvents.Protected(queue.args, queue.id)) }
            answer(Int.MAX_VALUE, cancel = false)
            return true
        }

        fun drain() {
            repeat(100) { if (!step()) return }
            error("Crafting queue never emptied")
        }

        private fun answer(quantity: Int, cancel: Boolean) {
            repeat(20) {
                if (coroutine.isIdle) {
                    result?.getOrThrow()
                    return
                }
                check(coroutine.isAwaiting(ResumePauseButtonInput::class)) { "Unexpected suspension" }
                val input =
                    if (player.ui.containsModal("interface.skillmulti")) {
                        if (cancel) {
                            // Closing the prompt cancels the suspended interaction.
                            coroutine.cancel()
                            check(result?.exceptionOrNull() is CancellationException)
                            return
                        }
                        ResumePauseButtonInput(validButtons[0], quantity)
                    } else {
                        ResumePauseButtonInput("component.messagebox:continue", -1)
                    }
                coroutine.resumeWith(input)
            }
            error("Interaction did not finish")
        }

        private fun start(block: suspend ProtectedAccess.() -> Unit) {
            result = null
            player.activeCoroutine = coroutine
            val body: suspend () -> Unit = { ProtectedAccess(player, coroutine, context).block() }
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
        private const val KNIFE = CraftingConstants.KNIFE
        private const val SPECIAL = CraftingConstants.FLETCHING_KNIFE
        private const val SPADE = "obj.spade"
        private const val WOOD = "obj.celastrus_wood"
        private const val STAFF = "obj.battlestaff"
        private const val FLETCH_FINE_XP = 800
        private const val SHIELD_SLOT = 5
        private val restored = mutableListOf<() -> Unit>()

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
            // Uninitialised lateinit cannot be reset without reflection; then it stays set.
            val oldRepo = runCatching { CraftingRuntime.worldRepo }.getOrNull()
            if (oldRepo != null) restored += { CraftingRuntime.worldRepo = oldRepo }
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
