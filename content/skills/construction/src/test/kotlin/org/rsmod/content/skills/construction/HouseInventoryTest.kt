package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class HouseInventoryTest {
    private val player =
        Player().apply {
            inv =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                    arrayOfNulls(INV_SIZE),
                )
        }

    private fun give(obj: String, count: Int) {
        assertTrue(player.invAdd(player.inv, obj, count).success, "could not give $count $obj")
    }

    private fun held(obj: String): Int = player.inv.count(obj)

    @Test
    fun `a build takes exactly what it costs`() {
        give(OAK, 4)
        give(BRONZE_NAILS, 10)

        assertTrue(player.spend(listOf(OAK to 3, OBJ_ANY_NAILS to 5)))

        assertEquals(1, held(OAK))
        assertEquals(5, held(BRONZE_NAILS))
    }

    @Test
    fun `falling short of a later material takes nothing at all`() {
        give(OAK, 3)
        give(BRONZE_NAILS, 2)

        assertFalse(player.spend(listOf(OAK to 3, OBJ_ANY_NAILS to 5)))

        assertEquals(3, held(OAK))
        assertEquals(2, held(BRONZE_NAILS))
    }

    @Test
    fun `nails are paid with the cheapest tier first`() {
        give(BRONZE_NAILS, 2)
        give(IRON_NAILS, 10)
        give(RUNE_NAILS, 10)

        assertTrue(player.spend(listOf(OBJ_ANY_NAILS to 5)))

        assertEquals(0, held(BRONZE_NAILS))
        assertEquals(7, held(IRON_NAILS))
        assertEquals(10, held(RUNE_NAILS))
    }

    @Test
    fun `nails short across every tier take none of them`() {
        give(BRONZE_NAILS, 2)
        give(IRON_NAILS, 2)

        assertFalse(player.spend(listOf(OBJ_ANY_NAILS to 5)))

        assertEquals(2, held(BRONZE_NAILS))
        assertEquals(2, held(IRON_NAILS))
    }

    @Test
    fun `a second build on the same materials gets nothing`() {
        give(OAK, 3)

        assertTrue(player.spend(listOf(OAK to 3)))
        assertFalse(player.spend(listOf(OAK to 3)))

        assertEquals(0, held(OAK))
    }

    @Test
    fun `a material gone between the check and the take leaves the rest`() {
        give(OAK, 3)
        give(BRONZE_NAILS, 5)
        val costs = listOf(OAK to 3, OBJ_ANY_NAILS to 5)
        assertNotNull(spendPlan(player.inv, costs))

        assertTrue(player.invDel(player.inv, OAK, 1).success)

        assertFalse(player.spend(costs))
        assertEquals(2, held(OAK))
        assertEquals(5, held(BRONZE_NAILS))
    }

    @Test
    fun `a room's coins come out of a full stack`() {
        give(OBJ_COINS, Int.MAX_VALUE)

        assertTrue(player.spend(listOf(OBJ_COINS to ROOM_COST)))

        assertEquals(Int.MAX_VALUE - ROOM_COST, held(OBJ_COINS))
    }

    @Test
    fun `too few coins for a room takes none`() {
        give(OBJ_COINS, ROOM_COST - 1)

        assertFalse(player.spend(listOf(OBJ_COINS to ROOM_COST)))

        assertEquals(ROOM_COST - 1, held(OBJ_COINS))
    }

    @Test
    fun `nexus runes are taken whole or not at all`() {
        give(LAW, 1000)
        give(AIR, 2999)
        val cost = listOf(LAW to 1000, AIR to 3000)

        assertFalse(player.spend(cost))
        assertEquals(1000, held(LAW))

        give(AIR, 1)
        assertTrue(player.spend(cost))
        assertEquals(0, held(LAW))
        assertEquals(0, held(AIR))
    }

    @Test
    fun `a dispenser gives exactly one`() {
        assertTrue(player.dispense(KETTLE))

        assertEquals(1, held(KETTLE))
    }

    @Test
    fun `a dispenser gives nothing into a full inventory`() {
        give(OAK, INV_SIZE)

        assertFalse(player.dispense(KETTLE))

        assertEquals(0, held(KETTLE))
        assertEquals(INV_SIZE, held(OAK))
    }

    @Test
    fun `filling swaps one for one`() {
        give(BUCKET, 2)

        assertTrue(player.fill(BUCKET, BUCKET_WATER))

        assertEquals(1, held(BUCKET))
        assertEquals(1, held(BUCKET_WATER))
    }

    @Test
    fun `filling with nothing to fill creates nothing`() {
        assertFalse(player.fill(BUCKET, BUCKET_WATER))

        assertEquals(0, held(BUCKET_WATER))
    }

    @Test
    fun `filling works in a full inventory`() {
        give(BUCKET, 1)
        give(OAK, INV_SIZE - 1)

        assertTrue(player.fill(BUCKET, BUCKET_WATER))

        assertEquals(0, held(BUCKET))
        assertEquals(1, held(BUCKET_WATER))
        assertEquals(INV_SIZE - 1, held(OAK))
    }

    companion object {
        private const val INV_SIZE = 28
        private const val ROOM_COST = 1000
        private const val OAK = "obj.plank_oak"
        private const val BRONZE_NAILS = "obj.nails_bronze"
        private const val IRON_NAILS = "obj.nails_iron"
        private const val RUNE_NAILS = "obj.nails_rune"
        private const val LAW = "obj.lawrune"
        private const val AIR = "obj.airrune"
        private const val KETTLE = "obj.poh_kettle_empty"
        private const val BUCKET = "obj.bucket_empty"
        private const val BUCKET_WATER = "obj.bucket_water"

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
