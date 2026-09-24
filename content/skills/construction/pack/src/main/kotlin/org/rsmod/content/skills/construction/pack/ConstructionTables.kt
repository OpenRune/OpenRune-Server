package org.rsmod.content.skills.construction.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * What a piece of furniture builds, keyed by its `dbcol.furniture:model_obj`.
 *
 * `dbtable.furniture` carries the name, materials and level of every build but neither a loc column
 * nor an xp column, and the obj and loc names are different vocabularies rather than spelling
 * variants - `obj.poh_armchair_1` builds `loc.poh_chair1` - so neither can be derived from it. The
 * values here were transcribed from an OSRS 184 reference server whose item and loc ids match this
 * cache, and every one has been resolved to a gameval symbol.
 *
 * `locs` is what the piece builds, several meaning a multi-tile piece. `parts` pairs a hotspot loc
 * with the loc that piece puts on that part of the hotspot, which is why a rug lays corners on
 * corners: the reference server pairs its hotspot array with its built-object array by position,
 * and these are those pairs. A hotspot part absent from `parts` falls back to the first of `locs`.
 *
 * Xp is stored multiplied by ten. A row with no xp falls back to its material total.
 */
object ConstructionTables {
    const val MODEL_OBJ = 0
    const val XP = 1
    const val LOCS = 2
    const val PARTS = 3

