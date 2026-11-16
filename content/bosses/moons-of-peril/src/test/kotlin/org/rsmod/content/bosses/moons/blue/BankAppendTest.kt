package org.rsmod.content.bosses.moons.blue

import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.interfaces.bank.bankAppend
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(dev.openrune.types.util.UncheckedType::class)
class BankAppendTest {
    private val scimitar = "obj.dragon_scimitar"

    @Test
    fun `appends to the end of an empty main tab and grows it`() {
        val f = BlueFixture()
        f.holding[0] = InvObj(scimitar, 1)
        assertTrue(f.player.bankAppend(f.holding, 0))
        assertEquals(InvObj(scimitar, 1), f.bank[0])
        assertEquals(1, f.player.bankMainSize)
        assertNull(f.holding[0])
    }

    @Test
    fun `appends after existing main tab entries`() {
        val f = BlueFixture()
        f.bank[0] = InvObj("obj.bronze_sword", 1)
        f.player.bankMainSize = 1
        f.holding[0] = InvObj(scimitar, 1)
        assertTrue(f.player.bankAppend(f.holding, 0))
        assertEquals(InvObj(scimitar, 1), f.bank[1])
        assertEquals(2, f.player.bankMainSize)
    }

    @Test
    fun `stacks onto an existing entry without growing the tab`() {
        val f = BlueFixture()
        f.bank[0] = InvObj("obj.bronze_dart", 10)
        f.player.bankMainSize = 1
        f.holding[0] = InvObj("obj.bronze_dart", 5)
        assertTrue(f.player.bankAppend(f.holding, 0))
        assertEquals(InvObj("obj.bronze_dart", 15), f.bank[0])
        assertEquals(1, f.player.bankMainSize)
        assertEquals(15L, f.total("obj.bronze_dart"))
    }

    @Test
    fun `refuses and moves nothing when the bank is full`() {
        val f = BlueFixture()
        f.player.bankCapacityVar = 0
        f.holding[0] = InvObj(scimitar, 1)
        assertFalse(f.player.bankAppend(f.holding, 0))
        assertEquals(InvObj(scimitar, 1), f.holding[0])
        assertEquals(0, f.player.bankMainSize)
        assertEquals(1L, f.total(scimitar))
    }

    @Test
    fun `placeholder entry is reused`() {
        val f = BlueFixture()
        val placeholder = "obj.placeholder_dragon_scimitar".asRSCM()
        f.bank[0] = InvObj(placeholder, 0)
        f.player.bankMainSize = 1
        f.holding[0] = InvObj(scimitar, 1)
        assertTrue(f.player.bankAppend(f.holding, 0))
        assertEquals(1, f.bank.count { it != null })
        assertEquals(1L, f.total(scimitar))
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() = BlueTestCache.init()

        @JvmStatic @AfterAll fun restore() = BlueTestCache.restore()
    }
}
