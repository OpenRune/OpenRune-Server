package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal enum class WardenGod(
    val displayName: String,
    val inactive: String,
    val active: String,
    val phaseTwo: String,
    val phaseTwoAlt: String,
    val phaseTwoExposed: String,
    val phaseTwoStyle: HitType,
    val core: String,
    val coreProjectile: String,
    val phaseTwoMaxHit: Int,
    val destroyed: String,
    val tile: CoordGrid,
    val facing: Direction,
    val orbSide: Int,
    val platformAngle: LocAngle,
    val pyramid: String,
    val pyramidLoad: String,
    val ball: String,
    val colour: String,
    val lanes: List<Lane>,
    private val pyramidParity: Int,
) {
    ELIDINIS(
        displayName = "Elidinis' Warden",
        inactive = "npc.toa_warden_elidinis_phase1_inactive",
        active = "npc.toa_warden_elidinis_phase1",
        phaseTwo = "npc.toa_warden_elidinis_phase2_mage",
        phaseTwoAlt = "npc.toa_warden_elidinis_phase2_range",
        phaseTwoExposed = "npc.toa_warden_elidinis_phase2_exposed",
        phaseTwoStyle = HitType.Magic,
        core = "npc.toa_warden_elidinis_core",
        coreProjectile = "spotanim.projectile_warden_elidinis_core",
        phaseTwoMaxHit = 20,
        destroyed = "npc.toa_warden_elidinis_phase3_inactive",
        tile = CoordGrid(3792, 5152, 1),
        facing = Direction.East,
        orbSide = -1,
        platformAngle = LocAngle.South,
        pyramid = "loc.toa_wardens_pyramid01",
        pyramidLoad = "loc.toa_wardens_pyramid01_load",
        ball = "spotanim.spotanim_wardens_phase01_ball02",
        colour = "3366ff",
        lanes = listOf(Lane.MIDDLE, Lane.NORTH, Lane.SOUTH),
        pyramidParity = 1,
    ),
    TUMEKEN(
        displayName = "Tumeken's Warden",
        inactive = "npc.toa_warden_tumeken_phase1_inactive",
        active = "npc.toa_warden_tumeken_phase1",
        phaseTwo = "npc.toa_warden_tumeken_phase2_range",
        phaseTwoAlt = "npc.toa_warden_tumeken_phase2_mage",
        phaseTwoExposed = "npc.toa_warden_tumeken_phase2_exposed",
        phaseTwoStyle = HitType.Ranged,
        core = "npc.toa_warden_tumeken_core",
        coreProjectile = "spotanim.projectile_warden_tumeken_core",
        phaseTwoMaxHit = 22,
        destroyed = "npc.toa_warden_tumeken_phase3_inactive",
        tile = CoordGrid(3820, 5152, 1),
        facing = Direction.West,
        orbSide = 1,
        platformAngle = LocAngle.North,
        pyramid = "loc.toa_wardens_pyramid02",
        pyramidLoad = "loc.toa_wardens_pyramid02_load",
        ball = "spotanim.spotanim_wardens_phase01_ball01",
        colour = "ff8e32",
        lanes = listOf(Lane.SOUTH, Lane.NORTH, Lane.MIDDLE),
        pyramidParity = 0,
    );

    enum class Lane(val dz: Int) {
        MIDDLE(0),
        NORTH(1),
        SOUTH(-1),
    }

    val centre: CoordGrid
        get() = tile.translate(WARDEN_RADIUS, WARDEN_RADIUS)

    val bossTypes: List<String>
        get() = listOf(inactive, active) + phaseTwoForms

    val phaseTwoForms: List<String>
        get() = listOf(phaseTwo, phaseTwoAlt, phaseTwoExposed)

    val pyramids: List<CoordGrid> by lazy {
        buildList {
            for (i in 0 until PYRAMID_COLUMNS) {
                for (j in 0 until PYRAMID_ROWS) {
                    if ((i + j) % 2 != pyramidParity) continue
                    val tile = CoordGrid(PYRAMID_X + i * PYRAMID_STEP, PYRAMID_Z + j * PYRAMID_STEP, 1)
                    if (tile != OBELISK_PYRAMID) add(tile)
                }
            }
        }
    }

    fun lane(index: Int): List<CoordGrid> {
        val lane = lanes[index % lanes.size]
        val originX = OBELISK_CENTRE.x + orbSide * ORB_SPAWN_OFFSET
        val z = OBELISK_CENTRE.z
        return buildList {
            for (step in 1..LANE_STEPS) {
                val x = originX + orbSide * step
                val offset =
                    when {
                        lane == Lane.MIDDLE -> 0
                        step == 1 -> lane.dz
                        else -> lane.dz * 2
                    }
                add(CoordGrid(x, z + offset, 1))
            }
        }
    }

    val arenaTile: CoordGrid
        get() = CoordGrid(if (orbSide < 0) ELIDINIS_ARENA_X else TUMEKEN_ARENA_X, tile.z, tile.level)

    val orbBlockTiles: List<CoordGrid>
        get() =
            (ORB_BLOCK_FIRST..ORB_SPAWN_OFFSET).map {
                OBELISK_CENTRE.translate(orbSide * it, 0)
            }

    val orbFacing: Direction
        get() = if (orbSide < 0) Direction.West else Direction.East

    val other: WardenGod
        get() = if (this == ELIDINIS) TUMEKEN else ELIDINIS

    companion object {
        val OBELISK_CENTRE = CoordGrid(3808, 5154, 1)
        const val WARDEN_RADIUS = 2
        const val ORB_SPAWN_OFFSET = 3
        const val ORB_BLOCK_FIRST = 2
        const val ELIDINIS_ARENA_X = 3797
        const val TUMEKEN_ARENA_X = 3814
        const val LANE_STEPS = 8

        private val OBELISK_PYRAMID = CoordGrid(3807, 5153, 1)
        private const val PYRAMID_X = 3798
        private const val PYRAMID_Z = 5141
        private const val PYRAMID_STEP = 3
        private const val PYRAMID_COLUMNS = 7
        private const val PYRAMID_ROWS = 9
    }
}
