package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.enums.HunterEnums.hunter_kit_contents
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class HunterKitTest {
    private val script = HunterKitScript(mockk(relaxed = true), mockk(relaxed = true))
    private val contents by lazy { hunter_kit_contents.values.filterNotNull().map { it.internalName } }

    @Test
    fun `opening the kit twice gives its contents once`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(KIT, 1)
        repeat(2) { f.call(script, "open", 0) }
        assertEquals(0L, f.total(KIT))
        for (obj in contents) assertEquals(1L, f.total(obj), obj)
    }

    @Test
    fun `a full inventory keeps the kit`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(KIT, 1)
        f.fill()
        f.player.inv[1] = null
        f.call(script, "open", 0)
        assertEquals(1L, f.total(KIT))
        for (obj in contents) assertEquals(0L, f.total(obj), obj)
    }

    @Test
    fun `a kit gone from the slot gives nothing`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(KIT, 1)
        f.player.inv[0] = null
        f.call(script, "open", 0)
        for (obj in contents) assertEquals(0L, f.total(obj), obj)
    }

    companion object {
        private const val KIT = "obj.dream_hunter_box"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
