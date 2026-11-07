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
import jakarta.inject.Provider
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDropReceiveHook
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.NpcKillExtraDrops
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.events.LootItem
import org.rsmod.api.player.events.LootSource
import org.rsmod.api.player.events.PlayerLootEvents
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.other.pets.PetRewards
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
class LootDeliveryTest {
    @Test
    fun `no subscriber drops the rolled item on the ground`() {
        val f = Fixture()

        f.kill(DropRollItem(BONES, 2))

        assertEquals(listOf(BONES to 2), f.ground)
    }

    @Test
    fun `a replaced item and count reach the ground and the log, not the rolled ones`() {
        val f = Fixture {
            items[0].item = COINS
            items[0].count = 3
        }

        f.kill(DropRollItem(BONES, 1))

        assertEquals(listOf(COINS to 3), f.ground)
        assertEquals(listOf(COINS to 3), f.logged)
        assertEquals(listOf(COINS to 3), f.received)
    }

    @Test
    fun `a destination with room holds the whole drop and nothing reaches the ground`() {
        val f = Fixture { items.forEach { it.destination = player.inv } }

        f.kill(DropRollItem(COINS, 5))

        assertEquals(5, f.count(f.player.inv, COINS_ID))
        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
        assertSame(f.player.inv, f.receivedEvents.single().items.single().inventory)
    }

    @Test
    fun `a full destination leaves the whole drop on the ground`() {
        val f = Fixture { items.forEach { it.destination = player.inv } }
        f.fillInv()

        f.kill(DropRollItem(BONES, 1))

        assertEquals(0, f.count(f.player.inv, BONES_ID))
        assertEquals(listOf(BONES to 1), f.ground)
        assertNull(f.receivedEvents.single().items.single().inventory)
    }

    @Test
    fun `a stack that would overflow stays whole and the drop goes to the ground`() {
        val f = Fixture { items.forEach { it.destination = player.inv } }
        f.player.inv[0] = InvObj(COINS_ID, Int.MAX_VALUE)

        f.kill(DropRollItem(COINS, 5))

        assertEquals(Int.MAX_VALUE, f.count(f.player.inv, COINS_ID))
        assertEquals(listOf(COINS to 5), f.ground)
    }

    @Test
    fun `non stackable items that only partly fit all go to the ground`() {
        val f = Fixture { items.forEach { it.destination = player.inv } }
        f.fillInv(leaveFree = 1)

        f.kill(DropRollItem(BONES, 2))

        assertEquals(0, f.count(f.player.inv, BONES_ID))
        assertEquals(listOf(BONES to 2), f.ground)
    }

    @Test
    fun `a receive hook that takes the drop stops the destination getting a second copy`() {
        val f = Fixture(receiveHook = { _, _, _ -> true }) {
            items.forEach { it.destination = player.inv }
        }

        f.kill(DropRollItem(COINS, 5))

        assertEquals(0, f.count(f.player.inv, COINS_ID))
        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
        assertEquals(listOf(COINS to 5), f.received)
    }

    @Test
    fun `a removed item gives nothing but its bonus drops still follow`() {
        val f = Fixture { items.removeAll { it.item == BONES } }

        f.kill(DropRollItem(BONES, 1, bonusDrops = listOf(DropRollItem(COINS, 2))))

        assertEquals(listOf(COINS to 2), f.ground)
        assertEquals(listOf(COINS to 2), f.logged)
        assertEquals(listOf(COINS to 2), f.received)
    }

    @Test
    fun `a count of zero gives nothing and publishes no received event`() {
        val f = Fixture { items[0].count = 0 }

        f.kill(DropRollItem(BONES, 1))

        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
        assertEquals(emptyList<Pair<String, Int>>(), f.logged)
        assertEquals(0, f.receivedEvents.size)
    }

    @Test
    fun `an empty roll publishes nothing`() {
        var prepared = 0
        val f = Fixture { prepared++ }

        f.kill(DropRollItem("", 1, isNothing = true))

        assertEquals(0, prepared)
    }

    @Test
    fun `a whole roll arrives in one prepare and one received event`() {
        val seen = mutableListOf<List<String>>()
        val f = Fixture { seen += items.map { it.item } }

        f.kill(
            DropRollItem(BONES, 1, bonusDrops = listOf(DropRollItem(COINS, 2))),
            DropRollItem(COINS, 7),
        )

        assertEquals(listOf(listOf(BONES, COINS, COINS)), seen)
        assertEquals(1, f.receivedEvents.size)
        assertEquals(listOf(BONES to 1, COINS to 2, COINS to 7), f.received)
    }

    @Test
    fun `an item added in prepare is delivered`() {
        val f = Fixture { items += LootItem(COINS, 4, destination = null) }

        f.kill(DropRollItem(BONES, 1))

        assertEquals(listOf(BONES to 1, COINS to 4), f.ground)
        assertEquals(listOf(BONES to 1, COINS to 4), f.received)
    }

    @Test
    fun `a failed condition skips the drop and its bonus drops`() {
        val f = Fixture()

        f.kill(
            DropRollItem(BONES, 1, condition = { false }, bonusDrops = listOf(DropRollItem(COINS, 2)))
        )

        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
        assertEquals(0, f.receivedEvents.size)
    }

