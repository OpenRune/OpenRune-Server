package org.rsmod.content.other.treasuretrails

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ClueLightBoxTest {
    @Test
    fun `a dealt box is never solved and some set of presses solves it`() {
        repeat(200) { seed ->
            val (boxSeed, lights) = ClueLightBox.deal(Random(seed))
            val masks = ClueLightBox.masks(boxSeed)
            assertNotEquals(0, lights, "seed=$seed")
            val solvable =
                (0 until (1 shl ClueLightBox.BUTTONS)).any { pressed ->
                    masks.indices.fold(lights) { acc, button ->
                        if (((pressed shr button) and 1) == 1) acc xor masks[button] else acc
                    } == 0
                }
            assertEquals(true, solvable, "seed=$seed")
        }
    }

    @Test
    fun `a box seed always wires the buttons the same way`() {
        assertArrayEquals(ClueLightBox.masks(1234), ClueLightBox.masks(1234))
        ClueLightBox.masks(1234).forEach { assertNotEquals(0, it) }
    }
}
