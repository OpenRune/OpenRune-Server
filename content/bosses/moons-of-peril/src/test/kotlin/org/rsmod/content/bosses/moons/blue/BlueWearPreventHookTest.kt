package org.rsmod.content.bosses.moons.blue

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BlueWearPreventHookTest {
    @Test
    fun `varbit set refuses right hand weapons before the disarm lands`() {
        val f = BlueFixture()
        val hook = BlueWearPreventHook(f.frozen)
        f.player.wearPrevent = 1
        assertEquals(
            BlueWearPreventHook.FROZEN_MESSAGE,
            hook.validate(f.player, f.type("obj.bronze_sword")),
        )
        assertNull(hook.validate(f.player, f.type("obj.bronze_full_helm")))
    }

    @Test
    fun `two handed weapon held also blocks the shield but not rings`() {
        val f = BlueFixture()
        val hook = BlueWearPreventHook(f.frozen)
        f.holding[0] = InvObj("obj.bronze_2h_sword", 1)
        assertEquals(
            BlueWearPreventHook.FROZEN_MESSAGE,
            hook.validate(f.player, f.type("obj.wooden_shield")),
        )
        assertNull(hook.validate(f.player, f.type("obj.gold_ring")))
        f.holding[0] = null
        assertNull(hook.validate(f.player, f.type("obj.wooden_shield")))
    }

    @Test
    fun `nothing held and varbit clear allows everything`() {
        val f = BlueFixture()
        val hook = BlueWearPreventHook(f.frozen)
        assertNull(hook.validate(f.player, f.type("obj.bronze_sword")))
        assertNull(hook.validate(f.player, f.type("obj.wooden_shield")))
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() = BlueTestCache.init()

        @JvmStatic @AfterAll fun restore() = BlueTestCache.restore()
    }
}
