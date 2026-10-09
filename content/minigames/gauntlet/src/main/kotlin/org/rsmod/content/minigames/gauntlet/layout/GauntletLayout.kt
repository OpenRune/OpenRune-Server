package org.rsmod.content.minigames.gauntlet.layout

import kotlin.random.Random

enum class RoomKind {
    MIDDLE,
    EDGE,
    CORNER,
    START,
    BOSS,
}

data class GauntletRoom(
    val x: Int,
    val z: Int,
    val kind: RoomKind,
    val variant: Int,
    val rotation: Int,
) {
    val index: Int
        get() = GauntletLayout.index(x, z)
}

class GauntletLayout(val startIndex: Int, private val rooms: List<GauntletRoom>) {
    val startRoom: GauntletRoom
        get() = rooms[startIndex]

    val bossRoom: GauntletRoom
        get() = rooms[BOSS_INDEX]

    operator fun get(x: Int, z: Int): GauntletRoom = rooms[index(x, z)]

    fun all(): List<GauntletRoom> = rooms

    companion object {
        const val GRID = 7
        const val BOSS_X = 3
        const val BOSS_Z = 3
        const val BOSS_INDEX = BOSS_Z * GRID + BOSS_X
        const val VARIANTS = 4

        private val START_CANDIDATES =
            intArrayOf(
                index(BOSS_X, BOSS_Z - 1),
                index(BOSS_X - 1, BOSS_Z),
                index(BOSS_X + 1, BOSS_Z),
                index(BOSS_X, BOSS_Z + 1),
            )

        fun index(x: Int, z: Int): Int = z * GRID + x

        fun generate(random: Random): GauntletLayout =
            generate(START_CANDIDATES[random.nextInt(START_CANDIDATES.size)], random)

        fun generate(startIndex: Int, random: Random): GauntletLayout {
            require(startIndex in START_CANDIDATES) { "Start room must neighbour the boss room." }
            val facing = bossFacingRotation(startIndex)
            val rooms =
                List(GRID * GRID) { i ->
                    val x = i % GRID
                    val z = i / GRID
                    when (i) {
                        BOSS_INDEX -> GauntletRoom(x, z, RoomKind.BOSS, 0, facing)
                        startIndex -> GauntletRoom(x, z, RoomKind.START, 0, facing)
                        else -> {
                            val kind = kindAt(x, z)
                            val rotation =
                                when (kind) {
                                    RoomKind.MIDDLE -> random.nextInt(4)
                                    else -> outwardRotation(x, z)
                                }
                            GauntletRoom(x, z, kind, random.nextInt(VARIANTS), rotation)
                        }
                    }
                }
            return GauntletLayout(startIndex, rooms)
        }

        fun kindAt(x: Int, z: Int): RoomKind {
            val edgeX = x == 0 || x == GRID - 1
            val edgeZ = z == 0 || z == GRID - 1
            return when {
                edgeX && edgeZ -> RoomKind.CORNER
                edgeX || edgeZ -> RoomKind.EDGE
                else -> RoomKind.MIDDLE
            }
        }

        /**
         * Unrotated boss/start templates assume the start room sits directly north of the boss room
         * (start index 31). Each further clockwise quarter-turn moves the start side east, south,
         * then west.
         */
        fun bossFacingRotation(startIndex: Int): Int =
            when (startIndex) {
                index(BOSS_X, BOSS_Z + 1) -> 0
                index(BOSS_X + 1, BOSS_Z) -> 1
                index(BOSS_X, BOSS_Z - 1) -> 2
                index(BOSS_X - 1, BOSS_Z) -> 3
                else -> error("Start room must neighbour the boss room.")
            }

        fun outwardRotation(x: Int, z: Int): Int =
            when {
                x == GRID - 1 && z == GRID - 1 -> 0
                x == 0 && z == GRID - 1 -> 3
                x == 0 && z == 0 -> 2
                x == GRID - 1 && z == 0 -> 1
                x == 0 -> 3
                x == GRID - 1 -> 1
                z == 0 -> 2
                else -> 0
            }
    }
}
