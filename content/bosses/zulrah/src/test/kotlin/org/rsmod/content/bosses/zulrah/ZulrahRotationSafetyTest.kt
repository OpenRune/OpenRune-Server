package org.rsmod.content.bosses.zulrah

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZulrahRotationSafetyTest {
    @Test
    fun `all reference phases preserve at least one cloud-free area`() {
        assertEquals(listOf(11, 11, 12, 13), ZulrahRotations.all.map { it.size })
        for (rotation in ZulrahRotations.all) {
            for (phase in rotation) {
                assertTrue(phase.safeOffsets.isNotEmpty(), "Missing safe area for $phase")
                assertEquals(phase.safeOffsets.distinct(), phase.safeOffsets)
            }
        }
    }

    @Test
    fun `reference safe centers retain north-positive coordinates and both stalling areas`() {
        for (rotation in ZulrahRotations.all) {
            assertEquals(listOf(8 to 4), rotation.first().safeOffsets)
            assertEquals(listOf(8 to 4), rotation.last().safeOffsets)
        }
        assertEquals(listOf(6 to 0, 6 to -3), ZulrahRotations.rotation1[2].safeOffsets)
        assertEquals(listOf(-2 to 0, -2 to 1), ZulrahRotations.rotation1[3].safeOffsets)
        assertEquals(listOf(-4 to 2), ZulrahRotations.rotation3[2].safeOffsets)
        assertEquals(listOf(6 to 0, 6 to -3), ZulrahRotations.rotation4[9].safeOffsets)
    }

    @Test
    fun `cloud-free footprints do not intersect the reference boss footprint`() {
        for (phase in ZulrahRotations.all.flatten()) {
            val (bossX, bossZ) = when (phase.position) {
                ZulrahPosition.North -> 0 to 0
                ZulrahPosition.South -> 0 to -11
                ZulrahPosition.East -> 10 to -2
                ZulrahPosition.West -> -10 to -2
            }
            for ((safeX, safeZ) in phase.safeOffsets) {
                val overlapsBoss =
                    safeX - 1 <= bossX + 4 && safeX + 1 >= bossX &&
                        safeZ - 1 <= bossZ + 4 && safeZ + 1 >= bossZ
                assertFalse(overlapsBoss, "Safe footprint intersects ${phase.position}: $phase")
            }
        }
    }
}
