package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.content.skills.hunter.HunterPouch.Companion.pouchHunted
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

    @Test
    fun `catches into an open pouch stop at its size`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(POUCH, 1)
        val stored = (1..20).count { f.access.pouchHunted(MEAT) }
        assertEquals(14, stored)
        assertEquals(14, HunterPouch.Meat.stored(f.player))
        assertEquals(14L, f.total(MEAT))
    }

    @Test
    fun `a catch of the maximum count stores nothing`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(POUCH, 1)
        f.access.pouchHunted(MEAT)
        assertFalse(f.access.pouchHunted(MEAT, Int.MAX_VALUE))
        assertEquals(1L, f.total(MEAT))
    }

    @Test
    fun `a catch after the pouch is closed or gone stores nothing`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(CLOSED_POUCH, 1)
        assertFalse(f.access.pouchHunted(MEAT))
        f.player.inv[0] = null
        assertFalse(f.access.pouchHunted(MEAT))
        assertEquals(0L, f.total(MEAT))
    }

    private fun destroy(f: HunterFixture, slot: Int = 0) {
        val type = checkNotNull(ServerCacheManager.getItem(POUCH.asRSCM()))
        f.call(script, "destroy", HunterPouch.Meat, HeldObjEvents.Op5(slot, InvObj(POUCH, 1), type, f.player.inv))
    }

    private fun confirm(f: HunterFixture) {
        f.coroutine.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", 1))
        f.finish()
    }

    @Test
    fun `a pouch destroyed twice destroys one pouch and its contents`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(POUCH, 1)
        f.player.inv[1] = InvObj(POUCH, 1)
        repeat(3) { f.access.pouchHunted(MEAT) }
        destroy(f)
        confirm(f)
        destroy(f)
        confirm(f)
        assertEquals(1L, f.total(POUCH))
        assertEquals(0L, f.total(MEAT))
    }

    @Test
    fun `a pouch moved during the prompt keeps its contents`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(POUCH, 1)
        repeat(3) { f.access.pouchHunted(MEAT) }
        destroy(f)
        f.player.inv[0] = null
        f.player.inv[5] = InvObj(POUCH, 1)
        confirm(f)
        assertEquals(1L, f.total(POUCH))
        assertEquals(3L, f.total(MEAT))
    }

    companion object {
        private const val POUCH = "obj.hg_meatpouch_small_open"
        private const val CLOSED_POUCH = "obj.hg_meatpouch_small"
        private const val MEAT = "obj.huntingbeast_wild_meat"
        private val GUILD_TOP_FLOOR = CoordGrid(1556, 3036, 1)

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
