package org.rsmod.content.skills.crafting.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.util.VarType
import dev.openrune.tables.production.ProductionColumns
import dev.openrune.tables.production.ProductionTableRowScope
import dev.openrune.tables.production.ProductionTableScope
import dev.openrune.tables.production.productionTable
import dev.openrune.tables.skills.QuestReq
import dev.openrune.tables.skills.VarbitCompare

/**
 * Columns that aren't specified defer to the section default. crafting_facilities and
 * crafting_hand share one column layout.
 *
 * Gold/Silver and Tanning have custom interface columns since those recipes map onto interfaces.
 *
 * Some columns accept more than one value:
 * - `anim`/`spotanim`: played one per craft, in order, cycling back to the first once the list runs
 *   out.
 * - `ticks`: per-craft timings for a batch. `1, 3, 2` makes the first craft take a tick, the second
 *   three, and the third onwards two - the last entry carries the rest of the batch.
 * - `tool`: every obj listed must be held or worn.
 * - `confirm_title`: each entry is one prompt, asked in order, and all of them must be confirmed.
 *   Second and later prompts reverse their options.
 * - `quest_req`: a quest key followed by a [QuestReq] id. Repeat the pair to require more than one
 *   quest. Resolved through the quest manager, so the server's quest-requirement mode applies.
 * - `unlock_varbit`: a varbit, a [VarbitCompare] id, and the value to compare against. Use it for
 *   non-quest unlocks such as Slayer rewards.
 *
 * A recipe whose `quest_req` and `unlock_varbit` entries don't all pass is hidden, so two recipes
 * for one output can differ by gate alone. This is how the slayer helmet gains its reinforced
 * goggles after A Porcine of Interest. The `locked_message` column is used when a requirement is missing.
 *
 * Optional columns:
 * - `success_low`/`success_high`: chance numerators out of 256 at levels 1 and 99 (see MathSkillUtiils.computeSkillingSuccess.
 *   Higher above 256 means success is guaranteed before 99). When omitted, the craft can never fail.
 * - `fail_xp`/`fail_item`: experience (tenths) and obj produced on a failed roll.
 * - `anim`/`spotanim`: The (spot) animation gameval, played on the player. A facility's own
 *   animation is unaffected.
 * - `ticks`: Craft time in ticks. A 0 means no pacing at all - a held recipe then crafts once per
 *   click like limestone, and any other kind crafts the whole requested amount in one cycle.
 * - `triggers`: for combines, the inputs that may be clicked on one another to start the craft. Omit
 *   it to make every input a trigger and list them only to exclude some (the bone staff's chaos
 *   runes), which stay consumed but inert to clicks.
 * - `xp_extra`: Experience paid out to any second skill (see noxious halberd for an example).
 * - `tool`: an obj that must be held but is never consumed, added to the section's tool list. The
 *   silver/gold moulds are declared here.
 * - `confirm_title`: shows a yes/no prompt titled with it before the craft runs. Its presence is
 *   what turns the confirmation flow on.
 * - `confirm_warning`: an optional message box shown ahead of the first `confirm_title` prompt.
 * - `result_dialogue`: an item box shown on a successful craft in place of the section's success
 *   line. Works with or without a confirmation.
 * - `sound`/`message`/`action_name`: Recipe overrides of the section's craft sound, success message,
 * and level-gate phrase. `{input}`/`{output}` will interpolate.
 *
 */
object Crafting {
    const val COL_SECTION = 7

    const val COL_SUCCESS_LOW = 8
    const val COL_SUCCESS_HIGH = 9

    const val COL_TICKS = 10

    const val COL_FAIL_XP = 11
    const val COL_FAIL_ITEM = 12
    const val COL_ANIM = 13
    const val COL_SPOTANIM = 14
    const val COL_TRIGGERS = 15
    const val COL_XP_EXTRA = 16

    const val COL_TOOL = 17

    const val COL_SOUND = 18

    const val COL_SPAM_MESSAGE = 19

    const val COL_ACTION_NAME = 20

    const val COL_CONFIRM_TITLE = 21
    const val COL_CONFIRM_WARNING = 22
    const val COL_RESULT_DIALOGUE = 23

    const val COL_QUEST_REQ = 24
    const val COL_UNLOCK_VARBIT = 25
    const val COL_LOCKED_MESSAGE = 26

    const val COL_INTERFACE_COMPONENT = 27
    const val COL_INTERFACE_SLOT = 28

    const val COL_TAN_COST = 7
    const val COL_TAN_SLOT_LETTER = 8
    const val COL_TAN_SLOT_LABEL = 9

    fun facilities() = craftingTable("dbtable.crafting_facilities") {
        section("Spinning", category = "Spin") {
            row("dbrow.crafting_spin_ball_of_wool") {
                production {
                    input("obj.wool")
                    statReq("stat.crafting", 1)
                    xp(25)
                    output("obj.ball_of_wool")
                }
            }
            row("dbrow.crafting_spin_bow_string") {
                production {
                    input("obj.flax")
                    statReq("stat.crafting", 10)
                    xp(150)
                    output("obj.bow_string")
                }
            }
            row("dbrow.crafting_spin_crossbow_string") {
                production {
                    input("obj.xbows_sinew")
                    statReq("stat.crafting", 10)
                    xp(150)
                    output("obj.xbows_crossbow_string")
                }
            }
            row("dbrow.crafting_spin_crossbow_string_roots") {
                production {
                    input("obj.oak_roots")
                    statReq("stat.crafting", 10)
                    xp(150)
                    output("obj.xbows_crossbow_string")
                }
            }
            row("dbrow.crafting_spin_magic_string") {
                production {
                    input("obj.magic_roots")
                    statReq("stat.crafting", 19)
                    xp(300)
                    output("obj.magic_string")
                }
            }
            row("dbrow.crafting_spin_rope") {
                production {
                    input("obj.yak_hair")
                    statReq("stat.crafting", 30)
                    xp(250)
                    output("obj.rope")
                }
            }
            row("dbrow.crafting_spin_linen_yarn") {
                production {
                    input("obj.flax")
                    statReq("stat.crafting", 12)
                    xp(160)
                    output("obj.linen_yarn")
                }
            }
            row("dbrow.crafting_spin_hemp_yarn") {
                production {
                    input("obj.hemp")
                    statReq("stat.crafting", 39)
                    xp(600)
                    output("obj.hemp_yarn")
                }
            }
            row("dbrow.crafting_spin_cotton_yarn") {
                production {
                    input("obj.cotton_boll")
                    statReq("stat.crafting", 73)
                    xp(1050)
                    output("obj.cotton_yarn")
                }
            }
        }
        section("Weaving", category = "Weave") {
            row("dbrow.crafting_weave_strip_of_cloth") {
                production {
                    input("obj.ball_of_wool", 4)
                    statReq("stat.crafting", 10)
                    xp(120)
                    output("obj.regicide_cloth")
                }
            }
            row("dbrow.crafting_weave_bolt_of_linen") {
                production {
                    input("obj.linen_yarn", 2)
                    statReq("stat.crafting", 12)
                    xp(200)
                    output("obj.bolt_of_linen")
                }
            }
            row("dbrow.crafting_weave_empty_sack") {
                production {
                    input("obj.jute_fibre", 4)
                    statReq("stat.crafting", 21)
                    xp(380)
                    output("obj.sack_empty")
                }
            }
            row("dbrow.crafting_weave_drift_net") {
                production {
                    input("obj.jute_fibre", 2)
                    statReq("stat.crafting", 26)
                    xp(550)
                    output("obj.fossil_drift_net")
                }
            }
            row("dbrow.crafting_weave_basket") {
                production {
                    input("obj.willow_branch", 6)
                    statReq("stat.crafting", 36)
                    xp(560)
                    output("obj.basket_empty")
                }
                column(COL_TICKS, 4)
            }
            row("dbrow.crafting_weave_bolt_of_canvas") {
                production {
                    input("obj.hemp_yarn", 2)
                    statReq("stat.crafting", 39)
                    xp(750)
                    output("obj.bolt_of_canvas")
                }
            }
            row("dbrow.crafting_weave_bolt_of_cotton") {
                production {
                    input("obj.cotton_yarn", 2)
                    statReq("stat.crafting", 73)
                    xp(1320)
                    output("obj.bolt_of_cotton")
                }
            }
        }
        section("PotteryShaping", category = "Shape") {
            row("dbrow.crafting_shape_pot") {
                production {
                    input("obj.softclay")
                    statReq("stat.crafting", 1)
                    xp(63)
                    output("obj.pot_unfired")
                }
            }
            row("dbrow.crafting_shape_cup") {
                production {
                    input("obj.softclay")
                    statReq("stat.crafting", 3)
                    xp(85)
                    output("obj.cup_unfired", 4)
                }
            }
            row("dbrow.crafting_shape_pie_dish") {
                production {
                    input("obj.softclay")
                    statReq("stat.crafting", 7)
                    xp(150)
                    output("obj.piedish_unfired")
                }
            }
            row("dbrow.crafting_shape_bowl") {
                production {
                    input("obj.softclay")
                    statReq("stat.crafting", 8)
                    xp(180)
                    output("obj.bowl_unfired")
                }
            }
            row("dbrow.crafting_shape_plant_pot") {
                production {
                    input("obj.softclay")
                    statReq("stat.crafting", 19)
                    xp(200)
                    output("obj.plantpot_unfired")
                }
            }

            row("dbrow.crafting_shape_pot_lid") {
                production {
                    input("obj.softclay")
                    statReq("stat.crafting", 25)
                    xp(200)
                    output("obj.potlid_unfired")
                }
            }
        }
        section("PotteryFiring", category = "Fire") {
            row("dbrow.crafting_fire_pot") {
                production {
                    input("obj.pot_unfired")
                    statReq("stat.crafting", 1)
                    xp(63)
                    output("obj.pot_empty")
                }
                column(COL_SUCCESS_LOW, 180)
                column(COL_SUCCESS_HIGH, 789)
            }
            row("dbrow.crafting_fire_cup") {
                production {
                    input("obj.cup_unfired")
                    statReq("stat.crafting", 1)
                    xp(85)
                    output("obj.cup_empty")
                }
                column(COL_SUCCESS_LOW, 180)
                column(COL_SUCCESS_HIGH, 789)
            }
            row("dbrow.crafting_fire_pie_dish") {
                production {
                    input("obj.piedish_unfired")
                    statReq("stat.crafting", 7)
                    xp(100)
                    output("obj.piedish")
                }
                column(COL_SUCCESS_LOW, 180)
                column(COL_SUCCESS_HIGH, 789)
            }
            row("dbrow.crafting_fire_bowl") {
                production {
                    input("obj.bowl_unfired")
                    statReq("stat.crafting", 8)
                    xp(150)
                    output("obj.bowl_empty")
                }
                column(COL_SUCCESS_LOW, 180)
                column(COL_SUCCESS_HIGH, 789)
            }
            row("dbrow.crafting_fire_plant_pot") {
                production {
                    input("obj.plantpot_unfired")
                    statReq("stat.crafting", 19)
                    xp(175)
                    output("obj.plantpot_empty")
                }
                column(COL_SUCCESS_LOW, 180)
                column(COL_SUCCESS_HIGH, 789)
            }
            row("dbrow.crafting_fire_pot_lid") {
                production {
                    input("obj.potlid_unfired")
                    statReq("stat.crafting", 25)
                    xp(200)
                    output("obj.potlid")
                }
                column(COL_SUCCESS_LOW, 180)
                column(COL_SUCCESS_HIGH, 789)
            }
        }
        section("GlassSmelting", category = "Smelt") {
            row("dbrow.crafting_molten_glass") {
                production {
                    input("obj.bucket_sand")
                    input("obj.soda_ash")
                    statReq("stat.crafting", 1)
                    xp(200)
                    output("obj.molten_glass")
                    output("obj.bucket_empty")
                }
            }
        }

        section("SandPit", category = "Fill") {
            row("dbrow.crafting_fill_bucket_sand") {
                production {
                    input("obj.bucket_empty")
                    statReq("stat.crafting", 1)
                    xp(0)
                    output("obj.bucket_sand")
                }
            }
        }
    }

