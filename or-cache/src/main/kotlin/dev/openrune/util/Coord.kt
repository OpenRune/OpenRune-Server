package dev.openrune.util

public const val Z_BIT_COUNT: Int = 14
public const val X_BIT_COUNT: Int = 14
public const val LEVEL_BIT_COUNT: Int = 2
public const val Z_BIT_MASK: Int = (1 shl Z_BIT_COUNT) - 1
public const val X_BIT_MASK: Int = (1 shl X_BIT_COUNT) - 1
public const val LEVEL_BIT_MASK: Int = (1 shl LEVEL_BIT_COUNT) - 1
public const val Z_BIT_OFFSET: Int = 0
public const val X_BIT_OFFSET: Int = Z_BIT_OFFSET + Z_BIT_COUNT
public const val LEVEL_BIT_OFFSET: Int = X_BIT_OFFSET + X_BIT_COUNT

public data class Coord(public val x: Int, public val z: Int, public val level: Int) {

    fun pack(): Int {
        require(x in 0..X_BIT_MASK) { "`x` value must be within range [0..$X_BIT_MASK]. (x=$x)" }
        require(z in 0..Z_BIT_MASK) { "`z` value must be within range [0..$Z_BIT_MASK]. (z=$z)" }
        require(level in 0..LEVEL_BIT_MASK) {
            "`level` value must be within range [0..$LEVEL_BIT_MASK]. (level=$level)"
        }
        return ((x and X_BIT_MASK) shl X_BIT_OFFSET) or
            ((z and Z_BIT_MASK) shl Z_BIT_OFFSET) or
            ((level and LEVEL_BIT_MASK) shl LEVEL_BIT_OFFSET)
    }

    companion object {
        fun unpack(packed: Int): Coord {
            val x = (packed shr X_BIT_OFFSET) and X_BIT_MASK
            val z = (packed shr Z_BIT_OFFSET) and Z_BIT_MASK
            val level = (packed shr LEVEL_BIT_OFFSET) and LEVEL_BIT_MASK

            return Coord(x, z, level)
        }
    }
}
