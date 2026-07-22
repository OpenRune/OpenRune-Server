package org.rsmod.content.other.toolbelt.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object ToolbeltTable {
    const val COL_SLOT_INDEX = 0
    const val COL_DISPLAY_NAME = 1
    const val COL_ALLOWED_ITEMS = 2
    const val COL_REMOVABLE = 3
    const val COL_TIERED = 4
    const val COL_SKILL_STAT = 5
    const val COL_REQUIRE_LEVEL = 6
    const val COL_CATEGORY = 7
    const val COL_UNLOCK_HINT = 8
    const val COL_HAS_SETTINGS = 9
    const val COL_UNLOCK_BIT = 10
    const val COL_STORE_VARBIT = 11

    fun slots() =
        dbTable("dbtable.toolbelt_slots", serverOnly = false) {
            column("slot_index", COL_SLOT_INDEX, VarType.INT)
            column("display_name", COL_DISPLAY_NAME, VarType.STRING)
            column("allowed_items", COL_ALLOWED_ITEMS, VarType.OBJ)
            column("removable", COL_REMOVABLE, VarType.BOOLEAN)
            column("tiered", COL_TIERED, VarType.BOOLEAN)
            column("skill_stat", COL_SKILL_STAT, VarType.STAT)
            column("require_level", COL_REQUIRE_LEVEL, VarType.INT)
            column("category", COL_CATEGORY, VarType.INT)
            column("unlock_hint", COL_UNLOCK_HINT, VarType.STRING)
            column("has_settings", COL_HAS_SETTINGS, VarType.BOOLEAN)
            column("unlock_bit", COL_UNLOCK_BIT, VarType.INT)
            column("store_varbit", COL_STORE_VARBIT, VarType.VARP)

            row("dbrow.toolbelt_pickaxe") {
                column(COL_SLOT_INDEX, 0)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_0")
                column(COL_DISPLAY_NAME, "Pickaxe")
                columnRSCM(
                    COL_ALLOWED_ITEMS,
                    "obj.bronze_pickaxe",
                    "obj.iron_pickaxe",
                    "obj.steel_pickaxe",
                    "obj.black_pickaxe",
                    "obj.mithril_pickaxe",
                    "obj.adamant_pickaxe",
                    "obj.rune_pickaxe",
                    "obj.dragon_pickaxe",
                    "obj.dragon_pickaxe_pretty",
                    "obj.infernal_pickaxe",
                    "obj.infernal_pickaxe_empty",
                    "obj.crystal_pickaxe",
                    "obj.crystal_pickaxe_inactive",
                    "obj.3a_pickaxe",
                    "obj.trailblazer_pickaxe",
                    "obj.trailblazer_pickaxe_empty",
                    "obj.trailblazer_pickaxe_no_infernal",
                    "obj.trailblazer_reloaded_pickaxe",
                    "obj.trailblazer_reloaded_pickaxe_empty",
                    "obj.trailblazer_reloaded_pickaxe_no_infernal",
                )
                column(COL_REMOVABLE, true)
                column(COL_TIERED, true)
                columnRSCM(COL_SKILL_STAT, "stat.mining")
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_axe") {
                column(COL_SLOT_INDEX, 1)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_1")
                column(COL_DISPLAY_NAME, "Axe")
                columnRSCM(
                    COL_ALLOWED_ITEMS,
                    "obj.bronze_axe",
                    "obj.iron_axe",
                    "obj.steel_axe",
                    "obj.black_axe",
                    "obj.mithril_axe",
                    "obj.adamant_axe",
                    "obj.rune_axe",
                    "obj.dragon_axe",
                    "obj.infernal_axe",
                    "obj.infernal_axe_empty",
                    "obj.crystal_axe",
                    "obj.crystal_axe_inactive",
                    "obj.3a_axe",
                    "obj.trailblazer_axe",
                    "obj.trailblazer_axe_empty",
                    "obj.trailblazer_axe_no_infernal",
                    "obj.trailblazer_reloaded_axe",
                    "obj.trailblazer_reloaded_axe_empty",
                    "obj.trailblazer_reloaded_axe_no_infernal",
                )
                column(COL_REMOVABLE, true)
                column(COL_TIERED, true)
                columnRSCM(COL_SKILL_STAT, "stat.woodcutting")
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_hammer") {
                column(COL_SLOT_INDEX, 2)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_2")
                column(COL_DISPLAY_NAME, "Hammer")
                columnRSCM(
                    COL_ALLOWED_ITEMS,
                    "obj.hammer",
                    "obj.imcando_hammer",
                    "obj.imcando_hammer_offhand",
                )
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_chisel") {
                column(COL_SLOT_INDEX, 3)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_3")
                column(COL_DISPLAY_NAME, "Chisel")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.chisel")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_knife") {
                column(COL_SLOT_INDEX, 4)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_4")
                column(COL_DISPLAY_NAME, "Knife")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.knife")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_tinderbox") {
                column(COL_SLOT_INDEX, 5)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_5")
                column(COL_DISPLAY_NAME, "Tinderbox")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.tinderbox")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_pestle_and_mortar") {
                column(COL_SLOT_INDEX, 6)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_6")
                column(COL_DISPLAY_NAME, "Pestle and mortar")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.pestle_and_mortar")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_spade") {
                column(COL_SLOT_INDEX, 7)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_7")
                column(COL_DISPLAY_NAME, "Spade")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.spade")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 1)
            }

            row("dbrow.toolbelt_rake") {
                column(COL_SLOT_INDEX, 8)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_8")
                column(COL_DISPLAY_NAME, "Rake")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.rake")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 1)
            }

            row("dbrow.toolbelt_dibber") {
                column(COL_SLOT_INDEX, 9)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_9")
                column(COL_DISPLAY_NAME, "Seed dibber")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.dibber")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 1)
            }

            row("dbrow.toolbelt_secateurs") {
                column(COL_SLOT_INDEX, 10)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_10")
                column(COL_DISPLAY_NAME, "Secateurs")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.secateurs", "obj.fairy_queen_secateurs")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 1)
            }

            row("dbrow.toolbelt_gardening_trowel") {
                column(COL_SLOT_INDEX, 11)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_11")
                column(COL_DISPLAY_NAME, "Gardening trowel")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.gardening_trowel")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 1)
            }

            row("dbrow.toolbelt_watering_can") {
                column(COL_SLOT_INDEX, 12)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_12")
                column(COL_DISPLAY_NAME, "Watering can")
                columnRSCM(
                    COL_ALLOWED_ITEMS,
                    "obj.watering_can_0",
                    "obj.watering_can_1",
                    "obj.watering_can_2",
                    "obj.watering_can_3",
                    "obj.watering_can_4",
                    "obj.watering_can_5",
                    "obj.watering_can_6",
                    "obj.watering_can_7",
                    "obj.watering_can_8",
                    "obj.zeah_wateringcan",
                )
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 1)
            }

            row("dbrow.toolbelt_shears") {
                column(COL_SLOT_INDEX, 13)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_13")
                column(COL_DISPLAY_NAME, "Shears")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.shears")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 0)
            }

            row("dbrow.toolbelt_needle") {
                column(COL_SLOT_INDEX, 14)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_14")
                column(COL_DISPLAY_NAME, "Needle")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.needle")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 3)
            }

            row("dbrow.toolbelt_glassblowing_pipe") {
                column(COL_SLOT_INDEX, 15)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_15")
                column(COL_DISPLAY_NAME, "Glassblowing pipe")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.glassblowingpipe")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 3)
            }

            row("dbrow.toolbelt_small_net") {
                column(COL_SLOT_INDEX, 16)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_16")
                column(COL_DISPLAY_NAME, "Small fishing net")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.net")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 2)
            }

            row("dbrow.toolbelt_big_net") {
                column(COL_SLOT_INDEX, 17)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_17")
                column(COL_DISPLAY_NAME, "Big fishing net")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.big_net")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 2)
            }

            row("dbrow.toolbelt_fishing_rod") {
                column(COL_SLOT_INDEX, 18)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_18")
                column(COL_DISPLAY_NAME, "Fishing rod")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.fishing_rod")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 2)
            }

            row("dbrow.toolbelt_fly_fishing_rod") {
                column(COL_SLOT_INDEX, 19)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_19")
                column(COL_DISPLAY_NAME, "Fly fishing rod")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.fly_fishing_rod")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 2)
            }

            row("dbrow.toolbelt_harpoon") {
                column(COL_SLOT_INDEX, 20)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_20")
                column(COL_DISPLAY_NAME, "Harpoon")
                columnRSCM(
                    COL_ALLOWED_ITEMS,
                    "obj.harpoon",
                    "obj.infernal_harpoon",
                    "obj.infernal_harpoon_empty",
                )
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 2)
            }

            row("dbrow.toolbelt_lobster_pot") {
                column(COL_SLOT_INDEX, 21)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_21")
                column(COL_DISPLAY_NAME, "Lobster pot")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.lobster_pot")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 2)
            }

            row("dbrow.toolbelt_amulet_mould") {
                column(COL_SLOT_INDEX, 22)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_22")
                column(COL_DISPLAY_NAME, "Amulet mould")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.amulet_mould")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 3)
            }

            row("dbrow.toolbelt_necklace_mould") {
                column(COL_SLOT_INDEX, 23)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_23")
                column(COL_DISPLAY_NAME, "Necklace mould")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.necklace_mould")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 3)
            }

            row("dbrow.toolbelt_ring_mould") {
                column(COL_SLOT_INDEX, 24)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_24")
                column(COL_DISPLAY_NAME, "Ring mould")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.ring_mould")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 3)
            }

            row("dbrow.toolbelt_bracelet_mould") {
                column(COL_SLOT_INDEX, 25)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_25")
                column(COL_DISPLAY_NAME, "Bracelet mould")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.jewl_bracelet_mould")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 3)
            }

            row("dbrow.toolbelt_tiara_mould") {
                column(COL_SLOT_INDEX, 26)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_26")
                column(COL_DISPLAY_NAME, "Tiara mould")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.tiara_mould")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 3)
            }

            row("dbrow.toolbelt_bonecrusher") {
                column(COL_SLOT_INDEX, 27)
                columnRSCM(COL_STORE_VARBIT, "varp.toolbelt_slot_27")
                column(COL_DISPLAY_NAME, "Bonecrusher")
                columnRSCM(COL_ALLOWED_ITEMS, "obj.bonecrusher")
                column(COL_REMOVABLE, true)
                column(COL_TIERED, false)
                column(COL_CATEGORY, 4)
                columnRSCM(COL_SKILL_STAT, "stat.prayer")
                column(COL_REQUIRE_LEVEL, 40)
                column(
                    COL_UNLOCK_HINT,
                    "Speak to the Ghost Disciple at the Ectofuntus while carrying a bonecrusher and pay 2,000 Ectotokens to unlock adding it to your tool belt.",
                )
                column(COL_HAS_SETTINGS, true)
                column(COL_UNLOCK_BIT, 0)
            }
        }
}
