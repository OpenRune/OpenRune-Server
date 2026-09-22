package org.rsmod.content.areas.misc.motherlode.veins

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType

/** The four ore vein variants, each paired with the depleted loc stored in the map. */
internal enum class MotherlodeVein(val depleted: String, val ore: String) {
    Single("loc.motherlode_depleted_single", "loc.motherlode_ore_single"),
    Left("loc.motherlode_depleted_left", "loc.motherlode_ore_left"),
    Middle("loc.motherlode_depleted_middle", "loc.motherlode_ore_middle"),
    Right("loc.motherlode_depleted_right", "loc.motherlode_ore_right");

    val oreId: Int
        get() = ore.asRSCM(RSCMType.LOC)

    companion object {
        private val byDepletedId by lazy { entries.associateBy { it.depleted.asRSCM(RSCMType.LOC) } }

        fun forDepletedId(id: Int): MotherlodeVein? = byDepletedId[id]
    }
}