    @Test
    fun `chest loot goes to the inventory and skips receive hooks`() {
        val f = Fixture(receiveHook = { _, _, _ -> true })

        f.open(DropRollItem(COINS, 5))

        assertEquals(5, f.count(f.player.inv, COINS_ID))
        assertEquals(emptyList<Pair<String, Int>>(), f.ground)
        assertSame(f.player.inv, f.receivedEvents.single().items.single().inventory)
    }

    @Test
    fun `chest loot that does not fit the inventory lands on the ground`() {
        val f = Fixture()
        f.fillInv()

        f.open(DropRollItem(BONES, 1), DropRollItem(COINS, 5))

        assertEquals(0, f.count(f.player.inv, COINS_ID))
        assertEquals(listOf(BONES to 1, COINS to 5), f.ground)
        assertEquals(listOf(BONES to 1, COINS to 5), f.logged)
    }

    @Test
    fun `a pet goes to the pet system, is logged and is listed as received`() {
        val f = Fixture()

        f.open(DropRollItem(PET, 1), DropRollItem(COINS, 5))

        assertEquals(listOf(PET), f.petsGiven)
        assertEquals(5, f.count(f.player.inv, COINS_ID))
        assertEquals(listOf(PET to 1, COINS to 5), f.logged)
        assertNull(f.receivedEvents.single().items.first().inventory)
        assertEquals(listOf(PET to 1, COINS to 5), f.received)
    }

    @Test
    fun `a pet removed in prepare is never given`() {
        val f = Fixture { items.removeAll { it.item == PET } }

        f.open(DropRollItem(PET, 1))

        assertEquals(emptyList<String>(), f.petsGiven)
        assertEquals(emptyList<Pair<String, Int>>(), f.logged)
    }

    @Test
    fun `extra kill drops join the kill's loot in the same prepare`() {
        val seen = mutableListOf<List<String>>()
        val f = Fixture(extraDrop = PET) { seen += items.map { it.item } }

        f.kill(DropRollItem(BONES, 1))

        assertEquals(listOf(listOf(BONES, PET)), seen)
        assertEquals(listOf(PET), f.petsGiven)
        assertEquals(listOf(BONES to 1), f.ground)
    }

    @Test
    fun `extra kill drops still come for an npc with no drop table`() {
        val f = Fixture(extraDrop = PET, hasTable = false)

        f.kill()

        assertEquals(listOf(PET), f.petsGiven)
    }

    @Test
    fun `a deferred collection log skips items but still logs pets`() {
        val f = Fixture()

        f.open(DropRollItem(PET, 1), DropRollItem(COINS, 5), collectionLog = false)

        assertEquals(listOf(PET to 1), f.logged)
        assertEquals(5, f.count(f.player.inv, COINS_ID))
    }

    private class Fixture(
        receiveHook: NpcDropReceiveHook? = null,
        extraDrop: String? = null,
        hasTable: Boolean = true,
        onPrepare: PlayerLootEvents.Prepare.() -> Unit = {},
    ) {
        val petsGiven = mutableListOf<String>()
        val player = Player()
        val npc = mockk<Npc>(relaxed = true)
        val ground = mutableListOf<Pair<String, Int>>()
        val logged = mutableListOf<Pair<String, Int>>()
        val receivedEvents = mutableListOf<PlayerLootEvents.Received>()
        val received
            get() = receivedEvents.flatMap { event -> event.items.map { it.item to it.count } }

        private val table = mockk<RSDropTable<Player, DropRollItem>>()
        private val loot: LootDelivery
        private val hook: NpcDropTableKillHook

        init {
            player.inv =
                Inventory(
                    InventoryServerType(size = 28, stack = InvStackType.Normal),
                    arrayOfNulls(28),
                )
            val registry = mockk<DropTableRegistry>()
            every { registry.forNpc(npc, any()) } returns if (hasTable) table else null
            val pets = mockk<PetRewards>()
            every { pets.give(any(), any()) } answers
                {
                    val obj = secondArg<String>()
                    (obj == PET).also { if (it) petsGiven += obj }
                }
            val extraDrops = NpcKillExtraDrops { _, _ -> listOfNotNull(extraDrop?.let { DropRollItem(it, 1) }) }
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
            bus.subscribeUnbound(PlayerLootEvents.Prepare::class.java) { onPrepare() }
            bus.subscribeUnbound(PlayerLootEvents.Received::class.java) { receivedEvents += this }
            val areaChecker = mockk<AreaChecker>(relaxed = true)
            loot =
                LootDelivery(
                    areaChecker = areaChecker,
                    objRepo = objRepo,
                    random = mockk(relaxed = true),
                    pets = pets,
                    deathDropHooks = Provider { emptySet() },
                    receiveHooks = Provider { setOfNotNull(receiveHook) },
                    eventBus = bus,
                )
            hook =
                NpcDropTableKillHook(
                    registry = registry,
                    areaChecker = areaChecker,
                    deathDropHooks = emptySet(),
                    extraDrops = setOf(extraDrops),
                    loot = loot,
                )
        }

        fun kill(vararg drops: DropRollItem) {
            val result =
                if (drops.size == 1) RollResult.Single(drops[0]) else RollResult.ListOf(drops.toList())
            every { table.roll(any(), any()) } returns result
            hook.onKill(NpcDeathKillContext(player, npc, 0, CoordGrid(3200, 3200)))
        }

        fun open(vararg drops: DropRollItem, collectionLog: Boolean = true) {
            loot.give(player, LootSource.Loc(CHEST), drops.toList(), collectionLog = collectionLog)
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
        const val CHEST = "loc.crystal_chestclosed"
        const val PET = "obj.dompet"
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
