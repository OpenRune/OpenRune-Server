package org.rsmod.content.other.bots.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object BotOutfitsTable {
    const val STYLE = 0
    const val MIN_COMBAT = 1
    const val MAX_COMBAT = 2
    const val ITEMS = 3

    fun outfits() =
        dbTable("dbtable.bot_outfits", serverOnly = true) {
            column("style", STYLE, VarType.STRING)
            column("min_combat", MIN_COMBAT, VarType.INT)
            column("max_combat", MAX_COMBAT, VarType.INT)
            column("items", ITEMS, VarType.OBJ)
            fun ironmanArmour(symbol: String, style: String, vararg pieces: String) {
                row(symbol) {
                    column(STYLE, style)
                    column(MIN_COMBAT, 3)
                    column(MAX_COMBAT, 126)
                    columnRSCM(ITEMS, *pieces)
                }
            }

            ironmanArmour(
                "dbrow.bot_outfit_ironman", "ironman",
                "obj.ironman_helm", "obj.ironman_platebody", "obj.ironman_platelegs",
            )
            ironmanArmour(
                "dbrow.bot_outfit_hardcore_ironman", "hardcore_ironman",
                "obj.hardcore_ironman_helm", "obj.hardcore_ironman_platebody", "obj.hardcore_ironman_platelegs",
            )
            ironmanArmour(
                "dbrow.bot_outfit_ultimate_ironman", "ultimate_ironman",
                "obj.ultimate_ironman_helm", "obj.ultimate_ironman_platebody", "obj.ultimate_ironman_platelegs",
            )
            ironmanArmour(
                "dbrow.bot_outfit_group_ironman", "group_ironman",
                "obj.group_ironman_helm", "obj.group_ironman_platebody02", "obj.group_ironman_platelegs",
            )
            ironmanArmour(
                "dbrow.bot_outfit_hardcore_group_ironman", "hardcore_group_ironman",
                "obj.hardcore_group_ironman_helm", "obj.hardcore_group_ironman_platebody02", "obj.hardcore_group_ironman_platelegs",
            )
            ironmanArmour(
                "dbrow.bot_outfit_unranked_group_ironman", "unranked_group_ironman",
                "obj.group_ironman_helm_unranked", "obj.group_ironman_platebody02_unranked", "obj.group_ironman_platelegs_unranked",
            )
            row("dbrow.bot_outfit_wealthy_torva") {
                column(STYLE, "wealthy")
                column(MIN_COMBAT, 115)
                column(MAX_COMBAT, 126)
                columnRSCM(
                    ITEMS,
                    "obj.torva_helm",
                    "obj.torva_chest",
                    "obj.torva_legs",
                    "obj.ghrazi_rapier",
                    "obj.infernal_cape",
                    "obj.primordial_boots",
                    "obj.ferocious_gloves",
                    "obj.amulet_of_glory",
                )
            }
            row("dbrow.bot_outfit_wealthy_bandos") {
                column(STYLE, "wealthy")
                column(MIN_COMBAT, 100)
                column(MAX_COMBAT, 120)
                columnRSCM(
                    ITEMS,
                    "obj.neitiznot_faceguard",
                    "obj.bandos_chestplate",
                    "obj.bandos_skirt",
                    "obj.bandos_boots",
                    "obj.abyssal_whip",
                    "obj.tzhaar_cape_fire",
                    "obj.amulet_of_glory",
                )
            }
            row("dbrow.bot_outfit_wealthy_armadyl") {
                column(STYLE, "wealthy")
                column(MIN_COMBAT, 105)
                column(MAX_COMBAT, 123)
                columnRSCM(
                    ITEMS,
                    "obj.armadyl_helmet",
                    "obj.armadyl_chestplate",
                    "obj.armadyl_skirt",
                    "obj.twisted_bow",
                    "obj.pegasian_boots",
                    "obj.cape_of_legends",
                    "obj.amulet_of_glory",
                )
            }
            row("dbrow.bot_outfit_wealthy_ancestral") {
                column(STYLE, "wealthy")
                column(MIN_COMBAT, 110)
                column(MAX_COMBAT, 126)
                columnRSCM(
                    ITEMS,
                    "obj.ancestral_hat",
                    "obj.ancestral_robe_top",
                    "obj.ancestral_robe_bottom",
                    "obj.tumekens_shadow",
                    "obj.occult_necklace",
                    "obj.eternal_boots",
                    "obj.infernal_cape",
                )
            }
            row("dbrow.bot_outfit_wealthy_partyhat") {
                column(STYLE, "wealthy")
                column(MIN_COMBAT, 90)
                column(MAX_COMBAT, 126)
                columnRSCM(
                    ITEMS,
                    "obj.blue_partyhat",
                    "obj.rune_platebody",
                    "obj.rune_platelegs",
                    "obj.abyssal_whip",
                    "obj.amulet_of_glory",
                    "obj.ring_of_wealth",
                )
            }
            row("dbrow.bot_outfit_wealthy_santa") {
                column(STYLE, "wealthy")
                column(MIN_COMBAT, 80)
                column(MAX_COMBAT, 126)
                columnRSCM(
                    ITEMS,
                    "obj.santa_hat",
                    "obj.bandos_chestplate",
                    "obj.bandos_skirt",
                    "obj.abyssal_whip",
                    "obj.amulet_of_glory",
                )
            }
            row("dbrow.bot_outfit_graceful") {
                column(STYLE, "graceful")
                column(MIN_COMBAT, 30)
                column(MAX_COMBAT, 110)
                columnRSCM(
                    ITEMS,
                    "obj.graceful_hood",
                    "obj.graceful_cape",
                    "obj.graceful_top",
                    "obj.graceful_legs",
                    "obj.graceful_gloves",
                    "obj.graceful_boots",
                )
            }
            row("dbrow.bot_outfit_adventurer_rune") {
                column(STYLE, "adventurer")
                column(MIN_COMBAT, 45)
                column(MAX_COMBAT, 80)
                columnRSCM(
                    ITEMS,
                    "obj.rune_full_helm",
                    "obj.rune_platebody",
                    "obj.rune_platelegs",
                    "obj.rune_kiteshield",
                    "obj.rune_scimitar",
                    "obj.amulet_of_power",
                )
            }
            row("dbrow.bot_outfit_adventurer_dragon") {
                column(STYLE, "adventurer")
                column(MIN_COMBAT, 60)
                column(MAX_COMBAT, 95)
                columnRSCM(
                    ITEMS,
                    "obj.dragon_med_helm",
                    "obj.dragon_chainbody",
                    "obj.dragon_platelegs",
                    "obj.dragon_scimitar",
                    "obj.dragon_boots",
                    "obj.amulet_of_glory",
                )
            }
            row("dbrow.bot_outfit_adventurer_mystic") {
                column(STYLE, "adventurer")
                column(MIN_COMBAT, 40)
                column(MAX_COMBAT, 85)
                columnRSCM(
                    ITEMS,
                    "obj.mystic_hat",
                    "obj.mystic_robe_top",
                    "obj.mystic_robe_bottom",
                    "obj.staff_of_fire",
                    "obj.amulet_of_magic",
                    "obj.mystic_boots",
                )
            }
            row("dbrow.bot_outfit_adventurer_iron") {
                column(STYLE, "adventurer")
                column(MIN_COMBAT, 15)
                column(MAX_COMBAT, 40)
                columnRSCM(
                    ITEMS,
                    "obj.iron_full_helm",
                    "obj.iron_platebody",
                    "obj.iron_platelegs",
                    "obj.iron_scimitar",
                )
            }
            row("dbrow.bot_outfit_adventurer_ranger") {
                column(STYLE, "adventurer")
                column(MIN_COMBAT, 25)
                column(MAX_COMBAT, 70)
                columnRSCM(
                    ITEMS,
                    "obj.coif",
                    "obj.hardleather_body",
                    "obj.leather_chaps",
                    "obj.magic_shortbow",
                    "obj.leather_vambraces",
                    "obj.leather_boots",
                )
            }
            row("dbrow.bot_outfit_skiller_lumberjack") {
                column(STYLE, "lumberjack")
                column(MIN_COMBAT, 3)
                column(MAX_COMBAT, 70)
                columnRSCM(
                    ITEMS,
                    "obj.forestry_lumberjack_hat",
                    "obj.forestry_lumberjack_top",
                    "obj.forestry_lumberjack_legs",
                    "obj.forestry_lumberjack_boots",
                )
            }
            row("dbrow.bot_outfit_skiller_prospector") {
                column(STYLE, "prospector")
                column(MIN_COMBAT, 3)
                column(MAX_COMBAT, 70)
                columnRSCM(
                    ITEMS,
                    "obj.motherlode_reward_hat",
                    "obj.motherlode_reward_top",
                    "obj.motherlode_reward_legs",
                    "obj.motherlode_reward_boots",
                )
            }
            row("dbrow.bot_outfit_skiller_angler") {
                column(STYLE, "angler")
                column(MIN_COMBAT, 3)
                column(MAX_COMBAT, 70)
                columnRSCM(
                    ITEMS,
                    "obj.spirit_angler_hat",
                    "obj.spirit_angler_top",
                    "obj.spirit_angler_legs",
                    "obj.spirit_angler_boots",
                )
            }
            row("dbrow.bot_outfit_plain") {
                column(STYLE, "plain")
                column(MIN_COMBAT, 3)
                column(MAX_COMBAT, 45)
                columnRSCM(
                    ITEMS,
                    "obj.leather_boots",
                    "obj.amulet_of_power",
                )
            }
        }
}
