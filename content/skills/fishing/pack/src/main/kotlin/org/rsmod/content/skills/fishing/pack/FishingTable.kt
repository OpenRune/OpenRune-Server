package org.rsmod.content.skills.fishing.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object FishingTable {
    const val COL_FISH = 0
    const val COL_SPOT = 1
    const val COL_METHOD = 2
    const val COL_LEVEL = 3
    const val COL_XP = 4
    const val COL_LOW = 5
    const val COL_HIGH = 6
    const val COL_STR_XP = 7
    const val COL_AGI_XP = 8
    const val COL_COUNT = 9
    const val COL_STR_REQ = 10
    const val COL_AGI_REQ = 11

    fun fishingSpot() = dbTable("dbtable.fishing_spot", serverOnly = true) {
        column("fish", COL_FISH, VarType.OBJ)
        column("spot", COL_SPOT, VarType.INT)
        column("method", COL_METHOD, VarType.INT)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("low", COL_LOW, VarType.INT)
        column("high", COL_HIGH, VarType.INT)
        column("str_xp", COL_STR_XP, VarType.INT)
        column("agi_xp", COL_AGI_XP, VarType.INT)
        column("count", COL_COUNT, VarType.INT)
        column("str_req", COL_STR_REQ, VarType.INT)
        column("agi_req", COL_AGI_REQ, VarType.INT)

        fun fish(
            name: String,
            obj: String,
            spot: Int,
            method: Int,
            level: Int,
            xp10: Int,
            low: Int,
            high: Int,
            strXp10: Int = 0,
            agiXp10: Int = 0,
            count: Int = 1,
            strReq: Int = 0,
            agiReq: Int = 0,
        ) =
            row(name) {
                columnRSCM(COL_FISH, obj)
                column(COL_SPOT, spot)
                column(COL_METHOD, method)
                column(COL_LEVEL, level)
                column(COL_XP, xp10)
                column(COL_LOW, low)
                column(COL_HIGH, high)
                column(COL_STR_XP, strXp10)
                column(COL_AGI_XP, agiXp10)
                column(COL_COUNT, count)
                column(COL_STR_REQ, strReq)
                column(COL_AGI_REQ, agiReq)
            }

        fish("dbrow.fishing_raw_shrimps", "obj.raw_shrimp", 0, 0, 1, 100, 48, 256)
        fish("dbrow.fishing_raw_anchovies", "obj.raw_anchovies", 0, 0, 15, 400, 24, 128)
        fish("dbrow.fishing_raw_sardine", "obj.raw_sardine", 0, 1, 5, 200, 32, 192)
        fish("dbrow.fishing_raw_herring", "obj.raw_herring", 0, 1, 10, 300, 24, 128)
        fish("dbrow.fishing_raw_trout", "obj.raw_trout", 1, 2, 20, 500, 32, 192)
        fish("dbrow.fishing_raw_salmon", "obj.raw_salmon", 1, 2, 30, 700, 16, 96)
        fish("dbrow.fishing_raw_pike", "obj.raw_pike", 1, 1, 25, 600, 16, 96)
        fish("dbrow.fishing_raw_lobster", "obj.raw_lobster", 2, 3, 40, 900, 6, 95)
        fish("dbrow.fishing_raw_tuna", "obj.raw_tuna", 2, 4, 35, 800, 8, 64)
        fish("dbrow.fishing_raw_swordfish", "obj.raw_swordfish", 2, 4, 50, 1000, 4, 48)
        fish("dbrow.fishing_raw_mackerel", "obj.raw_mackerel", 3, 5, 16, 200, 5, 65)
        fish("dbrow.fishing_raw_cod", "obj.raw_cod", 3, 5, 23, 450, 4, 55)
        fish("dbrow.fishing_raw_bass", "obj.raw_bass", 3, 5, 46, 1000, 3, 40)
        fish("dbrow.fishing_raw_shark", "obj.raw_shark", 3, 4, 76, 1100, 3, 40)
        fish("dbrow.fishing_raw_monkfish", "obj.raw_monkfish", 4, 0, 62, 1200, 48, 90)
        fish("dbrow.fishing_raw_rainbow_fish", "obj.hunting_raw_fish_special", 1, 2, 38, 800, 8, 64)
        fish("dbrow.fishing_raw_slimy_eel", "obj.mort_slimey_eel", 5, 1, 28, 650, 10, 80)
        fish("dbrow.fishing_raw_cave_eel", "obj.raw_cave_eel", 6, 1, 38, 800, 10, 80)
        fish("dbrow.fishing_raw_lava_eel", "obj.raw_lava_eel", 7, 6, 53, 300, 16, 96)
        fish("dbrow.fishing_raw_anglerfish", "obj.raw_anglerfish", 8, 7, 82, 1200, 3, 36)
        fish("dbrow.fishing_raw_dark_crab", "obj.raw_dark_crab", 9, 8, 85, 1300, 3, 40)
        fish("dbrow.fishing_raw_infernal_eel", "obj.infernal_eel", 10, 6, 80, 950, 30, 93)
        fish("dbrow.fishing_raw_karambwanji", "obj.tbwt_raw_karambwanji", 11, 0, 5, 50, 100, 250)
        fish("dbrow.fishing_leaping_trout", "obj.brut_spawning_trout", 12, 9, 48, 500, 32, 192, 50, 50, strReq = 15, agiReq = 15)
        fish("dbrow.fishing_leaping_salmon", "obj.brut_spawning_salmon", 12, 9, 58, 700, 16, 96, 60, 60, strReq = 30, agiReq = 30)
        fish("dbrow.fishing_leaping_sturgeon", "obj.brut_sturgeon", 12, 9, 70, 800, 8, 64, 70, 70, strReq = 45, agiReq = 45)
        fish("dbrow.fishing_minnow", "obj.minnow", 13, 0, 82, 261, 140, 280, count = 10)
        fish("dbrow.fishing_barehand_tuna", "obj.raw_tuna", 2, 10, 55, 800, 8, 64, 80, 0, 1, strReq = 35)
        fish("dbrow.fishing_barehand_swordfish", "obj.raw_swordfish", 2, 10, 70, 1000, 4, 48, 100, 0, 1, strReq = 50)
        fish("dbrow.fishing_barehand_shark", "obj.raw_shark", 3, 10, 96, 1100, 3, 40, 110, 0, 1, strReq = 76)
    }
}
