package org.rsmod.content.skills.hunter

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class HunterPouchTest {
    private val areas = mockk<AreaChecker> { every { inArea(any(), any()) } answers { secondArg<Int>() == GUILD_TOP_FLOOR.packed } }
    private val script = HunterPouchScript(mockk(relaxed = true), mockk(relaxed = true), areas)

    @Test
    fun `filling and emptying again and again keeps every meat`() {
        val f = HunterFixture(GUILD_TOP_FLOOR)
        f.player.inv[0] = InvObj(POUCH, 1)
        for (slot in 1..5) f.player.inv[slot] = InvObj(MEAT, 1)
        repeat(2) {
            f.call(script, "fill", HunterPouch.Meat)
            f.call(script, "fill", HunterPouch.Meat)
            assertEquals(5, HunterPouch.Meat.stored(f.player))
            assertEquals(0, f.player.inv.count(MEAT))
            f.call(script, "empty", HunterPouch.Meat)
            f.call(script, "empty", HunterPouch.Meat)
            assertEquals(5L, f.total(MEAT))
            assertEquals(5, f.player.inv.count(MEAT))
        }
    }

    @Test
    fun `a full pouch takes only up to its size`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(POUCH, 1)
        f.fill(obj = MEAT)
        f.call(script, "fill", HunterPouch.Meat)
        assertEquals(14, HunterPouch.Meat.stored(f.player))
        assertEquals(27L, f.total(MEAT))
    }

    @Test
    fun `emptying into a full inventory moves only what fits`() {
        val f = HunterFixture(GUILD_TOP_FLOOR)
        f.player.inv[0] = InvObj(POUCH, 1)
        for (slot in 1..5) f.player.inv[slot] = InvObj(MEAT, 1)
        f.call(script, "fill", HunterPouch.Meat)
        f.fill()
        f.player.inv[1] = null
        f.player.inv[2] = null
        f.call(script, "empty", HunterPouch.Meat)
        assertEquals(2, f.player.inv.count(MEAT))
        assertEquals(3, HunterPouch.Meat.stored(f.player))
        assertEquals(5L, f.total(MEAT))
    }

    @Test
    fun `a bank at the stack limit keeps the pouch contents`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(POUCH, 1)
        for (slot in 1..3) f.player.inv[slot] = InvObj(MEAT, 1)
        f.call(script, "fill", HunterPouch.Meat)
        f.bank[0] = InvObj(MEAT, Int.MAX_VALUE - 1)
        val emptyIntoBank = script.javaClass.declaredMethods.single { it.name == "emptyIntoBank" }
        emptyIntoBank.isAccessible = true
        repeat(2) { emptyIntoBank.invoke(script, f.player, HunterPouch.Meat) }
        assertEquals(Int.MAX_VALUE.toLong() - 1 + 3, f.total(MEAT))
        assertEquals(3, HunterPouch.Meat.stored(f.player) + f.bank.count(MEAT) - (Int.MAX_VALUE - 1))
    }

    companion object {
        private const val POUCH = "obj.hg_meatpouch_small_open"
        private const val MEAT = "obj.huntingbeast_wild_meat"
        private val GUILD_TOP_FLOOR = CoordGrid(1556, 3036, 1)

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
