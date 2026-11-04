package org.rsmod.content.minigames.wintertodt.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object WintertodtTables {
    const val CATEGORY = 0
    const val OBJ = 1
    const val MIN = 2
    const val MAX = 3
    const val LOW = 4
    const val HIGH = 5
    const val ORD = 6

    fun rewards() =
        dbTable("dbtable.wintertodt_reward", serverOnly = true) {
            column("category", CATEGORY, VarType.STRING)
            column("obj", OBJ, VarType.OBJ)
            column("min", MIN, VarType.INT)
            column("max", MAX, VarType.INT)
            column("low", LOW, VarType.INT)
            column("high", HIGH, VarType.INT)
            column("ord", ORD, VarType.INT)

            fun entry(
                category: String,
                name: String,
                obj: String,
                ord: Int,
                min: Int,
                max: Int,
                low: Int,
                high: Int,
            ) {
                row("dbrow.wintertodt_reward_$name") {
                    column(CATEGORY, category)
                    columnRSCM(OBJ, obj)
                    column(MIN, min)
                    column(MAX, max)
                    column(LOW, low)
                    column(HIGH, high)
                    column(ORD, ord)
                }
            }

            entry("logs", "magic_logs", "obj.magic_logs", 0, 10, 20, -60, 60)
            entry("logs", "yew_logs", "obj.yew_logs", 1, 10, 20, -50, 90)
            entry("logs", "mahogany_logs", "obj.mahogany_logs", 2, 10, 20, -40, 130)
            entry("logs", "maple_logs", "obj.maple_logs", 3, 10, 20, 0, 160)
            entry("logs", "teak_logs", "obj.teak_logs", 4, 10, 20, 30, 200)
            entry("logs", "willow_logs", "obj.willow_logs", 5, 10, 20, 120, 100)
            entry("logs", "oak_logs", "obj.oak_logs", 6, 10, 20, 255, 255)

            entry("gems", "uncut_diamond", "obj.uncut_diamond", 0, 1, 3, 10, 120)
            entry("gems", "uncut_ruby", "obj.uncut_ruby", 1, 2, 4, 50, 140)
            entry("gems", "uncut_emerald", "obj.uncut_emerald", 2, 1, 3, 90, 160)
            entry("gems", "uncut_sapphire", "obj.uncut_sapphire", 3, 1, 3, 255, 255)

            entry("ores", "runite_ore", "obj.runite_ore", 0, 1, 2, -60, 40)
            entry("ores", "adamantite_ore", "obj.adamantite_ore", 1, 2, 3, -50, 80)
            entry("ores", "mithril_ore", "obj.mithril_ore", 2, 3, 5, -20, 140)
            entry("ores", "gold_ore", "obj.gold_ore", 3, 8, 11, 0, 160)
            entry("ores", "coal", "obj.coal", 4, 10, 14, 0, 180)
            entry("ores", "pure_essence", "obj.blankrune_high", 5, 20, 70, 40, 190)
            entry("ores", "silver_ore", "obj.silver_ore", 6, 10, 12, 140, 10)
            entry("ores", "limestone", "obj.limestone", 7, 2, 7, 200, -20)
            entry("ores", "iron_ore", "obj.iron_ore", 8, 5, 15, 255, 255)

            entry("herbs", "grimy_torstol", "obj.unidentified_torstol", 0, 1, 3, -70, 40)
            entry("herbs", "grimy_dwarf_weed", "obj.unidentified_dwarf_weed", 1, 2, 4, -50, 50)
            entry("herbs", "grimy_lantadyme", "obj.unidentified_lantadyme", 2, 2, 4, -30, 60)
            entry("herbs", "grimy_cadantine", "obj.unidentified_cadantine", 3, 2, 4, -10, 70)
            entry("herbs", "grimy_kwuarm", "obj.unidentified_kwuarm", 4, 2, 4, 10, 85)
            entry("herbs", "grimy_avantoe", "obj.unidentified_avantoe", 5, 3, 5, 20, 100)
            entry("herbs", "grimy_irit_leaf", "obj.unidentified_irit", 6, 3, 5, 30, 115)
            entry("herbs", "grimy_ranarr_weed", "obj.unidentified_ranarr", 7, 1, 3, 10, 170)
            entry("herbs", "grimy_tarromin", "obj.unidentified_tarromin", 8, 3, 6, 70, -20)
            entry("herbs", "grimy_marrentill", "obj.unidentified_marentill", 9, 3, 6, 100, -30)
            entry("herbs", "grimy_guam_leaf", "obj.unidentified_guam", 10, 3, 6, 170, -40)
            entry("herbs", "grimy_harralander", "obj.unidentified_harralander", 11, 3, 6, 255, 255)

            entry("seeds", "spirit_seed", "obj.spirit_tree_seed", 0, 1, 1, -20, 5)
            entry("seeds", "dwarf_weed_seed", "obj.dwarf_weed_seed", 1, 1, 3, -60, 25)
            entry("seeds", "lantadyme_seed", "obj.lantadyme_seed", 2, 1, 3, -60, 30)
            entry("seeds", "cadantine_seed", "obj.cadantine_seed", 3, 1, 3, -40, 40)
            entry("seeds", "snapdragon_seed", "obj.snapdragon_seed", 4, 1, 3, -15, 60)
            entry("seeds", "yew_seed", "obj.yew_seed", 5, 1, 2, -10, 70)
            entry("seeds", "snape_grass_seed", "obj.snape_grass_seed", 6, 3, 7, -8, 78)
            entry("seeds", "kwuarm_seed", "obj.kwuarm_seed", 7, 1, 3, -10, 80)
            entry("seeds", "ranarr_seed", "obj.ranarr_seed", 8, 1, 3, 0, 110)
            entry("seeds", "avantoe_seed", "obj.avantoe_seed", 9, 1, 3, 0, 130)
            entry("seeds", "watermelon_seed", "obj.watermelon_seed", 10, 3, 7, -10, 180)
            entry("seeds", "irit_seed", "obj.irit_seed", 11, 1, 3, 0, 170)
            entry("seeds", "teak_seed", "obj.teak_seed", 12, 1, 2, 10, 160)
            entry("seeds", "maple_seed", "obj.maple_seed", 13, 1, 2, 15, 190)
            entry("seeds", "mahogany_seed", "obj.mahogany_seed", 14, 1, 2, 20, 190)
            entry("seeds", "toadflax_seed", "obj.toadflax_seed", 15, 1, 3, 20, 190)
            entry("seeds", "banana_tree_seed", "obj.banana_tree_seed", 16, 1, 2, 30, 180)
            entry("seeds", "willow_seed", "obj.willow_seed", 17, 1, 2, 60, 120)
            entry("seeds", "harralander_seed", "obj.harralander_seed", 18, 1, 3, 80, -150)
            entry("seeds", "tarromin_seed", "obj.tarromin_seed", 19, 1, 3, 150, -150)
            entry("seeds", "acorn", "obj.acorn", 20, 1, 1, 255, 255)

            entry("fish", "raw_shark", "obj.raw_shark", 0, 6, 11, -60, 80)
            entry("fish", "raw_swordfish", "obj.raw_swordfish", 1, 6, 11, -50, 100)
            entry("fish", "raw_lobster", "obj.raw_lobster", 2, 6, 11, -20, 130)
            entry("fish", "raw_tuna", "obj.raw_tuna", 3, 6, 11, 10, 160)
            entry("fish", "raw_salmon", "obj.raw_salmon", 4, 6, 11, 40, 180)
            entry("fish", "raw_anchovies", "obj.raw_anchovies", 5, 6, 11, 160, 0)
            entry("fish", "raw_trout", "obj.raw_trout", 6, 6, 11, 255, 255)
        }
}
