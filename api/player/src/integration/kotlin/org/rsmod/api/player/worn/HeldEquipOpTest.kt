package org.rsmod.api.player.worn

import dev.openrune.types.obj.Wearpos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class HeldEquipOpTest {
    @Test
    fun GameTestState.`equip standard obj into empty wearpos`() = runBasicGameTest {
        withPlayerInit {
            inv[4] = InvObj("obj.rune_axe")

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Success>(result)
            assertEquals(Wearpos.RightHand, result.equipWearpos)
            assertEquals(emptyList<Wearpos>(), result.unequipWearpos)
            assertEquals(InvObj("obj.rune_axe"), righthand)
        }
    }

    @Test
    fun GameTestState.`equip standard obj into occupied wearpos`() = runBasicGameTest {
        withPlayerInit {
            righthand = InvObj("obj.crystal_axe", vars = 32)
            inv[4] = InvObj("obj.rune_axe")

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Success>(result)
            assertEquals(Wearpos.RightHand, result.equipWearpos)
            assertEquals(emptyList<Wearpos>(), result.unequipWearpos)
            assertEquals(InvObj("obj.rune_axe"), righthand)
            assertEquals(InvObj("obj.crystal_axe", vars = 32), inv[4])
        }
    }

    @Test
    fun GameTestState.`equip standard obj into occupied wearpos and full inv`() = runBasicGameTest {
        withPlayerInit {
            righthand = InvObj("obj.crystal_axe", vars = 32)
            repeat(inv.size) { inv[it] = InvObj("obj.beer") }
            inv[4] = InvObj("obj.rune_axe")

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Success>(result)
            assertEquals(Wearpos.RightHand, result.equipWearpos)
            assertEquals(emptyList<Wearpos>(), result.unequipWearpos)
            assertEquals(InvObj("obj.rune_axe"), righthand)
            assertEquals(InvObj("obj.crystal_axe", vars = 32), inv[4])
        }
    }

    @Test
    fun GameTestState.`equip stackable obj into occupied wearpos`() = runBasicGameTest {
        withPlayerInit {
            quiver = InvObj("obj.rune_arrow", count = 50)
            inv[4] = InvObj("obj.rune_arrow", count = 50)

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Success>(result)
            assertEquals(Wearpos.Quiver, result.equipWearpos)
            assertEquals(emptyList<Wearpos>(), result.unequipWearpos)
            assertEquals(InvObj("obj.rune_arrow", count = 100), quiver)
            assertNull(inv[4])
        }
    }

    @Test
    fun GameTestState.`equip overflowing stackable obj into occupied wearpos`() = runBasicGameTest {
        withPlayerInit {
            quiver = InvObj("obj.rune_arrow", count = Int.MAX_VALUE - 5)
            inv[4] = InvObj("obj.rune_arrow", count = 10)

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Success>(result)
            assertEquals(Wearpos.Quiver, result.equipWearpos)
            assertEquals(emptyList<Wearpos>(), result.unequipWearpos)
            assertEquals(InvObj("obj.rune_arrow", count = Int.MAX_VALUE), quiver)
            assertEquals(InvObj("obj.rune_arrow", count = 5), inv[4])
        }
    }

    @Test
    fun GameTestState.`equip stackable obj into fully occupied wearpos`() = runBasicGameTest {
        withPlayerInit {
            quiver = InvObj("obj.rune_arrow", count = Int.MAX_VALUE)
            inv[4] = InvObj("obj.rune_arrow", count = 10)

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Fail.NotEnoughWornSpace>(result)
            assertEquals(InvObj("obj.rune_arrow", count = Int.MAX_VALUE), quiver)
            assertEquals(InvObj("obj.rune_arrow", count = 10), inv[4])
        }
    }

    @Test
    fun GameTestState.`swap stackable obj with fully occupied wearpos`() = runBasicGameTest {
        withPlayerInit {
            quiver = InvObj("obj.rune_arrow", count = Int.MAX_VALUE)
            inv[4] = InvObj("obj.adamant_arrow", count = Int.MAX_VALUE)

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Success>(result)
            assertEquals(Wearpos.Quiver, result.equipWearpos)
            assertEquals(emptyList<Wearpos>(), result.unequipWearpos)
            assertEquals(InvObj("obj.adamant_arrow", count = Int.MAX_VALUE), quiver)
            assertEquals(InvObj("obj.rune_arrow", count = Int.MAX_VALUE), inv[4])
        }
    }

    @Test
    fun GameTestState.`fail swap stackable obj with fully occupied wearpos`() = runBasicGameTest {
        withPlayerInit {
            quiver = InvObj("obj.rune_arrow", count = Int.MAX_VALUE)
            inv[3] = InvObj("obj.rune_arrow", count = 50)
            inv[4] = InvObj("obj.adamant_arrow", count = Int.MAX_VALUE)

            setMaxLevels(this)

            val operations = HeldEquipOp(cacheTypes.objs, EventBus())
            val result = operations.equip(this, invSlot = 4, inventory = inv)
            assertInstanceOf<HeldEquipResult.Fail.NotEnoughWornSpace>(result)
            assertEquals(InvObj("obj.rune_arrow", count = Int.MAX_VALUE), quiver)
            assertEquals(InvObj("obj.rune_arrow", count = 50), inv[3])
            assertEquals(InvObj("obj.adamant_arrow", count = Int.MAX_VALUE), inv[4])
        }
    }

    @Test
    fun GameTestState.`equip two-handed obj into occupied left and right hand`() =
        runBasicGameTest {
            withPlayerInit {
                righthand = InvObj("obj.abyssal_whip")
                lefthand = InvObj("obj.crystal_shield", vars = 24)
                inv[4] = InvObj("obj.dragon_claws")

                setMaxLevels(this)

                val operations = HeldEquipOp(cacheTypes.objs, EventBus())
                val result = operations.equip(this, invSlot = 4, inventory = inv)
                assertInstanceOf<HeldEquipResult.Success>(result)
                assertEquals(Wearpos.RightHand, result.equipWearpos)
                assertEquals(listOf(Wearpos.LeftHand), result.unequipWearpos)
                assertEquals(InvObj("obj.dragon_claws"), righthand)
                assertEquals(InvObj("obj.abyssal_whip"), inv[4])
                assertEquals(InvObj("obj.crystal_shield", vars = 24), inv[0])
            }
        }

    @Test
    fun GameTestState.`equip two-handed obj into occupied left and right hand with full inv`() =
        runBasicGameTest {
            withPlayerInit {
                righthand = InvObj("obj.abyssal_whip")
                lefthand = InvObj("obj.crystal_shield", vars = 24)
                repeat(inv.size) { inv[it] = InvObj("obj.beer") }
                inv[4] = InvObj("obj.dragon_claws")

                setMaxLevels(this)

                val operations = HeldEquipOp(cacheTypes.objs, EventBus())
                val result = operations.equip(this, invSlot = 4, inventory = inv)
                assertInstanceOf<HeldEquipResult.Fail.NotEnoughInvSpace>(result)
                assertEquals(InvObj("obj.dragon_claws"), inv[4])
                assertEquals(InvObj("obj.abyssal_whip"), righthand)
                assertEquals(InvObj("obj.crystal_shield", vars = 24), lefthand)
            }
        }

    @Test
    fun GameTestState.`equip two-handed obj into occupied left and right hand with 1 inv space`() =
        runBasicGameTest {
            withPlayerInit {
                righthand = InvObj("obj.abyssal_whip")
                lefthand = InvObj("obj.crystal_shield", vars = 24)
                repeat(inv.size) { inv[it] = InvObj("obj.beer") }
                inv[4] = InvObj("obj.dragon_claws")
                inv[3] = null

                setMaxLevels(this)

                val operations = HeldEquipOp(cacheTypes.objs, EventBus())
                val result = operations.equip(this, invSlot = 4, inventory = inv)
                assertInstanceOf<HeldEquipResult.Success>(result)
                assertEquals(Wearpos.RightHand, result.equipWearpos)
                assertEquals(listOf(Wearpos.LeftHand), result.unequipWearpos)
                assertEquals(InvObj("obj.dragon_claws"), righthand)
                assertEquals(InvObj("obj.abyssal_whip"), inv[4])
                assertEquals(InvObj("obj.crystal_shield", vars = 24), inv[3])
            }
        }
}
