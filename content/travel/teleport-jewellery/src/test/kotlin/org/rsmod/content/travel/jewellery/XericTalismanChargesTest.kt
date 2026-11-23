package org.rsmod.content.travel.jewellery

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.utils.bits.withBits

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class XericTalismanChargesTest {
    @Test
    fun `charging moves every fang into the talisman`() {
        val inv = inventory(InvObj(INERT), InvObj(FANG, 40))
        assertEquals(ChargeResult.Charged(40), talisman().charge(inv, 0))
        assertEquals(40, talisman().charges(inv[0]))
        assertNull(inv[1])
        assertEquals(40, fangsAndCharges(inv))
    }

    @Test
    fun `charging stops at the maximum and keeps the leftover fangs`() {
        val inv = inventory(charged(990), InvObj(FANG, 25))
        assertEquals(ChargeResult.Charged(1_000), talisman().charge(inv, 0))
        assertEquals(15, inv[1]?.count)
        assertEquals(1_015, fangsAndCharges(inv))
    }

    @Test
    fun `a full talisman takes no fangs`() {
        val inv = inventory(charged(1_000), InvObj(FANG, 5))
        assertEquals(ChargeResult.Full, talisman().charge(inv, 0))
        assertEquals(5, inv[1]?.count)
        assertEquals(1_000, talisman().charges(inv[0]))
    }

    @Test
    fun `charging a talisman that left its slot moves nothing`() {
        val inv = inventory(null, InvObj(FANG, 5))
        assertEquals(ChargeResult.Vanished, talisman().charge(inv, 0))
        assertEquals(5, inv[1]?.count)
    }

    @Test
    fun `uncharging returns the fangs onto the existing stack`() {
        val inv = inventory(charged(30), InvObj(FANG, 10))
        assertEquals(UnchargeResult.Uncharged(30), talisman().uncharge(inv, 0))
        assertEquals(id(INERT), inv[0]?.id)
        assertEquals(0, talisman().charges(inv[0]))
        assertEquals(40, inv[1]?.count)
        assertEquals(40, fangsAndCharges(inv))
    }

    @Test
    fun `uncharging needs a free slot when no fangs are held`() {
        val inv = inventory(charged(30))
        for (slot in 1 until inv.size) {
            inv[slot] = InvObj("obj.coins", 1)
        }
        assertEquals(UnchargeResult.NoSpace, talisman().uncharge(inv, 0))
        assertEquals(30, talisman().charges(inv[0]))
    }

    @Test
    fun `uncharging never overflows the fang stack`() {
        val inv = inventory(charged(30), InvObj(FANG, Int.MAX_VALUE - 10))
        assertEquals(UnchargeResult.NoSpace, talisman().uncharge(inv, 0))
        assertEquals(30, talisman().charges(inv[0]))
        assertEquals(Int.MAX_VALUE - 10, inv[1]?.count)
    }

    @Test
    fun `teleporting spends one charge and empties to the inert talisman`() {
        val inv = inventory(charged(2))
        assertEquals(1, talisman().spend(inv, 0))
        assertEquals(1, talisman().charges(inv[0]))
        assertEquals(0, talisman().spend(inv, 0))
        assertEquals(id(INERT), inv[0]?.id)
        assertNull(talisman().spend(inv, 0))
    }

    @Test
    fun `dismantling turns the talisman into fangs`() {
        val alone = inventory(InvObj(INERT))
        assertTrue(talisman().dismantle(alone, 0, 100))
        assertEquals(InvObj(FANG, 100), alone[0])

        val stacked = inventory(InvObj(INERT), InvObj(FANG, 7))
        assertTrue(talisman().dismantle(stacked, 0, 100))
        assertNull(stacked[0])
        assertEquals(107, stacked[1]?.count)
    }

    @Test
    fun `dismantling never overflows the fang stack`() {
        val inv = inventory(InvObj(INERT), InvObj(FANG, Int.MAX_VALUE - 50))
        assertFalse(talisman().dismantle(inv, 0, 100))
        assertEquals(id(INERT), inv[0]?.id)
        assertEquals(Int.MAX_VALUE - 50, inv[1]?.count)
    }

    private fun talisman() = XericTalismanCharges(CHARGED, INERT, FANG, bits(), 1_000)

    private fun charged(count: Int) = InvObj(CHARGED, 1, 0.withBits(bits(), count))

    private fun fangsAndCharges(inv: Inventory): Int =
        inv.filterNotNull { it.id == id(FANG) }.sumOf { it.count } +
            inv.filterNotNull { it.id == id(CHARGED) }.sumOf { talisman().charges(it) }

    private fun bits(): IntRange =
        checkNotNull(ServerCacheManager.getVarObj("varobj.charges_16383".asRSCM(RSCMType.VAROBJ)))
            .bits

    private fun id(name: String): Int = name.asRSCM(RSCMType.OBJ)

    private fun inventory(vararg objs: InvObj?): Inventory {
        val type = checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM()))
        val inv = Inventory(type, arrayOfNulls(28))
        objs.forEachIndexed { slot, obj -> inv[slot] = obj }
        return inv
    }

    companion object {
        private const val CHARGED = "obj.xeric_talisman"
        private const val INERT = "obj.xeric_talisman_empty"
        private const val FANG = "obj.lizardman_fang"

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