    fun hand() = craftingTable("dbtable.crafting_hand") {
        section("Needlework") {
            row("dbrow.crafting_leather_gloves") {
                production {
                    input("obj.leather")
                    statReq("stat.crafting", 1)
                    xp(138)
                    output("obj.leather_gloves")
                    category("Leather")
                }
            }
            row("dbrow.crafting_leather_boots") {
                production {
                    input("obj.leather")
                    statReq("stat.crafting", 7)
                    xp(163)
                    output("obj.leather_boots")
                    category("Leather")
                }
            }
            row("dbrow.crafting_leather_cowl") {
                production {
                    input("obj.leather")
                    statReq("stat.crafting", 9)
                    xp(185)
                    output("obj.leather_cowl")
                    category("Leather")
                }
            }
            row("dbrow.crafting_leather_vambraces") {
                production {
                    input("obj.leather")
                    statReq("stat.crafting", 11)
                    xp(220)
                    output("obj.leather_vambraces")
                    category("Leather")
                }
            }
            row("dbrow.crafting_leather_body") {
                production {
                    input("obj.leather")
                    statReq("stat.crafting", 14)
                    xp(250)
                    output("obj.leather_armour")
                    category("Leather")
                }
            }
            row("dbrow.crafting_leather_chaps") {
                production {
                    input("obj.leather")
                    statReq("stat.crafting", 18)
                    xp(270)
                    output("obj.leather_chaps")
                    category("Leather")
                }
            }
            row("dbrow.crafting_hardleather_body") {
                production {
                    input("obj.hard_leather")
                    statReq("stat.crafting", 28)
                    xp(350)
                    output("obj.hardleather_body")
                    category("Leather")
                }
            }
            row("dbrow.crafting_coif") {
                production {
                    input("obj.leather")
                    statReq("stat.crafting", 38)
                    xp(370)
                    output("obj.coif")
                    category("Leather")
                }
            }
            row("dbrow.crafting_studded_body") {
                production {
                    input("obj.leather_armour")
                    input("obj.studs")
                    statReq("stat.crafting", 41)
                    xp(400)
                    output("obj.studded_body")
                    category("Studded")
                }
            }
            row("dbrow.crafting_studded_chaps") {
                production {
                    input("obj.leather_chaps")
                    input("obj.studs")
                    statReq("stat.crafting", 44)
                    xp(420)
                    output("obj.studded_chaps")
                    category("Studded")
                }
            }

            row("dbrow.crafting_spiky_vambraces") {
                production {
                    input("obj.leather_vambraces")
                    input("obj.huntingbeast_claws")
                    statReq("stat.crafting", 32)
                    xp(55)
                    output("obj.spiked_vambraces")
                    category("Studded")
                }
                column(COL_ANIM, "seq.human_crafting_spikedvambraces")
            }

            row("dbrow.crafting_green_dhide_vambraces") {
                production {
                    input("obj.dragon_leather")
                    statReq("stat.crafting", 57)
                    xp(620)
                    output("obj.dragon_vambraces")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_green_dhide_chaps") {
                production {
                    input("obj.dragon_leather", 2)
                    statReq("stat.crafting", 60)
                    xp(1240)
                    output("obj.dragonhide_chaps")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_green_dhide_body") {
                production {
                    input("obj.dragon_leather", 3)
                    statReq("stat.crafting", 63)
                    xp(1860)
                    output("obj.dragonhide_body")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_blue_dhide_vambraces") {
                production {
                    input("obj.dragon_leather_blue")
                    statReq("stat.crafting", 66)
                    xp(700)
                    output("obj.blue_dragon_vambraces")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_blue_dhide_chaps") {
                production {
                    input("obj.dragon_leather_blue", 2)
                    statReq("stat.crafting", 68)
                    xp(1400)
                    output("obj.blue_dragonhide_chaps")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_blue_dhide_body") {
                production {
                    input("obj.dragon_leather_blue", 3)
                    statReq("stat.crafting", 71)
                    xp(2100)
                    output("obj.blue_dragonhide_body")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_red_dhide_vambraces") {
                production {
                    input("obj.dragon_leather_red")
                    statReq("stat.crafting", 73)
                    xp(780)
                    output("obj.red_dragon_vambraces")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_red_dhide_chaps") {
                production {
                    input("obj.dragon_leather_red", 2)
                    statReq("stat.crafting", 75)
                    xp(1560)
                    output("obj.red_dragonhide_chaps")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_red_dhide_body") {
                production {
                    input("obj.dragon_leather_red", 3)
                    statReq("stat.crafting", 77)
                    xp(2340)
                    output("obj.red_dragonhide_body")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_black_dhide_vambraces") {
                production {
                    input("obj.dragon_leather_black")
                    statReq("stat.crafting", 79)
                    xp(860)
                    output("obj.black_dragon_vambraces")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_black_dhide_chaps") {
                production {
                    input("obj.dragon_leather_black", 2)
                    statReq("stat.crafting", 82)
                    xp(1720)
                    output("obj.black_dragonhide_chaps")
                    category("Dragonhide")
                }
            }
            row("dbrow.crafting_black_dhide_body") {
                production {
                    input("obj.dragon_leather_black", 3)
                    statReq("stat.crafting", 84)
                    xp(2580)
                    output("obj.black_dragonhide_body")
                    category("Dragonhide")
                }
            }

            row("dbrow.crafting_snakeskin_boots") {
                production {
                    input("obj.village_snake_skin", 6)
                    statReq("stat.crafting", 45)
                    xp(300)
                    output("obj.snakeskin_boots")
                    category("Snakeskin")
                }
            }
            row("dbrow.crafting_snakeskin_vambraces") {
                production {
                    input("obj.village_snake_skin", 8)
                    statReq("stat.crafting", 47)
                    xp(350)
                    output("obj.snakeskin_vambraces")
                    category("Snakeskin")
                }
            }
            row("dbrow.crafting_snakeskin_bandana") {
                production {
                    input("obj.village_snake_skin", 5)
                    statReq("stat.crafting", 48)
                    xp(450)
                    output("obj.snakeskin_bandana")
                    category("Snakeskin")
                }
            }
            row("dbrow.crafting_snakeskin_chaps") {
                production {
                    input("obj.village_snake_skin", 12)
                    statReq("stat.crafting", 51)
                    xp(500)
                    output("obj.snakeskin_chaps")
                    category("Snakeskin")
                }
            }
            row("dbrow.crafting_snakeskin_body") {
                production {
                    input("obj.village_snake_skin", 15)
                    statReq("stat.crafting", 53)
                    xp(550)
                    output("obj.snakeskin_body")
                    category("Snakeskin")
                }
            }

            row("dbrow.crafting_yak_legs") {
                production {
                    input("obj.yak_hide_cured")
                    statReq("stat.crafting", 43)
                    xp(320)
                    output("obj.yak_hide_armour_greaves")
                    category("Yak")
                }
            }
            row("dbrow.crafting_yak_top") {
                production {
                    input("obj.yak_hide_cured", 2)
                    statReq("stat.crafting", 46)
                    xp(320)
                    output("obj.yak_hide_armour_body")
                    category("Yak")
                }
            }

            row("dbrow.crafting_xerician_hat") {
                production {
                    input("obj.xeric_fabric", 3)
                    statReq("stat.crafting", 14)
                    xp(660)
                    output("obj.xeric_hat")
                    category("Xerician")
                }
            }
            row("dbrow.crafting_xerician_robe") {
                production {
                    input("obj.xeric_fabric", 4)
                    statReq("stat.crafting", 17)
                    xp(880)
                    output("obj.xeric_robe")
                    category("Xerician")
                }
            }
            row("dbrow.crafting_xerician_top") {
                production {
                    input("obj.xeric_fabric", 5)
                    statReq("stat.crafting", 22)
                    xp(1100)
                    output("obj.xeric_top")
                    category("Xerician")
                }
            }

            row("dbrow.crafting_splitbark_gauntlets") {
                production {
                    input("obj.hollow_bark", 1)
                    input("obj.fine_cloth", 1)
                    statReq("stat.crafting", 60)
                    xp(620)
                    output("obj.splitbark_gauntlets")
                    category("Splitbark")
                }
            }
            row("dbrow.crafting_splitbark_boots") {
                production {
                    input("obj.hollow_bark", 1)
                    input("obj.fine_cloth", 1)
                    statReq("stat.crafting", 60)
                    xp(620)
                    output("obj.splitbark_greaves")
                    category("Splitbark")
                }
            }
            row("dbrow.crafting_splitbark_helm") {
                production {
                    input("obj.hollow_bark", 2)
                    input("obj.fine_cloth", 2)
                    statReq("stat.crafting", 61)
                    xp(1240)
                    output("obj.splitbark_helm")
                    category("Splitbark")
                }
            }
            row("dbrow.crafting_splitbark_legs") {
                production {
                    input("obj.hollow_bark", 3)
                    input("obj.fine_cloth", 3)
                    statReq("stat.crafting", 62)
                    xp(1860)
                    output("obj.splitbark_legs")
                    category("Splitbark")
                }
            }
            row("dbrow.crafting_splitbark_body") {
                production {
                    input("obj.hollow_bark", 4)
                    input("obj.fine_cloth", 4)
                    statReq("stat.crafting", 62)
                    xp(2480)
                    output("obj.splitbark_body")
                    category("Splitbark")
                }
            }

            row("dbrow.crafting_hueycoatl_vambraces") {
                production {
                    input("obj.huey_hide", 1)
                    statReq("stat.crafting", 76)
                    xp(950)
                    output("obj.huey_vambraces")
                    category("Hueycoatl")
                }
            }
            row("dbrow.crafting_hueycoatl_coif") {
                production {
                    input("obj.huey_hide", 2)
                    statReq("stat.crafting", 76)
                    xp(1900)
                    output("obj.huey_coif")
                    category("Hueycoatl")
                }
            }
            row("dbrow.crafting_hueycoatl_chaps") {
                production {
                    input("obj.huey_hide", 2)
                    statReq("stat.crafting", 77)
                    xp(1900)
                    output("obj.huey_chaps")
                    category("Hueycoatl")
                }
            }
            row("dbrow.crafting_hueycoatl_body") {
                production {
                    input("obj.huey_hide", 3)
                    statReq("stat.crafting", 78)
                    xp(2850)
                    output("obj.huey_body")
                    category("Hueycoatl")
                }
            }

            row("dbrow.crafting_mixed_hide_cape") {
                production {
                    input("obj.hg_mixedhide_base")
                    input("obj.varlamore_jaguar_fur")
                    statReq("stat.crafting", 68)
                    xp(620)
                    output("obj.hide_cape")
                    category("MixedHide")
                }
            }
            row("dbrow.crafting_mixed_hide_boots") {
                production {
                    input("obj.hg_mixedhide_base")
                    input("obj.hunting_antelopesun_fur")
                    statReq("stat.crafting", 69)
                    xp(750)
                    output("obj.hide_boots")
                    category("MixedHide")
                }
            }
            row("dbrow.crafting_mixed_hide_legs") {
                production {
                    input("obj.hg_mixedhide_base")
                    input("obj.hunting_fennecfox_fur", 3)
                    statReq("stat.crafting", 71)
                    xp(2100)
                    output("obj.hide_legs")
                    category("MixedHide")
                }
            }
            row("dbrow.crafting_mixed_hide_top") {
                production {
                    input("obj.hg_mixedhide_base")
                    input("obj.hunting_antelopesun_fur", 2)
                    statReq("stat.crafting", 72)
                    xp(1500)
                    output("obj.hide_top")
                    category("MixedHide")
                }
            }
        }

        section("Shields", category = "Shield") {
            row("dbrow.crafting_hard_leather_shield") {
                production {
                    input("obj.hard_leather", 2)
                    input("obj.oak_shield", 1)
                    input("obj.nails_bronze", 15)
                    statReq("stat.crafting", 41)
                    xp(700)
                    output("obj.leather_shield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_leather")
            }
            row("dbrow.crafting_snakeskin_shield") {
                production {
                    input("obj.village_snake_skin", 2)
                    input("obj.willow_shield", 1)
                    input("obj.nails_iron", 15)
                    statReq("stat.crafting", 56)
                    xp(1000)
                    output("obj.snakeskin_shield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_snakeskin")
            }
            row("dbrow.crafting_green_dhide_shield") {
                production {
                    input("obj.dragon_leather", 2)
                    input("obj.maple_shield", 1)
                    input("obj.nails", 15)
                    statReq("stat.crafting", 62)
                    xp(1240)
                    output("obj.green_dhide_shield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_green_dhide")
            }
            row("dbrow.crafting_blue_dhide_shield") {
                production {
                    input("obj.dragon_leather_blue", 2)
                    input("obj.yew_shield", 1)
                    input("obj.nails_mithril", 15)
                    statReq("stat.crafting", 69)
                    xp(1400)
                    output("obj.blue_dhide_shield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_blue_dhide")
            }
            row("dbrow.crafting_red_dhide_shield") {
                production {
                    input("obj.dragon_leather_red", 2)
                    input("obj.magic_shield", 1)
                    input("obj.nails_adamant", 15)
                    statReq("stat.crafting", 76)
                    xp(1560)
                    output("obj.red_dhide_shield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_red_dhide")
            }
            row("dbrow.crafting_black_dhide_shield") {
                production {
                    input("obj.dragon_leather_black", 2)
                    input("obj.redwood_shield", 1)
                    input("obj.nails_rune", 15)
                    statReq("stat.crafting", 83)
                    xp(1720)
                    output("obj.black_dhide_shield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_black_dhide")
            }
            row("dbrow.crafting_broodoo_shield_blue") {
                production {
                    input("obj.village_snake_skin", 2)
                    input("obj.broodoomask_combat", 1)
                    input("obj.nails_bronze", 8)
                    statReq("stat.crafting", 35)
                    xp(1000)
                    output("obj.broodoo_combatshield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_disease")
            }
            row("dbrow.crafting_broodoo_shield_green") {
                production {
                    input("obj.village_snake_skin", 2)
                    input("obj.broodoomask_poison", 1)
                    input("obj.nails_bronze", 8)
                    statReq("stat.crafting", 35)
                    xp(1000)
                    output("obj.broodoo_poisonshield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_poison")
            }
            row("dbrow.crafting_broodoo_shield_orange") {
                production {
                    input("obj.village_snake_skin", 2)
                    input("obj.broodoomask_disease", 1)
                    input("obj.nails_bronze", 8)
                    statReq("stat.crafting", 35)
                    xp(1000)
                    output("obj.broodoo_diseaseshield")
                }
                column(COL_ANIM, "seq.human_shield_crafting_combat")
            }
        }

        section("Carving", category = "Carve") {
            row("dbrow.crafting_snelm_red_pointed") {
                production {
                    input("obj.shellpoint_red+black")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_point_red+black")
                }
            }
            row("dbrow.crafting_snelm_red_round") {
                production {
                    input("obj.shellround_red+black")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_round_red+black")
                }
            }
            row("dbrow.crafting_snelm_bark") {
                production {
                    input("obj.shellround_orange")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_round_orange")
                }
            }
            row("dbrow.crafting_snelm_blue_pointed") {
                production {
                    input("obj.shellpoint_blue")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_point_blue")
                }
            }
            row("dbrow.crafting_snelm_blue_round") {
                production {
                    input("obj.shellround_blue")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_round_blue")
                }
            }
            row("dbrow.crafting_snelm_myre_pointed") {
                production {
                    input("obj.shellpoint_swamp")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_point_swamp")
                }
            }
            row("dbrow.crafting_snelm_myre_round") {
                production {
                    input("obj.shellround_swamp")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_round_swamp")
                }
            }
            row("dbrow.crafting_snelm_ochre_pointed") {
                production {
                    input("obj.shellpoint_yellow")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_point_yellow")
                }
            }
            row("dbrow.crafting_snelm_ochre_round") {
                production {
                    input("obj.shellround_yellow")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.snelm_round_yellow")
                }
            }
            row("dbrow.crafting_crab_helmet") {
                production {
                    input("obj.hundred_pirate_crab_shell_head")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.hundred_pirate_crab_shell_helm")
                }
            }
            row("dbrow.crafting_crab_claw") {
                production {
                    input("obj.hundred_pirate_crab_shell_claw")
                    statReq("stat.crafting", 15)
                    xp(325)
                    output("obj.hundred_pirate_crab_shell_gauntlet")
                }
            }
        }
        section("Knife", category = "Cut") {
            row("dbrow.crafting_dramen_staff") {
                production {
                    input("obj.dramen_branch")
                    statReq("stat.crafting", 31)
                    xp(0)
                    output("obj.dramen_staff")
                }
                column(COL_TICKS, 0)
                column(COL_SPAM_MESSAGE, "You carve the branch into a staff.")
            }

            row("dbrow.crafting_sinew") {
                production {
                    input("obj.damaged_ballista_rope")
                    statReq("stat.crafting", 10)
                    xp(150)
                    output("obj.xbows_sinew")
                }
            }
        }
        section("Gems", category = "Cut") {
            row("dbrow.crafting_cut_opal") {
                production {
                    input("obj.uncut_opal")
                    statReq("stat.crafting", 1)
                    xp(150)
                    output("obj.opal")
                }
                column(COL_ANIM, "seq.human_opalcutting")
                column(COL_SUCCESS_LOW, 100)
                column(COL_SUCCESS_HIGH, 252)
                column(COL_FAIL_XP, 38)
                columnRSCM(COL_FAIL_ITEM, "obj.crushed_gemstone")
            }
            row("dbrow.crafting_cut_jade") {
                production {
                    input("obj.uncut_jade")
                    statReq("stat.crafting", 13)
                    xp(200)
                    output("obj.jade")
                }
                column(COL_ANIM, "seq.human_jadecutting")
                column(COL_SUCCESS_LOW, 120)
                column(COL_SUCCESS_HIGH, 252)
                column(COL_FAIL_XP, 50)
                columnRSCM(COL_FAIL_ITEM, "obj.crushed_gemstone")
            }
            row("dbrow.crafting_cut_red_topaz") {
                production {
                    input("obj.uncut_red_topaz")
                    statReq("stat.crafting", 16)
                    xp(250)
                    output("obj.red_topaz")
                }
                column(COL_ANIM, "seq.human_redtopazcutting")
                column(COL_SUCCESS_LOW, 140)
                column(COL_SUCCESS_HIGH, 252)
                column(COL_FAIL_XP, 63)
                columnRSCM(COL_FAIL_ITEM, "obj.crushed_gemstone")
            }
            row("dbrow.crafting_cut_sapphire") {
                production {
                    input("obj.uncut_sapphire")
                    statReq("stat.crafting", 20)
                    xp(500)
                    output("obj.sapphire")
                }
                column(COL_ANIM, "seq.human_sapphirecutting")
            }
            row("dbrow.crafting_cut_emerald") {
                production {
                    input("obj.uncut_emerald")
                    statReq("stat.crafting", 27)
                    xp(675)
                    output("obj.emerald")
                }
                column(COL_ANIM, "seq.human_emeraldcutting")
            }
            row("dbrow.crafting_cut_ruby") {
                production {
                    input("obj.uncut_ruby")
                    statReq("stat.crafting", 34)
                    xp(850)
                    output("obj.ruby")
                }
                column(COL_ANIM, "seq.human_rubycutting")
            }
            row("dbrow.crafting_cut_diamond") {
                production {
                    input("obj.uncut_diamond")
                    statReq("stat.crafting", 43)
                    xp(1075)
                    output("obj.diamond")
                }
                column(COL_ANIM, "seq.human_diamondcutting")
            }
            row("dbrow.crafting_cut_dragonstone") {
                production {
                    input("obj.uncut_dragonstone")
                    statReq("stat.crafting", 55)
                    xp(1375)
                    output("obj.dragonstone")
                }
                column(COL_ANIM, "seq.human_dragonstonecutting")
            }
            row("dbrow.crafting_cut_onyx") {
                production {
                    input("obj.uncut_onyx")
                    statReq("stat.crafting", 67)
                    xp(1675)
                    output("obj.onyx")
                }
                column(COL_ANIM, "seq.human_onyxcutting")
            }
            row("dbrow.crafting_cut_zenyte") {
                production {
                    input("obj.uncut_zenyte")
                    statReq("stat.crafting", 89)
                    xp(2000)
                    output("obj.zenyte")
                }
                column(COL_ANIM, "seq.human_zenytecutting")
            }
        }
        section("Amethyst", category = "Cut") {
            row("dbrow.crafting_amethyst_bolt_tips") {
                production {
                    input("obj.amethyst")
                    statReq("stat.crafting", 83)
                    xp(600)
                    output("obj.xbows_bolt_tips_amethyst", 15)
                }
            }
            row("dbrow.crafting_amethyst_arrowtips") {
                production {
                    input("obj.amethyst")
                    statReq("stat.crafting", 85)
                    xp(600)
                    output("obj.amethyst_arrowheads", 15)
                }
            }
            row("dbrow.crafting_amethyst_javelin_heads") {
                production {
                    input("obj.amethyst")
                    statReq("stat.crafting", 87)
                    xp(600)
                    output("obj.amethyst_javelin_head", 5)
                }
            }
            row("dbrow.crafting_amethyst_dart_tips") {
                production {
                    input("obj.amethyst")
                    statReq("stat.crafting", 89)
                    xp(600)
                    output("obj.amethyst_dart_tip", 8)
                }
            }
        }
        section("Limestone", category = "Cut") {
            row("dbrow.crafting_limestone_brick") {
                production {
                    input("obj.limestone")
                    statReq("stat.crafting", 12)
                    xp(60)
                    output("obj.limestonebrick")
                }
                column(COL_SUCCESS_LOW, 137)
                column(COL_SUCCESS_HIGH, 434)
                columnRSCM(COL_FAIL_ITEM, "obj.rock")
            }
        }
        section("Glassblowing", category = "Blow") {
            row("dbrow.crafting_glass_beer_glass") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 1)
                    xp(175)
                    output("obj.beer_glass")
                }
            }
            row("dbrow.crafting_glass_candle_lantern") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 4)
                    xp(190)
                    output("obj.candle_lantern_empty")
                }
            }
            row("dbrow.crafting_glass_oil_lamp") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 12)
                    xp(250)
                    output("obj.oil_lamp_empty")
                }
            }
            row("dbrow.crafting_glass_vial") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 33)
                    xp(350)
                    output("obj.vial_empty")
                }
            }
            row("dbrow.crafting_glass_fishbowl") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 42)
                    xp(425)
                    output("obj.fishbowl_empty")
                }
            }
            row("dbrow.crafting_glass_unpowered_orb") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 46)
                    xp(525)
                    output("obj.stafforb")
                }
            }
            row("dbrow.crafting_glass_lantern_lens") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 49)
                    xp(550)
                    output("obj.bullseye_lantern_lens")
                }
            }
            row("dbrow.crafting_glass_light_orb") {
                production {
                    input("obj.molten_glass")
                    statReq("stat.crafting", 87)
                    xp(700)
                    output("obj.dorgesh_lightbulb_nofilament")
                }
            }
        }

        section("Battlestaves", category = "Attach") {
            row("dbrow.crafting_water_battlestaff") {
                production {
                    input("obj.battlestaff")
                    input("obj.water_orb")
                    statReq("stat.crafting", 54)
                    xp(1000)
                    output("obj.water_battlestaff")
                }
                column(COL_SPOTANIM, "spotanim.battlestaff_water_crafting_spotanim")
            }
            row("dbrow.crafting_earth_battlestaff") {
                production {
                    input("obj.battlestaff")
                    input("obj.earth_orb")
                    statReq("stat.crafting", 58)
                    xp(1125)
                    output("obj.earth_battlestaff")
                }
                column(COL_SPOTANIM, "spotanim.battlestaff_earth_crafting_spotanim")
            }
            row("dbrow.crafting_fire_battlestaff") {
                production {
                    input("obj.battlestaff")
                    input("obj.fire_orb")
                    statReq("stat.crafting", 62)
                    xp(1250)
                    output("obj.fire_battlestaff")
                }
                column(COL_SPOTANIM, "spotanim.battlestaff_fire_crafting_spotanim")
            }
            row("dbrow.crafting_air_battlestaff") {
                production {
                    input("obj.battlestaff")
                    input("obj.air_orb")
                    statReq("stat.crafting", 66)
                    xp(1375)
                    output("obj.air_battlestaff")
                }
                column(COL_SPOTANIM, "spotanim.battlestaff_air_crafting_spotanim")
            }
        }

        section("AmuletStringing", category = "String") {
            row("dbrow.crafting_string_gold_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_gold_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_gold_amulet")
                }
            }
            row("dbrow.crafting_string_sapphire_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_sapphire_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_sapphire_amulet")
                }
            }
            row("dbrow.crafting_string_emerald_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_emerald_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_emerald_amulet")
                }
            }
            row("dbrow.crafting_string_ruby_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_ruby_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_ruby_amulet")
                }
            }
            row("dbrow.crafting_string_diamond_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_diamond_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_diamond_amulet")
                }
            }
            row("dbrow.crafting_string_dragonstone_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_dragonstone_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_dragonstone_amulet")
                }
            }
            row("dbrow.crafting_string_onyx_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_onyx_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_onyx_amulet")
                }
            }
            row("dbrow.crafting_string_zenyte_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_zenyte_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.zenyte_amulet")
                }
            }
            row("dbrow.crafting_string_opal_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_opal_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_opal_amulet")
                }
            }
            row("dbrow.crafting_string_jade_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_jade_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_jade_amulet")
                }
            }
            row("dbrow.crafting_string_topaz_amulet") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.unstrung_topaz_amulet")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.strung_topaz_amulet")
                }
            }

            row("dbrow.crafting_string_emblem") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.nostringsnake")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.stringsnake")
                }
            }

            row("dbrow.crafting_string_symbol") {
                production {
                    input("obj.ball_of_wool")
                    input("obj.nostringstar")
                    statReq("stat.crafting", 1)
                    xp(40)
                    output("obj.stringstar")
                }
            }
        }
        section("Birdhouses", category = "Birdhouse") {
            row("dbrow.crafting_birdhouse_normal") {
                production {
                    input("obj.logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 5)
                    xp(150)
                    output("obj.birdhouse_normal")
                }
            }
            row("dbrow.crafting_birdhouse_oak") {
                production {
                    input("obj.oak_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 15)
                    xp(200)
                    output("obj.birdhouse_oak")
                }
            }
            row("dbrow.crafting_birdhouse_willow") {
                production {
                    input("obj.willow_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 25)
                    xp(250)
                    output("obj.birdhouse_willow")
                }
            }
            row("dbrow.crafting_birdhouse_teak") {
                production {
                    input("obj.teak_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 35)
                    xp(300)
                    output("obj.birdhouse_teak")
                }
            }
            row("dbrow.crafting_birdhouse_maple") {
                production {
                    input("obj.maple_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 45)
                    xp(350)
                    output("obj.birdhouse_maple")
                }
            }
            row("dbrow.crafting_birdhouse_mahogany") {
                production {
                    input("obj.mahogany_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 50)
                    xp(400)
                    output("obj.birdhouse_mahogany")
                }
            }
            row("dbrow.crafting_birdhouse_yew") {
                production {
                    input("obj.yew_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 60)
                    xp(450)
                    output("obj.birdhouse_yew")
                }
            }
            row("dbrow.crafting_birdhouse_magic") {
                production {
                    input("obj.magic_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 75)
                    xp(500)
                    output("obj.birdhouse_magic")
                }
            }
            row("dbrow.crafting_birdhouse_redwood") {
                production {
                    input("obj.redwood_logs")
                    input("obj.poh_clockwork_mechanism")
                    statReq("stat.crafting", 90)
                    xp(550)
                    output("obj.birdhouse_redwood")
                }
            }
        }

        section("Combining") {
            row("dbrow.crafting_slayer_helm") {
                production {
                    category("Assembly")
                    input("obj.harmless_black_mask")
                    input("obj.slayer_earmuffs")
                    input("obj.slayer_facemask")
                    input("obj.slayer_nosepeg")
                    input("obj.wallbeast_spike_helmet")
                    input("obj.slayer_gem")
                    statReq("stat.crafting", 55)
                    xp(0)
                    output("obj.slayer_helm")
                }
                column(COL_SPAM_MESSAGE, "You combine the pieces to make a {output}.")
                column(COL_QUEST_REQ, "quest_porcineofinterest", QuestReq.NotCompleted.id)
                column(COL_UNLOCK_VARBIT, "varbit.slayer_helm_unlocked", VarbitCompare.GTE.id, 1)
                column(COL_LOCKED_MESSAGE, "You need to learn how to combine these items first. Speak to a Slayer master about the 'Malevolent masquerade' ability.")
            }

            row("dbrow.crafting_slayer_helm_goggles") {
                production {
                    category("Assembly")
                    input("obj.harmless_black_mask")
                    input("obj.slayer_earmuffs")
                    input("obj.slayer_facemask")
                    input("obj.slayer_nosepeg")
                    input("obj.wallbeast_spike_helmet")
                    input("obj.slayer_gem")
                    input("obj.slayer_reinforced_goggles")
                    statReq("stat.crafting", 55)
                    xp(0)
                    output("obj.slayer_helm")
                }
                column(COL_SPAM_MESSAGE, "You combine the pieces to make a {output}.")
                column(COL_QUEST_REQ, "quest_porcineofinterest", QuestReq.Completed.id)
                column(COL_UNLOCK_VARBIT, "varbit.slayer_helm_unlocked", VarbitCompare.GTE.id, 1)
                column(COL_LOCKED_MESSAGE, "You need to learn how to combine these items first. Speak to a Slayer master about the 'Malevolent masquerade' ability.")
            }

            row("dbrow.crafting_noxious_halberd") {
                production {
                    category("Assembly")
                    input("obj.noxious_halberd_part_1")
                    input("obj.noxious_halberd_part_2")
                    input("obj.noxious_halberd_part_3")
                    statReq("stat.crafting", 72)
                    statReq("stat.smithing", 72)
                    xp(1000)
                    output("obj.noxious_halberd")
                }
                column(COL_ANIM, "seq.human_fletching_noxious_halberd")
                column(COL_XP_EXTRA, ConstantProvider.getMapping("stat.smithing"), 1000)
                column(COL_CONFIRM_TITLE, "Do you wish to create a noxious halberd?")
                column(COL_CONFIRM_WARNING, "Do you wish to combine all three pieces to create a noxious halberd?<br>This process is non-reversible")
                column(COL_RESULT_DIALOGUE, "You successfully create a noxious halberd.")
            }

            row("dbrow.crafting_amulet_of_rancour") {
                production {
                    category("Assembly")
                    input("obj.zenyte_amulet_enchanted")
                    input("obj.araxyte_fang")
                    statReq("stat.crafting", 86)
                    xp(5000)
                    output("obj.amulet_of_rancour")
                }
                column(COL_TICKS, 36)
                column(COL_ANIM, "seq.human_craft_rancor_start", "seq.human_craft_rancor_end")
                column(COL_SPOTANIM, "spotanim.vfx_human_craft_rancor_start", "spotanim.vfx_human_craft_rancor_end")
                column(COL_CONFIRM_WARNING, "Do you wish to use the araxyte fang on your amulet of torture?<br>This process is non-reversible and will consume both items.")
                column(COL_CONFIRM_TITLE, "Do you wish to create a amulet of rancour?")
                column(COL_RESULT_DIALOGUE, "You successfully create an amulet of rancour.")
            }

            row("dbrow.crafting_necklace_of_rupture") {
                production {
                    category("Assembly")
                    input("obj.zenyte_necklace_enchanted")
                    input("obj.etched_elder_venator_fang")
                    statReq("stat.crafting", 84)
                    xp(5000)
                    output("obj.necklace_of_rupture")
                }
                column(COL_TICKS, 36)
                column(COL_ANIM, "seq.human_craft_rupture_start", "seq.human_craft_rupture_end")
                column(COL_SPOTANIM, "spotanim.vfx_human_craft_rupture_start", "spotanim.vfx_human_craft_rupture_end")
                column(COL_CONFIRM_WARNING, "Do you wish to use the etched elder venator fang on your<br>necklace of anguish?<br>This process is non-reversible and will consume both items.")
                column(COL_CONFIRM_TITLE, "Do you wish to create a necklace of rupture?")
                column(COL_RESULT_DIALOGUE, "You successfully create a necklace of rupture.")
            }

            row("dbrow.crafting_confliction_gauntlets") {
                production {
                    category("Assembly")
                    input("obj.zenyte_bracelet_enchanted")
                    input("obj.mokhaiotl_cloth")
                    input("obj.demon_tear", 10000)
                    statReq("stat.crafting", 83)
                    statReq("stat.smithing", 70)
                    xp(5000)
                    output("obj.confliction_gauntlets")
                }
                column(COL_TICKS, 36)
                column(COL_XP_EXTRA, ConstantProvider.getMapping("stat.smithing"), 1000)
                column(COL_ANIM, "seq.human_craft_confliction")
                column(COL_SPOTANIM, "spotanim.spotanim_confliction_craft")
                column(COL_CONFIRM_WARNING, "Do you wish to make a pair of confliction gauntlets<br>" +
                                                    "from the mokhaiotl cloth, the tormented bracelets and<br>" +
                                                    "10,000 demon tears? This process is non-reversible and<br>" +
                                                    "will consume the items used.")
                column(COL_CONFIRM_TITLE, "Do you wish to make a pair of confliction gauntlets?")
                column(COL_RESULT_DIALOGUE, "You carefully craft a pair of confliction gauntlets.")
            }

            row("dbrow.crafting_toxic_staff_of_the_dead") {
                production {
                    category("Chisel")
                    input("obj.magic_fang")
                    input("obj.sotd")
                    statReq("stat.crafting", 59)
                    xp(0)
                    output("obj.toxic_sotd")
                }
                columnRSCM(COL_TOOL, "obj.chisel")
                column(COL_SOUND, "synth.chisel")
            }
            row("dbrow.crafting_trident_of_the_swamp") {
                production {
                    category("Chisel")
                    input("obj.magic_fang")
                    input("obj.tots_uncharged")
                    statReq("stat.crafting", 59)
                    xp(0)
                    output("obj.toxic_tots_uncharged")
                }
                columnRSCM(COL_TOOL, "obj.chisel")
                column(COL_SOUND, "synth.chisel")
            }

            row("dbrow.crafting_bone_staff") {
                production {
                    category("Chisel")
                    input("obj.rat_boss_spine")
                    input("obj.battlestaff")
                    input("obj.chaosrune", 1000)
                    statReq("stat.crafting", 35)
                    xp(0)
                    output("obj.rat_bone_staff")
                }
                columnRSCM(COL_TRIGGERS, "obj.rat_boss_spine", "obj.battlestaff")
                columnRSCM(COL_TOOL, "obj.chisel")
                column(COL_SOUND, "synth.chisel")
            }

            row("dbrow.crafting_accursed_sceptre") {
                production {
                    category("Attach")
                    input("obj.wbr_vetion_skull")
                    input("obj.wild_cave_sceptre_uncharged")
                    statReq("stat.crafting", 85)
                    xp(0)
                    output("obj.wild_cave_accursed_uncharged")
                }
            }

            row("dbrow.crafting_strung_rabbit_foot") {
                production {
                    category("String")
                    input("obj.hunting_rabbit_foot")
                    input("obj.ball_of_wool")
                    statReq("stat.crafting", 37)
                    xp(40)
                    output("obj.hunting_strung_rabbit_foot")
                }
                column(COL_SOUND, "synth.stringing")
                column(COL_SPAM_MESSAGE, "You string the {input}.")
                column(COL_ACTION_NAME, "string a {input}")
            }

            row("dbrow.crafting_serpentine_helm") {
                production {
                    category("Chisel")
                    input("obj.serpentine_visage")
                    statReq("stat.crafting", 52)
                    xp(1200)
                    output("obj.serpentine_helm")
                }
                columnRSCM(COL_TOOL, "obj.chisel")
                column(COL_SOUND, "synth.chisel")
                column(COL_RESULT_DIALOGUE, "You adapt the visage to fit on a human head.")
            }

            row("dbrow.crafting_break_armadyl_chestplate") {
                production {
                    category("Chisel")
                    input("obj.armadyl_chestplate")
                    statReq("stat.crafting", 90)
                    xp(8400)
                    output("obj.armadylean_component", 4)
                }
                columnRSCM(COL_TOOL, "obj.chisel")
                column(COL_SPAM_MESSAGE, "You use your chisel to break apart the armour down into its base components.")
                column(
                    COL_CONFIRM_TITLE,
                    "Break apart your Armadyl chestplate into 4 Armadylean plates?",
                    "Really break apart your Armadyl chestplate into 4 Armadylean plates?",
                )
            }
            row("dbrow.crafting_break_armadyl_skirt") {
                production {
                    category("Chisel")
                    input("obj.armadyl_skirt")
                    statReq("stat.crafting", 90)
                    xp(6300)
                    output("obj.armadylean_component", 3)
                }
                columnRSCM(COL_TOOL, "obj.chisel")
                column(COL_SPAM_MESSAGE, "You use your chisel to break apart the armour down into its base components.")
                column(
                    COL_CONFIRM_TITLE,
                    "Break apart your Armadyl chainskirt into 3 Armadylean plates?",
                    "Really break apart your Armadyl chainskirt into 3 Armadylean plates?",
                )
            }
            row("dbrow.crafting_break_armadyl_helmet") {
                production {
                    category("Chisel")
                    input("obj.armadyl_helmet")
                    statReq("stat.crafting", 90)
                    xp(2100)
                    output("obj.armadylean_component", 1)
                }
                columnRSCM(COL_TOOL, "obj.chisel")
                column(COL_SPAM_MESSAGE, "You use your chisel to break apart the armour down into its base components.")
                column(
                    COL_CONFIRM_TITLE,
                    "Break apart your Armadyl helmet into 1 Armadylean plate?",
                    "Really break apart your Armadyl helmet into 1 Armadylean plate?",
                )
            }
            row("dbrow.crafting_fortify_masori_body") {
                production {
                    category("Hammer")
                    input("obj.masori_body")
                    input("obj.armadylean_component", 4)
                    statReq("stat.crafting", 90)
                    xp(33200)
                    output("obj.masori_body_fortified")
                }
                columnRSCM(COL_TOOL, "obj.hammer")
                column(COL_CONFIRM_TITLE, "Fortify your Masori body with 4 Armadylean plates?")
                column(COL_RESULT_DIALOGUE, "You use 4 Armadylean plates to fortify the Masori body.")
            }
            row("dbrow.crafting_fortify_masori_chaps") {
                production {
                    category("Hammer")
                    input("obj.masori_chaps")
                    input("obj.armadylean_component", 3)
                    statReq("stat.crafting", 90)
                    xp(24900)
                    output("obj.masori_chaps_fortified")
                }
                columnRSCM(COL_TOOL, "obj.hammer")
                column(COL_CONFIRM_TITLE, "Fortify your Masori chaps with 3 Armadylean plates?")
                column(COL_RESULT_DIALOGUE, "You use 3 Armadylean plates to fortify the Masori chaps.")
            }
            row("dbrow.crafting_fortify_masori_mask") {
                production {
                    category("Hammer")
                    input("obj.masori_mask")
                    input("obj.armadylean_component", 1)
                    statReq("stat.crafting", 90)
                    xp(8300)
                    output("obj.masori_mask_fortified")
                }
                columnRSCM(COL_TOOL, "obj.hammer")
                column(COL_CONFIRM_TITLE, "Fortify your Masori mask with 1 Armadylean plate?")
                column(COL_RESULT_DIALOGUE, "You use 1 Armadylean plate to fortify the Masori mask.")
            }
            row("dbrow.crafting_light_orb") {
                production {
                    input("obj.dorgesh_lightbulb_nofilament")
                    input("obj.dorgesh_wire")
                    statReq("stat.crafting", 87)
                    xp(1040)
                    output("obj.dorgesh_light_bulb")
                }
                column(COL_SPAM_MESSAGE, "")
            }
        }

        section("SoftClayMixing") {
            row("dbrow.crafting_soft_clay_bucket_water") {
                production {
                    input("obj.clay")
                    input("obj.bucket_water")
                    statReq("stat.crafting", 1)
                    xp(10)
                    output("obj.softclay")
                    output("obj.bucket_empty")
                }
            }
            row("dbrow.crafting_soft_clay_jug_water") {
                production {
                    input("obj.clay")
                    input("obj.jug_water")
                    statReq("stat.crafting", 1)
                    xp(10)
                    output("obj.softclay")
                    output("obj.jug_empty")
                }
            }
            row("dbrow.crafting_soft_clay_bowl_water") {
                production {
                    input("obj.clay")
                    input("obj.bowl_water")
                    statReq("stat.crafting", 1)
                    xp(10)
                    output("obj.softclay")
                    output("obj.bowl_empty")
                }
            }
            row("dbrow.crafting_soft_clay_cup_water") {
                production {
                    input("obj.clay")
                    input("obj.cup_water")
                    statReq("stat.crafting", 1)
                    xp(10)
                    output("obj.softclay")
                    output("obj.cup_empty")
                }
            }
        }

        section("PheasantCostume") {
            row("dbrow.crafting_pheasant_boots") {
                production {
                    input("obj.forestry_pheasant_feathers", 15)
                    statReq("stat.crafting", 2)
                    xp(150)
                    output("obj.forestry_pheasant_boots")
                }
            }
            row("dbrow.crafting_pheasant_hat") {
                production {
                    input("obj.forestry_pheasant_feathers", 15)
                    statReq("stat.crafting", 2)
                    xp(150)
                    output("obj.forestry_pheasant_hat")
                }
            }
            row("dbrow.crafting_pheasant_legs") {
                production {
                    input("obj.forestry_pheasant_feathers", 15)
                    statReq("stat.crafting", 2)
                    xp(150)
                    output("obj.forestry_pheasant_legs")
                }
            }
            row("dbrow.crafting_pheasant_cape") {
                production {
                    input("obj.forestry_pheasant_feathers", 15)
                    statReq("stat.crafting", 2)
                    xp(150)
                    output("obj.forestry_pheasant_cape")
                }
            }
        }
    }

    fun tanning() = productionTable("dbtable.crafting_tanning", serverOnly = true,
        extraColumns = { tanningColumns() }) {
        row("dbrow.crafting_tan_soft_leather") {
            production {
                input("obj.cow_hide")
                output("obj.leather")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 1)
            column(COL_TAN_SLOT_LETTER, "a")
            column(COL_TAN_SLOT_LABEL, "Soft leather")
        }
        row("dbrow.crafting_tan_hard_leather") {
            production {
                input("obj.cow_hide")
                output("obj.hard_leather")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 3)
            column(COL_TAN_SLOT_LETTER, "b")
            column(COL_TAN_SLOT_LABEL, "Hard leather")
        }
        row("dbrow.crafting_tan_snakeskin_swamp") {
            production {
                input("obj.templetrek_swamp_snake_hide")
                output("obj.village_snake_skin")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 20)
            column(COL_TAN_SLOT_LETTER, "c")
            column(COL_TAN_SLOT_LABEL, "Snakeskin")
        }
        row("dbrow.crafting_tan_snakeskin") {
            production {
                input("obj.village_snake_hide")
                output("obj.village_snake_skin")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 15)
            column(COL_TAN_SLOT_LETTER, "d")
            column(COL_TAN_SLOT_LABEL, "Snakeskin")
        }
        row("dbrow.crafting_tan_green_dhide") {
            production {
                input("obj.dragonhide_green")
                output("obj.dragon_leather")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 20)
            column(COL_TAN_SLOT_LETTER, "e")
            column(COL_TAN_SLOT_LABEL, "Green d'hide")
        }
        row("dbrow.crafting_tan_blue_dhide") {
            production {
                input("obj.dragonhide_blue")
                output("obj.dragon_leather_blue")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 20)
            column(COL_TAN_SLOT_LETTER, "f")
            column(COL_TAN_SLOT_LABEL, "Blue d'hide")
        }
        row("dbrow.crafting_tan_red_dhide") {
            production {
                input("obj.dragonhide_red")
                output("obj.dragon_leather_red")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 20)
            column(COL_TAN_SLOT_LETTER, "g")
            column(COL_TAN_SLOT_LABEL, "Red d'hide")
        }
        row("dbrow.crafting_tan_black_dhide") {
            production {
                input("obj.dragonhide_black")
                output("obj.dragon_leather_black")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 20)
            column(COL_TAN_SLOT_LETTER, "h")
            column(COL_TAN_SLOT_LABEL, "Black d'hide")
        }
        row("dbrow.crafting_cure_yak_hide") {
            production {
                input("obj.yak_hide")
                output("obj.yak_hide_cured")
                statReq("stat.crafting", 0)
                xp(0)
            }
            column(COL_TAN_COST, 5)
        }
    }

    fun silver() = craftingTable("dbtable.crafting_silver", extraColumns = { craftingInterfaceColumns() }) {
        section("Jewellery", category = "Silver") {
            row("dbrow.crafting_unstrung_symbol") {
                production {
                    input("obj.silver_bar")
                    statReq("stat.crafting", 16)
                    xp(500)
                    output("obj.nostringstar")
                }
                columnRSCM(COL_TOOL, "obj.holy_symbol_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:holy_symbol")
                column(COL_INTERFACE_SLOT, 13)
            }
            row("dbrow.crafting_unstrung_emblem") {
                production {
                    input("obj.silver_bar")
                    statReq("stat.crafting", 17)
                    xp(500)
                    output("obj.nostringsnake")
                }
                columnRSCM(COL_TOOL, "obj.unholy_symbol_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:unholy_symbol")
                column(COL_INTERFACE_SLOT, 14)
            }
            row("dbrow.crafting_silver_sickle") {
                production {
                    input("obj.silver_bar")
                    statReq("stat.crafting", 18)
                    xp(500)
                    output("obj.silver_sickle")
                }
                columnRSCM(COL_TOOL, "obj.sickle_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:sickle")
                column(COL_INTERFACE_SLOT, 15)
            }
            row("dbrow.crafting_silver_bolts") {
                production {
                    input("obj.silver_bar")
                    statReq("stat.crafting", 21)
                    xp(500)
                    output("obj.xbows_crossbow_bolts_silver_unfeathered", 10)
                }
                columnRSCM(COL_TOOL, "obj.xbows_silver_bolt_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:crossbow_bolt")
                column(COL_INTERFACE_SLOT, 17)
            }
            row("dbrow.crafting_conductor") {
                production {
                    input("obj.silver_bar")
                    statReq("stat.crafting", 20)
                    xp(500)
                    output("obj.fenk_conductor")
                }
                columnRSCM(COL_TOOL, "obj.fenk_lightning_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:lightning_rod")
                column(COL_INTERFACE_SLOT, 16)
            }
            row("dbrow.crafting_silvthrill_rod") {
                production {
                    input("obj.silver_bar")
                    input("obj.mithril_bar")
                    input("obj.sapphire")
                    statReq("stat.crafting", 25)
                    xp(550)
                    output("obj.burgh_rod_command1")
                }
                columnRSCM(COL_TOOL, "obj.burgh_rod_clay")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:ivandis")
                column(COL_INTERFACE_SLOT, 19)
            }
            row("dbrow.crafting_demonic_sigil") {
                production {
                    input("obj.silver_bar")
                    statReq("stat.crafting", 30)
                    xp(500)
                    output("obj.agrith_sigil")
                }
                columnRSCM(COL_TOOL, "obj.agrith_sigil_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:agrith_sigil")
                column(COL_INTERFACE_SLOT, 20)
            }
            row("dbrow.crafting_tiara") {
                production {
                    input("obj.silver_bar")
                    statReq("stat.crafting", 23)
                    xp(525)
                    output("obj.tiara")
                }
                columnRSCM(COL_TOOL, "obj.tiara_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:tiara")
                column(COL_INTERFACE_SLOT, 18)
            }

            row("dbrow.crafting_opal_ring") {
                production {
                    input("obj.silver_bar")
                    input("obj.opal")
                    statReq("stat.crafting", 1)
                    xp(100)
                    output("obj.opal_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:opal_ring")
                column(COL_INTERFACE_SLOT, 1)
            }
            row("dbrow.crafting_opal_necklace") {
                production {
                    input("obj.silver_bar")
                    input("obj.opal")
                    statReq("stat.crafting", 16)
                    xp(350)
                    output("obj.opal_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:opal_necklace")
                column(COL_INTERFACE_SLOT, 4)
            }
            row("dbrow.crafting_opal_bracelet") {
                production {
                    input("obj.silver_bar")
                    input("obj.opal")
                    statReq("stat.crafting", 22)
                    xp(450)
                    output("obj.opal_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:opal_bracelet")
                column(COL_INTERFACE_SLOT, 10)
            }
            row("dbrow.crafting_opal_amulet") {
                production {
                    input("obj.silver_bar")
                    input("obj.opal")
                    statReq("stat.crafting", 27)
                    xp(550)
                    output("obj.unstrung_opal_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:opal_amulet")
                column(COL_INTERFACE_SLOT, 7)
            }

            row("dbrow.crafting_jade_ring") {
                production {
                    input("obj.silver_bar")
                    input("obj.jade")
                    statReq("stat.crafting", 13)
                    xp(320)
                    output("obj.jade_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:jade_ring")
                column(COL_INTERFACE_SLOT, 2)
            }
            row("dbrow.crafting_jade_necklace") {
                production {
                    input("obj.silver_bar")
                    input("obj.jade")
                    statReq("stat.crafting", 25)
                    xp(540)
                    output("obj.jade_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:jade_necklace")
                column(COL_INTERFACE_SLOT, 5)
            }
            row("dbrow.crafting_jade_bracelet") {
                production {
                    input("obj.silver_bar")
                    input("obj.jade")
                    statReq("stat.crafting", 29)
                    xp(600)
                    output("obj.jade_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:jade_bracelet")
                column(COL_INTERFACE_SLOT, 11)
            }
            row("dbrow.crafting_jade_amulet") {
                production {
                    input("obj.silver_bar")
                    input("obj.jade")
                    statReq("stat.crafting", 34)
                    xp(700)
                    output("obj.unstrung_jade_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:jade_amulet")
                column(COL_INTERFACE_SLOT, 8)
            }

            row("dbrow.crafting_topaz_ring") {
                production {
                    input("obj.silver_bar")
                    input("obj.red_topaz")
                    statReq("stat.crafting", 16)
                    xp(350)
                    output("obj.topaz_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:topaz_ring")
                column(COL_INTERFACE_SLOT, 3)
            }
            row("dbrow.crafting_topaz_necklace") {
                production {
                    input("obj.silver_bar")
                    input("obj.red_topaz")
                    statReq("stat.crafting", 32)
                    xp(700)
                    output("obj.topaz_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:topaz_necklace")
                column(COL_INTERFACE_SLOT, 6)
            }
            row("dbrow.crafting_topaz_bracelet") {
                production {
                    input("obj.silver_bar")
                    input("obj.red_topaz")
                    statReq("stat.crafting", 38)
                    xp(750)
                    output("obj.topaz_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:topaz_bracelet")
                column(COL_INTERFACE_SLOT, 12)
            }
            row("dbrow.crafting_topaz_amulet") {
                production {
                    input("obj.silver_bar")
                    input("obj.red_topaz")
                    statReq("stat.crafting", 45)
                    xp(800)
                    output("obj.unstrung_topaz_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.silver_crafting:topaz_amulet")
                column(COL_INTERFACE_SLOT, 9)
            }
        }
    }

    fun gold() = craftingTable("dbtable.crafting_gold", extraColumns = { craftingInterfaceColumns() }) {
        section("Jewellery", category = "Gold") {
            row("dbrow.crafting_gold_ring") {
                production {
                    input("obj.gold_bar")
                    statReq("stat.crafting", 5)
                    xp(150)
                    output("obj.gold_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:gold_ring")
                column(COL_INTERFACE_SLOT, 1)
            }
            row("dbrow.crafting_gold_necklace") {
                production {
                    input("obj.gold_bar")
                    statReq("stat.crafting", 6)
                    xp(200)
                    output("obj.gold_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:gold_necklace")
                column(COL_INTERFACE_SLOT, 10)
            }
            row("dbrow.crafting_gold_bracelet") {
                production {
                    input("obj.gold_bar")
                    statReq("stat.crafting", 7)
                    xp(250)
                    output("obj.jewl_gold_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:gold_bracelet")
                column(COL_INTERFACE_SLOT, 26)
            }
            row("dbrow.crafting_gold_amulet") {
                production {
                    input("obj.gold_bar")
                    statReq("stat.crafting", 8)
                    xp(300)
                    output("obj.unstrung_gold_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:gold_amulet")
                column(COL_INTERFACE_SLOT, 18)
            }

            row("dbrow.crafting_sapphire_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.sapphire")
                    statReq("stat.crafting", 20)
                    xp(400)
                    output("obj.sapphire_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:sapphire_ring")
                column(COL_INTERFACE_SLOT, 2)
            }
            row("dbrow.crafting_sapphire_necklace") {
                production {
                    input("obj.gold_bar")
                    input("obj.sapphire")
                    statReq("stat.crafting", 22)
                    xp(550)
                    output("obj.sapphire_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:sapphire_necklace")
                column(COL_INTERFACE_SLOT, 11)
            }
            row("dbrow.crafting_sapphire_bracelet") {
                production {
                    input("obj.gold_bar")
                    input("obj.sapphire")
                    statReq("stat.crafting", 23)
                    xp(600)
                    output("obj.jewl_sapphire_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:sapphire_bracelet")
                column(COL_INTERFACE_SLOT, 27)
            }
            row("dbrow.crafting_sapphire_amulet") {
                production {
                    input("obj.gold_bar")
                    input("obj.sapphire")
                    statReq("stat.crafting", 24)
                    xp(650)
                    output("obj.unstrung_sapphire_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:sapphire_amulet")
                column(COL_INTERFACE_SLOT, 19)
            }

            row("dbrow.crafting_emerald_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.emerald")
                    statReq("stat.crafting", 27)
                    xp(550)
                    output("obj.emerald_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:emerald_ring")
                column(COL_INTERFACE_SLOT, 3)
            }
            row("dbrow.crafting_emerald_necklace") {
                production {
                    input("obj.gold_bar")
                    input("obj.emerald")
                    statReq("stat.crafting", 29)
                    xp(600)
                    output("obj.emerald_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:emerald_necklace")
                column(COL_INTERFACE_SLOT, 12)
            }
            row("dbrow.crafting_emerald_bracelet") {
                production {
                    input("obj.gold_bar")
                    input("obj.emerald")
                    statReq("stat.crafting", 30)
                    xp(650)
                    output("obj.jewl_emerald_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:emerald_bracelet")
                column(COL_INTERFACE_SLOT, 28)
            }
            row("dbrow.crafting_emerald_amulet") {
                production {
                    input("obj.gold_bar")
                    input("obj.emerald")
                    statReq("stat.crafting", 31)
                    xp(700)
                    output("obj.unstrung_emerald_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:emerald_amulet")
                column(COL_INTERFACE_SLOT, 20)
            }

            row("dbrow.crafting_ruby_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.ruby")
                    statReq("stat.crafting", 34)
                    xp(700)
                    output("obj.ruby_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:ruby_ring")
                column(COL_INTERFACE_SLOT, 4)
            }
            row("dbrow.crafting_ruby_necklace") {
                production {
                    input("obj.gold_bar")
                    input("obj.ruby")
                    statReq("stat.crafting", 40)
                    xp(750)
                    output("obj.ruby_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:ruby_necklace")
                column(COL_INTERFACE_SLOT, 13)
            }
            row("dbrow.crafting_ruby_bracelet") {
                production {
                    input("obj.gold_bar")
                    input("obj.ruby")
                    statReq("stat.crafting", 42)
                    xp(800)
                    output("obj.jewl_ruby_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:ruby_bracelet")
                column(COL_INTERFACE_SLOT, 29)
            }
            row("dbrow.crafting_ruby_amulet") {
                production {
                    input("obj.gold_bar")
                    input("obj.ruby")
                    statReq("stat.crafting", 50)
                    xp(850)
                    output("obj.unstrung_ruby_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:ruby_amulet")
                column(COL_INTERFACE_SLOT, 21)
            }

            row("dbrow.crafting_diamond_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.diamond")
                    statReq("stat.crafting", 43)
                    xp(850)
                    output("obj.diamond_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:diamond_ring")
                column(COL_INTERFACE_SLOT, 5)
            }
            row("dbrow.crafting_diamond_necklace") {
                production {
                    input("obj.gold_bar")
                    input("obj.diamond")
                    statReq("stat.crafting", 56)
                    xp(900)
                    output("obj.diamond_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:diamond_necklace")
                column(COL_INTERFACE_SLOT, 14)
            }
            row("dbrow.crafting_diamond_bracelet") {
                production {
                    input("obj.gold_bar")
                    input("obj.diamond")
                    statReq("stat.crafting", 58)
                    xp(950)
                    output("obj.jewl_diamond_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:diamond_bracelet")
                column(COL_INTERFACE_SLOT, 30)
            }
            row("dbrow.crafting_diamond_amulet") {
                production {
                    input("obj.gold_bar")
                    input("obj.diamond")
                    statReq("stat.crafting", 70)
                    xp(1000)
                    output("obj.unstrung_diamond_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:diamond_amulet")
                column(COL_INTERFACE_SLOT, 22)
            }

            row("dbrow.crafting_dragonstone_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.dragonstone")
                    statReq("stat.crafting", 55)
                    xp(1000)
                    output("obj.dragonstone_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:dragon_ring")
                column(COL_INTERFACE_SLOT, 6)
            }
            row("dbrow.crafting_dragonstone_necklace") {
                production {
                    input("obj.gold_bar")
                    input("obj.dragonstone")
                    statReq("stat.crafting", 72)
                    xp(1050)
                    output("obj.dragonstone_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:dragon_necklace")
                column(COL_INTERFACE_SLOT, 15)
            }
            row("dbrow.crafting_dragonstone_bracelet") {
                production {
                    input("obj.gold_bar")
                    input("obj.dragonstone")
                    statReq("stat.crafting", 74)
                    xp(1100)
                    output("obj.jewl_dragonstone_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:dragon_bracelet")
                column(COL_INTERFACE_SLOT, 31)
            }
            row("dbrow.crafting_dragonstone_amulet") {
                production {
                    input("obj.gold_bar")
                    input("obj.dragonstone")
                    statReq("stat.crafting", 80)
                    xp(1500)
                    output("obj.unstrung_dragonstone_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:dragon_amulet")
                column(COL_INTERFACE_SLOT, 23)
            }

            row("dbrow.crafting_onyx_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.onyx")
                    statReq("stat.crafting", 67)
                    xp(1150)
                    output("obj.onyx_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:onyx_ring")
                column(COL_INTERFACE_SLOT, 7)
            }
            row("dbrow.crafting_onyx_necklace") {
                production {
                    input("obj.gold_bar")
                    input("obj.onyx")
                    statReq("stat.crafting", 82)
                    xp(1200)
                    output("obj.onyx_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:onyx_necklace")
                column(COL_INTERFACE_SLOT, 16)
            }
            row("dbrow.crafting_onyx_bracelet") {
                production {
                    input("obj.gold_bar")
                    input("obj.onyx")
                    statReq("stat.crafting", 84)
                    xp(1250)
                    output("obj.jewl_onyx_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:onyx_bracelet")
                column(COL_INTERFACE_SLOT, 32)
            }
            row("dbrow.crafting_onyx_amulet") {
                production {
                    input("obj.gold_bar")
                    input("obj.onyx")
                    statReq("stat.crafting", 90)
                    xp(1650)
                    output("obj.unstrung_onyx_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:onyx_amulet")
                column(COL_INTERFACE_SLOT, 24)
            }

            row("dbrow.crafting_zenyte_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.zenyte")
                    statReq("stat.crafting", 89)
                    xp(1500)
                    output("obj.zenyte_ring")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:zenyte_ring")
                column(COL_INTERFACE_SLOT, 8)
            }
            row("dbrow.crafting_zenyte_necklace") {
                production {
                    input("obj.gold_bar")
                    input("obj.zenyte")
                    statReq("stat.crafting", 92)
                    xp(1650)
                    output("obj.zenyte_necklace")
                }
                columnRSCM(COL_TOOL, "obj.necklace_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:zenyte_necklace")
                column(COL_INTERFACE_SLOT, 17)
            }
            row("dbrow.crafting_zenyte_bracelet") {
                production {
                    input("obj.gold_bar")
                    input("obj.zenyte")
                    statReq("stat.crafting", 95)
                    xp(1800)
                    output("obj.zenyte_bracelet")
                }
                columnRSCM(COL_TOOL, "obj.jewl_bracelet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:zenyte_bracelet")
                column(COL_INTERFACE_SLOT, 33)
            }
            row("dbrow.crafting_zenyte_amulet") {
                production {
                    input("obj.gold_bar")
                    input("obj.zenyte")
                    statReq("stat.crafting", 98)
                    xp(2000)
                    output("obj.unstrung_zenyte_amulet")
                }
                columnRSCM(COL_TOOL, "obj.amulet_mould")
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:zenyte_amulet")
                column(COL_INTERFACE_SLOT, 25)
            }

            row("dbrow.crafting_slayer_ring_eternal") {
                production {
                    input("obj.gold_bar")
                    input("obj.slayer_eternal_gem")
                    statReq("stat.crafting", 75)
                    xp(150)
                    output("obj.slayer_ring_eternal")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_UNLOCK_VARBIT, "varbit.slayer_ring_unlocked", VarbitCompare.GTE.id, 1)
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:slayer_ring")
                column(COL_INTERFACE_SLOT, 9)
            }
            row("dbrow.crafting_slayer_ring") {
                production {
                    input("obj.gold_bar")
                    input("obj.slayer_gem")
                    statReq("stat.crafting", 75)
                    xp(150)
                    output("obj.slayer_ring_8")
                }
                columnRSCM(COL_TOOL, "obj.ring_mould")
                column(COL_UNLOCK_VARBIT, "varbit.slayer_ring_unlocked", VarbitCompare.GTE.id, 1)
                column(COL_INTERFACE_COMPONENT, "component.crafting_gold:slayer_ring")
                column(COL_INTERFACE_SLOT, 9)
            }
        }
    }
}

private fun DBTableBuilder.craftingColumns() {
    column("section", Crafting.COL_SECTION, VarType.STRING)
    column("success_low", Crafting.COL_SUCCESS_LOW, VarType.INT)
    column("success_high", Crafting.COL_SUCCESS_HIGH, VarType.INT)
    column("ticks", Crafting.COL_TICKS, VarType.INT)
    column("fail_xp", Crafting.COL_FAIL_XP, VarType.INT)
    column("fail_item", Crafting.COL_FAIL_ITEM, VarType.OBJ)
    column("anim", Crafting.COL_ANIM, VarType.STRING)
    column("spotanim", Crafting.COL_SPOTANIM, VarType.STRING)
    column("triggers", Crafting.COL_TRIGGERS, VarType.OBJ)
    column("xp_extra", Crafting.COL_XP_EXTRA, VarType.STAT, VarType.INT)
    column("tool", Crafting.COL_TOOL, VarType.OBJ)
    column("sound", Crafting.COL_SOUND, VarType.STRING)
    column("message", Crafting.COL_SPAM_MESSAGE, VarType.STRING)
    column("action_name", Crafting.COL_ACTION_NAME, VarType.STRING)
    column("confirm_title", Crafting.COL_CONFIRM_TITLE, VarType.STRING)
    column("confirm_warning", Crafting.COL_CONFIRM_WARNING, VarType.STRING)
    column("result_dialogue", Crafting.COL_RESULT_DIALOGUE, VarType.STRING)
    column("quest_req", Crafting.COL_QUEST_REQ, VarType.STRING, VarType.INT)
    column("unlock_varbit", Crafting.COL_UNLOCK_VARBIT, VarType.STRING, VarType.INT, VarType.INT)
    column("locked_message", Crafting.COL_LOCKED_MESSAGE, VarType.STRING)
}

private fun DBTableBuilder.craftingInterfaceColumns() {
    craftingColumns()
    column("interface_component", Crafting.COL_INTERFACE_COMPONENT, VarType.STRING)
    column("interface_slot", Crafting.COL_INTERFACE_SLOT, VarType.INT)
}

private fun DBTableBuilder.tanningColumns() {
    column("cost", Crafting.COL_TAN_COST, VarType.INT)
    column("slot_letter", Crafting.COL_TAN_SLOT_LETTER, VarType.STRING)
    column("slot_label", Crafting.COL_TAN_SLOT_LABEL, VarType.STRING)
}

private fun craftingTable(
    tableId: String,
    extraColumns: DBTableBuilder.() -> Unit = { craftingColumns() },
    block: CraftingTableScope.() -> Unit,
) = productionTable(tableId, serverOnly = true, extraColumns = extraColumns) {
    CraftingTableScope(this).block()
}

private class CraftingTableScope(private val table: ProductionTableScope) {
    fun section(name: String, category: String? = null, block: CraftingSectionScope.() -> Unit) {
        CraftingSectionScope(table, name, category).block()
    }
}

private class CraftingSectionScope(
    private val table: ProductionTableScope,
    private val section: String,
    private val category: String?,
) {
    fun row(rowId: String, block: ProductionTableRowScope.() -> Unit) {
        table.row(rowId) {
            column(Crafting.COL_SECTION, section)
            category?.let { column(ProductionColumns.COL_CATEGORY, it) }
            block()
        }
    }
}
