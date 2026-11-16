package org.rsmod.content.travel.jewellery

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ChargeChainTest {
    @Test
    fun `spending a charge swaps to the next obj in the chain`() {
        val inv = inventory("obj.amulet_of_glory_4")
        val result = glory().spend(inv, 0, id("obj.amulet_of_glory_4"))
        assertEquals(SpendResult.Remaining(3), result)
        assertEquals(id("obj.amulet_of_glory_3"), inv[0]?.id)
        assertEquals(1, inv.occupiedSpace())
    }

    @Test
    fun `last charge leaves the spent obj behind`() {
        val inv = inventory("obj.amulet_of_glory_1")
        assertEquals(SpendResult.Depleted, glory().spend(inv, 0, id("obj.amulet_of_glory_1")))
        assertEquals(id("obj.amulet_of_glory"), inv[0]?.id)
        assertEquals(1, inv[0]?.count)
    }

    @Test
    fun `last charge leaves the remains when there is no spent obj`() {
        val inv = inventory("obj.slayer_ring_1")
        assertEquals(SpendResult.Depleted, slayer().spend(inv, 0, id("obj.slayer_ring_1")))
        assertEquals(id("obj.slayer_gem"), inv[0]?.id)
    }

    @Test
    fun `last charge of a crumbling piece empties the slot`() {
        val chain = chain("obj.ring_of_dueling_2", "obj.ring_of_dueling_1")
        val inv = inventory("obj.ring_of_dueling_1")
        assertEquals(SpendResult.Depleted, chain.spend(inv, 0, id("obj.ring_of_dueling_1")))
        assertNull(inv[0])
    }

    @Test
    fun `eternal pieces are never changed`() {
        val chain = ChargeChain(listOf(id("obj.amulet_of_glory_inf")), null, null, eternal = true)
        val inv = inventory("obj.amulet_of_glory_inf")
        assertEquals(SpendResult.Unlimited, chain.spend(inv, 0, id("obj.amulet_of_glory_inf")))
        assertEquals(InvObj("obj.amulet_of_glory_inf"), inv[0])
    }

    @Test
    fun `nothing is spent when the piece left the slot before the teleport landed`() {
        val emptied = inventory("obj.amulet_of_glory_4")
        emptied[0] = null
        assertEquals(SpendResult.Vanished, glory().spend(emptied, 0, id("obj.amulet_of_glory_4")))
        assertNull(emptied[0])

        val swapped = inventory("obj.coins")
        assertEquals(SpendResult.Vanished, glory().spend(swapped, 0, id("obj.amulet_of_glory_4")))
        assertEquals(InvObj("obj.coins"), swapped[0])
    }

    @Test
    fun `a second spend in the same tick cannot take another charge`() {
        val inv = inventory("obj.amulet_of_glory_4")
        val expected = id("obj.amulet_of_glory_4")
        glory().spend(inv, 0, expected)
        assertEquals(SpendResult.Vanished, glory().spend(inv, 0, expected))
        assertEquals(id("obj.amulet_of_glory_3"), inv[0]?.id)
    }

    @Test
    fun `charges count down from the first obj`() {
        assertEquals(6, glory().charges(id("obj.amulet_of_glory_6")))
        assertEquals(1, glory().charges(id("obj.amulet_of_glory_1")))
        assertNull(glory().charges(id("obj.amulet_of_glory")))
    }

    private fun glory() =
        ChargeChain(
            charged =
                listOf(
                        "obj.amulet_of_glory_6",
                        "obj.amulet_of_glory_5",
                        "obj.amulet_of_glory_4",
                        "obj.amulet_of_glory_3",
                        "obj.amulet_of_glory_2",
                        "obj.amulet_of_glory_1",
                    )
                    .map(::id),
            spent = id("obj.amulet_of_glory"),
            remains = null,
            eternal = false,
        )

    private fun slayer() =
        ChargeChain(
            charged = listOf(id("obj.slayer_ring_2"), id("obj.slayer_ring_1")),
            spent = null,
            remains = id("obj.slayer_gem"),
            eternal = false,
        )

    private fun chain(vararg objs: String) =
        ChargeChain(objs.map(::id), spent = null, remains = null, eternal = false)

    private fun id(name: String): Int = name.asRSCM(RSCMType.OBJ)

    private fun inventory(vararg objs: String): Inventory {
        val type = checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM()))
        val inv = Inventory(type, arrayOfNulls(28))
        objs.forEachIndexed { slot, obj -> inv[slot] = InvObj(obj) }
        return inv
    }

    companion object {
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
        }
    }
}
