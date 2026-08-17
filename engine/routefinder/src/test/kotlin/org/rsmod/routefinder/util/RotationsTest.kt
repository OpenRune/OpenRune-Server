package org.rsmod.routefinder.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.routefinder.flag.BlockAccessFlag.ALL_APPROACHES
import org.rsmod.routefinder.flag.BlockAccessFlag.DIRECTIONS
import org.rsmod.routefinder.flag.BlockAccessFlag.EAST
import org.rsmod.routefinder.flag.BlockAccessFlag.NORTH
import org.rsmod.routefinder.flag.BlockAccessFlag.SOUTH
import org.rsmod.routefinder.flag.BlockAccessFlag.WEST

class RotationsTest {
    // The only `forceApproachFlags` values present in the rev 240 cache.
    private val northOpen = ALL_APPROACHES and NORTH.inv() // 0x1e
    private val eastOpen = ALL_APPROACHES and EAST.inv() // 0x1d
    private val southOpen = ALL_APPROACHES and SOUTH.inv() // 0x1b
    private val westOpen = ALL_APPROACHES and WEST.inv() // 0x17
    private val fifthOpen = DIRECTIONS // 0x0f

    private val compassRestricted = listOf(northOpen, eastOpen, southOpen, westOpen)

    @Test
    fun `north approach advances clockwise`() {
        assertRotations(
            northOpen,
            EAST or SOUTH or WEST, // north
            NORTH or SOUTH or WEST, // east
            NORTH or EAST or WEST, // south
            NORTH or EAST or SOUTH, // west
        )
    }

    @Test
    fun `east approach advances clockwise`() {
        assertRotations(
            eastOpen,
            NORTH or SOUTH or WEST, // east
            NORTH or EAST or WEST, // south
            NORTH or EAST or SOUTH, // west
            EAST or SOUTH or WEST, // north
        )
    }

    @Test
    fun `south approach advances clockwise`() {
        assertRotations(
            southOpen,
            NORTH or EAST or WEST, // south
            NORTH or EAST or SOUTH, // west
            EAST or SOUTH or WEST, // north
            NORTH or SOUTH or WEST, // east
        )
    }

    @Test
    fun `west approach advances clockwise`() {
        assertRotations(
            westOpen,
            NORTH or EAST or SOUTH, // west
            EAST or SOUTH or WEST, // north
            NORTH or SOUTH or WEST, // east
            NORTH or EAST or WEST, // south
        )
    }

    @Test
    fun `locs with no compass approach are unaffected by rotation`() {
        assertRotations(fifthOpen, fifthOpen, fifthOpen, fifthOpen, fifthOpen)
    }

    @Test
    fun `unrestricted locs are unaffected by rotation`() {
        assertRotations(0, 0, 0, 0, 0)
    }

    @Test
    fun `restricted locs keep exactly one open side at every angle`() {
        for (flags in compassRestricted) {
            for (angle in 0..3) {
                val blocked = Rotations.rotate(angle, flags)
                assertEquals(
                    3,
                    blocked.countOneBits(),
                    "angle=$angle flags=0x${flags.toString(16)} blocked=0x${blocked.toString(16)}",
                )
            }
        }
    }

    @Test
    fun `rotation never returns bits outside the compass`() {
        for (flags in compassRestricted + listOf(fifthOpen, 0)) {
            for (angle in 0..3) {
                assertEquals(
                    0,
                    Rotations.rotate(angle, flags) and DIRECTIONS.inv(),
                    "angle=$angle flags=0x${flags.toString(16)} leaked a non-compass bit",
                )
            }
        }
    }

    private fun assertRotations(flags: Int, vararg expected: Int) {
        for (angle in expected.indices) {
            assertEquals(
                expected[angle],
                Rotations.rotate(angle, flags),
                "angle=$angle flags=0x${flags.toString(16)}",
            )
        }
    }
}
