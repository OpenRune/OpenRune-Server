package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterAerialFishingTable {
    const val FISH = 0
    const val ROLL = 1
    const val FISHING_LEVEL = 2
    const val FISHING_XP_TENTHS = 3
    const val HUNTER_LEVEL = 4
    const val HUNTER_XP_TENTHS = 5
    const val COOKING_XP_TENTHS = 6

    private class Fish(
        val key: String,
        val obj: String,
        val roll: Int,
        val fishing: Int,
        val fishingXp: Int,
        val hunter: Int,
        val hunterXp: Int,
        val cookingXp: Int,
    )

    private val FISHES =
        listOf(
            Fish("greater_siren", "obj.aerial_fishing_greater_siren", 82, 91, 1000, 87, 1300, 250),
            Fish("mottled_eel", "obj.aerial_fishing_mottled_eel", 67, 73, 650, 68, 900, 200),
            Fish("common_tench", "obj.aerial_fishing_common_tench", 52, 56, 400, 51, 450, 100),
            Fish("bluegill", "obj.aerial_fishing_bluegill", 0, 43, 115, 35, 165, 35),
        )

    fun aerialFishing() =
        dbTable("dbtable.hunter_aerial_fish", serverOnly = true) {
            column("fish", FISH, VarType.OBJ)
            column("roll", ROLL, VarType.INT)
            column("fishing_level", FISHING_LEVEL, VarType.INT)
            column("fishing_xp_tenths", FISHING_XP_TENTHS, VarType.INT)
            column("hunter_level", HUNTER_LEVEL, VarType.INT)
            column("hunter_xp_tenths", HUNTER_XP_TENTHS, VarType.INT)
            column("cooking_xp_tenths", COOKING_XP_TENTHS, VarType.INT)

            for (f in FISHES) {
                row("dbrow.hunter_aerial_fish_${f.key}") {
                    columnRSCM(FISH, f.obj)
                    column(ROLL, f.roll)
                    column(FISHING_LEVEL, f.fishing)
                    column(FISHING_XP_TENTHS, f.fishingXp)
                    column(HUNTER_LEVEL, f.hunter)
                    column(HUNTER_XP_TENTHS, f.hunterXp)
                    column(COOKING_XP_TENTHS, f.cookingXp)
                }
            }
        }
}
