package org.rsmod.api.registry.region

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.registry.region.RegionRegistry.Companion.LARGE_MIN_X
import org.rsmod.api.registry.region.RegionRegistry.Companion.LARGE_REGION_SQUARE_LENGTH
import org.rsmod.api.registry.region.RegionRegistry.Companion.MAX_CONCURRENT_LARGE_REGIONS
import org.rsmod.api.registry.region.RegionRegistry.Companion.MAX_CONCURRENT_SMALL_REGIONS
import org.rsmod.api.registry.region.RegionRegistry.Companion.MAX_CONCURRENT_WORLDENTITY_REGIONS
import org.rsmod.api.registry.region.RegionRegistry.Companion.SMALL_MIN_X
import org.rsmod.api.registry.region.RegionRegistry.Companion.SMALL_REGION_SQUARE_LENGTH
import org.rsmod.api.registry.region.RegionRegistry.Companion.WORLDENTITY_MIN_X
import org.rsmod.api.registry.region.RegionRegistry.Companion.WORLDENTITY_REGION_SQUARE_LENGTH
import org.rsmod.api.registry.region.RegionRegistry.Companion.workingAreaLarge
import org.rsmod.api.registry.region.RegionRegistry.Companion.workingAreaSmall
import org.rsmod.api.registry.region.RegionRegistry.Companion.workingAreaWorldEntity
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid

class RegionAllocationTest {
    private val bands =
        listOf(
            Band("large", workingAreaLarge, LARGE_MIN_X, SMALL_MIN_X, LARGE_REGION_SQUARE_LENGTH),
            Band(
                "small",
                workingAreaSmall,
                SMALL_MIN_X,
                WORLDENTITY_MIN_X,
                SMALL_REGION_SQUARE_LENGTH,
            ),
            Band(
                "worldentity",
                workingAreaWorldEntity,
                WORLDENTITY_MIN_X,
                CoordGrid.MAP_WIDTH,
                WORLDENTITY_REGION_SQUARE_LENGTH,
            ),
        )

    @Test
    fun `every slot round-trips between coord and slot`() {
        for (band in bands) {
            val slots = band.area.horizontalRegionCap * band.area.verticalRegionCap
            for (slot in 0 until slots) {
                val coord = band.area.calculateCoord(slot)
                assertEquals(slot, band.area.calculateSlot(coord), "${band.name} slot $slot")
            }
        }
    }

    @Test
    fun `every allocated square stays inside its own band`() {
        for (band in bands) {
            val slots = band.area.horizontalRegionCap * band.area.verticalRegionCap
            for (slot in 0 until slots) {
                val southWest = band.area.calculateCoord(slot)
                val endX = southWest.x + band.squareLength
                val endZ = southWest.z + band.squareLength
                assertTrue(southWest.x >= band.minX, "${band.name} slot $slot underflows x")
                assertTrue(endX <= band.maxX, "${band.name} slot $slot overflows x (endX=$endX)")
                assertTrue(southWest.z >= 0, "${band.name} slot $slot underflows z")
                assertTrue(
                    endZ <= CoordGrid.MAP_LENGTH,
                    "${band.name} slot $slot overflows z (endZ=$endZ)",
                )
            }
        }
    }

    @Test
    fun `bands do not overlap`() {
        for (band in bands) {
            val slots = band.area.horizontalRegionCap * band.area.verticalRegionCap
            val others = bands.filter { it !== band }
            for (slot in 0 until slots) {
                val coord = band.area.calculateCoord(slot)
                for (other in others) {
                    assertFalse(
                        coord in other.area,
                        "${band.name} slot $slot at $coord also falls in ${other.name}",
                    )
                }
            }
        }
    }

    @Test
    fun `first and last square sit flush against the band edges`() {
        // Large and small squares are inset by one padding step; world entity squares are not.
        assertEquals(CoordGrid(6464, 64), workingAreaLarge.calculateCoord(0))
        assertEquals(CoordGrid(10304, 64), workingAreaSmall.calculateCoord(0))
        assertEquals(CoordGrid(WORLDENTITY_MIN_X, 0), workingAreaWorldEntity.calculateCoord(0))

        assertEquals(SMALL_MIN_X, lastSquareEndX(bands[0]))
        assertEquals(WORLDENTITY_MIN_X, lastSquareEndX(bands[1]))
        assertEquals(CoordGrid.MAP_WIDTH, lastSquareEndX(bands[2]))
    }

    @Test
    fun `padded bands leave a gap between neighbours and world entity does not`() {
        assertEquals(64, horizontalGap(bands[0]))
        assertEquals(64, horizontalGap(bands[1]))
        assertEquals(0, horizontalGap(bands[2]))
    }

    @Test
    fun `working area capacity matches its region list`() {
        assertEquals(MAX_CONCURRENT_LARGE_REGIONS, slotCount(workingAreaLarge))
        assertEquals(MAX_CONCURRENT_SMALL_REGIONS, slotCount(workingAreaSmall))
        assertEquals(MAX_CONCURRENT_WORLDENTITY_REGIONS, slotCount(workingAreaWorldEntity))

        assertEquals(RegionListLarge().capacity, slotCount(workingAreaLarge))
        assertEquals(RegionListSmall().capacity, slotCount(workingAreaSmall))
        assertEquals(RegionListWorldEntity().capacity, slotCount(workingAreaWorldEntity))
    }

    @Test
    fun `bounds assertions hold for each band`() {
        workingAreaLarge.assertValidBounds(RegionListLarge().capacity)
        workingAreaSmall.assertValidBounds(RegionListSmall().capacity)
        workingAreaWorldEntity.assertValidBounds(RegionListWorldEntity().capacity)
    }

    @Test
    fun `static map coords belong to no band`() {
        val staticCoords =
            listOf(CoordGrid(0, 0), CoordGrid(3222, 3218), CoordGrid(LARGE_MIN_X - 1, 3000))
        for (coord in staticCoords) {
            for (band in bands) {
                assertFalse(coord in band.area, "$coord unexpectedly falls in ${band.name}")
            }
        }
    }

    private fun slotCount(area: RegionRegistry.WorkingArea): Int =
        area.horizontalRegionCap * area.verticalRegionCap

    private fun lastSquareEndX(band: Band): Int {
        val lastInRow = band.area.horizontalRegionCap - 1
        return band.area.calculateCoord(lastInRow).x + band.squareLength
    }

    private fun horizontalGap(band: Band): Int {
        val first = band.area.calculateCoord(0)
        val second = band.area.calculateCoord(1)
        return second.x - (first.x + band.squareLength)
    }

    private data class Band(
        val name: String,
        val area: RegionRegistry.WorkingArea,
        val minX: Int,
        val maxX: Int,
        val squareLength: Int,
    )
}
