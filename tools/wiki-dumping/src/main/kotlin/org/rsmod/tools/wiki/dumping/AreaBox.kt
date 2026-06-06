package org.rsmod.tools.wiki.dumping

data class AreaBox(
    val minX: Int,
    val minZ: Int,
    val maxX: Int,
    val maxZ: Int,
    val minLevel: Int = 0,
    val maxLevel: Int = 3,
) {
    val area: Long
        get() = (maxX - minX).toLong() * (maxZ - minZ)

    fun contains(x: Int, z: Int, level: Int): Boolean =
        x in minX..maxX &&
            z in minZ..maxZ &&
            level in minLevel..maxLevel

    companion object {
        private const val REGION_SIZE = 64

        fun fromRegionId(regionId: Int, minLevel: Int = 0, maxLevel: Int = 3): AreaBox {
            val minX = (regionId ushr 8) shl 6
            val minZ = (regionId and 0xFF) shl 6
            return AreaBox(
                minX = minX,
                minZ = minZ,
                maxX = minX + REGION_SIZE - 1,
                maxZ = minZ + REGION_SIZE - 1,
                minLevel = minLevel,
                maxLevel = maxLevel,
            )
        }

        fun fromInts(values: IntArray): AreaBox? =
            when (values.size) {
                2 ->
                    AreaBox(
                        minX = values[0],
                        minZ = values[1],
                        maxX = values[0],
                        maxZ = values[1],
                    )
                3 ->
                    AreaBox(
                        minX = values[0],
                        minZ = values[1],
                        maxX = values[0],
                        maxZ = values[1],
                        minLevel = values[2],
                        maxLevel = values[2],
                    )
                4 ->
                    AreaBox(
                        minX = minOf(values[0], values[2]),
                        minZ = minOf(values[1], values[3]),
                        maxX = maxOf(values[0], values[2]),
                        maxZ = maxOf(values[1], values[3]),
                    )
                5 ->
                    AreaBox(
                        minX = minOf(values[0], values[2]),
                        minZ = minOf(values[1], values[3]),
                        maxX = maxOf(values[0], values[2]),
                        maxZ = maxOf(values[1], values[3]),
                        minLevel = values[4],
                        maxLevel = values[4],
                    )
                6 ->
                    AreaBox(
                        minX = minOf(values[0], values[3]),
                        minZ = minOf(values[1], values[4]),
                        maxX = maxOf(values[0], values[3]),
                        maxZ = maxOf(values[1], values[4]),
                        minLevel = minOf(values[2], values[5]),
                        maxLevel = maxOf(values[2], values[5]),
                    )
                else -> null
            }
    }
}
