package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class HuntsmansKitTest {
    private val script = HuntsmansKitScript()

    private val HunterFixture.kit: Inventory
        get() = player.invMap.getOrPut(KIT_INV)

    @Test
    fun `filling and emptying again and again keeps every item`() {
        val f = HunterFixture()
        for (slot in 0..2) f.player.inv[slot] = InvObj(ROPE, 1)
        f.player.inv[3] = InvObj(DAGGER, 1)
        repeat(2) {
            f.call(script, "fill")
            f.call(script, "fill")
            assertEquals(3, f.kit.count(ROPE))
            assertEquals(0, f.player.inv.count(ROPE))
            assertEquals(1, f.player.inv.count(DAGGER))
            f.call(script, "empty")
            f.call(script, "empty")
            assertEquals(3, f.player.inv.count(ROPE))
            assertEquals(0, f.kit.count(ROPE))
        }
        assertEquals(3L, f.total(ROPE))
        assertEquals(1L, f.total(DAGGER))
    }

    @Test
    fun `emptying into a full inventory moves only what fits`() {
        val f = HunterFixture()
        f.kit[0] = InvObj(ROPE, 5)
        f.fill()
        f.player.inv[0] = null
        f.player.inv[1] = null
        f.call(script, "empty")
        assertEquals(2, f.player.inv.count(ROPE))
        assertEquals(3, f.kit.count(ROPE))
        assertEquals(5L, f.total(ROPE))
    }

    @Test
    fun `withdrawing more than is stored moves only what is stored`() {
        val f = HunterFixture()
        f.kit[0] = InvObj(ROPE, 2)
        repeat(2) { f.call(script, "move", f.kit, f.player.inv, ROPE, 5) }
        assertEquals(2, f.player.inv.count(ROPE))
        assertEquals(0, f.kit.count(ROPE))
        assertEquals(2L, f.total(ROPE))
    }

    @Test
    fun `a kit stack at the maximum keeps the deposit in the inventory`() {
        val f = HunterFixture()
        f.kit[0] = InvObj(ROPE, Int.MAX_VALUE)
        f.player.inv[0] = InvObj(ROPE, 1)
        f.call(script, "deposit", ROPE, Int.MAX_VALUE)
        assertEquals(1, f.player.inv.count(ROPE))
        assertEquals(Int.MAX_VALUE.toLong() + 1, f.total(ROPE))
    }

    @Test
    fun `a kit holding 32 kinds takes no new kind`() {
        val f = HunterFixture()
        f.fill(f.kit, DAGGER)
        f.player.inv[0] = InvObj(ROPE, 1)
        f.call(script, "deposit", ROPE, Int.MAX_VALUE)
        assertEquals(1, f.player.inv.count(ROPE))
        assertEquals(1L, f.total(ROPE))
    }

    @Test
    fun `unstorable items and the kit itself stay in the inventory`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(DAGGER, 1)
        f.player.inv[1] = InvObj(KIT, 1)
        f.call(script, "deposit", DAGGER, Int.MAX_VALUE)
        f.call(script, "store", checkNotNull(ServerCacheManager.getItem(KIT.asRSCM())))
        assertEquals(1, f.player.inv.count(DAGGER))
        assertEquals(1, f.player.inv.count(KIT))
        assertEquals(0, f.kit.occupiedSpace())
    }

    companion object {
        private const val KIT = "obj.huntsmans_kit"
        private const val KIT_INV = "inv.huntsmans_kit"
        private const val ROPE = "obj.rope"
        private const val DAGGER = "obj.bronze_dagger"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