    fun furnitureBuilds() =
        dbTable("dbtable.construction_furniture_build", serverOnly = true) {
            column("model_obj", MODEL_OBJ, VarType.OBJ)
            column("xp", XP, VarType.INT)
            column("locs", LOCS, VarType.LOC)
            column("parts", PARTS, VarType.LOC, VarType.LOC)

            row("dbrow.build_poh_combat_ring_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_combat_ring_1")
                column(XP, 4200)
                columnRSCM(LOCS, "loc.poh_boxing_ringwall_white", "loc.poh_boxing_ringwall_blue", "loc.poh_boxing_ringwall_corner", "loc.poh_boxing_ringwall_red", "loc.poh_boxing_ring_mat_side", "loc.poh_boxing_ring_mat_corner", "loc.poh_boxing_ring_mat_middle")
                columnRSCM(
                    PARTS,
                    "loc.poh_gr_1_wall_combat", "loc.poh_boxing_ringwall_white", "loc.poh_gr_1_wall_bluecorner", "loc.poh_boxing_ringwall_blue",
                    "loc.poh_gr_1_wall_cobat_corner", "loc.poh_boxing_ringwall_corner", "loc.poh_gr_1_wall_everything", "loc.poh_boxing_ringwall_corner",
                    "loc.poh_gr_1_wall_redcorner", "loc.poh_boxing_ringwall_red", "loc.poh_gr_1_wall_combat_agility_corner", "loc.poh_boxing_ringwall_white",
                    "loc.poh_gr_1_wall_combat_agility", "loc.poh_boxing_ringwall_white", "loc.poh_gr_1_wall_ranging_combat", "loc.poh_boxing_ringwall_white",
                    "loc.poh_gr_1_floor_n", "loc.poh_boxing_ring_mat_side", "loc.poh_gr_1_floor_ne", "loc.poh_boxing_ring_mat_corner",
                    "loc.poh_gr_1_floor_nw", "loc.poh_boxing_ring_mat_corner", "loc.poh_gr_1_floor_side", "loc.poh_boxing_ring_mat_side",
                    "loc.poh_gr_1_floor_middle", "loc.poh_boxing_ring_mat_middle", "loc.poh_gr_1_floor_s", "loc.poh_boxing_ring_mat_side",
                    "loc.poh_gr_1_floor_sw", "loc.poh_boxing_ring_mat_corner", "loc.poh_gr_1_floor_se", "loc.poh_boxing_ring_mat_corner",
                )
            }
            row("dbrow.build_poh_combat_ring_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_combat_ring_2")
                column(XP, 5700)
                columnRSCM(LOCS, "loc.poh_fencing_ringwall", "loc.poh_fencing_ring_mat_side", "loc.poh_fencing_ring_mat_corner", "loc.poh_fencing_ring_mat_middle")
                columnRSCM(
                    PARTS,
                    "loc.poh_gr_1_wall_combat", "loc.poh_fencing_ringwall", "loc.poh_gr_1_wall_bluecorner", "loc.poh_fencing_ringwall",
                    "loc.poh_gr_1_wall_cobat_corner", "loc.poh_fencing_ringwall", "loc.poh_gr_1_wall_everything", "loc.poh_fencing_ringwall",
                    "loc.poh_gr_1_wall_redcorner", "loc.poh_fencing_ringwall", "loc.poh_gr_1_wall_combat_agility_corner", "loc.poh_fencing_ringwall",
                    "loc.poh_gr_1_wall_combat_agility", "loc.poh_fencing_ringwall", "loc.poh_gr_1_wall_ranging_combat", "loc.poh_fencing_ringwall",
                    "loc.poh_gr_1_floor_n", "loc.poh_fencing_ring_mat_side", "loc.poh_gr_1_floor_ne", "loc.poh_fencing_ring_mat_corner",
                    "loc.poh_gr_1_floor_nw", "loc.poh_fencing_ring_mat_corner", "loc.poh_gr_1_floor_side", "loc.poh_fencing_ring_mat_side",
                    "loc.poh_gr_1_floor_middle", "loc.poh_fencing_ring_mat_middle", "loc.poh_gr_1_floor_s", "loc.poh_fencing_ring_mat_side",
                    "loc.poh_gr_1_floor_sw", "loc.poh_fencing_ring_mat_corner", "loc.poh_gr_1_floor_se", "loc.poh_fencing_ring_mat_corner",
                )
            }
            row("dbrow.build_poh_combat_ring_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_combat_ring_3")
                column(XP, 6300)
                columnRSCM(LOCS, "loc.poh_combat_ringwall", "loc.poh_combat_mat_side", "loc.poh_combat_mat_corner", "loc.poh_combat_mat_middle")
                columnRSCM(
                    PARTS,
                    "loc.poh_gr_1_wall_combat", "loc.poh_combat_ringwall", "loc.poh_gr_1_wall_bluecorner", "loc.poh_combat_ringwall",
                    "loc.poh_gr_1_wall_cobat_corner", "loc.poh_combat_ringwall", "loc.poh_gr_1_wall_everything", "loc.poh_combat_ringwall",
                    "loc.poh_gr_1_wall_redcorner", "loc.poh_combat_ringwall", "loc.poh_gr_1_wall_combat_agility_corner", "loc.poh_combat_ringwall",
                    "loc.poh_gr_1_wall_combat_agility", "loc.poh_combat_ringwall", "loc.poh_gr_1_wall_ranging_combat", "loc.poh_combat_ringwall",
                    "loc.poh_gr_1_floor_n", "loc.poh_combat_mat_side", "loc.poh_gr_1_floor_ne", "loc.poh_combat_mat_corner",
                    "loc.poh_gr_1_floor_nw", "loc.poh_combat_mat_corner", "loc.poh_gr_1_floor_side", "loc.poh_combat_mat_side",
                    "loc.poh_gr_1_floor_middle", "loc.poh_combat_mat_middle", "loc.poh_gr_1_floor_s", "loc.poh_combat_mat_side",
                    "loc.poh_gr_1_floor_sw", "loc.poh_combat_mat_corner", "loc.poh_gr_1_floor_se", "loc.poh_combat_mat_corner",
                )
            }
            row("dbrow.build_poh_combat_ring_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_combat_ring_4")
                column(XP, 7200)
                columnRSCM(LOCS, "loc.poh_invisible_type1", "loc.poh_magic_circle_wall", "loc.poh_statue_saradomin", "loc.poh_magic_circle_mat", "loc.poh_invisible_type0")
                columnRSCM(
                    PARTS,
                    "loc.poh_gr_1_wall_combat", "loc.poh_invisible_type1", "loc.poh_gr_1_wall_everything", "loc.poh_magic_circle_wall",
                    "loc.poh_gr_1_wall_ranging", "loc.poh_magic_circle_wall", "loc.poh_gr_1_wall_ranging_agility", "loc.poh_magic_circle_wall",
                    "loc.poh_gr_1_wall_agility", "loc.poh_statue_saradomin", "loc.poh_gr_1_wall_combat_agility", "loc.poh_statue_saradomin",
                    "loc.poh_gr_1_wall_ranging_combat", "loc.poh_magic_circle_wall", "loc.poh_gr_1_floor_n", "loc.poh_statue_saradomin",
                    "loc.poh_gr_1_floor_ne", "loc.poh_statue_saradomin", "loc.poh_gr_1_floor_nw", "loc.poh_magic_circle_mat",
                    "loc.poh_gr_1_floor_side", "loc.poh_invisible_type0", "loc.poh_gr_1_floor_middle", "loc.poh_invisible_type0",
                    "loc.poh_gr_1_floor_s", "loc.poh_invisible_type0", "loc.poh_gr_1_floor_sw", "loc.poh_invisible_type0",
                    "loc.poh_gr_1_floor_se", "loc.poh_magic_circle_mat",
                )
            }
            row("dbrow.build_poh_combat_ring_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_combat_ring_5")
                column(XP, 10000)
                columnRSCM(LOCS, "loc.poh_invisible_type1", "loc.poh_agility_rail", "loc.poh_balancebeam_middle", "loc.poh_balancebeam_endr", "loc.poh_balancebeam_endl", "loc.poh_invisible_type0")
                columnRSCM(
                    PARTS,
                    "loc.poh_gr_1_wall_combat", "loc.poh_invisible_type1", "loc.poh_gr_1_wall_everything", "loc.poh_agility_rail",
                    "loc.poh_gr_1_wall_ranging", "loc.poh_invisible_type1", "loc.poh_gr_1_wall_ranging_agility", "loc.poh_agility_rail",
                    "loc.poh_gr_1_wall_agility", "loc.poh_agility_rail", "loc.poh_gr_1_wall_combat_agility", "loc.poh_agility_rail",
                    "loc.poh_gr_1_wall_ranging_combat", "loc.poh_invisible_type1", "loc.poh_gr_1_floor_n", "loc.poh_balancebeam_middle",
                    "loc.poh_gr_1_floor_ne", "loc.poh_balancebeam_endr", "loc.poh_gr_1_floor_nw", "loc.poh_balancebeam_endl",
                    "loc.poh_gr_1_floor_side", "loc.poh_invisible_type0", "loc.poh_gr_1_floor_middle", "loc.poh_invisible_type0",
                    "loc.poh_gr_1_floor_s", "loc.poh_invisible_type0", "loc.poh_gr_1_floor_sw", "loc.poh_invisible_type0",
                    "loc.poh_gr_1_floor_se", "loc.poh_invisible_type0",
                )
            }
            row("dbrow.build_poh_weapon_rack_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_weapon_rack_1")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_weapons_rack_gloves")
                columnRSCM(PARTS, "loc.poh_combat_room_4", "loc.poh_weapons_rack_gloves")
            }
            row("dbrow.build_poh_weapon_rack_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_weapon_rack_2")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_weapons_rack_gloves+woodenstuff")
                columnRSCM(PARTS, "loc.poh_combat_room_4", "loc.poh_weapons_rack_gloves+woodenstuff")
            }
            row("dbrow.build_poh_weapon_rack_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_weapon_rack_3")
                column(XP, 4400)
                columnRSCM(LOCS, "loc.poh_weapons_rack_gloves+woodenstuff+pugels")
                columnRSCM(PARTS, "loc.poh_combat_room_4", "loc.poh_weapons_rack_gloves+woodenstuff+pugels")
            }
            row("dbrow.build_poh_bed_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_bed_1")
                column(XP, 1170)
                columnRSCM(LOCS, "loc.poh_bed_1")
                columnRSCM(PARTS, "loc.poh_bedroom_1_doublebed", "loc.poh_bed_1")
            }
            row("dbrow.build_poh_bed_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_bed_2")
                column(XP, 2100)
                columnRSCM(LOCS, "loc.poh_bed_2")
                columnRSCM(PARTS, "loc.poh_bedroom_1_doublebed", "loc.poh_bed_2")
            }
            row("dbrow.build_poh_bed_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_bed_3")
                column(XP, 3300)
                columnRSCM(LOCS, "loc.poh_bed_3")
                columnRSCM(PARTS, "loc.poh_bedroom_1_doublebed", "loc.poh_bed_3")
            }
            row("dbrow.build_poh_bed_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_bed_4")
                column(XP, 3000)
                columnRSCM(LOCS, "loc.poh_bed_4")
                columnRSCM(PARTS, "loc.poh_bedroom_1_doublebed", "loc.poh_bed_4")
            }
            row("dbrow.build_poh_bed_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_bed_5")
                column(XP, 4800)
                columnRSCM(LOCS, "loc.poh_bed_5")
                columnRSCM(PARTS, "loc.poh_bedroom_1_doublebed", "loc.poh_bed_5")
            }
            row("dbrow.build_poh_bed_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_bed_6")
                column(XP, 4500)
                columnRSCM(LOCS, "loc.poh_bed_6")
                columnRSCM(PARTS, "loc.poh_bedroom_1_doublebed", "loc.poh_bed_6")
            }
            row("dbrow.build_poh_bed_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_bed_7")
                column(XP, 4500)
                columnRSCM(LOCS, "loc.poh_bed_7")
                columnRSCM(PARTS, "loc.poh_bedroom_1_doublebed", "loc.poh_bed_7")
            }
            row("dbrow.build_poh_wardrobe_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_wardrobe_1")
                column(XP, 580)
                columnRSCM(LOCS, "loc.poh_wardrobe_1")
                columnRSCM(PARTS, "loc.poh_bedroom_2", "loc.poh_wardrobe_1")
            }
            row("dbrow.build_poh_wardrobe_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_wardrobe_2")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_wardrobe_2")
                columnRSCM(PARTS, "loc.poh_bedroom_2", "loc.poh_wardrobe_2")
            }
            row("dbrow.build_poh_wardrobe_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_wardrobe_3")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_wardrobe_3")
                columnRSCM(PARTS, "loc.poh_bedroom_2", "loc.poh_wardrobe_3")
            }
            row("dbrow.build_poh_wardrobe_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_wardrobe_4")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_wardrobe_4")
                columnRSCM(PARTS, "loc.poh_bedroom_2", "loc.poh_wardrobe_4")
            }
            row("dbrow.build_poh_wardrobe_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_wardrobe_5")
                column(XP, 2700)
                columnRSCM(LOCS, "loc.poh_wardrobe_5")
                columnRSCM(PARTS, "loc.poh_bedroom_2", "loc.poh_wardrobe_5")
            }
            row("dbrow.build_poh_wardrobe_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_wardrobe_6")
                column(XP, 4200)
                columnRSCM(LOCS, "loc.poh_wardrobe_6")
                columnRSCM(PARTS, "loc.poh_bedroom_2", "loc.poh_wardrobe_6")
            }
            row("dbrow.build_poh_wardrobe_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_wardrobe_7")
                column(XP, 7200)
                columnRSCM(LOCS, "loc.poh_wardrobe_7")
                columnRSCM(PARTS, "loc.poh_bedroom_2", "loc.poh_wardrobe_7")
            }
            row("dbrow.build_poh_mirror_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_mirror_1")
                column(XP, 300)
                columnRSCM(LOCS, "loc.poh_mirror_1")
                columnRSCM(PARTS, "loc.poh_bedroom_3", "loc.poh_mirror_1")
            }
            row("dbrow.build_poh_mirror_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_mirror_2")
                column(XP, 610)
                columnRSCM(LOCS, "loc.poh_mirror_2")
                columnRSCM(PARTS, "loc.poh_bedroom_3", "loc.poh_mirror_2")
            }
            row("dbrow.build_poh_mirror_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_mirror_3")
                column(XP, 1210)
                columnRSCM(LOCS, "loc.poh_mirror_3")
                columnRSCM(PARTS, "loc.poh_bedroom_3", "loc.poh_mirror_3")
            }
            row("dbrow.build_poh_mirror_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_mirror_4")
                column(XP, 1810)
                columnRSCM(LOCS, "loc.poh_mirror_4")
                columnRSCM(PARTS, "loc.poh_bedroom_3", "loc.poh_mirror_4")
            }
            row("dbrow.build_poh_mirror_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_mirror_5")
                column(XP, 1820)
                columnRSCM(LOCS, "loc.poh_mirror_5")
                columnRSCM(PARTS, "loc.poh_bedroom_3", "loc.poh_mirror_5")
            }
            row("dbrow.build_poh_mirror_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_mirror_6")
                column(XP, 2810)
                columnRSCM(LOCS, "loc.poh_mirror_6")
                columnRSCM(PARTS, "loc.poh_bedroom_3", "loc.poh_mirror_6")
            }
            row("dbrow.build_poh_mirror_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_mirror_7")
                column(XP, 5820)
                columnRSCM(LOCS, "loc.poh_mirror_7")
                columnRSCM(PARTS, "loc.poh_bedroom_3", "loc.poh_mirror_7")
            }
            row("dbrow.build_poh_clock_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_clock_1")
                column(XP, 1420)
                columnRSCM(LOCS, "loc.poh_clock_1")
                columnRSCM(PARTS, "loc.poh_bedroom_7", "loc.poh_clock_1")
            }
            row("dbrow.build_poh_clock_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_clock_2")
                column(XP, 2020)
                columnRSCM(LOCS, "loc.poh_clock_2")
                columnRSCM(PARTS, "loc.poh_bedroom_7", "loc.poh_clock_2")
            }
            row("dbrow.build_poh_clock_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_clock_3")
                column(XP, 6020)
                columnRSCM(LOCS, "loc.poh_clock_3")
                columnRSCM(PARTS, "loc.poh_bedroom_7", "loc.poh_clock_3")
            }
            row("dbrow.build_poh_symbol_saradomin") {
                columnRSCM(MODEL_OBJ, "obj.poh_symbol_saradomin")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_icon_1")
                columnRSCM(PARTS, "loc.poh_chapel_1", "loc.poh_icon_1")
            }
            row("dbrow.build_poh_symbol_zamorak") {
                columnRSCM(MODEL_OBJ, "obj.poh_symbol_zamorak")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_icon_2")
                columnRSCM(PARTS, "loc.poh_chapel_1", "loc.poh_icon_2")
            }
            row("dbrow.build_poh_symbol_guthix") {
                columnRSCM(MODEL_OBJ, "obj.poh_symbol_guthix")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_icon_3")
                columnRSCM(PARTS, "loc.poh_chapel_1", "loc.poh_icon_3")
            }
            row("dbrow.build_poh_icon_saradomin") {
                columnRSCM(MODEL_OBJ, "obj.poh_icon_saradomin")
                column(XP, 9600)
                columnRSCM(LOCS, "loc.poh_icon_4")
                columnRSCM(PARTS, "loc.poh_chapel_1", "loc.poh_icon_4")
            }
            row("dbrow.build_poh_icon_zamorak") {
                columnRSCM(MODEL_OBJ, "obj.poh_icon_zamorak")
                column(XP, 9600)
                columnRSCM(LOCS, "loc.poh_icon_5")
                columnRSCM(PARTS, "loc.poh_chapel_1", "loc.poh_icon_5")
            }
            row("dbrow.build_poh_icon_guthix") {
                columnRSCM(MODEL_OBJ, "obj.poh_icon_guthix")
                column(XP, 9600)
                columnRSCM(LOCS, "loc.poh_icon_6")
                columnRSCM(PARTS, "loc.poh_chapel_1", "loc.poh_icon_6")
            }
            row("dbrow.build_poh_icon_bob") {
                columnRSCM(MODEL_OBJ, "obj.poh_icon_bob")
                column(XP, 11600)
                columnRSCM(LOCS, "loc.poh_icon_7")
                columnRSCM(PARTS, "loc.poh_chapel_1", "loc.poh_icon_7")
            }
            row("dbrow.build_poh_altar_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_altar_oak")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_altar_saradomin_1")
                columnRSCM(PARTS, "loc.poh_chapel_2", "loc.poh_altar_saradomin_1")
            }
            row("dbrow.build_poh_altar_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_altar_teak")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_altar_saradomin_2")
                columnRSCM(PARTS, "loc.poh_chapel_2", "loc.poh_altar_saradomin_2")
            }
            row("dbrow.build_poh_altar_teak_cloth") {
                columnRSCM(MODEL_OBJ, "obj.poh_altar_teak_cloth")
                column(XP, 3900)
                columnRSCM(LOCS, "loc.poh_altar_saradomin_3")
                columnRSCM(PARTS, "loc.poh_chapel_2", "loc.poh_altar_saradomin_3")
            }
            row("dbrow.build_poh_altar_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_altar_mahogany")
                column(XP, 5900)
                columnRSCM(LOCS, "loc.poh_altar_saradomin_4")
                columnRSCM(PARTS, "loc.poh_chapel_2", "loc.poh_altar_saradomin_4")
            }
            row("dbrow.build_poh_altar_limestone") {
                columnRSCM(MODEL_OBJ, "obj.poh_altar_limestone")
                column(XP, 9100)
                columnRSCM(LOCS, "loc.poh_altar_saradomin_5")
                columnRSCM(PARTS, "loc.poh_chapel_2", "loc.poh_altar_saradomin_5")
            }
            row("dbrow.build_poh_altar_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_altar_marble")
                column(XP, 10300)
                columnRSCM(LOCS, "loc.poh_altar_saradomin_6")
                columnRSCM(PARTS, "loc.poh_chapel_2", "loc.poh_altar_saradomin_6")
            }
            row("dbrow.build_poh_altar_marble_and_gilt") {
                columnRSCM(MODEL_OBJ, "obj.poh_altar_marble+gilt")
                column(XP, 22300)
                columnRSCM(LOCS, "loc.poh_altar_saradomin_7")
                columnRSCM(PARTS, "loc.poh_chapel_2", "loc.poh_altar_saradomin_7")
            }
            row("dbrow.build_poh_torches_wooden") {
                columnRSCM(MODEL_OBJ, "obj.poh_torches_wooden")
                column(XP, 580)
                columnRSCM(LOCS, "loc.poh_torch_1")
                columnRSCM(PARTS, "loc.poh_chapel_3", "loc.poh_torch_1")
            }
            row("dbrow.build_poh_torches_steel") {
                columnRSCM(MODEL_OBJ, "obj.poh_torches_steel")
                column(XP, 800)
                columnRSCM(LOCS, "loc.poh_torch_2")
                columnRSCM(PARTS, "loc.poh_chapel_3", "loc.poh_torch_2")
            }
            row("dbrow.build_poh_candlesticks_steel") {
                columnRSCM(MODEL_OBJ, "obj.poh_candlesticks_steel")
                column(XP, 1240)
                columnRSCM(LOCS, "loc.poh_torch_3")
                columnRSCM(PARTS, "loc.poh_chapel_3", "loc.poh_torch_3")
            }
            row("dbrow.build_poh_candlesticks_gilt") {
                columnRSCM(MODEL_OBJ, "obj.poh_candlesticks_gilt")
                column(XP, 460)
                columnRSCM(LOCS, "loc.poh_torch_4")
                columnRSCM(PARTS, "loc.poh_chapel_3", "loc.poh_torch_4")
            }
            row("dbrow.build_poh_incense_burner_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_incense_burner_oak")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_torch_5")
                columnRSCM(PARTS, "loc.poh_chapel_3", "loc.poh_torch_5")
            }
            row("dbrow.build_poh_incense_burner_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_incense_burner_mahogany")
                column(XP, 6000)
                columnRSCM(LOCS, "loc.poh_torch_6")
                columnRSCM(PARTS, "loc.poh_chapel_3", "loc.poh_torch_6")
            }
            row("dbrow.build_poh_incense_burner_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_incense_burner_marble")
                column(XP, 16000)
                columnRSCM(LOCS, "loc.poh_torch_7")
                columnRSCM(PARTS, "loc.poh_chapel_3", "loc.poh_torch_7")
            }
            row("dbrow.build_poh_chapel_window_shutters") {
                columnRSCM(MODEL_OBJ, "obj.poh_chapel_window_shutters")
                column(XP, 2280)
                columnRSCM(LOCS, "loc.poh_brimhaven_window_shutters", "loc.poh_lumbridge_window_shutters", "loc.poh_pollnivneach_window_shutters", "loc.poh_rellekka_window_shutters", "loc.poh_rimmington_window_shutters", "loc.poh_yanille_window_shutters", "loc.poh_deathly_window_shutters")
                columnRSCM(
                    PARTS,
                    "loc.poh_chapelwindow_hotspot_lumbridge", "loc.poh_lumbridge_window_shutters", "loc.poh_chapelwindow_hotspot_rellekka", "loc.poh_rellekka_window_shutters",
                    "loc.poh_chapelwindow_hotspot_rimmington", "loc.poh_rimmington_window_shutters", "loc.poh_chapelwindow_hotspot_yanille", "loc.poh_yanille_window_shutters",
                    "loc.poh_chapelwindow_hotspot_pollnivneach", "loc.poh_pollnivneach_window_shutters", "loc.poh_chapelwindow_hotspot_brimhaven", "loc.poh_brimhaven_window_shutters",
                    "loc.poh_chapelwindow_hotspot_deathly", "loc.poh_deathly_window_shutters",
                )
            }
            row("dbrow.build_poh_chapel_window_decorative") {
                columnRSCM(MODEL_OBJ, "obj.poh_chapel_window_decorative")
                column(XP, 40)
                columnRSCM(LOCS, "loc.poh_brimhaven_window_bob", "loc.poh_lumbridge_window_bob", "loc.poh_pollnivneach_window_bob", "loc.poh_rellekka_window_bob", "loc.poh_rimmington_window_bob", "loc.poh_yanille_window_bob", "loc.poh_deathly_window_bob")
                columnRSCM(
                    PARTS,
                    "loc.poh_chapelwindow_hotspot_lumbridge", "loc.poh_lumbridge_window_bob", "loc.poh_chapelwindow_hotspot_rellekka", "loc.poh_rellekka_window_bob",
                    "loc.poh_chapelwindow_hotspot_rimmington", "loc.poh_rimmington_window_bob", "loc.poh_chapelwindow_hotspot_yanille", "loc.poh_yanille_window_bob",
                    "loc.poh_chapelwindow_hotspot_pollnivneach", "loc.poh_pollnivneach_window_bob", "loc.poh_chapelwindow_hotspot_brimhaven", "loc.poh_brimhaven_window_bob",
                    "loc.poh_chapelwindow_hotspot_deathly", "loc.poh_deathly_window_bob",
                )
            }
            row("dbrow.build_poh_chapel_window_stainedglass") {
                columnRSCM(MODEL_OBJ, "obj.poh_chapel_window_stainedglass")
                column(XP, 50)
                columnRSCM(LOCS, "loc.poh_brimhaven_window_bob2", "loc.poh_lumbridge_window_bob2", "loc.poh_pollnivneach_window_bob2", "loc.poh_rellekka_window_bob2", "loc.poh_rimmington_window_bob2", "loc.poh_yanille_window_bob2", "loc.poh_deathly_window_bob2")
                columnRSCM(
                    PARTS,
                    "loc.poh_chapelwindow_hotspot_lumbridge", "loc.poh_lumbridge_window_bob2", "loc.poh_chapelwindow_hotspot_rellekka", "loc.poh_rellekka_window_bob2",
                    "loc.poh_chapelwindow_hotspot_rimmington", "loc.poh_rimmington_window_bob2", "loc.poh_chapelwindow_hotspot_yanille", "loc.poh_yanille_window_bob2",
                    "loc.poh_chapelwindow_hotspot_pollnivneach", "loc.poh_pollnivneach_window_bob2", "loc.poh_chapelwindow_hotspot_brimhaven", "loc.poh_brimhaven_window_bob2",
                    "loc.poh_chapelwindow_hotspot_deathly", "loc.poh_deathly_window_bob2",
                )
            }
            row("dbrow.build_poh_windchimes") {
                columnRSCM(MODEL_OBJ, "obj.poh_windchimes")
                column(XP, 3230)
                columnRSCM(LOCS, "loc.poh_musical_thing_1")
                columnRSCM(PARTS, "loc.poh_chapel_7", "loc.poh_musical_thing_1")
            }
            row("dbrow.build_poh_bells") {
                columnRSCM(MODEL_OBJ, "obj.poh_bells")
                column(XP, 4800)
                columnRSCM(LOCS, "loc.poh_musical_thing_2")
                columnRSCM(PARTS, "loc.poh_chapel_7", "loc.poh_musical_thing_2")
            }
            row("dbrow.build_poh_organ") {
                columnRSCM(MODEL_OBJ, "obj.poh_organ")
                column(XP, 6800)
                columnRSCM(LOCS, "loc.poh_musical_thing_3")
                columnRSCM(PARTS, "loc.poh_chapel_7", "loc.poh_musical_thing_3")
            }
            row("dbrow.build_poh_statue_saint") {
                columnRSCM(MODEL_OBJ, "obj.poh_statue_saint")
                column(XP, 400)
                columnRSCM(LOCS, "loc.poh_statue_monk")
                columnRSCM(PARTS, "loc.poh_chapel_6", "loc.poh_statue_monk")
            }
            row("dbrow.build_poh_statue_angel") {
                columnRSCM(MODEL_OBJ, "obj.poh_statue_angel")
                column(XP, 5000)
                columnRSCM(LOCS, "loc.poh_statue_angel")
                columnRSCM(PARTS, "loc.poh_chapel_6", "loc.poh_statue_angel")
            }
            row("dbrow.build_poh_statue_god") {
                columnRSCM(MODEL_OBJ, "obj.poh_statue_god")
                column(XP, 15000)
                columnRSCM(LOCS, "loc.poh_statue_saradomin")
                columnRSCM(PARTS, "loc.poh_chapel_6", "loc.poh_statue_saradomin")
            }
            row("dbrow.build_poh_bellpull_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_bellpull_1")
                column(XP, 640)
                columnRSCM(LOCS, "loc.poh_bellpull_1")
                columnRSCM(PARTS, "loc.poh_dining_room_7", "loc.poh_bellpull_1")
            }
            row("dbrow.build_poh_bellpull_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_bellpull_2")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_bellpull_2")
                columnRSCM(PARTS, "loc.poh_dining_room_7", "loc.poh_bellpull_2")
            }
            row("dbrow.build_poh_bellpull_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_bellpull_3")
                column(XP, 4200)
                columnRSCM(LOCS, "loc.poh_bellpull_3")
                columnRSCM(PARTS, "loc.poh_dining_room_7", "loc.poh_bellpull_3")
            }
            row("dbrow.build_poh_wall_crest_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_wall_crest_1")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_decor_oak_arrav")
                columnRSCM(
                    PARTS,
                    "loc.poh_combat_room_5", "loc.poh_decor_oak_arrav", "loc.poh_dining_room_6", "loc.poh_decor_oak_arrav",
                    "loc.poh_throne_room_3_q", "loc.poh_decor_oak_arrav",
                )
            }
            row("dbrow.build_poh_wall_crest_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_wall_crest_2")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_decor_teak_arrav")
                columnRSCM(
                    PARTS,
                    "loc.poh_combat_room_5", "loc.poh_decor_teak_arrav", "loc.poh_dining_room_6", "loc.poh_decor_teak_arrav",
                    "loc.poh_throne_room_3_q", "loc.poh_decor_teak_arrav",
                )
            }
            row("dbrow.build_poh_wall_crest_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_wall_crest_3")
                column(XP, 10200)
                columnRSCM(LOCS, "loc.poh_decor_mahogany_arrav")
                columnRSCM(
                    PARTS,
                    "loc.poh_combat_room_5", "loc.poh_decor_mahogany_arrav", "loc.poh_dining_room_6", "loc.poh_decor_mahogany_arrav",
                    "loc.poh_throne_room_3_q", "loc.poh_decor_mahogany_arrav",
                )
            }
            row("dbrow.build_poh_wall_crest_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_wall_crest_4")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_round_shield_dragon")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_5", "loc.poh_round_shield_dragon", "loc.poh_throne_room_3_q", "loc.poh_round_shield_dragon")
            }
            row("dbrow.build_poh_wall_crest_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_wall_crest_5")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_square_shield_dragon")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_5", "loc.poh_square_shield_dragon", "loc.poh_throne_room_3_q", "loc.poh_square_shield_dragon")
            }
            row("dbrow.build_poh_wall_crest_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_wall_crest_6")
                column(XP, 4200)
                columnRSCM(LOCS, "loc.poh_kite_shield_dragon")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_5", "loc.poh_kite_shield_dragon", "loc.poh_throne_room_3_q", "loc.poh_kite_shield_dragon")
            }
            row("dbrow.build_poh_dining_chairs_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_chairs_1")
                column(XP, 1150)
                columnRSCM(LOCS, "loc.poh_diningchairs_1")
                columnRSCM(PARTS, "loc.poh_dining_room_2", "loc.poh_diningchairs_1", "loc.poh_dining_room_3", "loc.poh_diningchairs_1")
            }
            row("dbrow.build_poh_dining_chairs_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_chairs_2")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_diningchairs_2")
                columnRSCM(PARTS, "loc.poh_dining_room_2", "loc.poh_diningchairs_2", "loc.poh_dining_room_3", "loc.poh_diningchairs_2")
            }
            row("dbrow.build_poh_dining_chairs_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_chairs_3")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_diningchairs_3")
                columnRSCM(PARTS, "loc.poh_dining_room_2", "loc.poh_diningchairs_3", "loc.poh_dining_room_3", "loc.poh_diningchairs_3")
            }
            row("dbrow.build_poh_dining_chairs_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_chairs_4")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_diningchairs_4")
                columnRSCM(PARTS, "loc.poh_dining_room_2", "loc.poh_diningchairs_4", "loc.poh_dining_room_3", "loc.poh_diningchairs_4")
            }
            row("dbrow.build_poh_dining_chairs_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_chairs_5")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_diningchairs_5")
                columnRSCM(
                    PARTS,
                    "loc.poh_dining_room_2", "loc.poh_diningchairs_5", "loc.poh_dining_room_3", "loc.poh_diningchairs_5",
                    "loc.poh_throne_room_5", "loc.poh_throneroom_bench_1", "loc.poh_throne_room_6", "loc.poh_throneroom_bench_1",
                )
            }
            row("dbrow.build_poh_dining_chairs_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_chairs_6")
                column(XP, 5600)
                columnRSCM(LOCS, "loc.poh_diningchairs_6")
                columnRSCM(
                    PARTS,
                    "loc.poh_dining_room_2", "loc.poh_diningchairs_6", "loc.poh_dining_room_3", "loc.poh_diningchairs_6",
                    "loc.poh_throne_room_5", "loc.poh_throneroom_bench_2", "loc.poh_throne_room_6", "loc.poh_throneroom_bench_2",
                )
            }
            row("dbrow.build_poh_dining_chairs_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_chairs_7")
                column(XP, 17600)
                columnRSCM(LOCS, "loc.poh_diningchairs_7")
                columnRSCM(
                    PARTS,
                    "loc.poh_dining_room_2", "loc.poh_diningchairs_7", "loc.poh_dining_room_3", "loc.poh_diningchairs_7",
                    "loc.poh_throne_room_5", "loc.poh_throneroom_bench_3", "loc.poh_throne_room_6", "loc.poh_throneroom_bench_3",
                )
            }
            row("dbrow.build_poh_dining_table_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_table_1")
                column(XP, 1150)
                columnRSCM(LOCS, "loc.poh_diningtable_1")
                columnRSCM(PARTS, "loc.poh_dining_room_1", "loc.poh_diningtable_1")
            }
            row("dbrow.build_poh_dining_table_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_table_2")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_diningtable_2")
                columnRSCM(PARTS, "loc.poh_dining_room_1", "loc.poh_diningtable_2")
            }
            row("dbrow.build_poh_dining_table_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_table_3")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_diningtable_3")
                columnRSCM(PARTS, "loc.poh_dining_room_1", "loc.poh_diningtable_3")
            }
            row("dbrow.build_poh_dining_table_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_table_4")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_diningtable_4")
                columnRSCM(PARTS, "loc.poh_dining_room_1", "loc.poh_diningtable_4")
            }
            row("dbrow.build_poh_dining_table_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_table_5")
                column(XP, 6000)
                columnRSCM(LOCS, "loc.poh_diningtable_5")
                columnRSCM(PARTS, "loc.poh_dining_room_1", "loc.poh_diningtable_5")
            }
            row("dbrow.build_poh_dining_table_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_table_6")
                column(XP, 8400)
                columnRSCM(LOCS, "loc.poh_diningtable_6")
                columnRSCM(PARTS, "loc.poh_dining_room_1", "loc.poh_diningtable_6")
            }
            row("dbrow.build_poh_dining_table_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_dining_table_7")
                column(XP, 31000)
                columnRSCM(LOCS, "loc.poh_diningtable_7")
                columnRSCM(PARTS, "loc.poh_dining_room_1", "loc.poh_diningtable_7")
            }
            row("dbrow.build_poh_dungeon_door_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_door_oak")
                column(XP, 6000)
                columnRSCM(LOCS, "loc.poh_dungeon_ldoor_oak", "loc.poh_dungeon_rdoor_oak")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_4l", "loc.poh_dungeon_ldoor_oak", "loc.poh_dungeon_4r", "loc.poh_dungeon_rdoor_oak",
                    "loc.poh_dungeon_5l", "loc.poh_dungeon_ldoor_oak", "loc.poh_dungeon_5r", "loc.poh_dungeon_rdoor_oak",
                )
            }
            row("dbrow.build_poh_dungeon_door_steel") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_door_steel")
                column(XP, 8000)
                columnRSCM(LOCS, "loc.poh_dungeon_ldoor_steel", "loc.poh_dungeon_rdoor_steel")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_4l", "loc.poh_dungeon_ldoor_steel", "loc.poh_dungeon_4r", "loc.poh_dungeon_rdoor_steel",
                    "loc.poh_dungeon_5l", "loc.poh_dungeon_ldoor_steel", "loc.poh_dungeon_5r", "loc.poh_dungeon_rdoor_steel",
                )
            }
            row("dbrow.build_poh_dungeon_door_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_door_marble")
                column(XP, 20000)
                columnRSCM(LOCS, "loc.poh_dungeon_ldoor_marble", "loc.poh_dungeon_rdoor_marble")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_4l", "loc.poh_dungeon_ldoor_marble", "loc.poh_dungeon_4r", "loc.poh_dungeon_rdoor_marble",
                    "loc.poh_dungeon_5l", "loc.poh_dungeon_ldoor_marble", "loc.poh_dungeon_5r", "loc.poh_dungeon_rdoor_marble",
                )
            }
            row("dbrow.build_poh_dungeon_bloodstain") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_bloodstain")
                column(XP, 40)
                columnRSCM(LOCS, "loc.poh_dungeon_walldecor_blood")
                columnRSCM(PARTS, "loc.poh_dungeon_7", "loc.poh_dungeon_walldecor_blood")
            }
            row("dbrow.build_poh_dungeon_pipe") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_pipe")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_dungeon_walldecor_pipe")
                columnRSCM(PARTS, "loc.poh_dungeon_7", "loc.poh_dungeon_walldecor_pipe")
            }
            row("dbrow.build_poh_dungeon_skeleton_decorative") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_skeleton_decorative")
                column(XP, 30)
                columnRSCM(LOCS, "loc.poh_dungeon_walldecor_skeleton")
                columnRSCM(PARTS, "loc.poh_dungeon_7", "loc.poh_dungeon_walldecor_skeleton")
            }
            row("dbrow.build_poh_dungeon_candle") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_candle")
                column(XP, 2430)
                columnRSCM(LOCS, "loc.poh_dungeon_candle")
                columnRSCM(PARTS, "loc.poh_dungeon_6", "loc.poh_dungeon_candle", "loc.poh_oubliette_4", "loc.poh_dungeon_candle")
            }
            row("dbrow.build_poh_dungeon_torch") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_torch")
                column(XP, 2440)
                columnRSCM(LOCS, "loc.poh_dungeon_torch")
                columnRSCM(PARTS, "loc.poh_dungeon_6", "loc.poh_dungeon_torch", "loc.poh_oubliette_4", "loc.poh_dungeon_torch")
            }
            row("dbrow.build_poh_dungeon_skulltorch") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_skulltorch")
                column(XP, 2460)
                columnRSCM(LOCS, "loc.poh_dungeon_skulltorch")
                columnRSCM(PARTS, "loc.poh_dungeon_6", "loc.poh_dungeon_skulltorch", "loc.poh_oubliette_4", "loc.poh_dungeon_skulltorch")
            }
            row("dbrow.build_poh_skeleton_guard") {
                columnRSCM(MODEL_OBJ, "obj.poh_skeleton_guard")
                column(XP, 2230)
                columnRSCM(LOCS, "loc.poh_skeleton")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_1", "loc.poh_skeleton", "loc.poh_dungeon_stairs_2", "loc.poh_skeleton",
                    "loc.poh_dungeon_stairs_3", "loc.poh_skeleton", "loc.poh_oubliette_3", "loc.poh_skeleton",
                )
            }
            row("dbrow.build_poh_guard_dog") {
                columnRSCM(MODEL_OBJ, "obj.poh_guard_dog")
                column(XP, 2730)
                columnRSCM(LOCS, "loc.poh_guarddog")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_1", "loc.poh_guarddog", "loc.poh_dungeon_stairs_2", "loc.poh_guarddog",
                    "loc.poh_dungeon_stairs_3", "loc.poh_guarddog", "loc.poh_oubliette_3", "loc.poh_guarddog",
                )
            }
            row("dbrow.build_poh_hobgoblin") {
                columnRSCM(MODEL_OBJ, "obj.poh_hobgoblin")
                column(XP, 3160)
                columnRSCM(LOCS, "loc.poh_hobgoblin")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_1", "loc.poh_hobgoblin", "loc.poh_dungeon_stairs_2", "loc.poh_hobgoblin",
                    "loc.poh_dungeon_stairs_3", "loc.poh_hobgoblin", "loc.poh_oubliette_3", "loc.poh_hobgoblin",
                )
            }
            row("dbrow.build_poh_dragon") {
                columnRSCM(MODEL_OBJ, "obj.poh_dragon")
                column(XP, 3870)
                columnRSCM(LOCS, "loc.poh_babyreddragon")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_1", "loc.poh_babyreddragon", "loc.poh_dungeon_stairs_2", "loc.poh_babyreddragon",
                    "loc.poh_dungeon_stairs_3", "loc.poh_babyreddragon", "loc.poh_oubliette_3", "loc.poh_babyreddragon",
                )
            }
            row("dbrow.build_poh_spider") {
                columnRSCM(MODEL_OBJ, "obj.poh_spider")
                column(XP, 4470)
                columnRSCM(LOCS, "loc.poh_giantspider")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_1", "loc.poh_giantspider", "loc.poh_dungeon_stairs_2", "loc.poh_giantspider",
                    "loc.poh_dungeon_stairs_3", "loc.poh_giantspider", "loc.poh_oubliette_3", "loc.poh_giantspider",
                )
            }
            row("dbrow.build_poh_troll") {
                columnRSCM(MODEL_OBJ, "obj.poh_troll")
                column(XP, 10000)
                columnRSCM(LOCS, "loc.poh_troll")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_1", "loc.poh_troll", "loc.poh_dungeon_stairs_2", "loc.poh_troll",
                    "loc.poh_dungeon_stairs_3", "loc.poh_troll", "loc.poh_oubliette_3", "loc.poh_troll",
                )
            }
            row("dbrow.build_poh_hellhound") {
                columnRSCM(MODEL_OBJ, "obj.poh_hellhound")
                column(XP, 22360)
                columnRSCM(LOCS, "loc.poh_hellhound")
                columnRSCM(
                    PARTS,
                    "loc.poh_dungeon_1", "loc.poh_hellhound", "loc.poh_dungeon_stairs_2", "loc.poh_hellhound",
                    "loc.poh_dungeon_stairs_3", "loc.poh_hellhound", "loc.poh_oubliette_3", "loc.poh_hellhound",
                )
            }
            row("dbrow.build_poh_demon") {
                columnRSCM(MODEL_OBJ, "obj.poh_demon")
                column(XP, 7070)
                columnRSCM(LOCS, "loc.poh_demon")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_2", "loc.poh_demon")
            }
            row("dbrow.build_poh_kalphite_soldier") {
                columnRSCM(MODEL_OBJ, "obj.poh_kalphite_soldier")
                column(XP, 8660)
                columnRSCM(LOCS, "loc.poh_kalphite_soldier")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_2", "loc.poh_kalphite_soldier")
            }
            row("dbrow.build_poh_tok_xil") {
                columnRSCM(MODEL_OBJ, "obj.poh_tok_xil")
                column(XP, 22360)
                columnRSCM(LOCS, "loc.poh_tok_xil")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_2", "loc.poh_tok_xil")
            }
            row("dbrow.build_poh_dagganoth") {
                columnRSCM(MODEL_OBJ, "obj.poh_dagganoth")
                column(XP, 27380)
                columnRSCM(LOCS, "loc.poh_dagganoth")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_2", "loc.poh_dagganoth")
            }
            row("dbrow.build_poh_steel_dragon") {
                columnRSCM(MODEL_OBJ, "obj.poh_steel_dragon")
                column(XP, 31620)
                columnRSCM(LOCS, "loc.poh_steel_dragon")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_2", "loc.poh_steel_dragon")
            }
            row("dbrow.build_poh_spike_trap") {
                columnRSCM(MODEL_OBJ, "obj.poh_spike_trap")
                column(XP, 2230)
                columnRSCM(LOCS, "loc.poh_trap_1")
                columnRSCM(PARTS, "loc.poh_dungeon_2", "loc.poh_trap_1", "loc.poh_dungeon_3", "loc.poh_trap_1")
            }
            row("dbrow.build_poh_man_trap") {
                columnRSCM(MODEL_OBJ, "obj.poh_man_trap")
                column(XP, 2730)
                columnRSCM(LOCS, "loc.poh_trap_2")
                columnRSCM(PARTS, "loc.poh_dungeon_2", "loc.poh_trap_2", "loc.poh_dungeon_3", "loc.poh_trap_2")
            }
            row("dbrow.build_poh_vine_trap") {
                columnRSCM(MODEL_OBJ, "obj.poh_vine_trap")
                column(XP, 3160)
                columnRSCM(LOCS, "loc.poh_trap_3")
                columnRSCM(PARTS, "loc.poh_dungeon_2", "loc.poh_trap_3", "loc.poh_dungeon_3", "loc.poh_trap_3")
            }
            row("dbrow.build_poh_marble_trap") {
                columnRSCM(MODEL_OBJ, "obj.poh_marble_trap")
                column(XP, 3870)
                columnRSCM(LOCS, "loc.poh_trap_4")
                columnRSCM(PARTS, "loc.poh_dungeon_2", "loc.poh_trap_4", "loc.poh_dungeon_3", "loc.poh_trap_4")
            }
            row("dbrow.build_poh_teleport_trap") {
                columnRSCM(MODEL_OBJ, "obj.poh_teleport_trap")
                column(XP, 4470)
                columnRSCM(LOCS, "loc.poh_trap_5")
                columnRSCM(PARTS, "loc.poh_dungeon_2", "loc.poh_trap_5", "loc.poh_dungeon_3", "loc.poh_trap_5")
            }
            row("dbrow.build_poh_treasure_wood") {
                columnRSCM(MODEL_OBJ, "obj.poh_treasure_wood")
                column(XP, 1430)
                columnRSCM(LOCS, "loc.poh_treasure_woodencrate")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_1", "loc.poh_treasure_woodencrate")
            }
            row("dbrow.build_poh_treasure_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_treasure_oak")
                column(XP, 3400)
                columnRSCM(LOCS, "loc.poh_treasure_oak_chest")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_1", "loc.poh_treasure_oak_chest")
            }
            row("dbrow.build_poh_treasure_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_treasure_teak")
                column(XP, 5300)
                columnRSCM(LOCS, "loc.poh_treasure_teak_chest")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_1", "loc.poh_treasure_teak_chest")
            }
            row("dbrow.build_poh_treasure_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_treasure_mahogany")
                column(XP, 10000)
                columnRSCM(LOCS, "loc.poh_treasure_mag_chest")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_1", "loc.poh_treasure_mag_chest")
            }
            row("dbrow.build_poh_treasure_magic") {
                columnRSCM(MODEL_OBJ, "obj.poh_treasure_magic")
                column(XP, 10000)
                columnRSCM(LOCS, "loc.poh_treasure_magic_chest")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_1", "loc.poh_treasure_magic_chest")
            }
            row("dbrow.build_poh_attack_stone_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_attack_stone_1")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_tbt_limestone_new")
                columnRSCM(PARTS, "loc.poh_games_room_5", "loc.poh_tbt_limestone_new")
            }
            row("dbrow.build_poh_attack_stone_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_attack_stone_2")
                column(XP, 2000)
                columnRSCM(LOCS, "loc.poh_tbt_clay_new")
                columnRSCM(PARTS, "loc.poh_games_room_5", "loc.poh_tbt_clay_new")
            }
            row("dbrow.build_poh_attack_stone_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_attack_stone_3")
                column(XP, 20000)
                columnRSCM(LOCS, "loc.poh_tbt_marble_new")
                columnRSCM(PARTS, "loc.poh_games_room_5", "loc.poh_tbt_marble_new")
            }
            row("dbrow.build_poh_elemental_balance_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_elemental_balance_1")
                column(XP, 1760)
                columnRSCM(LOCS, "loc.poh_elemental_orb_1")
                columnRSCM(PARTS, "loc.poh_games_room_6", "loc.poh_elemental_orb_1")
            }
            row("dbrow.build_poh_elemental_balance_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_elemental_balance_2")
                column(XP, 2520)
                columnRSCM(LOCS, "loc.poh_elemental_orb_2")
                columnRSCM(PARTS, "loc.poh_games_room_6", "loc.poh_elemental_orb_2")
            }
            row("dbrow.build_poh_elemental_balance_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_elemental_balance_3")
                column(XP, 3560)
                columnRSCM(LOCS, "loc.poh_elemental_orb_3")
                columnRSCM(PARTS, "loc.poh_games_room_6", "loc.poh_elemental_orb_3")
            }
            row("dbrow.build_poh_party_game_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_party_game_1")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_mime_jester")
                columnRSCM(PARTS, "loc.poh_games_room_2", "loc.poh_mime_jester")
            }
            row("dbrow.build_poh_party_game_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_party_game_2")
                column(XP, 8000)
                columnRSCM(LOCS, "loc.poh_fairy_house")
                columnRSCM(PARTS, "loc.poh_games_room_2", "loc.poh_fairy_house")
            }
            row("dbrow.build_poh_party_game_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_party_game_3")
                column(XP, 12000)
                columnRSCM(LOCS, "loc.poh_hangman_chest")
                columnRSCM(PARTS, "loc.poh_games_room_2", "loc.poh_hangman_chest")
            }
            row("dbrow.build_poh_ranging_game_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_ranging_game_1")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_stick+hoop1")
                columnRSCM(PARTS, "loc.poh_games_room_7", "loc.poh_stick+hoop1")
            }
            row("dbrow.build_poh_ranging_game_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_ranging_game_2")
                column(XP, 2900)
                columnRSCM(LOCS, "loc.poh_dartboard1")
                columnRSCM(PARTS, "loc.poh_games_room_7", "loc.poh_dartboard1")
            }
            row("dbrow.build_poh_ranging_game_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_ranging_game_3")
                column(XP, 6000)
                columnRSCM(LOCS, "loc.poh_archery_target1")
                columnRSCM(PARTS, "loc.poh_games_room_7", "loc.poh_archery_target1")
            }
            row("dbrow.build_poh_prize_chest_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_prize_chest_1")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_prize_chest_oak_closed")
                columnRSCM(PARTS, "loc.poh_games_room_4", "loc.poh_prize_chest_oak_closed")
            }
            row("dbrow.build_poh_prize_chest_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_prize_chest_2")
                column(XP, 6600)
                columnRSCM(LOCS, "loc.poh_prize_chest_teak_closed")
                columnRSCM(PARTS, "loc.poh_games_room_4", "loc.poh_prize_chest_teak_closed")
            }
            row("dbrow.build_poh_prize_chest_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_prize_chest_3")
                column(XP, 8600)
                columnRSCM(LOCS, "loc.poh_prize_chest_mag_closed")
                columnRSCM(PARTS, "loc.poh_games_room_4", "loc.poh_prize_chest_mag_closed")
            }
            row("dbrow.build_poh_garden_centrepiece_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_garden_centrepiece_1")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_exit_portal")
                columnRSCM(PARTS, "loc.poh_crude_garden_1", "loc.poh_exit_portal", "loc.poh_posh_garden_1", "loc.poh_exit_portal")
            }
            row("dbrow.build_poh_garden_centrepiece_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_garden_centrepiece_2")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_crude_garden_centrepiece2")
                columnRSCM(PARTS, "loc.poh_crude_garden_1", "loc.poh_crude_garden_centrepiece2")
            }
            row("dbrow.build_poh_garden_centrepiece_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_garden_centrepiece_3")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_crude_garden_centrepiece3")
                columnRSCM(PARTS, "loc.poh_crude_garden_1", "loc.poh_crude_garden_centrepiece3")
            }
            row("dbrow.build_poh_garden_centrepiece_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_garden_centrepiece_4")
                column(XP, 1500)
                columnRSCM(LOCS, "loc.poh_crude_garden_centrepiece4")
                columnRSCM(PARTS, "loc.poh_crude_garden_1", "loc.poh_crude_garden_centrepiece4")
            }
            row("dbrow.build_poh_garden_centrepiece_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_garden_centrepiece_5")
                column(XP, 5000)
                columnRSCM(LOCS, "loc.poh_crude_garden_centrepiece5")
                columnRSCM(PARTS, "loc.poh_crude_garden_1", "loc.poh_crude_garden_centrepiece5", "loc.poh_posh_garden_1", "loc.poh_crude_garden_centrepiece5")
            }
            row("dbrow.build_poh_tree_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_tree_1")
                column(XP, 310)
                columnRSCM(LOCS, "loc.poh_big_tree1_4")
                columnRSCM(PARTS, "loc.poh_crude_garden_2", "loc.poh_big_tree1_4", "loc.poh_crude_garden_3", "loc.poh_small_tree1_5")
            }
            row("dbrow.build_poh_tree_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_tree_2")
                column(XP, 440)
                columnRSCM(LOCS, "loc.poh_big_tree2_4")
                columnRSCM(PARTS, "loc.poh_crude_garden_2", "loc.poh_big_tree2_4", "loc.poh_crude_garden_3", "loc.poh_small_tree2_5")
            }
            row("dbrow.build_poh_tree_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_tree_3")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_big_tree3_4")
                columnRSCM(PARTS, "loc.poh_crude_garden_2", "loc.poh_big_tree3_4", "loc.poh_crude_garden_3", "loc.poh_small_tree3_5")
            }
            row("dbrow.build_poh_tree_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_tree_4")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_big_tree4_4")
                columnRSCM(PARTS, "loc.poh_crude_garden_2", "loc.poh_big_tree4_4", "loc.poh_crude_garden_3", "loc.poh_small_tree4_5")
            }
            row("dbrow.build_poh_tree_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_tree_5")
                column(XP, 1220)
                columnRSCM(LOCS, "loc.poh_big_tree5_4")
                columnRSCM(PARTS, "loc.poh_crude_garden_2", "loc.poh_big_tree5_4", "loc.poh_crude_garden_3", "loc.poh_small_tree5_5")
            }
            row("dbrow.build_poh_tree_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_tree_6")
                column(XP, 1410)
                columnRSCM(LOCS, "loc.poh_big_tree6_4")
                columnRSCM(PARTS, "loc.poh_crude_garden_2", "loc.poh_big_tree6_4", "loc.poh_crude_garden_3", "loc.poh_small_tree6_5")
            }
            row("dbrow.build_poh_tree_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_tree_7")
                column(XP, 2230)
                columnRSCM(LOCS, "loc.poh_big_tree7_4")
                columnRSCM(PARTS, "loc.poh_crude_garden_2", "loc.poh_big_tree7_4", "loc.poh_crude_garden_3", "loc.poh_small_tree7_5")
            }
            row("dbrow.build_poh_plantsmall1a") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantsmall1a")
                column(XP, 310)
                columnRSCM(LOCS, "loc.poh_plantbsmall1a")
                columnRSCM(PARTS, "loc.poh_crude_garden_6", "loc.poh_plantbsmall1a")
            }
            row("dbrow.build_poh_plantsmall1b") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantsmall1b")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_plantbsmall1b")
                columnRSCM(PARTS, "loc.poh_crude_garden_6", "loc.poh_plantbsmall1b")
            }
            row("dbrow.build_poh_plantsmall1c") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantsmall1c")
                column(XP, 310)
                columnRSCM(LOCS, "loc.poh_plantbsmall1c")
                columnRSCM(PARTS, "loc.poh_crude_garden_4", "loc.poh_plantbsmall1c", "loc.poh_crude_garden_6", "loc.poh_plantbsmall1c")
            }
            row("dbrow.build_poh_plantsmall2a") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantsmall2a")
                column(XP, 310)
                columnRSCM(LOCS, "loc.poh_plantbsmall2a")
                columnRSCM(PARTS, "loc.poh_crude_garden_7", "loc.poh_plantbsmall2a")
            }
            row("dbrow.build_poh_plantsmall2b") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantsmall2b")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_plantbsmall2b")
                columnRSCM(PARTS, "loc.poh_crude_garden_7", "loc.poh_plantbsmall2b")
            }
            row("dbrow.build_poh_plantsmall2c") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantsmall2c")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_plantbsmall2c")
                columnRSCM(PARTS, "loc.poh_crude_garden_7", "loc.poh_plantbsmall2c")
            }
            row("dbrow.build_poh_plantbig1b") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantbig1b")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_plantbig1b")
                columnRSCM(PARTS, "loc.poh_crude_garden_4", "loc.poh_plantbig1b")
            }
            row("dbrow.build_poh_plantbig1c") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantbig1c")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_plantbig1c")
                columnRSCM(PARTS, "loc.poh_crude_garden_4", "loc.poh_plantbig1c")
            }
            row("dbrow.build_poh_plantbig2a") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantbig2a")
                column(XP, 310)
                columnRSCM(LOCS, "loc.poh_plantbig2a")
                columnRSCM(PARTS, "loc.poh_crude_garden_5", "loc.poh_plantbig2a")
            }
            row("dbrow.build_poh_plantbig2b") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantbig2b")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_plantbig2b")
                columnRSCM(PARTS, "loc.poh_crude_garden_5", "loc.poh_plantbig2b")
            }
            row("dbrow.build_poh_plantbig2c") {
                columnRSCM(MODEL_OBJ, "obj.poh_plantbig2c")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_plantbig2c")
                columnRSCM(PARTS, "loc.poh_crude_garden_5", "loc.poh_plantbig2c")
            }
            row("dbrow.build_poh_formal_garden_centrepiece_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_formal_garden_centrepiece_2")
                column(XP, 12000)
                columnRSCM(LOCS, "loc.poh_posh_garden_centrepiece2")
                columnRSCM(PARTS, "loc.poh_posh_garden_1", "loc.poh_posh_garden_centrepiece2")
            }
            row("dbrow.build_poh_formal_garden_centrepiece_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_formal_garden_centrepiece_3")
                column(XP, 5000)
                columnRSCM(LOCS, "loc.poh_posh_garden_centrepiece3")
                columnRSCM(PARTS, "loc.poh_posh_garden_1", "loc.poh_posh_garden_centrepiece3")
            }
            row("dbrow.build_poh_formal_garden_centrepiece_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_formal_garden_centrepiece_4")
                column(XP, 10000)
                columnRSCM(LOCS, "loc.poh_posh_garden_centrepiece4")
                columnRSCM(PARTS, "loc.poh_posh_garden_1", "loc.poh_posh_garden_centrepiece4")
            }
            row("dbrow.build_poh_formal_garden_centrepiece_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_formal_garden_centrepiece_5")
                column(XP, 15000)
                columnRSCM(LOCS, "loc.poh_posh_garden_centrepiece5")
                columnRSCM(PARTS, "loc.poh_posh_garden_1", "loc.poh_posh_garden_centrepiece5")
            }
            row("dbrow.build_poh_fencing1") {
                columnRSCM(MODEL_OBJ, "obj.poh_fencing1")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_fencing1")
                columnRSCM(PARTS, "loc.poh_posh_garden_4", "loc.poh_fencing1")
            }
            row("dbrow.build_poh_fencing2") {
                columnRSCM(MODEL_OBJ, "obj.poh_fencing2")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_fencing2")
                columnRSCM(PARTS, "loc.poh_posh_garden_4", "loc.poh_fencing2")
            }
            row("dbrow.build_poh_fencing3") {
                columnRSCM(MODEL_OBJ, "obj.poh_fencing3")
                column(XP, 2000)
                columnRSCM(LOCS, "loc.poh_fencing3")
                columnRSCM(PARTS, "loc.poh_posh_garden_4", "loc.poh_fencing3")
            }
            row("dbrow.build_poh_fencing4") {
                columnRSCM(MODEL_OBJ, "obj.poh_fencing4")
                column(XP, 2200)
                columnRSCM(LOCS, "loc.poh_fencing4")
                columnRSCM(PARTS, "loc.poh_posh_garden_4", "loc.poh_fencing4")
            }
            row("dbrow.build_poh_fencing5") {
                columnRSCM(MODEL_OBJ, "obj.poh_fencing5")
                column(XP, 6400)
                columnRSCM(LOCS, "loc.poh_fencing5")
                columnRSCM(PARTS, "loc.poh_posh_garden_4", "loc.poh_fencing5")
            }
            row("dbrow.build_poh_fencing6") {
                columnRSCM(MODEL_OBJ, "obj.poh_fencing6")
                column(XP, 9400)
                columnRSCM(LOCS, "loc.poh_fencing6")
                columnRSCM(PARTS, "loc.poh_posh_garden_4", "loc.poh_fencing6")
            }
            row("dbrow.build_poh_fencing7") {
                columnRSCM(MODEL_OBJ, "obj.poh_fencing7")
                column(XP, 40000)
                columnRSCM(LOCS, "loc.poh_fencing7")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_4", "loc.poh_fencing7", "loc.poh_superior_garden_hotspot_fence_middle", "loc.poh_fencing7",
                    "loc.poh_superior_garden_hotspot_fence_post", "loc.poh_fencing7", "loc.poh_superior_garden_hotspot_fence_post_m", "loc.poh_fencing7",
                )
            }
            row("dbrow.build_poh_hedge1") {
                columnRSCM(MODEL_OBJ, "obj.poh_hedge1")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_hedgeend1", "loc.poh_hedgecorner1", "loc.poh_hedgemiddle1")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_5end", "loc.poh_hedgemiddle1", "loc.poh_posh_garden_5mid", "loc.poh_hedgeend1",
                    "loc.poh_posh_garden_5cor", "loc.poh_hedgecorner1",
                )
            }
            row("dbrow.build_poh_hedge2") {
                columnRSCM(MODEL_OBJ, "obj.poh_hedge2")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_hedgeend2", "loc.poh_hedgecorner2", "loc.poh_hedgemiddle2")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_5end", "loc.poh_hedgemiddle2", "loc.poh_posh_garden_5mid", "loc.poh_hedgeend2",
                    "loc.poh_posh_garden_5cor", "loc.poh_hedgecorner2",
                )
            }
            row("dbrow.build_poh_hedge3") {
                columnRSCM(MODEL_OBJ, "obj.poh_hedge3")
                column(XP, 1220)
                columnRSCM(LOCS, "loc.poh_hedgeend3", "loc.poh_hedgecorner3", "loc.poh_hedgemiddle3")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_5end", "loc.poh_hedgemiddle3", "loc.poh_posh_garden_5mid", "loc.poh_hedgeend3",
                    "loc.poh_posh_garden_5cor", "loc.poh_hedgecorner3",
                )
            }
            row("dbrow.build_poh_hedge4") {
                columnRSCM(MODEL_OBJ, "obj.poh_hedge4")
                column(XP, 1410)
                columnRSCM(LOCS, "loc.poh_hedgeend4", "loc.poh_hedgecorner4", "loc.poh_hedgemiddle4")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_5end", "loc.poh_hedgemiddle4", "loc.poh_posh_garden_5mid", "loc.poh_hedgeend4",
                    "loc.poh_posh_garden_5cor", "loc.poh_hedgecorner4",
                )
            }
            row("dbrow.build_poh_hedge5") {
                columnRSCM(MODEL_OBJ, "obj.poh_hedge5")
                column(XP, 1580)
                columnRSCM(LOCS, "loc.poh_hedgeend5", "loc.poh_hedgecorner5", "loc.poh_hedgemiddle5")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_5end", "loc.poh_hedgemiddle5", "loc.poh_posh_garden_5mid", "loc.poh_hedgeend5",
                    "loc.poh_posh_garden_5cor", "loc.poh_hedgecorner5",
                )
            }
            row("dbrow.build_poh_hedge6") {
                columnRSCM(MODEL_OBJ, "obj.poh_hedge6")
                column(XP, 2230)
                columnRSCM(LOCS, "loc.poh_hedgeend6", "loc.poh_hedgecorner6", "loc.poh_hedgemiddle6")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_5end", "loc.poh_hedgemiddle6", "loc.poh_posh_garden_5mid", "loc.poh_hedgeend6",
                    "loc.poh_posh_garden_5cor", "loc.poh_hedgecorner6",
                )
            }
            row("dbrow.build_poh_hedge7") {
                columnRSCM(MODEL_OBJ, "obj.poh_hedge7")
                column(XP, 3160)
                columnRSCM(LOCS, "loc.poh_hedgeend7", "loc.poh_hedgecorner7", "loc.poh_hedgemiddle7")
                columnRSCM(
                    PARTS,
                    "loc.poh_posh_garden_5end", "loc.poh_hedgemiddle7", "loc.poh_posh_garden_5mid", "loc.poh_hedgeend7",
                    "loc.poh_posh_garden_5cor", "loc.poh_hedgecorner7",
                )
            }
            row("dbrow.build_poh_flowerb1") {
                columnRSCM(MODEL_OBJ, "obj.poh_flowerb1")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_flowerb1_big")
                columnRSCM(PARTS, "loc.poh_posh_garden_3", "loc.poh_flowerb1_big", "loc.poh_posh_garden_7", "loc.poh_flowerb1_small")
            }
            row("dbrow.build_poh_flowerb2") {
                columnRSCM(MODEL_OBJ, "obj.poh_flowerb2")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_flowerb2_big")
                columnRSCM(PARTS, "loc.poh_posh_garden_3", "loc.poh_flowerb2_big", "loc.poh_posh_garden_7", "loc.poh_flowerb2_small")
            }
            row("dbrow.build_poh_flowerb3") {
                columnRSCM(MODEL_OBJ, "obj.poh_flowerb3")
                column(XP, 1220)
                columnRSCM(LOCS, "loc.poh_flowerb3_big")
                columnRSCM(PARTS, "loc.poh_posh_garden_3", "loc.poh_flowerb3_big", "loc.poh_posh_garden_7", "loc.poh_flowerb3_small")
            }
            row("dbrow.build_poh_flowera1") {
                columnRSCM(MODEL_OBJ, "obj.poh_flowera1")
                column(XP, 700)
                columnRSCM(LOCS, "loc.poh_flowera1_big")
                columnRSCM(PARTS, "loc.poh_posh_garden_2", "loc.poh_flowera1_big", "loc.poh_posh_garden_6", "loc.poh_flowera1_small")
            }
            row("dbrow.build_poh_flowera2") {
                columnRSCM(MODEL_OBJ, "obj.poh_flowera2")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_flowera2_big")
                columnRSCM(PARTS, "loc.poh_posh_garden_2", "loc.poh_flowera2_big", "loc.poh_posh_garden_6", "loc.poh_flowera2_small")
            }
            row("dbrow.build_poh_flowera3") {
                columnRSCM(MODEL_OBJ, "obj.poh_flowera3")
                column(XP, 1220)
                columnRSCM(LOCS, "loc.poh_flowera3_big")
                columnRSCM(PARTS, "loc.poh_posh_garden_2", "loc.poh_flowera3_big", "loc.poh_posh_garden_6", "loc.poh_flowera3_small")
            }
            row("dbrow.build_poh_stove_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_stove_1")
                column(XP, 400)
                columnRSCM(LOCS, "loc.poh_stove_1")
                columnRSCM(PARTS, "loc.poh_kitchen_1", "loc.poh_stove_1")
            }
            row("dbrow.build_poh_stove_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_stove_2")
                column(XP, 600)
                columnRSCM(LOCS, "loc.poh_stove_2")
                columnRSCM(PARTS, "loc.poh_kitchen_1", "loc.poh_stove_2")
            }
            row("dbrow.build_poh_stove_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_stove_3")
                column(XP, 800)
                columnRSCM(LOCS, "loc.poh_stove_3")
                columnRSCM(PARTS, "loc.poh_kitchen_1", "loc.poh_stove_3")
            }
            row("dbrow.build_poh_stove_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_stove_4")
                column(XP, 800)
                columnRSCM(LOCS, "loc.poh_stove_4")
                columnRSCM(PARTS, "loc.poh_kitchen_1", "loc.poh_stove_4")
            }
            row("dbrow.build_poh_stove_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_stove_5")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_stove_5")
                columnRSCM(PARTS, "loc.poh_kitchen_1", "loc.poh_stove_5")
            }
            row("dbrow.build_poh_stove_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_stove_6")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_stove_6")
                columnRSCM(PARTS, "loc.poh_kitchen_1", "loc.poh_stove_6")
            }
            row("dbrow.build_poh_stove_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_stove_7")
                column(XP, 1600)
                columnRSCM(LOCS, "loc.poh_stove_7")
                columnRSCM(PARTS, "loc.poh_kitchen_1", "loc.poh_stove_7")
            }
            row("dbrow.build_poh_kitchen_shelves_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchen_shelves_1")
                column(XP, 870)
                columnRSCM(LOCS, "loc.poh_kitchen_shelves_1", "loc.poh_kitchen_crockery_1")
                columnRSCM(PARTS, "loc.poh_kitchen_2", "loc.poh_kitchen_shelves_1", "loc.poh_kitchen_2_crockery", "loc.poh_kitchen_crockery_1")
            }
            row("dbrow.build_poh_kitchen_shelves_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchen_shelves_2")
                column(XP, 1470)
                columnRSCM(LOCS, "loc.poh_kitchen_shelves_2", "loc.poh_kitchen_crockery_2")
                columnRSCM(PARTS, "loc.poh_kitchen_2", "loc.poh_kitchen_shelves_2", "loc.poh_kitchen_2_crockery", "loc.poh_kitchen_crockery_2")
            }
            row("dbrow.build_poh_kitchen_shelves_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchen_shelves_3")
                column(XP, 1470)
                columnRSCM(LOCS, "loc.poh_kitchen_shelves_3", "loc.poh_kitchen_crockery_3")
                columnRSCM(PARTS, "loc.poh_kitchen_2", "loc.poh_kitchen_shelves_3", "loc.poh_kitchen_2_crockery", "loc.poh_kitchen_crockery_3")
            }
            row("dbrow.build_poh_kitchen_shelves_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchen_shelves_4")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_kitchen_shelves_4", "loc.poh_kitchen_crockery_4")
                columnRSCM(PARTS, "loc.poh_kitchen_2", "loc.poh_kitchen_shelves_4", "loc.poh_kitchen_2_crockery", "loc.poh_kitchen_crockery_4")
            }
            row("dbrow.build_poh_kitchen_shelves_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchen_shelves_5")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_kitchen_shelves_5", "loc.poh_kitchen_crockery_5")
                columnRSCM(PARTS, "loc.poh_kitchen_2", "loc.poh_kitchen_shelves_5", "loc.poh_kitchen_2_crockery", "loc.poh_kitchen_crockery_5")
            }
            row("dbrow.build_poh_kitchen_shelves_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchen_shelves_6")
                column(XP, 3300)
                columnRSCM(LOCS, "loc.poh_kitchen_shelves_6", "loc.poh_kitchen_crockery_6")
                columnRSCM(PARTS, "loc.poh_kitchen_2", "loc.poh_kitchen_shelves_6", "loc.poh_kitchen_2_crockery", "loc.poh_kitchen_crockery_6")
            }
            row("dbrow.build_poh_kitchen_shelves_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchen_shelves_7")
                column(XP, 9300)
                columnRSCM(LOCS, "loc.poh_kitchen_shelves_7", "loc.poh_kitchen_crockery_7")
                columnRSCM(PARTS, "loc.poh_kitchen_2", "loc.poh_kitchen_shelves_7", "loc.poh_kitchen_2_crockery", "loc.poh_kitchen_crockery_7")
            }
            row("dbrow.build_poh_sink_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_sink_1")
                column(XP, 1000)
                columnRSCM(LOCS, "loc.poh_sink_1")
                columnRSCM(PARTS, "loc.poh_kitchen_6", "loc.poh_sink_1")
            }
            row("dbrow.build_poh_sink_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_sink_2")
                column(XP, 2000)
                columnRSCM(LOCS, "loc.poh_sink_2")
                columnRSCM(PARTS, "loc.poh_kitchen_6", "loc.poh_sink_2")
            }
            row("dbrow.build_poh_sink_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_sink_3")
                column(XP, 3000)
                columnRSCM(LOCS, "loc.poh_sink_3")
                columnRSCM(PARTS, "loc.poh_kitchen_6", "loc.poh_sink_3")
            }
            row("dbrow.build_poh_larder_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_larder_1")
                column(XP, 2280)
                columnRSCM(LOCS, "loc.poh_larder_1")
                columnRSCM(PARTS, "loc.poh_kitchen_5", "loc.poh_larder_1")
            }
            row("dbrow.build_poh_larder_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_larder_2")
                column(XP, 4800)
                columnRSCM(LOCS, "loc.poh_larder_2")
                columnRSCM(PARTS, "loc.poh_kitchen_5", "loc.poh_larder_2")
            }
            row("dbrow.build_poh_larder_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_larder_3")
                column(XP, 7500)
                columnRSCM(LOCS, "loc.poh_larder_3")
                columnRSCM(PARTS, "loc.poh_kitchen_5", "loc.poh_larder_3")
            }
            row("dbrow.build_poh_cat_basket_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_cat_basket_1")
                column(XP, 150)
                columnRSCM(LOCS, "loc.poh_pet_1")
                columnRSCM(PARTS, "loc.poh_kitchen_4", "loc.poh_pet_1")
            }
            row("dbrow.build_poh_cat_basket_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_cat_basket_2")
                column(XP, 580)
                columnRSCM(LOCS, "loc.poh_pet_2")
                columnRSCM(PARTS, "loc.poh_kitchen_4", "loc.poh_pet_2")
            }
            row("dbrow.build_poh_cat_basket_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_cat_basket_3")
                column(XP, 580)
                columnRSCM(LOCS, "loc.poh_pet_3")
                columnRSCM(PARTS, "loc.poh_kitchen_4", "loc.poh_pet_3")
            }
            row("dbrow.build_poh_barrel_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_barrel_1")
                column(XP, 870)
                columnRSCM(LOCS, "loc.poh_barrel_1")
                columnRSCM(PARTS, "loc.poh_kitchen_3", "loc.poh_barrel_1")
            }
            row("dbrow.build_poh_barrel_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_barrel_2")
                column(XP, 910)
                columnRSCM(LOCS, "loc.poh_barrel_2")
                columnRSCM(PARTS, "loc.poh_kitchen_3", "loc.poh_barrel_2")
            }
            row("dbrow.build_poh_barrel_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_barrel_3")
                column(XP, 1840)
                columnRSCM(LOCS, "loc.poh_barrel_3")
                columnRSCM(PARTS, "loc.poh_kitchen_3", "loc.poh_barrel_3")
            }
            row("dbrow.build_poh_barrel_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_barrel_4")
                column(XP, 1840)
                columnRSCM(LOCS, "loc.poh_barrel_4")
                columnRSCM(PARTS, "loc.poh_kitchen_3", "loc.poh_barrel_4")
            }
            row("dbrow.build_poh_barrel_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_barrel_5")
                column(XP, 2240)
                columnRSCM(LOCS, "loc.poh_barrel_5")
                columnRSCM(PARTS, "loc.poh_kitchen_3", "loc.poh_barrel_5")
            }
            row("dbrow.build_poh_barrel_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_barrel_6")
                column(XP, 2240)
                columnRSCM(LOCS, "loc.poh_barrel_6")
                columnRSCM(PARTS, "loc.poh_kitchen_3", "loc.poh_barrel_6")
            }
            row("dbrow.build_poh_kitchentable_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchentable_1")
                column(XP, 870)
                columnRSCM(LOCS, "loc.poh_kitchentable_1")
                columnRSCM(PARTS, "loc.poh_kitchen_7", "loc.poh_kitchentable_1")
            }
            row("dbrow.build_poh_kitchentable_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchentable_2")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_kitchentable_2")
                columnRSCM(PARTS, "loc.poh_kitchen_7", "loc.poh_kitchentable_2")
            }
            row("dbrow.build_poh_kitchentable_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_kitchentable_3")
                column(XP, 2700)
                columnRSCM(LOCS, "loc.poh_kitchentable_3")
                columnRSCM(PARTS, "loc.poh_kitchen_7", "loc.poh_kitchentable_3")
            }
            row("dbrow.build_poh_stairs_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_stairs_oak")
                column(XP, 6800)
                columnRSCM(LOCS, "loc.poh_stairs_3")
                columnRSCM(
                    PARTS,
                    "loc.poh_hall1_1_stairs_up", "loc.poh_stairs_3", "loc.poh_hall1_1_stairs_top", "loc.poh_stairstop_3",
                    "loc.poh_hall2_1_stairs_up", "loc.poh_stairs_3", "loc.poh_hall2_1_stairs_top", "loc.poh_stairstop_3",
                )
            }
            row("dbrow.build_poh_stairs_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_stairs_teak")
                column(XP, 9800)
                columnRSCM(LOCS, "loc.poh_stairs_4")
                columnRSCM(
                    PARTS,
                    "loc.poh_hall1_1_stairs_up", "loc.poh_stairs_4", "loc.poh_hall1_1_stairs_top", "loc.poh_stairstop_4",
                    "loc.poh_hall2_1_stairs_up", "loc.poh_stairs_4", "loc.poh_hall2_1_stairs_top", "loc.poh_stairstop_4",
                )
            }
            row("dbrow.build_poh_stairs_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_stairs_marble")
                column(XP, 32000)
                columnRSCM(LOCS, "loc.poh_stairs_5")
                columnRSCM(
                    PARTS,
                    "loc.poh_hall1_1_stairs_up", "loc.poh_stairs_5", "loc.poh_hall1_1_stairs_top", "loc.poh_stairstop_5",
                    "loc.poh_hall2_1_stairs_up", "loc.poh_stairs_5", "loc.poh_hall2_1_stairs_top", "loc.poh_stairstop_5",
                )
            }
            row("dbrow.build_poh_spiralstairs_limestone") {
                columnRSCM(MODEL_OBJ, "obj.poh_spiralstairs_limestone")
                column(XP, 10400)
                columnRSCM(LOCS, "loc.poh_spiralstairs")
                columnRSCM(
                    PARTS,
                    "loc.poh_hall1_1_stairs_up", "loc.poh_spiralstairs", "loc.poh_hall1_1_stairs_top", "loc.poh_spiralstairs",
                    "loc.poh_hall2_1_stairs_up", "loc.poh_spiralstairs", "loc.poh_hall2_1_stairs_top", "loc.poh_spiralstairs",
                )
            }
            row("dbrow.build_poh_spiralstairs_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_spiralstairs_marble")
                column(XP, 44000)
                columnRSCM(LOCS, "loc.poh_spiralstairs_2")
                columnRSCM(
                    PARTS,
                    "loc.poh_hall1_1_stairs_up", "loc.poh_spiralstairs_2", "loc.poh_hall1_1_stairs_top", "loc.poh_spiralstairs_2",
                    "loc.poh_hall2_1_stairs_up", "loc.poh_spiralstairs_2", "loc.poh_hall2_1_stairs_top", "loc.poh_spiralstairs_2",
                )
            }
            row("dbrow.build_poh_trophy_crawlinghand") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_crawlinghand")
                column(XP, 2110)
                columnRSCM(LOCS, "loc.100_cutscene_chair_gnome")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.100_cutscene_chair_gnome")
            }
            row("dbrow.build_poh_trophy_cockatrice") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_cockatrice")
                column(XP, 2240)
                columnRSCM(LOCS, "loc.raids_tekton_book_vis")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.raids_tekton_book_vis")
            }
            row("dbrow.build_poh_trophy_basilisk") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_basilisk")
                column(XP, 2430)
                columnRSCM(LOCS, "loc.raids_vespula_book_vis")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.raids_vespula_book_vis")
            }
            row("dbrow.build_poh_trophy_kurask") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_kurask")
                column(XP, 3570)
                columnRSCM(LOCS, "loc.raids_vanguard_book_vis")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.raids_vanguard_book_vis")
            }
            row("dbrow.build_poh_trophy_abyssal") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_abyssal")
                column(XP, 3890)
                columnRSCM(LOCS, "loc.raids_houndmaster_book_vis")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.raids_houndmaster_book_vis")
            }
            row("dbrow.build_poh_trophy_kbd") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_kbd")
                column(XP, 11030)
                columnRSCM(LOCS, "loc.transportation_icon_aip")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.transportation_icon_aip")
            }
            row("dbrow.build_poh_trophy_kalphitequeen") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_kalphitequeen")
                column(XP, 11030)
                columnRSCM(LOCS, "loc.transportation_icon_ais")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.transportation_icon_ais")
            }
            row("dbrow.build_poh_trophy_bass") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_bass")
                column(XP, 1510)
                columnRSCM(LOCS, "loc.transportation_icon_air")
                columnRSCM(PARTS, "loc.poh_hall1_4", "loc.transportation_icon_air")
            }
            row("dbrow.build_poh_trophy_swordfish") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_swordfish")
                column(XP, 1510)
                columnRSCM(LOCS, "loc.transportation_icon_aiq")
                columnRSCM(PARTS, "loc.poh_hall1_4", "loc.transportation_icon_aiq")
            }
            row("dbrow.build_poh_trophy_shark") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_shark")
                column(XP, 1510)
                columnRSCM(LOCS, "loc.transportation_icon_alp")
                columnRSCM(PARTS, "loc.poh_hall1_4", "loc.transportation_icon_alp")
            }
            row("dbrow.build_poh_trophy_armour_mithril") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_armour_mithril")
                column(XP, 1350)
                columnRSCM(LOCS, "loc.poh_armour_mithril_5")
                columnRSCM(PARTS, "loc.poh_hall1_5", "loc.poh_armour_mithril_5")
            }
            row("dbrow.build_poh_trophy_armour_adamant") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_armour_adamant")
                column(XP, 1500)
                columnRSCM(LOCS, "loc.poh_armour_adamant_5")
                columnRSCM(PARTS, "loc.poh_hall1_5", "loc.poh_armour_adamant_5")
            }
            row("dbrow.build_poh_trophy_armour_rune") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_armour_rune")
                column(XP, 1650)
                columnRSCM(LOCS, "loc.poh_armour_rune_5")
                columnRSCM(PARTS, "loc.poh_hall1_5", "loc.poh_armour_rune_5")
            }
            row("dbrow.build_poh_trophy_castlewars_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_castlewars_1")
                column(XP, 1350)
                columnRSCM(LOCS, "loc.poh_armour_castlewars_red_6")
                columnRSCM(PARTS, "loc.poh_hall1_6", "loc.poh_armour_castlewars_red_6")
            }
            row("dbrow.build_poh_trophy_castlewars_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_castlewars_2")
                column(XP, 1500)
                columnRSCM(LOCS, "loc.poh_armour_castlewars_white_6")
                columnRSCM(PARTS, "loc.poh_hall1_6", "loc.poh_armour_castlewars_white_6")
            }
            row("dbrow.build_poh_trophy_castlewars_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_castlewars_3")
                column(XP, 1650)
                columnRSCM(LOCS, "loc.poh_armour_castlewars_gold_6")
                columnRSCM(PARTS, "loc.poh_hall1_6", "loc.poh_armour_castlewars_gold_6")
            }
            row("dbrow.build_poh_trophy_runecrafting_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_runecrafting_1")
                column(XP, 1900)
                columnRSCM(LOCS, "loc.poh_display_case_rune1_6")
                columnRSCM(PARTS, "loc.poh_hall1_7", "loc.poh_display_case_rune1_6")
            }
            row("dbrow.build_poh_trophy_runecrafting_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_runecrafting_2")
                column(XP, 2120)
                columnRSCM(LOCS, "loc.poh_display_case_rune2_6")
                columnRSCM(PARTS, "loc.poh_hall1_7", "loc.poh_display_case_rune2_6")
            }
            row("dbrow.build_poh_trophy_runecrafting_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_runecrafting_3")
                column(XP, 2470)
                columnRSCM(LOCS, "loc.poh_display_case_rune3_6")
                columnRSCM(PARTS, "loc.poh_hall1_7", "loc.poh_display_case_rune3_6")
            }
            row("dbrow.build_poh_trophy_silverlight") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_silverlight")
                column(XP, 1870)
                columnRSCM(LOCS, "loc.poh_trophy_silverlight_5")
                columnRSCM(PARTS, "loc.poh_hall2_5", "loc.poh_trophy_silverlight_5")
            }
            row("dbrow.build_poh_trophy_excalibur") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_excalibur")
                column(XP, 1940)
                columnRSCM(LOCS, "loc.poh_trophy_excalibur_5")
                columnRSCM(PARTS, "loc.poh_hall2_5", "loc.poh_trophy_excalibur_5")
            }
            row("dbrow.build_poh_trophy_darklight") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_darklight")
                column(XP, 2020)
                columnRSCM(LOCS, "loc.poh_trophy_darklight_5")
                columnRSCM(PARTS, "loc.poh_hall2_5", "loc.poh_trophy_darklight_5")
            }
            row("dbrow.build_poh_trophy_antidragonbreath") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_antidragonbreath")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_trophy_antidragonbreath_4")
                columnRSCM(PARTS, "loc.poh_hall2_4", "loc.poh_trophy_antidragonbreath_4")
            }
            row("dbrow.build_poh_trophy_amuletofglory") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_amuletofglory")
                column(XP, 2900)
                columnRSCM(LOCS, "loc.poh_trophy_amuletofglory_4")
                columnRSCM(PARTS, "loc.poh_hall2_4", "loc.poh_trophy_amuletofglory_4")
            }
            row("dbrow.build_poh_trophy_legendscape") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_legendscape")
                column(XP, 3000)
                columnRSCM(LOCS, "loc.poh_trophy_legendscape_4")
                columnRSCM(PARTS, "loc.poh_hall2_4", "loc.poh_trophy_legendscape_4")
            }
            row("dbrow.build_poh_portrait_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_portrait_1")
                column(XP, 2110)
                columnRSCM(LOCS, "loc.poh_portrait_kingarthur_1")
                columnRSCM(PARTS, "loc.poh_hall2_2", "loc.poh_portrait_kingarthur_1")
            }
            row("dbrow.build_poh_portrait_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_portrait_2")
                column(XP, 2110)
                columnRSCM(LOCS, "loc.poh_portrait_elena_1")
                columnRSCM(PARTS, "loc.poh_hall2_2", "loc.poh_portrait_elena_1")
            }
            row("dbrow.build_poh_portrait_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_portrait_3")
                column(XP, 2110)
                columnRSCM(LOCS, "loc.poh_portrait_giantdwarf_1")
                columnRSCM(PARTS, "loc.poh_hall2_2", "loc.poh_portrait_giantdwarf_1")
            }
            row("dbrow.build_poh_portrait_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_portrait_4")
                column(XP, 3110)
                columnRSCM(LOCS, "loc.poh_portrait_prince+princess_1")
                columnRSCM(PARTS, "loc.poh_hall2_2", "loc.poh_portrait_prince+princess_1")
            }
            row("dbrow.build_poh_landscape_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_landscape_1")
                column(XP, 3140)
                columnRSCM(LOCS, "loc.poh_landscape_lumbridge_1")
                columnRSCM(PARTS, "loc.poh_hall2_3", "loc.poh_landscape_lumbridge_1")
            }
            row("dbrow.build_poh_landscape_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_landscape_2")
                column(XP, 3140)
                columnRSCM(LOCS, "loc.poh_landscape_desert_1")
                columnRSCM(PARTS, "loc.poh_hall2_3", "loc.poh_landscape_desert_1")
            }
            row("dbrow.build_poh_landscape_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_landscape_3")
                column(XP, 3140)
                columnRSCM(LOCS, "loc.poh_landscape_morytania_1")
                columnRSCM(PARTS, "loc.poh_hall2_3", "loc.poh_landscape_morytania_1")
            }
            row("dbrow.build_poh_landscape_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_landscape_4")
                column(XP, 4640)
                columnRSCM(LOCS, "loc.poh_landscape_karamja_1")
                columnRSCM(PARTS, "loc.poh_hall2_3", "loc.poh_landscape_karamja_1")
            }
            row("dbrow.build_poh_landscape_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_landscape_5")
                column(XP, 4640)
                columnRSCM(LOCS, "loc.poh_landscape_istafar_1")
                columnRSCM(PARTS, "loc.poh_hall2_3", "loc.poh_landscape_istafar_1")
            }
            row("dbrow.build_poh_map_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_map_1")
                column(XP, 2110)
                columnRSCM(LOCS, "loc.poh_wall_map_freearea")
                columnRSCM(PARTS, "loc.poh_hall2_6", "loc.poh_wall_map_freearea")
            }
            row("dbrow.build_poh_map_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_map_2")
                column(XP, 4510)
                columnRSCM(LOCS, "loc.poh_wall_map_world")
                columnRSCM(PARTS, "loc.poh_hall2_6", "loc.poh_wall_map_world")
            }
            row("dbrow.build_poh_map_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_map_3")
                column(XP, 5910)
                columnRSCM(LOCS, "loc.poh_wall_map_world+underground")
                columnRSCM(PARTS, "loc.poh_hall2_6", "loc.poh_wall_map_world+underground")
            }
            row("dbrow.build_poh_cage_dungeon_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cage_dungeon_oak")
                column(XP, 6400)
                columnRSCM(LOCS, "loc.poh_cage_dungeon_oak", "loc.poh_cage_dungeon_oak_door")
                columnRSCM(PARTS, "loc.poh_oubliette_2_front", "loc.poh_cage_dungeon_oak", "loc.poh_oubliette_2_door", "loc.poh_cage_dungeon_oak_door")
            }
            row("dbrow.build_poh_cage_dungeon_oak_and_steel") {
                columnRSCM(MODEL_OBJ, "obj.poh_cage_dungeon_oak+steel")
                column(XP, 8000)
                columnRSCM(LOCS, "loc.poh_cage_dungeon_oak+steel", "loc.poh_cage_dungeon_oak+steel_door")
                columnRSCM(PARTS, "loc.poh_oubliette_2_front", "loc.poh_cage_dungeon_oak+steel", "loc.poh_oubliette_2_door", "loc.poh_cage_dungeon_oak+steel_door")
            }
            row("dbrow.build_poh_cage_dungeon_steel") {
                columnRSCM(MODEL_OBJ, "obj.poh_cage_dungeon_steel")
                column(XP, 4000)
                columnRSCM(LOCS, "loc.poh_cage_dungeon_steel", "loc.poh_cage_dungeon_steel_door")
                columnRSCM(PARTS, "loc.poh_oubliette_2_front", "loc.poh_cage_dungeon_steel", "loc.poh_oubliette_2_door", "loc.poh_cage_dungeon_steel_door")
            }
            row("dbrow.build_poh_cage_dungeon_steel_and_spikes") {
                columnRSCM(MODEL_OBJ, "obj.poh_cage_dungeon_steel+spikes")
                column(XP, 5000)
                columnRSCM(LOCS, "loc.poh_cage_dungeon_steel+spikes", "loc.poh_cage_dungeon_steel+spikes_door")
                columnRSCM(PARTS, "loc.poh_oubliette_2_front", "loc.poh_cage_dungeon_steel+spikes", "loc.poh_oubliette_2_door", "loc.poh_cage_dungeon_steel+spikes_door")
            }
            row("dbrow.build_poh_cage_dungeon_bones") {
                columnRSCM(MODEL_OBJ, "obj.poh_cage_dungeon_bones")
                column(XP, 6030)
                columnRSCM(LOCS, "loc.poh_cage_dungeon_bones", "loc.poh_cage_dungeon_bones_door")
                columnRSCM(PARTS, "loc.poh_oubliette_2_front", "loc.poh_cage_dungeon_bones", "loc.poh_oubliette_2_door", "loc.poh_cage_dungeon_bones_door")
            }
            row("dbrow.build_poh_oubliette_spikes") {
                columnRSCM(MODEL_OBJ, "obj.poh_oubliette_spikes")
                column(XP, 6230)
                columnRSCM(LOCS, "loc.poh_oubliette_spikes_mid", "loc.poh_oubliette_spikes_side", "loc.poh_oubliette_spikes_corner")
                columnRSCM(
                    PARTS,
                    "loc.poh_oubliette_1", "loc.poh_oubliette_spikes_mid", "loc.poh_oubliette_1_side", "loc.poh_oubliette_spikes_side",
                    "loc.poh_oubliette_1_corner", "loc.poh_oubliette_spikes_corner",
                )
            }
            row("dbrow.build_poh_oubliette_pool") {
                columnRSCM(MODEL_OBJ, "obj.poh_oubliette_pool")
                column(XP, 3260)
                columnRSCM(LOCS, "loc.poh_oubliette_pool_mid", "loc.poh_oubliette_pool_side", "loc.poh_oubliette_pool_corner")
                columnRSCM(
                    PARTS,
                    "loc.poh_oubliette_1", "loc.poh_oubliette_pool_mid", "loc.poh_oubliette_1_side", "loc.poh_oubliette_pool_side",
                    "loc.poh_oubliette_1_corner", "loc.poh_oubliette_pool_corner",
                )
            }
            row("dbrow.build_poh_oubliette_fire") {
                columnRSCM(MODEL_OBJ, "obj.poh_oubliette_fire")
                column(XP, 3570)
                columnRSCM(LOCS, "loc.poh_oubliette_floor_fire")
                columnRSCM(PARTS, "loc.poh_oubliette_1_type8", "loc.poh_oubliette_floor_fire", "loc.poh_oubliette_1_type8_ogre", "loc.poh_oubliette_floor_fire")
            }
            row("dbrow.build_poh_oub_monster1") {
                columnRSCM(MODEL_OBJ, "obj.poh_oub_monster1")
                column(XP, 3870)
                columnRSCM(LOCS, "loc.poh_oub_monster1")
                columnRSCM(PARTS, "loc.poh_oubliette_1_type8_ogre", "loc.poh_oub_monster1")
            }
            row("dbrow.build_poh_dungeon_ladder_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_ladder_oak")
                column(XP, 3000)
                columnRSCM(LOCS, "loc.poh_dungeon_ladder_oak")
                columnRSCM(PARTS, "loc.poh_oubliette_5", "loc.poh_dungeon_ladder_oak")
            }
            row("dbrow.build_poh_dungeon_ladder_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_ladder_teak")
                column(XP, 4500)
                columnRSCM(LOCS, "loc.poh_dungeon_ladder_teak")
                columnRSCM(PARTS, "loc.poh_oubliette_5", "loc.poh_dungeon_ladder_teak")
            }
            row("dbrow.build_poh_dungeon_ladder_mag") {
                columnRSCM(MODEL_OBJ, "obj.poh_dungeon_ladder_mag")
                column(XP, 7000)
                columnRSCM(LOCS, "loc.poh_dungeon_ladder_mag")
                columnRSCM(PARTS, "loc.poh_oubliette_5", "loc.poh_dungeon_ladder_mag")
            }
            row("dbrow.build_poh_armchair_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_armchair_1")
                column(XP, 580)
                columnRSCM(LOCS, "loc.poh_chair1")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_1", "loc.poh_chair1", "loc.poh_parlour_2", "loc.poh_chair1",
                    "loc.poh_parlour_3", "loc.poh_chair1",
                )
            }
            row("dbrow.build_poh_armchair_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_armchair_2")
                column(XP, 870)
                columnRSCM(LOCS, "loc.poh_chair2")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_1", "loc.poh_chair2", "loc.poh_parlour_2", "loc.poh_chair2",
                    "loc.poh_parlour_3", "loc.poh_chair2",
                )
            }
            row("dbrow.build_poh_armchair_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_armchair_3")
                column(XP, 870)
                columnRSCM(LOCS, "loc.poh_chair3")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_1", "loc.poh_chair3", "loc.poh_parlour_2", "loc.poh_chair3",
                    "loc.poh_parlour_3", "loc.poh_chair3",
                )
            }
            row("dbrow.build_poh_armchair_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_armchair_4")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_chair4")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_1", "loc.poh_chair4", "loc.poh_parlour_2", "loc.poh_chair4",
                    "loc.poh_parlour_3", "loc.poh_chair4",
                )
            }
            row("dbrow.build_poh_armchair_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_armchair_5")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_chair5")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_1", "loc.poh_chair5", "loc.poh_parlour_2", "loc.poh_chair5",
                    "loc.poh_parlour_3", "loc.poh_chair5",
                )
            }
            row("dbrow.build_poh_armchair_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_armchair_6")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_chair6")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_1", "loc.poh_chair6", "loc.poh_parlour_2", "loc.poh_chair6",
                    "loc.poh_parlour_3", "loc.poh_chair6",
                )
            }
            row("dbrow.build_poh_armchair_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_armchair_7")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_chair7")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_1", "loc.poh_chair7", "loc.poh_parlour_2", "loc.poh_chair7",
                    "loc.poh_parlour_3", "loc.poh_chair7",
                )
            }
            row("dbrow.build_poh_rug_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_rug_1")
                column(XP, 300)
                columnRSCM(LOCS, "loc.poh_rugmiddle1", "loc.poh_rugside1", "loc.poh_rugcorner1")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_4_middle", "loc.poh_rugmiddle1", "loc.poh_parlour_4_side", "loc.poh_rugside1",
                    "loc.poh_parlour_4_corner", "loc.poh_rugcorner1", "loc.poh_bedroom_5_middle", "loc.poh_rugmiddle1",
                    "loc.poh_bedroom_5_side", "loc.poh_rugside1", "loc.poh_bedroom_5_corner", "loc.poh_rugcorner1",
                    "loc.poh_chapel_5_middle", "loc.poh_rugmiddle1", "loc.poh_chapel_5_side", "loc.poh_rugside1",
                    "loc.poh_chapel_5_corner", "loc.poh_rugcorner1",
                )
            }
            row("dbrow.build_poh_rug_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_rug_2")
                column(XP, 600)
                columnRSCM(LOCS, "loc.poh_rugmiddle2", "loc.poh_rugside2", "loc.poh_rugcorner2")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_4_middle", "loc.poh_rugmiddle2", "loc.poh_parlour_4_side", "loc.poh_rugside2",
                    "loc.poh_parlour_4_corner", "loc.poh_rugcorner2", "loc.poh_bedroom_5_middle", "loc.poh_rugmiddle2",
                    "loc.poh_bedroom_5_side", "loc.poh_rugside2", "loc.poh_bedroom_5_corner", "loc.poh_rugcorner2",
                    "loc.poh_chapel_5_middle", "loc.poh_rugmiddle2", "loc.poh_chapel_5_side", "loc.poh_rugside2",
                    "loc.poh_chapel_5_corner", "loc.poh_rugcorner2", "loc.poh_hall1_1_middle", "loc.poh_rugmiddle2",
                    "loc.poh_hall1_1_side", "loc.poh_rugside2", "loc.poh_hall1_1_corner", "loc.poh_rugcorner2",
                    "loc.poh_hall2_1_middle", "loc.poh_rugside2", "loc.poh_hall2_1_side", "loc.poh_rugmiddle2",
                )
            }
            row("dbrow.build_poh_rug_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_rug_3")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_rugmiddle3", "loc.poh_rugside3", "loc.poh_rugcorner3")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_4_middle", "loc.poh_rugmiddle3", "loc.poh_parlour_4_side", "loc.poh_rugside3",
                    "loc.poh_parlour_4_corner", "loc.poh_rugcorner3", "loc.poh_bedroom_5_middle", "loc.poh_rugmiddle3",
                    "loc.poh_bedroom_5_side", "loc.poh_rugside3", "loc.poh_bedroom_5_corner", "loc.poh_rugcorner3",
                    "loc.poh_chapel_5_middle", "loc.poh_rugmiddle3", "loc.poh_chapel_5_side", "loc.poh_rugside3",
                    "loc.poh_chapel_5_corner", "loc.poh_rugcorner3", "loc.poh_hall1_1_middle", "loc.poh_rugmiddle3",
                    "loc.poh_hall1_1_side", "loc.poh_rugside3", "loc.poh_hall1_1_corner", "loc.poh_rugcorner3",
                    "loc.poh_hall2_1_middle", "loc.poh_rugside3", "loc.poh_hall2_1_side", "loc.poh_rugmiddle3",
                )
            }
            row("dbrow.build_poh_bookcase_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_bookcase_1")
                column(XP, 1150)
                columnRSCM(LOCS, "loc.poh_bookcase1")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_5", "loc.poh_bookcase1", "loc.poh_hall2_7", "loc.poh_bookcase1",
                    "loc.poh_study_7", "loc.poh_bookcase1",
                )
            }
            row("dbrow.build_poh_bookcase_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_bookcase_2")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_bookcase2")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_5", "loc.poh_bookcase2", "loc.poh_hall2_7", "loc.poh_bookcase2",
                    "loc.poh_study_7", "loc.poh_bookcase2",
                )
            }
            row("dbrow.build_poh_bookcase_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_bookcase_3")
                column(XP, 4200)
                columnRSCM(LOCS, "loc.poh_bookcase3")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_5", "loc.poh_bookcase3", "loc.poh_hall2_7", "loc.poh_bookcase3",
                    "loc.poh_study_7", "loc.poh_bookcase3",
                )
            }
            row("dbrow.build_poh_curtains_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_curtains_1")
                column(XP, 1320)
                columnRSCM(LOCS, "loc.poh_curtains_1")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_7", "loc.poh_curtains_1", "loc.poh_bedroom_4", "loc.poh_curtains_1",
                    "loc.poh_dining_room_5", "loc.poh_curtains_1",
                )
            }
            row("dbrow.build_poh_curtains_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_curtains_2")
                column(XP, 2250)
                columnRSCM(LOCS, "loc.poh_curtains_2")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_7", "loc.poh_curtains_2", "loc.poh_bedroom_4", "loc.poh_curtains_2",
                    "loc.poh_dining_room_5", "loc.poh_curtains_2",
                )
            }
            row("dbrow.build_poh_curtains_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_curtains_3")
                column(XP, 3150)
                columnRSCM(LOCS, "loc.poh_curtains_3")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_7", "loc.poh_curtains_3", "loc.poh_bedroom_4", "loc.poh_curtains_3",
                    "loc.poh_dining_room_5", "loc.poh_curtains_3",
                )
            }
            row("dbrow.build_poh_fireplace_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_fireplace_1")
                column(XP, 300)
                columnRSCM(LOCS, "loc.poh_fireplace_1")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_6", "loc.poh_fireplace_1", "loc.poh_bedroom_6", "loc.poh_fireplace_1",
                    "loc.poh_dining_room_4", "loc.poh_fireplace_1",
                )
            }
            row("dbrow.build_poh_fireplace_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_fireplace_2")
                column(XP, 400)
                columnRSCM(LOCS, "loc.poh_fireplace_2")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_6", "loc.poh_fireplace_2", "loc.poh_bedroom_6", "loc.poh_fireplace_2",
                    "loc.poh_dining_room_4", "loc.poh_fireplace_2",
                )
            }
            row("dbrow.build_poh_fireplace_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_fireplace_3")
                column(XP, 5000)
                columnRSCM(LOCS, "loc.poh_fireplace_3")
                columnRSCM(
                    PARTS,
                    "loc.poh_parlour_6", "loc.poh_fireplace_3", "loc.poh_bedroom_6", "loc.poh_fireplace_3",
                    "loc.poh_dining_room_4", "loc.poh_fireplace_3",
                )
            }
            row("dbrow.build_poh_portal_frame_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_portal_frame_1")
                column(XP, 2700)
                columnRSCM(LOCS, "loc.poh_portal_teak_empty")
                columnRSCM(
                    PARTS,
                    "loc.poh_teleroom_1", "loc.poh_portal_teak_empty", "loc.poh_teleroom_2", "loc.poh_portal_teak_empty",
                    "loc.poh_teleroom_3", "loc.poh_portal_teak_empty",
                )
            }
            row("dbrow.build_poh_portal_frame_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_portal_frame_2")
                column(XP, 4200)
                columnRSCM(LOCS, "loc.poh_portal_mag_empty")
                columnRSCM(
                    PARTS,
                    "loc.poh_teleroom_1", "loc.poh_portal_mag_empty", "loc.poh_teleroom_2", "loc.poh_portal_mag_empty",
                    "loc.poh_teleroom_3", "loc.poh_portal_mag_empty",
                )
            }
            row("dbrow.build_poh_portal_frame_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_portal_frame_3")
                column(XP, 15000)
                columnRSCM(LOCS, "loc.poh_portal_marble_empty")
                columnRSCM(
                    PARTS,
                    "loc.poh_teleroom_1", "loc.poh_portal_marble_empty", "loc.poh_teleroom_2", "loc.poh_portal_marble_empty",
                    "loc.poh_teleroom_3", "loc.poh_portal_marble_empty",
                )
            }
            row("dbrow.build_poh_teleport_centrepiece_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_teleport_centrepiece_1")
                column(XP, 400)
                columnRSCM(LOCS, "loc.poh_teleport_centrepiece")
                columnRSCM(PARTS, "loc.poh_teleroom_7", "loc.poh_teleport_centrepiece")
            }
            row("dbrow.build_poh_teleport_centrepiece_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_teleport_centrepiece_2")
                column(XP, 5000)
                columnRSCM(LOCS, "loc.poh_teleport_centrepiece_grand")
                columnRSCM(PARTS, "loc.poh_teleroom_7", "loc.poh_teleport_centrepiece_grand")
            }
            row("dbrow.build_poh_teleport_centrepiece_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_teleport_centrepiece_3")
                column(XP, 20000)
                columnRSCM(LOCS, "loc.poh_scrying_pool")
                columnRSCM(PARTS, "loc.poh_teleroom_7", "loc.poh_scrying_pool")
            }
            row("dbrow.build_poh_lectern_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_lectern_1")
                column(XP, 600)
                columnRSCM(LOCS, "loc.poh_lectern_1")
                columnRSCM(PARTS, "loc.poh_study_1", "loc.poh_lectern_1")
            }
            row("dbrow.build_poh_lectern_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_lectern_2")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_lectern_2")
                columnRSCM(PARTS, "loc.poh_study_1", "loc.poh_lectern_2")
            }
            row("dbrow.build_poh_lectern_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_lectern_3")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_lectern_3")
                columnRSCM(PARTS, "loc.poh_study_1", "loc.poh_lectern_3")
            }
            row("dbrow.build_poh_lectern_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_lectern_4")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_lectern_4")
                columnRSCM(PARTS, "loc.poh_study_1", "loc.poh_lectern_4")
            }
            row("dbrow.build_poh_lectern_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_lectern_5")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_lectern_5")
                columnRSCM(PARTS, "loc.poh_study_1", "loc.poh_lectern_5")
            }
            row("dbrow.build_poh_lectern_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_lectern_6")
                column(XP, 5800)
                columnRSCM(LOCS, "loc.poh_lectern_6")
                columnRSCM(PARTS, "loc.poh_study_1", "loc.poh_lectern_6")
            }
            row("dbrow.build_poh_lectern_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_lectern_7")
                column(XP, 5800)
                columnRSCM(LOCS, "loc.poh_lectern_7")
                columnRSCM(PARTS, "loc.poh_study_1", "loc.poh_lectern_7")
            }
            row("dbrow.build_poh_globe_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_globe_1")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_globe_1")
                columnRSCM(PARTS, "loc.poh_study_2", "loc.poh_globe_1")
            }
            row("dbrow.build_poh_globe_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_globe_2")
                column(XP, 2700)
                columnRSCM(LOCS, "loc.poh_globe_2")
                columnRSCM(PARTS, "loc.poh_study_2", "loc.poh_globe_2")
            }
            row("dbrow.build_poh_globe_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_globe_3")
                column(XP, 5700)
                columnRSCM(LOCS, "loc.poh_globe_3")
                columnRSCM(PARTS, "loc.poh_study_2", "loc.poh_globe_3")
            }
            row("dbrow.build_poh_globe_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_globe_4")
                column(XP, 5700)
                columnRSCM(LOCS, "loc.poh_globe_4")
                columnRSCM(PARTS, "loc.poh_study_2", "loc.poh_globe_4")
            }
            row("dbrow.build_poh_globe_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_globe_5")
                column(XP, 9600)
                columnRSCM(LOCS, "loc.poh_globe_5")
                columnRSCM(PARTS, "loc.poh_study_2", "loc.poh_globe_5")
            }
            row("dbrow.build_poh_globe_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_globe_6")
                column(XP, 13200)
                columnRSCM(LOCS, "loc.poh_globe_6")
                columnRSCM(PARTS, "loc.poh_study_2", "loc.poh_globe_6")
            }
            row("dbrow.build_poh_globe_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_globe_7")
                column(XP, 14200)
                columnRSCM(LOCS, "loc.poh_globe_7")
                columnRSCM(PARTS, "loc.poh_study_2", "loc.poh_globe_7")
            }
            row("dbrow.build_poh_telescope_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_telescope_1")
                column(XP, 1210)
                columnRSCM(LOCS, "loc.poh_telescope_1")
                columnRSCM(PARTS, "loc.poh_study_6", "loc.poh_telescope_1")
            }
            row("dbrow.build_poh_telescope_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_telescope_2")
                column(XP, 1810)
                columnRSCM(LOCS, "loc.poh_telescope_2")
                columnRSCM(PARTS, "loc.poh_study_6", "loc.poh_telescope_2")
            }
            row("dbrow.build_poh_telescope_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_telescope_3")
                column(XP, 5800)
                columnRSCM(LOCS, "loc.poh_telescope_3")
                columnRSCM(PARTS, "loc.poh_study_6", "loc.poh_telescope_3")
            }
            row("dbrow.build_poh_crystalball_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_crystalball_1")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_crystalball_1")
                columnRSCM(PARTS, "loc.poh_study_4", "loc.poh_crystalball_1")
            }
            row("dbrow.build_poh_crystalball_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_crystalball_2")
                column(XP, 5800)
                columnRSCM(LOCS, "loc.poh_crystalball_2")
                columnRSCM(PARTS, "loc.poh_study_4", "loc.poh_crystalball_2")
            }
            row("dbrow.build_poh_crystalball_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_crystalball_3")
                column(XP, 8900)
                columnRSCM(LOCS, "loc.poh_crystalball_3")
                columnRSCM(PARTS, "loc.poh_study_4", "loc.poh_crystalball_3")
            }
            row("dbrow.build_poh_wallchart_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_wallchart_1")
                column(XP, 300)
                columnRSCM(LOCS, "loc.poh_wallchart_1")
                columnRSCM(PARTS, "loc.poh_study_5", "loc.poh_wallchart_1")
            }
            row("dbrow.build_poh_wallchart_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_wallchart_2")
                column(XP, 450)
                columnRSCM(LOCS, "loc.poh_wallchart_2")
                columnRSCM(PARTS, "loc.poh_study_5", "loc.poh_wallchart_2")
            }
            row("dbrow.build_poh_wallchart_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_wallchart_3")
                column(XP, 600)
                columnRSCM(LOCS, "loc.poh_wallchart_3")
                columnRSCM(PARTS, "loc.poh_study_5", "loc.poh_wallchart_3")
            }
            row("dbrow.build_poh_throne_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_1")
                column(XP, 8000)
                columnRSCM(LOCS, "loc.poh_throne_1")
                columnRSCM(PARTS, "loc.poh_throne_room_1", "loc.poh_throne_1")
            }
            row("dbrow.build_poh_throne_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_2")
                column(XP, 14500)
                columnRSCM(LOCS, "loc.poh_throne_2")
                columnRSCM(PARTS, "loc.poh_throne_room_1", "loc.poh_throne_2")
            }
            row("dbrow.build_poh_throne_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_3")
                column(XP, 22000)
                columnRSCM(LOCS, "loc.poh_throne_3")
                columnRSCM(PARTS, "loc.poh_throne_room_1", "loc.poh_throne_3")
            }
            row("dbrow.build_poh_throne_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_4")
                column(XP, 17000)
                columnRSCM(LOCS, "loc.poh_throne_4")
                columnRSCM(PARTS, "loc.poh_throne_room_1", "loc.poh_throne_4")
            }
            row("dbrow.build_poh_throne_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_5")
                column(XP, 70030)
                columnRSCM(LOCS, "loc.poh_throne_5")
                columnRSCM(PARTS, "loc.poh_throne_room_1", "loc.poh_throne_5")
            }
            row("dbrow.build_poh_throne_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_6")
                column(XP, 150000)
                columnRSCM(LOCS, "loc.poh_throne_6")
                columnRSCM(PARTS, "loc.poh_throne_room_1", "loc.poh_throne_6")
            }
            row("dbrow.build_poh_throne_7") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_7")
                column(XP, 250000)
                columnRSCM(LOCS, "loc.poh_throne_7")
                columnRSCM(PARTS, "loc.poh_throne_room_1", "loc.poh_throne_7")
            }
            row("dbrow.build_poh_lever_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_lever_oak")
                column(XP, 3000)
                columnRSCM(LOCS, "loc.poh_lever_oak_4")
                columnRSCM(PARTS, "loc.poh_throne_room_4", "loc.poh_lever_oak_4")
            }
            row("dbrow.build_poh_lever_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_lever_teak")
                column(XP, 4500)
                columnRSCM(LOCS, "loc.poh_lever_teak_4")
                columnRSCM(PARTS, "loc.poh_throne_room_4", "loc.poh_lever_teak_4")
            }
            row("dbrow.build_poh_lever_mag") {
                columnRSCM(MODEL_OBJ, "obj.poh_lever_mag")
                column(XP, 7000)
                columnRSCM(LOCS, "loc.poh_lever_mag_4")
                columnRSCM(PARTS, "loc.poh_throne_room_4", "loc.poh_lever_mag_4")
            }
            row("dbrow.build_poh_trapdoor_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_trapdoor_oak")
                column(XP, 3000)
                columnRSCM(LOCS, "loc.poh_trapdoor_oak_7")
                columnRSCM(PARTS, "loc.poh_throne_room_7", "loc.poh_trapdoor_oak_7")
            }
            row("dbrow.build_poh_trapdoor_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_trapdoor_teak")
                column(XP, 4500)
                columnRSCM(LOCS, "loc.poh_trapdoor_teak_7")
                columnRSCM(PARTS, "loc.poh_throne_room_7", "loc.poh_trapdoor_teak_7")
            }
            row("dbrow.build_poh_trapdoor_mag") {
                columnRSCM(MODEL_OBJ, "obj.poh_trapdoor_mag")
                column(XP, 7000)
                columnRSCM(LOCS, "loc.poh_trapdoor_mag_7")
                columnRSCM(PARTS, "loc.poh_throne_room_7", "loc.poh_trapdoor_mag_7")
            }
            row("dbrow.build_poh_throne_room_cage_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_room_cage_1")
                column(XP, 7000)
                columnRSCM(LOCS, "loc.poh_floordecor_rimmington", "loc.poh_floordecor_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_floordecor_lumbridge", "loc.poh_floordecor_brimhaven", "loc.poh_floordecor_yanille", "loc.poh_floordecor_deathly")
                columnRSCM(
                    PARTS,
                    "loc.poh_throne_room_3_rimmington", "loc.poh_floordecor_rimmington", "loc.poh_throne_room_3_lumbridge", "loc.poh_floordecor_pollnivneach",
                    "loc.poh_throne_room_3_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_throne_room_3_rellekka", "loc.poh_floordecor_lumbridge",
                    "loc.poh_throne_room_3_brimhaven", "loc.poh_floordecor_brimhaven", "loc.poh_throne_room_3_yanille", "loc.poh_floordecor_yanille",
                    "loc.poh_throne_room_3_deathly", "loc.poh_floordecor_deathly",
                )
            }
            row("dbrow.build_poh_throne_room_cage_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_room_cage_2")
                column(XP, 11000)
                columnRSCM(LOCS, "loc.poh_floordecor_rimmington", "loc.poh_floordecor_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_floordecor_lumbridge", "loc.poh_floordecor_brimhaven", "loc.poh_floordecor_yanille", "loc.poh_floordecor_deathly")
                columnRSCM(
                    PARTS,
                    "loc.poh_throne_room_3_rimmington", "loc.poh_floordecor_rimmington", "loc.poh_throne_room_3_lumbridge", "loc.poh_floordecor_pollnivneach",
                    "loc.poh_throne_room_3_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_throne_room_3_rellekka", "loc.poh_floordecor_lumbridge",
                    "loc.poh_throne_room_3_brimhaven", "loc.poh_floordecor_brimhaven", "loc.poh_throne_room_3_yanille", "loc.poh_floordecor_yanille",
                    "loc.poh_throne_room_3_deathly", "loc.poh_floordecor_deathly",
                )
            }
            row("dbrow.build_poh_throne_room_cage_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_room_cage_3")
                column(XP, 7700)
                columnRSCM(LOCS, "loc.poh_floordecor_rimmington", "loc.poh_floordecor_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_floordecor_lumbridge", "loc.poh_floordecor_brimhaven", "loc.poh_floordecor_yanille", "loc.poh_floordecor_deathly")
                columnRSCM(
                    PARTS,
                    "loc.poh_throne_room_3_rimmington", "loc.poh_floordecor_rimmington", "loc.poh_throne_room_3_lumbridge", "loc.poh_floordecor_pollnivneach",
                    "loc.poh_throne_room_3_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_throne_room_3_rellekka", "loc.poh_floordecor_lumbridge",
                    "loc.poh_throne_room_3_brimhaven", "loc.poh_floordecor_brimhaven", "loc.poh_throne_room_3_yanille", "loc.poh_floordecor_yanille",
                    "loc.poh_throne_room_3_deathly", "loc.poh_floordecor_deathly",
                )
            }
            row("dbrow.build_poh_throne_room_cage_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_room_cage_4")
                column(XP, 27000)
                columnRSCM(LOCS, "loc.poh_floordecor_rimmington", "loc.poh_floordecor_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_floordecor_lumbridge", "loc.poh_floordecor_brimhaven", "loc.poh_floordecor_yanille", "loc.poh_floordecor_deathly")
                columnRSCM(
                    PARTS,
                    "loc.poh_throne_room_3_rimmington", "loc.poh_floordecor_rimmington", "loc.poh_throne_room_3_lumbridge", "loc.poh_floordecor_pollnivneach",
                    "loc.poh_throne_room_3_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_throne_room_3_rellekka", "loc.poh_floordecor_lumbridge",
                    "loc.poh_throne_room_3_brimhaven", "loc.poh_floordecor_brimhaven", "loc.poh_throne_room_3_yanille", "loc.poh_floordecor_yanille",
                    "loc.poh_throne_room_3_deathly", "loc.poh_floordecor_deathly",
                )
            }
            row("dbrow.build_poh_throne_room_cage_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_throne_room_cage_5")
                column(XP, 47000)
                columnRSCM(LOCS, "loc.poh_floordecor_rimmington", "loc.poh_floordecor_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_floordecor_lumbridge", "loc.poh_floordecor_brimhaven", "loc.poh_floordecor_yanille", "loc.poh_floordecor_deathly")
                columnRSCM(
                    PARTS,
                    "loc.poh_throne_room_3_rimmington", "loc.poh_floordecor_rimmington", "loc.poh_throne_room_3_lumbridge", "loc.poh_floordecor_pollnivneach",
                    "loc.poh_throne_room_3_pollnivneach", "loc.poh_floordecor_rellekka", "loc.poh_throne_room_3_rellekka", "loc.poh_floordecor_lumbridge",
                    "loc.poh_throne_room_3_brimhaven", "loc.poh_floordecor_brimhaven", "loc.poh_throne_room_3_yanille", "loc.poh_floordecor_yanille",
                    "loc.poh_throne_room_3_deathly", "loc.poh_floordecor_deathly",
                )
            }
            row("dbrow.build_poh_workbench_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_workbench_1")
                column(XP, 1430)
                columnRSCM(LOCS, "loc.poh_workbench_1", "loc.poh_stool_1")
                columnRSCM(PARTS, "loc.poh_workshop_1", "loc.poh_workbench_1", "loc.poh_workshop_1_stool", "loc.poh_stool_1")
            }
            row("dbrow.build_poh_workbench_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_workbench_2")
                column(XP, 3000)
                columnRSCM(LOCS, "loc.poh_workbench_2", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_1", "loc.poh_workbench_2", "loc.poh_workshop_1_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_workbench_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_workbench_3")
                column(XP, 4400)
                columnRSCM(LOCS, "loc.poh_workbench_3", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_1", "loc.poh_workbench_3", "loc.poh_workshop_1_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_workbench_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_workbench_4")
                column(XP, 1400)
                columnRSCM(LOCS, "loc.poh_workbench_4", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_1", "loc.poh_workbench_4", "loc.poh_workshop_1_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_workbench_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_workbench_5")
                column(XP, 1400)
                columnRSCM(LOCS, "loc.poh_workbench_5", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_1", "loc.poh_workbench_5", "loc.poh_workshop_1_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_crafting_table_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_crafting_table_1")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_clockmaking_1", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_2", "loc.poh_clockmaking_1", "loc.poh_workshop_2_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_crafting_table_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_crafting_table_2")
                column(XP, 10)
                columnRSCM(LOCS, "loc.poh_clockmaking_2", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_2", "loc.poh_clockmaking_2", "loc.poh_workshop_2_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_crafting_table_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_crafting_table_3")
                column(XP, 20)
                columnRSCM(LOCS, "loc.poh_clockmaking_3", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_2", "loc.poh_clockmaking_3", "loc.poh_workshop_2_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_crafting_table_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_crafting_table_4")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_clockmaking_4", "loc.poh_stool_2")
                columnRSCM(PARTS, "loc.poh_workshop_2", "loc.poh_clockmaking_4", "loc.poh_workshop_2_stool", "loc.poh_stool_2")
            }
            row("dbrow.build_poh_tool_store_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_tool_store_1")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_tools1")
                columnRSCM(PARTS, "loc.poh_workshop_3a", "loc.poh_tools1")
            }
            row("dbrow.build_poh_tool_store_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_tool_store_2")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_tools1", "loc.poh_tools2")
                columnRSCM(PARTS, "loc.poh_workshop_3a", "loc.poh_tools1", "loc.poh_workshop_3b", "loc.poh_tools2")
            }
            row("dbrow.build_poh_tool_store_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_tool_store_3")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_tools1", "loc.poh_tools2", "loc.poh_tools3")
                columnRSCM(
                    PARTS,
                    "loc.poh_workshop_3a", "loc.poh_tools1", "loc.poh_workshop_3b", "loc.poh_tools2",
                    "loc.poh_workshop_3c", "loc.poh_tools3",
                )
            }
            row("dbrow.build_poh_tool_store_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_tool_store_4")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_tools1", "loc.poh_tools2", "loc.poh_tools3", "loc.poh_tools4")
                columnRSCM(
                    PARTS,
                    "loc.poh_workshop_3a", "loc.poh_tools1", "loc.poh_workshop_3b", "loc.poh_tools2",
                    "loc.poh_workshop_3c", "loc.poh_tools3", "loc.poh_workshop_3d", "loc.poh_tools4",
                )
            }
            row("dbrow.build_poh_tool_store_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_tool_store_5")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_tools1", "loc.poh_tools2", "loc.poh_tools3", "loc.poh_tools4", "loc.poh_tools5")
                columnRSCM(
                    PARTS,
                    "loc.poh_workshop_3a", "loc.poh_tools1", "loc.poh_workshop_3b", "loc.poh_tools2",
                    "loc.poh_workshop_3c", "loc.poh_tools3", "loc.poh_workshop_3d", "loc.poh_tools4",
                    "loc.poh_workshop_3e", "loc.poh_tools5",
                )
            }
            row("dbrow.build_poh_repair_bench_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_repair_bench_1")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_repair_1")
                columnRSCM(PARTS, "loc.poh_workshop_4", "loc.poh_repair_1")
            }
            row("dbrow.build_poh_repair_bench_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_repair_bench_2")
                column(XP, 2600)
                columnRSCM(LOCS, "loc.poh_repair_2")
                columnRSCM(PARTS, "loc.poh_workshop_4", "loc.poh_repair_2")
            }
            row("dbrow.build_poh_repair_bench_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_repair_bench_3")
                column(XP, 5000)
                columnRSCM(LOCS, "loc.poh_repair_3")
                columnRSCM(PARTS, "loc.poh_workshop_4", "loc.poh_repair_3")
            }
            row("dbrow.build_poh_heraldry_bench_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_heraldry_bench_1")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_repair_4")
                columnRSCM(PARTS, "loc.poh_workshop_5", "loc.poh_repair_4")
            }
            row("dbrow.build_poh_heraldry_bench_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_heraldry_bench_2")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_repair_5")
                columnRSCM(PARTS, "loc.poh_workshop_5", "loc.poh_repair_5")
            }
            row("dbrow.build_poh_heraldry_bench_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_heraldry_bench_3")
                column(XP, 5100)
                columnRSCM(LOCS, "loc.poh_repair_6")
                columnRSCM(PARTS, "loc.poh_workshop_5", "loc.poh_repair_6")
            }
            row("dbrow.build_poh_cos_room_cape_rack_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_cape_rack_oak")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_cos_room_cape_rack_oak")
                columnRSCM(PARTS, "loc.poh_cos_room_cape_rack_hotspot", "loc.poh_cos_room_cape_rack_oak")
            }
            row("dbrow.build_poh_cos_room_cape_rack_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_cape_rack_teak")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_cos_room_cape_rack_teak")
                columnRSCM(PARTS, "loc.poh_cos_room_cape_rack_hotspot", "loc.poh_cos_room_cape_rack_teak")
            }
            row("dbrow.build_poh_cos_room_cape_rack_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_cape_rack_mahogany")
                column(XP, 7200)
                columnRSCM(LOCS, "loc.poh_cos_room_cape_rack_mahogany")
                columnRSCM(PARTS, "loc.poh_cos_room_cape_rack_hotspot", "loc.poh_cos_room_cape_rack_mahogany")
            }
            row("dbrow.build_poh_cos_room_cape_rack_mahogany_gilded") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_cape_rack_mahogany_gilded")
                column(XP, 9000)
                columnRSCM(LOCS, "loc.poh_cos_room_cape_rack_mahogany_gilded")
                columnRSCM(PARTS, "loc.poh_cos_room_cape_rack_hotspot", "loc.poh_cos_room_cape_rack_mahogany_gilded")
            }
            row("dbrow.build_poh_cos_room_cape_rack_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_cape_rack_marble")
                column(XP, 20000)
                columnRSCM(LOCS, "loc.poh_cos_room_cape_rack_marble")
                columnRSCM(PARTS, "loc.poh_cos_room_cape_rack_hotspot", "loc.poh_cos_room_cape_rack_marble")
            }
            row("dbrow.build_poh_cos_room_cape_rack_magic_stone") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_cape_rack_magic_stone")
                column(XP, 20000)
                columnRSCM(LOCS, "loc.poh_cos_room_cape_rack_magic_stone")
                columnRSCM(PARTS, "loc.poh_cos_room_cape_rack_hotspot", "loc.poh_cos_room_cape_rack_magic_stone")
            }
            row("dbrow.build_poh_cos_room_fancy_dress_box_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_fancy_dress_box_oak")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_cos_room_fancy_dress_box_oak")
                columnRSCM(PARTS, "loc.poh_cos_room_fancy_dress_box_hotspot", "loc.poh_cos_room_fancy_dress_box_oak")
            }
            row("dbrow.build_poh_cos_room_fancy_dress_box_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_fancy_dress_box_teak")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_cos_room_fancy_dress_box_teak")
                columnRSCM(PARTS, "loc.poh_cos_room_fancy_dress_box_hotspot", "loc.poh_cos_room_fancy_dress_box_teak")
            }
            row("dbrow.build_poh_cos_room_fancy_dress_box_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_fancy_dress_box_mahogany")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_cos_room_fancy_dress_box_mahogany")
                columnRSCM(PARTS, "loc.poh_cos_room_fancy_dress_box_hotspot", "loc.poh_cos_room_fancy_dress_box_mahogany")
            }
            row("dbrow.build_poh_cos_room_armour_case_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_armour_case_oak")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_cos_room_armour_case_oak")
                columnRSCM(PARTS, "loc.poh_cos_room_armour_case_hotspot", "loc.poh_cos_room_armour_case_oak")
            }
            row("dbrow.build_poh_cos_room_armour_case_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_armour_case_teak")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_cos_room_armour_case_teak")
                columnRSCM(PARTS, "loc.poh_cos_room_armour_case_hotspot", "loc.poh_cos_room_armour_case_teak")
            }
            row("dbrow.build_poh_cos_room_armour_case_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_armour_case_mahogany")
                column(XP, 4200)
                columnRSCM(LOCS, "loc.poh_cos_room_armour_case_mahogany")
                columnRSCM(PARTS, "loc.poh_cos_room_armour_case_hotspot", "loc.poh_cos_room_armour_case_mahogany")
            }
            row("dbrow.build_poh_cos_room_magic_wardrobe_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_magic_wardrobe_oak")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_cos_room_magic_wardrobe_oak")
                columnRSCM(PARTS, "loc.poh_cos_room_magic_wardrobe_hotspot", "loc.poh_cos_room_magic_wardrobe_oak")
            }
            row("dbrow.build_poh_cos_room_magic_wardrobe_carved_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_magic_wardrobe_carved_oak")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_cos_room_magic_wardrobe_carved_oak")
                columnRSCM(PARTS, "loc.poh_cos_room_magic_wardrobe_hotspot", "loc.poh_cos_room_magic_wardrobe_carved_oak")
            }
            row("dbrow.build_poh_cos_room_magic_wardrobe_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_magic_wardrobe_teak")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_cos_room_magic_wardrobe_teak")
                columnRSCM(PARTS, "loc.poh_cos_room_magic_wardrobe_hotspot", "loc.poh_cos_room_magic_wardrobe_teak")
            }
            row("dbrow.build_poh_cos_room_magic_wardrobe_teak_carved") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_magic_wardrobe_teak_carved")
                column(XP, 4500)
                columnRSCM(LOCS, "loc.poh_cos_room_magic_wardrobe_carved_teak")
                columnRSCM(PARTS, "loc.poh_cos_room_magic_wardrobe_hotspot", "loc.poh_cos_room_magic_wardrobe_carved_teak")
            }
            row("dbrow.build_poh_cos_room_magic_wardrobe_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_magic_wardrobe_mahogany")
                column(XP, 5600)
                columnRSCM(LOCS, "loc.poh_cos_room_magic_wardrobe_mahogany")
                columnRSCM(PARTS, "loc.poh_cos_room_magic_wardrobe_hotspot", "loc.poh_cos_room_magic_wardrobe_mahogany")
            }
            row("dbrow.build_poh_cos_room_magic_wardrobe_mahogany_gilded") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_magic_wardrobe_mahogany_gilded")
                column(XP, 7000)
                columnRSCM(LOCS, "loc.poh_cos_room_magic_wardrobe_mahogany_gilded")
                columnRSCM(PARTS, "loc.poh_cos_room_magic_wardrobe_hotspot", "loc.poh_cos_room_magic_wardrobe_mahogany_gilded")
            }
            row("dbrow.build_poh_cos_room_magic_wardrobe_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_magic_wardrobe_marble")
                column(XP, 10000)
                columnRSCM(LOCS, "loc.poh_cos_room_magic_wardrobe_marble")
                columnRSCM(PARTS, "loc.poh_cos_room_magic_wardrobe_hotspot", "loc.poh_cos_room_magic_wardrobe_marble")
            }
            row("dbrow.build_poh_cos_room_toy_box_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_toy_box_oak")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_cos_room_toy_box_oak")
                columnRSCM(PARTS, "loc.poh_cos_room_toy_box_hotspot", "loc.poh_cos_room_toy_box_oak")
            }
            row("dbrow.build_poh_cos_room_toy_box_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_toy_box_teak")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_cos_room_toy_box_teak")
                columnRSCM(PARTS, "loc.poh_cos_room_toy_box_hotspot", "loc.poh_cos_room_toy_box_teak")
            }
            row("dbrow.build_poh_cos_room_toy_box_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_toy_box_mahogany")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_cos_room_toy_box_mahogany")
                columnRSCM(PARTS, "loc.poh_cos_room_toy_box_hotspot", "loc.poh_cos_room_toy_box_mahogany")
            }
            row("dbrow.build_poh_cos_room_treasure_chest_oak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_treasure_chest_oak")
                column(XP, 1200)
                columnRSCM(LOCS, "loc.poh_cos_room_tresure_chest_oak")
                columnRSCM(PARTS, "loc.poh_cos_room_tresure_chest_hotspot", "loc.poh_cos_room_tresure_chest_oak")
            }
            row("dbrow.build_poh_cos_room_treasure_chest_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_treasure_chest_teak")
                column(XP, 1800)
                columnRSCM(LOCS, "loc.poh_cos_room_tresure_chest_teak")
                columnRSCM(PARTS, "loc.poh_cos_room_tresure_chest_hotspot", "loc.poh_cos_room_tresure_chest_teak")
            }
            row("dbrow.build_poh_cos_room_treasure_chest_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_cos_room_treasure_chest_mahogany")
                column(XP, 2800)
                columnRSCM(LOCS, "loc.poh_cos_room_tresure_chest_mahogany")
                columnRSCM(PARTS, "loc.poh_cos_room_tresure_chest_hotspot", "loc.poh_cos_room_tresure_chest_mahogany")
            }
            row("dbrow.build_poh_menagerie_pethouse_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_pethouse_1")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_menagerie_pethouse_1")
                columnRSCM(PARTS, "loc.poh_menagerie_pethouse_hotspot", "loc.poh_menagerie_pethouse_1")
            }
            row("dbrow.build_poh_menagerie_pethouse_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_pethouse_2")
                column(XP, 3600)
                columnRSCM(LOCS, "loc.poh_menagerie_pethouse_2")
                columnRSCM(PARTS, "loc.poh_menagerie_pethouse_hotspot", "loc.poh_menagerie_pethouse_2")
            }
            row("dbrow.build_poh_menagerie_pethouse_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_pethouse_3")
                column(XP, 5600)
                columnRSCM(LOCS, "loc.poh_menagerie_pethouse_3")
                columnRSCM(PARTS, "loc.poh_menagerie_pethouse_hotspot", "loc.poh_menagerie_pethouse_3")
            }
            row("dbrow.build_poh_menagerie_pethouse_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_pethouse_4")
                column(XP, 15600)
                columnRSCM(LOCS, "loc.poh_menagerie_pethouse_4")
                columnRSCM(PARTS, "loc.poh_menagerie_pethouse_hotspot", "loc.poh_menagerie_pethouse_4")
            }
            row("dbrow.build_poh_menagerie_pethouse_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_pethouse_5")
                column(XP, 1600)
                columnRSCM(LOCS, "loc.poh_menagerie_pethouse_5")
                columnRSCM(PARTS, "loc.poh_menagerie_pethouse_hotspot", "loc.poh_menagerie_pethouse_5")
            }
            row("dbrow.build_poh_menagerie_pethouse_6") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_pethouse_6")
                column(XP, 1580)
                columnRSCM(LOCS, "loc.poh_menagerie_pethouse_6")
                columnRSCM(PARTS, "loc.poh_menagerie_pethouse_hotspot", "loc.poh_menagerie_pethouse_6")
            }
            row("dbrow.build_poh_menagerie_habitat_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_habitat_1")
                column(XP, 370)
                columnRSCM(LOCS, "loc.poh_menagerie_habitat_feature_1", "loc.poh_menagerie_habitat_ground_middle_1", "loc.poh_menagerie_habitat_ground_side_1", "loc.poh_menagerie_habitat_ground_corner_1")
                columnRSCM(
                    PARTS,
                    "loc.poh_menagerie_habitat_feature", "loc.poh_menagerie_habitat_feature_1", "loc.poh_menagerie_habitat_ground_middle", "loc.poh_menagerie_habitat_ground_middle_1",
                    "loc.poh_menagerie_habitat_ground_side", "loc.poh_menagerie_habitat_ground_side_1", "loc.poh_menagerie_habitat_ground_corner", "loc.poh_menagerie_habitat_ground_corner_1",
                )
            }
            row("dbrow.build_poh_menagerie_habitat_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_habitat_2")
                column(XP, 510)
                columnRSCM(LOCS, "loc.poh_menagerie_habitat_feature_2", "loc.poh_menagerie_habitat_ground_middle_2", "loc.poh_menagerie_habitat_ground_side_2", "loc.poh_menagerie_habitat_ground_corner_2")
                columnRSCM(
                    PARTS,
                    "loc.poh_menagerie_habitat_feature", "loc.poh_menagerie_habitat_feature_2", "loc.poh_menagerie_habitat_ground_middle", "loc.poh_menagerie_habitat_ground_middle_2",
                    "loc.poh_menagerie_habitat_ground_side", "loc.poh_menagerie_habitat_ground_side_2", "loc.poh_menagerie_habitat_ground_corner", "loc.poh_menagerie_habitat_ground_corner_2",
                )
            }
            row("dbrow.build_poh_menagerie_habitat_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_habitat_3")
                column(XP, 340)
                columnRSCM(LOCS, "loc.poh_menagerie_habitat_feature_3", "loc.poh_menagerie_habitat_ground_middle_3", "loc.poh_menagerie_habitat_ground_side_3", "loc.poh_menagerie_habitat_ground_corner_3")
                columnRSCM(
                    PARTS,
                    "loc.poh_menagerie_habitat_feature", "loc.poh_menagerie_habitat_feature_3", "loc.poh_menagerie_habitat_ground_middle", "loc.poh_menagerie_habitat_ground_middle_3",
                    "loc.poh_menagerie_habitat_ground_side", "loc.poh_menagerie_habitat_ground_side_3", "loc.poh_menagerie_habitat_ground_corner", "loc.poh_menagerie_habitat_ground_corner_3",
                )
            }
            row("dbrow.build_poh_menagerie_habitat_4") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_habitat_4")
                column(XP, 2710)
                columnRSCM(LOCS, "loc.poh_menagerie_habitat_feature_4", "loc.poh_menagerie_habitat_ground_middle_4", "loc.poh_menagerie_habitat_ground_side_4", "loc.poh_menagerie_habitat_ground_corner_4")
                columnRSCM(
                    PARTS,
                    "loc.poh_menagerie_habitat_feature", "loc.poh_menagerie_habitat_feature_4", "loc.poh_menagerie_habitat_ground_middle", "loc.poh_menagerie_habitat_ground_middle_4",
                    "loc.poh_menagerie_habitat_ground_side", "loc.poh_menagerie_habitat_ground_side_4", "loc.poh_menagerie_habitat_ground_corner", "loc.poh_menagerie_habitat_ground_corner_4",
                )
            }
            row("dbrow.build_poh_menagerie_habitat_5") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_habitat_5")
                column(XP, 460)
                columnRSCM(LOCS, "loc.poh_menagerie_habitat_feature_5", "loc.poh_menagerie_habitat_ground_middle_5", "loc.poh_menagerie_habitat_ground_side_5", "loc.poh_menagerie_habitat_ground_corner_5")
                columnRSCM(
                    PARTS,
                    "loc.poh_menagerie_habitat_feature", "loc.poh_menagerie_habitat_feature_5", "loc.poh_menagerie_habitat_ground_middle", "loc.poh_menagerie_habitat_ground_middle_5",
                    "loc.poh_menagerie_habitat_ground_side", "loc.poh_menagerie_habitat_ground_side_5", "loc.poh_menagerie_habitat_ground_corner", "loc.poh_menagerie_habitat_ground_corner_5",
                )
            }
            row("dbrow.build_poh_menagerie_scratchingpost_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_scratchingpost_1")
                column(XP, 1240)
                columnRSCM(LOCS, "loc.poh_menagerie_scratchingpost_1")
                columnRSCM(PARTS, "loc.poh_menagerie_scratchingpost_hotspot", "loc.poh_menagerie_scratchingpost_1")
            }
            row("dbrow.build_poh_menagerie_scratchingpost_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_scratchingpost_2")
                column(XP, 2040)
                columnRSCM(LOCS, "loc.poh_menagerie_scratchingpost_2")
                columnRSCM(PARTS, "loc.poh_menagerie_scratchingpost_hotspot", "loc.poh_menagerie_scratchingpost_2")
            }
            row("dbrow.build_poh_menagerie_scratchingpost_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_scratchingpost_3")
                column(XP, 3040)
                columnRSCM(LOCS, "loc.poh_menagerie_scratchingpost_3")
                columnRSCM(PARTS, "loc.poh_menagerie_scratchingpost_hotspot", "loc.poh_menagerie_scratchingpost_3")
            }
            row("dbrow.build_poh_menagerie_combatring_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_combatring_1")
                column(XP, 1390)
                columnRSCM(LOCS, "loc.poh_menagerie_combatring_1", "loc.poh_menagerie_combatring_mat")
                columnRSCM(PARTS, "loc.poh_menagerie_combatring_hotspot", "loc.poh_menagerie_combatring_1", "loc.poh_menagerie_combatring_mat_hotspot", "loc.poh_menagerie_combatring_mat")
            }
            row("dbrow.build_poh_menagerie_combatring_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_combatring_2")
                column(XP, 1990)
                columnRSCM(LOCS, "loc.poh_menagerie_combatring_2", "loc.poh_menagerie_combatring_mat")
                columnRSCM(PARTS, "loc.poh_menagerie_combatring_hotspot", "loc.poh_menagerie_combatring_2", "loc.poh_menagerie_combatring_mat_hotspot", "loc.poh_menagerie_combatring_mat")
            }
            row("dbrow.build_poh_menagerie_combatring_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_combatring_3")
                column(XP, 2990)
                columnRSCM(LOCS, "loc.poh_menagerie_combatring_3", "loc.poh_menagerie_combatring_mat")
                columnRSCM(PARTS, "loc.poh_menagerie_combatring_hotspot", "loc.poh_menagerie_combatring_3", "loc.poh_menagerie_combatring_mat_hotspot", "loc.poh_menagerie_combatring_mat")
            }
            row("dbrow.build_poh_menagerie_petlist_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_petlist_1")
                column(XP, 1980)
                columnRSCM(LOCS, "loc.poh_menagerie_petlist_1")
                columnRSCM(PARTS, "loc.poh_menagerie_petlist_hotspot", "loc.poh_menagerie_petlist_1")
            }
            row("dbrow.build_poh_menagerie_petfeeder_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_petfeeder_1")
                column(XP, 1820)
                columnRSCM(LOCS, "loc.poh_menagerie_petfeeder_1")
                columnRSCM(PARTS, "loc.poh_menagerie_petfeeder_hotspot", "loc.poh_menagerie_petfeeder_1")
            }
            row("dbrow.build_poh_menagerie_petfeeder_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_petfeeder_2")
                column(XP, 2720)
                columnRSCM(LOCS, "loc.poh_menagerie_petfeeder_2")
                columnRSCM(PARTS, "loc.poh_menagerie_petfeeder_hotspot", "loc.poh_menagerie_petfeeder_2")
            }
            row("dbrow.build_poh_menagerie_petfeeder_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_menagerie_petfeeder_3")
                column(XP, 8620)
                columnRSCM(LOCS, "loc.poh_menagerie_petfeeder_3")
                columnRSCM(PARTS, "loc.poh_menagerie_petfeeder_hotspot", "loc.poh_menagerie_petfeeder_3")
            }
            row("dbrow.build_poh_achievementgallery_altar_ancient") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_altar_ancient")
                column(XP, 14900)
                columnRSCM(LOCS, "loc.poh_altar_ancient")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_altar", "loc.poh_altar_ancient")
            }
            row("dbrow.build_poh_achievementgallery_altar_lunar") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_altar_lunar")
                column(XP, 19500)
                columnRSCM(LOCS, "loc.poh_altar_lunar")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_altar", "loc.poh_altar_lunar")
            }
            row("dbrow.build_poh_achievementgallery_altar_arceuus") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_altar_arceuus")
                column(XP, 38880)
                columnRSCM(LOCS, "loc.poh_altar_dark")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_altar", "loc.poh_altar_dark")
            }
            row("dbrow.build_poh_achievementgallery_altar_ecumenical_fromancient") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_altar_ecumenical_fromancient")
                column(XP, 34450)
                columnRSCM(LOCS, "loc.poh_altar_occult")
            }
            row("dbrow.build_poh_achievementgallery_adventurelog_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_adventurelog_1")
                column(XP, 5040)
                columnRSCM(LOCS, "loc.poh_adventure_log_1")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_log", "loc.poh_adventure_log_1")
            }
            row("dbrow.build_poh_achievementgallery_adventurelog_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_adventurelog_2")
                column(XP, 11000)
                columnRSCM(LOCS, "loc.poh_adventure_log_2")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_log", "loc.poh_adventure_log_2")
            }
            row("dbrow.build_poh_achievementgallery_adventurelog_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_adventurelog_3")
                column(XP, 11600)
                columnRSCM(LOCS, "loc.poh_adventure_log_3")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_log", "loc.poh_adventure_log_3")
            }
            row("dbrow.build_poh_achievementgallery_jewellerybox_1") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_jewellerybox_1")
                column(XP, 6050)
                columnRSCM(LOCS, "loc.poh_jewellery_box_1")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_jewellerybox", "loc.poh_jewellery_box_1")
            }
            row("dbrow.build_poh_achievementgallery_jewellerybox_2") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_jewellerybox_2")
                column(XP, 13500)
                columnRSCM(LOCS, "loc.poh_jewellery_box_2")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_jewellerybox", "loc.poh_jewellery_box_2")
            }
            row("dbrow.build_poh_achievementgallery_jewellerybox_3") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_jewellerybox_3")
                column(XP, 26800)
                columnRSCM(LOCS, "loc.poh_jewellery_box_3")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_jewellerybox", "loc.poh_jewellery_box_3")
            }
            row("dbrow.build_poh_achievementgallery_bosslair_blank") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_bosslair_blank")
                column(XP, 14830)
                columnRSCM(LOCS, "loc.poh_display_blank")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_lair", "loc.poh_display_blank")
            }
            row("dbrow.build_poh_achievementgallery_mounteddisplay_emblem") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_mounteddisplay_emblem")
                column(XP, 53000)
                columnRSCM(LOCS, "loc.poh_mounted_emblem")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_display", "loc.poh_mounted_emblem")
            }
            row("dbrow.build_poh_achievementgallery_mounteddisplay_coins") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_mounteddisplay_coins")
                column(XP, 8000)
                columnRSCM(LOCS, "loc.poh_mounted_coins")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_display", "loc.poh_mounted_coins")
            }
            row("dbrow.build_poh_achievementgallery_mounteddisplay_capestand") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_mounteddisplay_capestand")
                column(XP, 8000)
                columnRSCM(LOCS, "loc.poh_mounted_capestand_blank")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_display", "loc.poh_mounted_capestand_blank")
            }
            row("dbrow.build_poh_achievementgallery_questlist") {
                columnRSCM(MODEL_OBJ, "obj.poh_achievementgallery_questlist")
                column(XP, 3100)
                columnRSCM(LOCS, "loc.poh_quest_list")
                columnRSCM(PARTS, "loc.poh_achievement_hotspot_questlist", "loc.poh_quest_list")
            }
            row("dbrow.build_poh_tipjar") {
                columnRSCM(MODEL_OBJ, "obj.poh_tipjar")
                column(XP, 6510)
                columnRSCM(LOCS, "loc.poh_tipjar")
                columnRSCM(PARTS, "loc.poh_garden_8", "loc.poh_tipjar")
            }
            row("dbrow.build_poh_superior_garden_teleport_tree") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_teleport_tree")
                column(XP, 7000)
                columnRSCM(LOCS, "loc.poh_spirit_tree")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_treering", "loc.poh_spirit_tree")
            }
            row("dbrow.build_poh_superior_garden_teleport_ring") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_teleport_ring")
                column(XP, 5350)
                columnRSCM(LOCS, "loc.poh_fairy_ring")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_treering", "loc.poh_fairy_ring")
            }
            row("dbrow.build_poh_superior_garden_teleport_treering") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_teleport_treering")
                column(XP, 11700)
                columnRSCM(LOCS, "loc.poh_spirit_ring")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_treering", "loc.poh_spirit_ring")
            }
            row("dbrow.build_poh_superior_garden_topiary") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_topiary")
                column(XP, 1410)
                columnRSCM(LOCS, "loc.poh_topiary_null")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_topiary", "loc.poh_topiary_null")
            }
            row("dbrow.build_poh_superior_garden_pool_restoration") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_pool_restoration")
                column(XP, 7060)
                columnRSCM(LOCS, "loc.poh_pool_restoration")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_pool", "loc.poh_pool_restoration")
            }
            row("dbrow.build_poh_superior_garden_pool_revitalisation") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_pool_revitalisation")
                column(XP, 8500)
                columnRSCM(LOCS, "loc.poh_pool_revitalisation")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_pool", "loc.poh_pool_revitalisation")
            }
            row("dbrow.build_poh_superior_garden_pool_rejuvenation") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_pool_rejuvenation")
                column(XP, 9000)
                columnRSCM(LOCS, "loc.poh_pool_rejuvenation")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_pool", "loc.poh_pool_rejuvenation")
            }
            row("dbrow.build_poh_superior_garden_pool_recovery") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_pool_recovery")
                column(XP, 19500)
                columnRSCM(LOCS, "loc.poh_pool_recovery")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_pool", "loc.poh_pool_recovery")
            }
            row("dbrow.build_poh_superior_garden_pool_regeneration") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_pool_regeneration")
                column(XP, 31070)
                columnRSCM(LOCS, "loc.poh_pool_regeneration")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_pool", "loc.poh_pool_regeneration")
            }
            row("dbrow.build_poh_superior_garden_theme_zen") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_theme_zen")
                column(XP, 2970)
                columnRSCM(LOCS, "loc.poh_theme_zen_path", "loc.poh_theme_zen_path_corner", "loc.poh_theme_zen_edge", "loc.poh_theme_zen_inner_corner", "loc.poh_theme_zen_outer_corner", "loc.poh_theme_zen_hero")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_theme_outercorner", "loc.poh_theme_zen_outer_corner", "loc.poh_superior_garden_hotspot_theme_innercorner", "loc.poh_theme_zen_inner_corner",
                    "loc.poh_superior_garden_hotspot_theme_edge", "loc.poh_theme_zen_edge", "loc.poh_superior_garden_hotspot_theme_path_1", "loc.poh_theme_zen_path",
                    "loc.poh_superior_garden_hotspot_theme_path_2", "loc.poh_theme_zen_path", "loc.poh_superior_garden_hotspot_theme_path_3", "loc.poh_theme_zen_path",
                    "loc.poh_superior_garden_hotspot_theme_pathcorner", "loc.poh_theme_zen_path_corner", "loc.poh_superior_garden_hotspot_theme_feature", "loc.poh_theme_zen_hero",
                )
            }
            row("dbrow.build_poh_superior_garden_theme_zanaris") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_theme_zanaris")
                column(XP, 3160)
                columnRSCM(LOCS, "loc.poh_theme_zanaris_path", "loc.poh_theme_zanaris_path_corner", "loc.poh_theme_zanaris_edge", "loc.poh_theme_zanaris_inner_corner", "loc.poh_theme_zanaris_outer_corner", "loc.poh_theme_zanaris_hero")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_theme_outercorner", "loc.poh_theme_zanaris_outer_corner", "loc.poh_superior_garden_hotspot_theme_innercorner", "loc.poh_theme_zanaris_inner_corner",
                    "loc.poh_superior_garden_hotspot_theme_edge", "loc.poh_theme_zanaris_edge", "loc.poh_superior_garden_hotspot_theme_path_1", "loc.poh_theme_zanaris_path",
                    "loc.poh_superior_garden_hotspot_theme_path_2", "loc.poh_theme_zanaris_path", "loc.poh_superior_garden_hotspot_theme_path_3", "loc.poh_theme_zanaris_path",
                    "loc.poh_superior_garden_hotspot_theme_pathcorner", "loc.poh_theme_zanaris_path_corner", "loc.poh_superior_garden_hotspot_theme_feature", "loc.poh_theme_zanaris_hero",
                )
            }
            row("dbrow.build_poh_superior_garden_theme_tzhaar") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_theme_tzhaar")
                column(XP, 44640)
                columnRSCM(LOCS, "loc.poh_theme_tzhaar_path_1", "loc.poh_theme_tzhaar_path_2", "loc.poh_theme_tzhaar_path_3", "loc.poh_theme_tzhaar_path_4", "loc.poh_theme_tzhaar_edge", "loc.poh_theme_tzhaar_inner_corner", "loc.poh_theme_tzhaar_outer_corner", "loc.poh_theme_tzhaar_hero")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_theme_outercorner", "loc.poh_theme_tzhaar_outer_corner", "loc.poh_superior_garden_hotspot_theme_innercorner", "loc.poh_theme_tzhaar_inner_corner",
                    "loc.poh_superior_garden_hotspot_theme_edge", "loc.poh_theme_tzhaar_edge", "loc.poh_superior_garden_hotspot_theme_path_1", "loc.poh_theme_tzhaar_path_1",
                    "loc.poh_superior_garden_hotspot_theme_path_2", "loc.poh_theme_tzhaar_path_2", "loc.poh_superior_garden_hotspot_theme_path_3", "loc.poh_theme_tzhaar_path_3",
                    "loc.poh_superior_garden_hotspot_theme_pathcorner", "loc.poh_theme_tzhaar_path_4", "loc.poh_superior_garden_hotspot_theme_feature", "loc.poh_theme_tzhaar_hero",
                )
            }
            row("dbrow.build_poh_superior_garden_redwood_fence") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_redwood_fence")
                column(XP, 2400)
                columnRSCM(LOCS, "loc.poh_redwood_fence_middle", "loc.poh_redwood_fence_post", "loc.poh_redwood_fence_post_m")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_fence_middle", "loc.poh_redwood_fence_middle", "loc.poh_superior_garden_hotspot_fence_post", "loc.poh_redwood_fence_post",
                    "loc.poh_superior_garden_hotspot_fence_post_m", "loc.poh_redwood_fence_post_m",
                )
            }
            row("dbrow.build_poh_superior_garden_obsidian_fence") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_obsidian_fence")
                column(XP, 27410)
                columnRSCM(LOCS, "loc.poh_obsidian_fence_middle", "loc.poh_obsidian_fence_post", "loc.poh_obsidian_fence_post_m")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_fence_middle", "loc.poh_obsidian_fence_middle", "loc.poh_superior_garden_hotspot_fence_post", "loc.poh_obsidian_fence_post",
                    "loc.poh_superior_garden_hotspot_fence_post_m", "loc.poh_obsidian_fence_post_m",
                )
            }
            row("dbrow.build_poh_superior_garden_bench_teak") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_bench_teak")
                column(XP, 5400)
                columnRSCM(LOCS, "loc.poh_garden_bench_teak_left", "loc.poh_garden_bench_teak_right")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_seating_a_left", "loc.poh_garden_bench_teak_left", "loc.poh_superior_garden_hotspot_seating_a_right", "loc.poh_garden_bench_teak_right",
                    "loc.poh_superior_garden_hotspot_seating_b_left", "loc.poh_garden_bench_teak_left", "loc.poh_superior_garden_hotspot_seating_b_right", "loc.poh_garden_bench_teak_right",
                )
            }
            row("dbrow.build_poh_superior_garden_bench_mahogany") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_bench_mahogany")
                column(XP, 8400)
                columnRSCM(LOCS, "loc.poh_garden_bench_gnome_left", "loc.poh_garden_bench_gnome_right")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_seating_a_left", "loc.poh_garden_bench_gnome_left", "loc.poh_superior_garden_hotspot_seating_a_right", "loc.poh_garden_bench_gnome_right",
                    "loc.poh_superior_garden_hotspot_seating_b_left", "loc.poh_garden_bench_gnome_left", "loc.poh_superior_garden_hotspot_seating_b_right", "loc.poh_garden_bench_gnome_right",
                )
            }
            row("dbrow.build_poh_superior_garden_bench_marble") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_bench_marble")
                column(XP, 30000)
                columnRSCM(LOCS, "loc.poh_garden_bench_marble_left", "loc.poh_garden_bench_marble_right")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_seating_a_left", "loc.poh_garden_bench_marble_left", "loc.poh_superior_garden_hotspot_seating_a_right", "loc.poh_garden_bench_marble_right",
                    "loc.poh_superior_garden_hotspot_seating_b_left", "loc.poh_garden_bench_marble_left", "loc.poh_superior_garden_hotspot_seating_b_right", "loc.poh_garden_bench_marble_right",
                )
            }
            row("dbrow.build_poh_superior_garden_bench_obsidian") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_bench_obsidian")
                column(XP, 23310)
                columnRSCM(LOCS, "loc.poh_garden_bench_obsidian_left", "loc.poh_garden_bench_obsidian_right")
                columnRSCM(
                    PARTS,
                    "loc.poh_superior_garden_hotspot_seating_a_left", "loc.poh_garden_bench_obsidian_left", "loc.poh_superior_garden_hotspot_seating_a_right", "loc.poh_garden_bench_obsidian_right",
                    "loc.poh_superior_garden_hotspot_seating_b_left", "loc.poh_garden_bench_obsidian_left", "loc.poh_superior_garden_hotspot_seating_b_right", "loc.poh_garden_bench_obsidian_right",
                )
            }
            row("dbrow.build_poh_combat_dummy") {
                columnRSCM(MODEL_OBJ, "obj.poh_combat_dummy")
                column(XP, 6600)
                columnRSCM(LOCS, "loc.poh_combat_dummy")
                columnRSCM(PARTS, "loc.poh_combat_room_6", "loc.poh_combat_dummy")
            }
            row("dbrow.build_poh_combat_dummy_undeadslayer") {
                columnRSCM(MODEL_OBJ, "obj.poh_combat_dummy_undeadslayer")
                column(XP, 2200)
                columnRSCM(LOCS, "loc.poh_combat_dummy_undeadslayer")
                columnRSCM(PARTS, "loc.poh_combat_room_6", "loc.poh_combat_dummy_undeadslayer")
            }
            row("dbrow.build_raids_storage_1") {
                columnRSCM(MODEL_OBJ, "obj.raids_storage_1")
                column(XP, 1500)
                columnRSCM(LOCS, "loc.raids_storage_1")
            }
            row("dbrow.build_raids_storage_2") {
                columnRSCM(MODEL_OBJ, "obj.raids_storage_2")
                column(XP, 1500)
                columnRSCM(LOCS, "loc.raids_storage_2")
            }
            row("dbrow.build_raids_storage_3") {
                columnRSCM(MODEL_OBJ, "obj.raids_storage_3")
                column(XP, 1500)
                columnRSCM(LOCS, "loc.raids_storage_3")
            }
            row("dbrow.build_poh_servant_moneybag") {
                columnRSCM(MODEL_OBJ, "obj.poh_servant_moneybag")
                column(XP, 5950)
                columnRSCM(LOCS, "loc.poh_servant_moneybag")
                columnRSCM(PARTS, "loc.poh_bedroom_7", "loc.poh_servant_moneybag")
            }
            row("dbrow.build_poh_superior_garden_wilderness_obelisk") {
                columnRSCM(MODEL_OBJ, "obj.poh_superior_garden_wilderness_obelisk")
                column(XP, 30000)
                columnRSCM(LOCS, "loc.poh_wilderness_obelisk")
                columnRSCM(PARTS, "loc.poh_superior_garden_hotspot_treering", "loc.poh_wilderness_obelisk")
            }
            row("dbrow.build_poh_rune_dragon") {
                columnRSCM(MODEL_OBJ, "obj.poh_rune_dragon")
                column(XP, 50000)
                columnRSCM(LOCS, "loc.poh_rune_dragon")
                columnRSCM(PARTS, "loc.poh_dungeon_treasure_2", "loc.poh_rune_dragon")
            }
            row("dbrow.build_poh_trophy_vorkath") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_vorkath")
                column(XP, 11030)
                columnRSCM(LOCS, "loc.transportation_icon_alr")
                columnRSCM(PARTS, "loc.poh_hall1_2", "loc.transportation_icon_alr")
            }
            row("dbrow.build_poh_trophy_mythical_cape") {
                columnRSCM(MODEL_OBJ, "obj.poh_trophy_mythical_cape")
                column(XP, 3700)
                columnRSCM(LOCS, "loc.poh_trophy_mythical_cape")
                columnRSCM(PARTS, "loc.poh_hall2_4", "loc.poh_trophy_mythical_cape")
            }
        }
}
