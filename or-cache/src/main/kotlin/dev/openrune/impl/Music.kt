package dev.openrune.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

public const val Z_BIT_COUNT: Int = 14
public const val X_BIT_COUNT: Int = 14

public const val Z_BIT_OFFSET: Int = 0
public const val X_BIT_OFFSET: Int = Z_BIT_OFFSET + Z_BIT_COUNT

public const val Z_BIT_MASK: Int = (1 shl Z_BIT_COUNT) - 1
public const val X_BIT_MASK: Int = (1 shl X_BIT_COUNT) - 1
public const val LENGTH: Int = 64

fun packVertex(mx: Int, mz: Int, lx: Int, lz: Int): Int {
    require(mx in 0..X_BIT_MASK)
    require(mz in 0..Z_BIT_MASK)
    require(lx in 0..X_BIT_MASK)
    require(lz in 0..Z_BIT_MASK)

    val x = mx * LENGTH + lx
    val z = mz * LENGTH + lz

    require(x in 0..X_BIT_MASK)
    require(z in 0..Z_BIT_MASK)

    return ((x and X_BIT_MASK) shl X_BIT_OFFSET) or ((z and Z_BIT_MASK) shl Z_BIT_OFFSET)
}

object Music {

    fun musicModern() =
        dbTable("dbtable.music_modern", serverOnly = true) {
            column("area", 0, VarType.STRING)
            column("tracks", 1, VarType.DBROW)
            column("auto_script", 2, VarType.BOOLEAN)

            row("dbrow.music_modern_lumbridge") {
                column(0, "lumbridge")
                columnRSCM(
                    1,
                    "dbrow.music_autumn_voyage",
                    "dbrow.music_book_of_spells",
                    "dbrow.music_dream",
                    "dbrow.music_flute_salad",
                    "dbrow.music_harmony",
                    "dbrow.music_yesteryear",
                )
                column(2, true)
            }
        }

    fun musicClassic() =
        dbTable("dbtable.music_classic", serverOnly = true) {
            column("area", 0, VarType.AREA)
            column("track", 1, VarType.DBROW)
            column("auto_script", 2, VarType.BOOLEAN)
        }
}
