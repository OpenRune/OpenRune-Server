@file:OptIn(UncheckedType::class)

package org.rsmod.content.drops

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InvStackType
import dev.openrune.types.InventoryServerType
import dev.openrune.types.util.UncheckedType
import dtx.core.RollResult
import dtx.rs.RSDropTable
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDropReceiveHook
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.events.PlayerNpcDropEvents
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class NpcDropTableKillHookTest {
    @Test
    fun `no subscriber drops the rolled item on the ground`() {
        val f = Fixture()

        f.kill(DropRollItem(BONES, 2))

        assertEquals(listOf(BONES to 2), f.ground)
    }

    @Test
    fun `a replaced item and count reach the ground and the log, not the rolled ones`() {
        val f = Fixture {
            item = COINS
            count = 3
        }

        f.kill(DropRollItem(BONES, 1))

        assertEquals(listOf(COINS to 3), f.ground)
        assertEquals(listOf(COINS to 3), f.logged)
        assertEquals(listOf(COINS to 3), f.dropped)
    }

    @Test
    fun `a destination with room holds the whole drop and nothing reaches the ground`() {
        val f = Fixture { destination = player.inv }

        f.kill(DropRollItem(COINS, 5))

        assertEquals(5, f.count(f.player.inv, COINS_ID))
        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
    }

    @Test
    fun `a full destination leaves the whole drop on the ground`() {
        val f = Fixture { destination = player.inv }
        f.fillInv()

        f.kill(DropRollItem(BONES, 1))

        assertEquals(0, f.count(f.player.inv, BONES_ID))
        assertEquals(listOf(BONES to 1), f.ground)
    }

    @Test
    fun `a stack that would overflow stays whole and the drop goes to the ground`() {
        val f = Fixture { destination = player.inv }
        f.player.inv[0] = InvObj(COINS_ID, Int.MAX_VALUE)

        f.kill(DropRollItem(COINS, 5))

        assertEquals(Int.MAX_VALUE, f.count(f.player.inv, COINS_ID))
        assertEquals(listOf(COINS to 5), f.ground)
    }

    @Test
    fun `non stackable items that only partly fit all go to the ground`() {
        val f = Fixture { destination = player.inv }
        f.fillInv(leaveFree = 1)

        f.kill(DropRollItem(BONES, 2))

        assertEquals(0, f.count(f.player.inv, BONES_ID))
        assertEquals(listOf(BONES to 2), f.ground)
    }

    @Test
    fun `a receive hook that takes the drop stops the destination getting a second copy`() {
        val f = Fixture(receiveHook = { _, _, _ -> true }) { destination = player.inv }

        f.kill(DropRollItem(COINS, 5))

        assertEquals(0, f.count(f.player.inv, COINS_ID))
        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
        assertEquals(listOf(COINS to 5), f.dropped)
    }

    @Test
    fun `a cancelled drop gives nothing but its bonus drops still follow`() {
        val f = Fixture { cancelled = item == BONES }

        f.kill(DropRollItem(BONES, 1, bonusDrops = listOf(DropRollItem(COINS, 2))))

        assertEquals(listOf(COINS to 2), f.ground)
        assertEquals(listOf(COINS to 2), f.logged)
        assertEquals(listOf(COINS to 2), f.dropped)
    }

    @Test
    fun `a count of zero gives nothing`() {
        val f = Fixture { count = 0 }

        f.kill(DropRollItem(BONES, 1))

        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
        assertEquals(emptyList<Pair<String, Int>>(), f.logged)
        assertEquals(emptyList<PlayerNpcDropEvents.Dropped>(), f.droppedEvents)
    }

    @Test
    fun `an empty roll publishes nothing`() {
        var prepared = 0
        val f = Fixture { prepared++ }

        f.kill(DropRollItem("", 1, isNothing = true))

        assertEquals(0, prepared)
    }

    private class Fixture(
        receiveHook: NpcDropReceiveHook? = null,
        onPrepare: PlayerNpcDropEvents.Prepare.() -> Unit = {},
    ) {
        val player = Player()
        val npc = mockk<Npc>(relaxed = true)
        val ground = mutableListOf<Pair<String, Int>>()
        val logged = mutableListOf<Pair<String, Int>>()
        val droppedEvents = mutableListOf<PlayerNpcDropEvents.Dropped>()
        val dropped
            get() = droppedEvents.map { it.item to it.count }

        private val table = mockk<RSDropTable<Player, DropRollItem>>()
        private val hook: NpcDropTableKillHook

        init {
            player.inv =
                Inventory(
                    InventoryServerType(size = 28, stack = InvStackType.Normal),
                    arrayOfNulls(28),
                )
            val registry = mockk<DropTableRegistry>()
            every { registry.forNpc(npc, any()) } returns table
            val objRepo = mockk<ObjRepository>(relaxed = true)
            every { objRepo.add(any<String>(), any(), any(), any(), any(), any()) } answers
                {
                    ground += firstArg<String>() to arg<Int>(4)
                    mockk(relaxed = true)
                }
            every { CollectionLog.grant(any(), any<String>(), any()) } answers
                {
                    logged += secondArg<String>() to thirdArg<Int>()
                }
            val bus = EventBus()
            bus.subscribeUnbound(PlayerNpcDropEvents.Prepare::class.java) { onPrepare() }
            bus.subscribeUnbound(PlayerNpcDropEvents.Dropped::class.java) { droppedEvents += this }
            hook =
                NpcDropTableKillHook(
                    registry = registry,
                    areaChecker = mockk(relaxed = true),
                    objRepo = objRepo,
                    random = mockk(relaxed = true),
                    deathDropHooks = emptySet(),
                    receiveHooks = setOfNotNull(receiveHook),
                    eventBus = bus,
                )
        }

        fun kill(drop: DropRollItem) {
            every { table.roll(any(), any()) } returns RollResult.Single(drop)
            hook.onKill(NpcDeathKillContext(player, npc, 0, CoordGrid(3200, 3200)))
        }

        fun count(inv: Inventory, id: Int) =
            inv.filterNotNull().filter { it.id == id }.sumOf { it.count }

        fun fillInv(leaveFree: Int = 0) {
            for (slot in 0 until player.inv.size - leaveFree) {
                player.inv[slot] = InvObj(FILLER_ID, 1)
            }
        }
    }

    private companion object {
        const val BONES = "obj.bones"
        const val COINS = "obj.coins"
        val BONES_ID by lazy { BONES.asRSCM(RSCMType.OBJ) }
        val COINS_ID by lazy { COINS.asRSCM(RSCMType.OBJ) }
        val FILLER_ID by lazy { "obj.bronze_sword".asRSCM(RSCMType.OBJ) }
        val RESTORED = mutableListOf<() -> Unit>()

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
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
            mockkObject(CollectionLog, ClientScripts)
            justRun { ClientScripts.lootTrackerAddLoot(any(), any(), any(), any(), any()) }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            unmockkObject(CollectionLog, ClientScripts)
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
