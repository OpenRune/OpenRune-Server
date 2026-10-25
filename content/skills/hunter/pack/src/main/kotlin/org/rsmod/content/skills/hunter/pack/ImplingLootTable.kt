package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object ImplingLootTable {
    const val JAR = 0
    const val LOOT = 1
    const val NOTHING_RATE = 2

    const val PRE_ROLL = 0
    const val MAIN = 1
    const val TERTIARY = 2

    private fun drop(roll: Int, obj: String, min: Int, max: Int, noted: Boolean, num: Int, den: Int) =
        listOf(roll, ConstantProvider.getMapping(obj), min, max, noted, num, den)

    private fun loot(vararg drops: List<Any>) = drops.toList().flatten()

    fun implingLoot() =
        dbTable("dbtable.impling_loot", serverOnly = true) {
            column("jar", JAR, VarType.OBJ)
            column(
                "loot",
                LOOT,
                VarType.INT,
                VarType.OBJ,
                VarType.INT,
                VarType.INT,
                VarType.BOOLEAN,
                VarType.INT,
                VarType.INT,
            )
            column("nothing_rate", NOTHING_RATE, VarType.INT)

            row("dbrow.impling_loot_baby") {
                columnRSCM(JAR, "obj.ii_captured_impling_1")
                column(NOTHING_RATE, 10)
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.chisel", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.thread", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.needle", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.knife", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.cheese", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.hammer", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.ball_of_wool", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.anchovies", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.spicespot", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.flax", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.mud_pie", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.seaweed", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.air_talisman", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.silver_bar", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.sapphire", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.hard_leather", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.lobster", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.softclay", 1, 1, false, 1, 100),
                        drop(TERTIARY, "obj.trail_clue_beginner", 1, 1, false, 1, 50),
                        drop(TERTIARY, "obj.trail_clue_easy_simple001", 1, 1, false, 1, 100),
                    ),
                )
            }

            row("dbrow.impling_loot_young") {
                columnRSCM(JAR, "obj.ii_captured_impling_2")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.nails", 5, 5, false, 1, 10),
                        drop(MAIN, "obj.lockpick", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.blankrune_high", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.tuna", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.chocolate_slice", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.steel_axe", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.meat_pizza", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.coal", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.bow_string", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.snape_grass", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.softclay", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.studded_chaps", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.steel_full_helm", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.plank_oak", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.3dose1defense", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.mithril_bar", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.yew_longbow", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.garden_pie", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.jangerberries", 1, 1, false, 1, 100),
                        drop(TERTIARY, "obj.trail_clue_beginner", 1, 1, false, 1, 25),
                        drop(TERTIARY, "obj.trail_clue_easy_simple001", 1, 1, false, 1, 50),
                    ),
                )
            }

            row("dbrow.impling_loot_gourmet") {
                columnRSCM(JAR, "obj.ii_captured_impling_3")
                column(
                    LOOT,
                    loot(
                        drop(PRE_ROLL, "obj.hosdun_grubby_key", 1, 1, false, 1, 500),
                        drop(MAIN, "obj.tuna", 1, 1, false, 1, 5),
                        drop(MAIN, "obj.bass", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.curry", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.meat_pie", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.chocolate_cake", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.giant_frogspawn", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.spicespot", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.curry_leaf", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.ugthanki_kebab", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.lobster", 4, 4, true, 1, 100),
                        drop(MAIN, "obj.shark", 3, 3, true, 1, 100),
                        drop(MAIN, "obj.fish_pie", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.chefs_delight", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.hunting_fish_special", 5, 5, true, 1, 100),
                        drop(MAIN, "obj.garden_pie", 6, 6, true, 1, 100),
                        drop(MAIN, "obj.swordfish", 3, 3, true, 1, 100),
                        drop(MAIN, "obj.basket_strawberry_5", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.tbwt_cooked_karambwan", 2, 2, true, 1, 100),
                        drop(TERTIARY, "obj.trail_clue_easy_simple001", 1, 1, false, 1, 25),
                    ),
                )
            }

            row("dbrow.impling_loot_earth") {
                columnRSCM(JAR, "obj.ii_captured_impling_4")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.fire_talisman", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.earth_talisman", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.tiara_earth", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.earthrune", 32, 32, false, 1, 10),
                        drop(MAIN, "obj.mithril_ore", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.bucket_sand", 4, 4, true, 1, 10),
                        drop(MAIN, "obj.unicorn_horn", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.bucket_compost", 6, 6, true, 1, 10),
                        drop(MAIN, "obj.gold_ore", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.steel_bar", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.mithril_pickaxe", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.wildblood_hop_seed", 2, 2, false, 1, 100),
                        drop(MAIN, "obj.jangerberry_bush_seed", 2, 2, false, 1, 100),
                        drop(MAIN, "obj.bucket_supercompost", 2, 2, true, 1, 100),
                        drop(MAIN, "obj.mithril_ore", 3, 3, true, 1, 100),
                        drop(MAIN, "obj.harralander_seed", 2, 2, false, 1, 100),
                        drop(MAIN, "obj.coal", 6, 6, true, 1, 100),
                        drop(MAIN, "obj.emerald", 2, 2, true, 1, 100),
                        drop(MAIN, "obj.ruby", 1, 1, false, 1, 100),
                        drop(TERTIARY, "obj.trail_clue_medium_sextant001", 1, 1, false, 1, 100),
                    ),
                )
            }

            row("dbrow.impling_loot_essence") {
                columnRSCM(JAR, "obj.ii_captured_impling_5")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.blankrune_high", 20, 20, true, 1, 10),
                        drop(MAIN, "obj.waterrune", 30, 30, false, 1, 10),
                        drop(MAIN, "obj.airrune", 30, 30, false, 1, 10),
                        drop(MAIN, "obj.firerune", 50, 50, false, 1, 10),
                        drop(MAIN, "obj.mindrune", 25, 25, false, 1, 10),
                        drop(MAIN, "obj.bodyrune", 28, 28, false, 1, 10),
                        drop(MAIN, "obj.chaosrune", 4, 4, false, 1, 10),
                        drop(MAIN, "obj.cosmicrune", 4, 4, false, 1, 10),
                        drop(MAIN, "obj.mind_talisman", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.blankrune_high", 35, 35, true, 1, 100),
                        drop(MAIN, "obj.lavarune", 4, 4, false, 1, 100),
                        drop(MAIN, "obj.mudrune", 4, 4, false, 1, 100),
                        drop(MAIN, "obj.smokerune", 4, 4, false, 1, 100),
                        drop(MAIN, "obj.steamrune", 4, 4, false, 1, 100),
                        drop(MAIN, "obj.deathrune", 13, 13, false, 1, 100),
                        drop(MAIN, "obj.lawrune", 13, 13, false, 1, 100),
                        drop(MAIN, "obj.bloodrune", 7, 7, false, 1, 100),
                        drop(MAIN, "obj.soulrune", 11, 11, false, 1, 100),
                        drop(MAIN, "obj.naturerune", 13, 13, false, 1, 100),
                        drop(TERTIARY, "obj.trail_clue_medium_sextant001", 1, 1, false, 1, 50),
                    ),
                )
            }

            row("dbrow.impling_loot_eclectic") {
                columnRSCM(JAR, "obj.ii_captured_impling_6")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.mithril_pickaxe", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.curry_leaf", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.snape_grass", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.airrune", 30, 58, false, 1, 10),
                        drop(MAIN, "obj.plank_oak", 4, 4, true, 1, 10),
                        drop(MAIN, "obj.candle_lantern_empty", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.gold_ore", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.gold_bar", 5, 5, true, 1, 10),
                        drop(MAIN, "obj.unicorn_horn", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.adamant_kiteshield", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.blue_dragonhide_chaps", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.hunting_red_spiked_vambraces", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.rune_dagger", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.battlestaff", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.adamantite_ore", 10, 10, true, 1, 100),
                        drop(MAIN, "obj.slayers_respite", 2, 2, true, 1, 100),
                        drop(MAIN, "obj.wild_pie", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.watermelon_seed", 3, 3, false, 1, 100),
                        drop(MAIN, "obj.diamond", 1, 1, false, 1, 100),
                        drop(TERTIARY, "obj.trail_clue_medium_sextant001", 1, 1, false, 1, 25),
                    ),
                )
            }

            row("dbrow.impling_loot_nature") {
                columnRSCM(JAR, "obj.ii_captured_impling_7")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.limpwurt_seed", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.jangerberry_bush_seed", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.belladonna_seed", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.harralander_seed", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.cactus_spine", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.magic_logs", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.tarromin", 4, 4, true, 1, 10),
                        drop(MAIN, "obj.coconut", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.irit_seed", 1, 1, false, 1, 10),
                        drop(MAIN, "obj.curry_tree_seed", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.orange_tree_seed", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.snapdragon", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.kwuarm_seed", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.avantoe_seed", 5, 5, false, 1, 100),
                        drop(MAIN, "obj.willow_seed", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.torstol_seed", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.ranarr_seed", 1, 1, false, 1, 100),
                        drop(MAIN, "obj.torstol", 2, 2, true, 1, 100),
                        drop(MAIN, "obj.dwarf_weed_seed", 1, 1, false, 1, 100),
                        drop(TERTIARY, "obj.trail_clue_hard_map001", 1, 1, false, 1, 100),
                    ),
                )
            }

            row("dbrow.impling_loot_magpie") {
                columnRSCM(JAR, "obj.ii_captured_impling_8")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.dragonhide_black", 6, 6, true, 2, 21),
                        drop(MAIN, "obj.strung_diamond_amulet", 3, 3, true, 1, 21),
                        drop(MAIN, "obj.amulet_of_power", 3, 3, true, 1, 21),
                        drop(MAIN, "obj.ring_of_forging", 3, 3, true, 1, 21),
                        drop(MAIN, "obj.splitbark_gauntlets", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.mystic_boots", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.mystic_gloves", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.rune_warhammer", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.ring_of_life", 4, 4, true, 1, 21),
                        drop(MAIN, "obj.rune_sq_shield", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.dragon_dagger", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.tiara_nature", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.runite_bar", 2, 2, true, 1, 21),
                        drop(MAIN, "obj.diamond", 4, 4, true, 1, 21),
                        drop(MAIN, "obj.pineapple_tree_seed", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.ring_of_recoil", 3, 3, true, 1, 21),
                        drop(MAIN, "obj.keyhalf2", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.keyhalf1", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.snapdragon_seed", 1, 1, false, 1, 21),
                        drop(MAIN, "obj.sinister_key", 1, 1, false, 1, 21),
                        drop(TERTIARY, "obj.trail_clue_hard_map001", 1, 1, false, 1, 50),
                    ),
                )
            }

            row("dbrow.impling_loot_ninja") {
                columnRSCM(JAR, "obj.ii_captured_impling_9")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.snakeskin_boots", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.splitbark_helm", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.mystic_boots", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.rune_chainbody", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.mystic_gloves", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.machette_opal", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.rune_claws", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.rune_scimitar", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.dragon_dagger_p+", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.rune_arrow", 70, 70, false, 1, 19),
                        drop(MAIN, "obj.rune_dart", 70, 70, false, 1, 19),
                        drop(MAIN, "obj.rune_knife", 40, 40, false, 1, 19),
                        drop(MAIN, "obj.rune_thrownaxe", 50, 50, false, 1, 19),
                        drop(MAIN, "obj.xbows_crossbow_bolts_runite_tipped_onyx", 2, 2, false, 1, 19),
                        drop(MAIN, "obj.xbows_bolt_tips_onyx", 4, 4, false, 1, 19),
                        drop(MAIN, "obj.dragonhide_black", 10, 10, true, 1, 19),
                        drop(MAIN, "obj.3doseprayerrestore", 4, 4, true, 1, 19),
                        drop(MAIN, "obj.weapon_poison+", 4, 4, true, 1, 19),
                        drop(MAIN, "obj.dagganoth_hide", 3, 3, true, 1, 19),
                        drop(TERTIARY, "obj.trail_clue_hard_map001", 1, 1, false, 1, 25),
                    ),
                )
            }

            row("dbrow.impling_loot_dragon") {
                columnRSCM(JAR, "obj.ii_captured_impling_10")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.xbows_bolt_tips_dragonstone", 10, 30, false, 1, 19),
                        drop(MAIN, "obj.xbows_bolt_tips_dragonstone", 36, 36, false, 1, 19),
                        drop(MAIN, "obj.mystic_robe_bottom", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.amulet_of_glory", 3, 3, true, 1, 19),
                        drop(MAIN, "obj.strung_dragonstone_amulet", 2, 2, true, 1, 19),
                        drop(MAIN, "obj.dragon_arrow", 100, 250, false, 1, 19),
                        drop(MAIN, "obj.xbows_crossbow_bolts_runite_tipped_dragonstone", 10, 40, false, 1, 19),
                        drop(MAIN, "obj.dragon_longsword", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.dragon_dagger_p++", 3, 3, true, 1, 19),
                        drop(MAIN, "obj.dragon_dart", 100, 250, false, 1, 19),
                        drop(MAIN, "obj.dragonstone", 3, 3, true, 1, 19),
                        drop(MAIN, "obj.dragon_dart_tip", 100, 350, false, 1, 19),
                        drop(MAIN, "obj.dragon_arrowheads", 100, 350, false, 1, 19),
                        drop(MAIN, "obj.dragon_javelin_head", 25, 35, false, 1, 19),
                        drop(MAIN, "obj.babydragon_bones", 100, 300, true, 1, 19),
                        drop(MAIN, "obj.dragon_bones", 50, 100, true, 1, 19),
                        drop(MAIN, "obj.magic_tree_seed", 1, 1, false, 1, 19),
                        drop(MAIN, "obj.snapdragon_seed", 6, 6, false, 1, 19),
                        drop(MAIN, "obj.summer_pie", 15, 15, true, 1, 19),
                        drop(TERTIARY, "obj.trail_elite_emote_exp1", 1, 1, false, 1, 50),
                    ),
                )
            }

            row("dbrow.impling_loot_crystal") {
                columnRSCM(JAR, "obj.ii_captured_impling_12")
                column(
                    LOOT,
                    loot(
                        drop(MAIN, "obj.amulet_of_power", 5, 7, true, 1, 18),
                        drop(MAIN, "obj.crystal_tree_seed", 1, 1, false, 1, 18),
                        drop(MAIN, "obj.prif_crystal_shard", 30, 40, false, 1, 18),
                        drop(MAIN, "obj.strung_dragonstone_amulet", 1, 1, false, 1, 18),
                        drop(MAIN, "obj.dragonstone", 2, 2, true, 1, 18),
                        drop(MAIN, "obj.xbows_bolt_tips_ruby", 50, 125, false, 1, 18),
                        drop(MAIN, "obj.xbows_bolt_tips_onyx", 6, 10, false, 1, 18),
                        drop(MAIN, "obj.rune_arrowheads", 150, 250, false, 1, 18),
                        drop(MAIN, "obj.rune_arrow", 400, 750, false, 1, 18),
                        drop(MAIN, "obj.rune_javelin_head", 20, 60, false, 1, 18),
                        drop(MAIN, "obj.rune_dart_tip", 25, 75, false, 1, 18),
                        drop(MAIN, "obj.rune_dart", 50, 100, false, 1, 18),
                        drop(MAIN, "obj.dragon_dart_tip", 10, 15, false, 1, 18),
                        drop(MAIN, "obj.dragon_dagger", 2, 2, true, 1, 18),
                        drop(MAIN, "obj.rune_scimitar", 3, 6, true, 1, 18),
                        drop(MAIN, "obj.babydragon_bones", 75, 125, true, 1, 18),
                        drop(MAIN, "obj.ranarr_seed", 3, 8, false, 1, 18),
                        drop(MAIN, "obj.yew_seed", 1, 1, false, 1, 18),
                        drop(TERTIARY, "obj.trail_elite_emote_exp1", 1, 1, false, 1, 50),
                        drop(TERTIARY, "obj.elven_signet", 1, 1, false, 1, 128),
                    ),
                )
            }
        }
}
