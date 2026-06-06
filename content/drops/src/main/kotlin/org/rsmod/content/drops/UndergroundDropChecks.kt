package org.rsmod.content.drops

import org.rsmod.map.CoordGrid

public const val UNDERGROUND_Z_PLANE: Int = 6400

public fun CoordGrid.isUnderground(): Boolean = z >= UNDERGROUND_Z_PLANE

public fun CoordGrid.isOverworld(): Boolean = z < UNDERGROUND_Z_PLANE
