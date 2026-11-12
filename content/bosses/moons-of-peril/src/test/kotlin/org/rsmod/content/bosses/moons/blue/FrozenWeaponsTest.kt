package org.rsmod.content.bosses.moons.blue

import dev.openrune.util.Wearpos
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.bosses.moons.blue.FrozenWeapons.FreezeResult
import org.rsmod.content.bosses.moons.blue.FrozenWeapons.ReleaseResult
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FrozenWeaponsTest {
    private val scimitar = "obj.dragon_scimitar"
    private val dart = "obj.bronze_dart"
    private val blowpipe = "obj.tumekens_shadow"

    @Test
    fun `plain freeze then release returns the weapon to the hands`() {
        val f = BlueFixture()
        f.player.worn[Wearpos.RightHand.slot] = InvObj(blowpipe, 1, 77)
        assertEquals(FreezeResult.Frozen, f.frozen.freeze(f.player))
        assertNull(f.player.worn[Wearpos.RightHand.slot])
        assertEquals(InvObj(blowpipe, 1, 77), f.holding[0])
        assertEquals(ReleaseResult.ToHands, f.frozen.release(f.player))
        assertEquals(InvObj(blowpipe, 1, 77), f.player.worn[Wearpos.RightHand.slot])
        assertNull(f.holding[0])
        assertEquals(1L, f.total(blowpipe))
    }

    @Test
    fun `unequipped before the disarm leaves holding empty`() {
        val f = BlueFixture()
        f.player.inv[0] = InvObj(scimitar, 1)
        assertEquals(FreezeResult.NoWeapon, f.frozen.freeze(f.player))
        assertNull(f.holding[0])
        assertEquals(1L, f.total(scimitar))
        assertNotNull(f.player.inv[0])
    }

    @Test
    fun `every call site releasing in one tick restores the weapon exactly once`() {
        val f = BlueFixture()
        f.player.worn[Wearpos.RightHand.slot] = InvObj(scimitar, 1)
        f.frozen.freeze(f.player)
        val results = List(5) { f.frozen.release(f.player) }
        assertEquals(ReleaseResult.ToHands, results[0])
        assertEquals(List(4) { ReleaseResult.Nothing }, results.drop(1))
        assertEquals(1L, f.total(scimitar))
        assertNull(f.holding[0])
        assertNotNull(f.player.worn[Wearpos.RightHand.slot])
    }

    @Test
    fun `refused release makes the next disarm report already holding and moves nothing`() {
        val f = BlueFixture()
        f.holding[0] = InvObj(scimitar, 1)
        f.player.worn[Wearpos.RightHand.slot] = InvObj("obj.bronze_sword", 1)
        f.fillInv()
        f.player.bankCapacityVar = 0
        assertEquals(FreezeResult.AlreadyHolding, f.frozen.freeze(f.player))
        assertEquals(InvObj(scimitar, 1), f.holding[0])
        assertEquals(InvObj("obj.bronze_sword", 1), f.player.worn[Wearpos.RightHand.slot])
        assertEquals(1L, f.total(scimitar))
        assertEquals(1L, f.total("obj.bronze_sword"))
    }

    @Test
    fun `stackable weapon at int max never duplicates when the inventory stack would overflow`() {
        val f = BlueFixture()
        f.holding[0] = InvObj(dart, Int.MAX_VALUE)
        f.player.inv[0] = InvObj(dart, 5)
        val before = f.total(dart)
        val result = f.frozen.releaseAtLogin(f.player)
        assertEquals(before, f.total(dart))
        if (result == ReleaseResult.Kept) {
            assertEquals(InvObj(dart, Int.MAX_VALUE), f.holding[0])
        } else {
            assertNull(f.holding[0])
        }
    }

    @Test
    fun `vars survive the round trip`() {
        val f = BlueFixture()
        f.player.worn[Wearpos.RightHand.slot] = InvObj(blowpipe, 1, 12345)
        assertEquals(FreezeResult.Frozen, f.frozen.freeze(f.player))
        assertEquals(ReleaseResult.ToHands, f.frozen.release(f.player))
        assertEquals(12345, f.player.worn[Wearpos.RightHand.slot]?.vars)
    }

    @Test
    fun `login with free inventory returns the weapon to the inventory`() {
        val f = BlueFixture()
        f.holding[0] = InvObj(scimitar, 1)
        assertEquals(ReleaseResult.ToInventory, f.frozen.releaseAtLogin(f.player))
        assertEquals(1L, f.total(scimitar))
        assertNull(f.holding[0])
    }

    @Test
    fun `login with full inventory banks the weapon at the end of the main tab`() {
        val f = BlueFixture()
        f.holding[0] = InvObj(scimitar, 1)
        f.fillInv()
        f.bank[0] = InvObj("obj.bronze_sword", 1)
        f.player.bankMainSize = 1
        assertEquals(ReleaseResult.ToBank, f.frozen.releaseAtLogin(f.player))
        assertEquals(InvObj(scimitar, 1), f.bank[1])
        assertEquals(2, f.player.bankMainSize)
        assertEquals(1L, f.total(scimitar))
        assertNull(f.holding[0])
    }

    @Test
    fun `login with full inventory and full bank keeps the weapon held`() {
        val f = BlueFixture()
        f.holding[0] = InvObj(scimitar, 1)
        f.fillInv()
        f.player.bankCapacityVar = 0
        assertEquals(ReleaseResult.Kept, f.frozen.releaseAtLogin(f.player))
        assertEquals(InvObj(scimitar, 1), f.holding[0])
        assertEquals(1L, f.total(scimitar))
    }

    @Test
    fun `protected access denies the freeze and moves nothing`() {
        val f = BlueFixture()
        f.player.worn[Wearpos.RightHand.slot] = InvObj(scimitar, 1)
        f.protectWorn()
        assertEquals(FreezeResult.Denied, f.frozen.freeze(f.player))
        assertNull(f.holding[0])
        assertNotNull(f.player.worn[Wearpos.RightHand.slot])
    }

    @Test
    fun `releasing an empty holding does nothing`() {
        val f = BlueFixture()
        f.player.worn[Wearpos.RightHand.slot] = InvObj(scimitar, 1)
        assertEquals(ReleaseResult.Nothing, f.frozen.release(f.player))
        assertEquals(InvObj(scimitar, 1), f.player.worn[Wearpos.RightHand.slot])
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() = BlueTestCache.init()

        @JvmStatic @AfterAll fun restore() = BlueTestCache.restore()
    }
}
