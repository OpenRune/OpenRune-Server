package org.rsmod.content.travel.jewellery.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * `teleport_jewellery_pieces` holds one row per charge chain. `charged` lists the objs from most
 * to fewest charges; `spent` is the uncharged obj that can still be rubbed and `remains` is what is
 * left behind when the last charge crumbles the piece. An `eternal` piece never loses charges, and
 * a `respawn` piece sends the player to their respawn point instead of a destination row.
 *
 * `held_op` is the inventory op that teleports (its sub-ops pick a destination). Worn pieces get
 * one op per destination starting at op2, unless `worn_menu_op` names a single op that asks.
 * `worn_subop_op` is a worn op whose sub-menu picks the destination.
 *
 * A destination may need a quest, a varbit set to non-zero, or land elsewhere once a stat reaches
 * `boost_level` (the Farming Guild's inner gate).
 */
object TeleportJewelleryTable {
    const val LABEL = 0
    const val COORDS = 1
    const val QUEST = 2
    const val UNLOCK_VARBIT = 3
    const val BOOST_STAT = 4
    const val BOOST_LEVEL = 5
    const val BOOST_COORDS = 6

    const val CHARGED = 0
    const val SPENT = 1
    const val REMAINS = 2
    const val NOUN = 3
    const val UNIT = 4
    const val RUB_MESSAGE = 5
    const val EMPTY_MESSAGE = 6
    const val LAST_CHARGE_MESSAGE = 7
    const val WILDERNESS_LEVEL = 8
    const val CONFIRM_WILDERNESS = 9
    const val DESTINATIONS = 10
    const val ETERNAL = 11
    const val RESPAWN = 12
    const val HELD_OP = 13
    const val WORN_MENU_OP = 14
    const val WORN_SUBOP_OP = 15

    private const val CHARGE = "charge"
    private const val USE = "use"
    private const val STANDARD_WILDERNESS = 20
    private const val DRAGONSTONE_WILDERNESS = 30
    private const val RUB = 4
    private const val TELEPORT = 3

    private val GLORY =
        listOf(
            "dbrow.teleport_jewellery_glory_edgeville",
            "dbrow.teleport_jewellery_glory_karamja",
            "dbrow.teleport_jewellery_glory_draynor_village",
            "dbrow.teleport_jewellery_glory_al_kharid",
        )
    private val WEALTH =
        listOf(
            "dbrow.teleport_jewellery_wealth_miscellania",
            "dbrow.teleport_jewellery_wealth_grand_exchange",
            "dbrow.teleport_jewellery_wealth_falador",
            "dbrow.teleport_jewellery_wealth_dondakan",
        )
    private val SLAYER =
        listOf(
            "dbrow.teleport_jewellery_slayer_stronghold",
            "dbrow.teleport_jewellery_slayer_slayer_tower",
            "dbrow.teleport_jewellery_slayer_fremennik_dungeon",
            "dbrow.teleport_jewellery_slayer_tarns_lair",
            "dbrow.teleport_jewellery_slayer_dark_beasts",
            "dbrow.teleport_jewellery_slayer_wyrmscraig_cavern",
        )

    fun destinations() =
        dbTable("dbtable.teleport_jewellery_destinations", serverOnly = true) {
            column("label", LABEL, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            column("quest", QUEST, VarType.STRING)
            column("unlock_varbit", UNLOCK_VARBIT, VarType.INT)
            column("boost_stat", BOOST_STAT, VarType.STAT)
            column("boost_level", BOOST_LEVEL, VarType.INT)
            column("boost_coords", BOOST_COORDS, VarType.COORDGRID)

            destination(
                "dbrow.teleport_jewellery_glory_edgeville",
                "Edgeville",
                CoordGrid(3087, 3496, 0),
            )
            destination(
                "dbrow.teleport_jewellery_glory_karamja",
                "Karamja",
                CoordGrid(2918, 3176, 0),
            )
            destination(
                "dbrow.teleport_jewellery_glory_draynor_village",
                "Draynor Village",
                CoordGrid(3105, 3251, 0),
            )
            destination(
                "dbrow.teleport_jewellery_glory_al_kharid",
                "Al Kharid",
                CoordGrid(3293, 3163, 0),
            )

            destination(
                "dbrow.teleport_jewellery_games_burthorpe",
                "Burthorpe",
                CoordGrid(2898, 3552, 0),
            )
            destination(
                "dbrow.teleport_jewellery_games_barbarian_outpost",
                "Barbarian Outpost",
                CoordGrid(2519, 3572, 0),
            )
            destination(
                "dbrow.teleport_jewellery_games_corporeal_beast",
                "Corporeal Beast",
                CoordGrid(2967, 4381, 2),
            )
            destination(
                "dbrow.teleport_jewellery_games_tears_of_guthix",
                "Tears of Guthix",
                CoordGrid(3245, 9500, 2),
                quest = "quest_tearsofguthix",
            )
            destination(
                "dbrow.teleport_jewellery_games_wintertodt_camp",
                "Wintertodt Camp",
                CoordGrid(1627, 3941, 0),
                varbit = "varbit.zeah_playerhasvisited",
            )

            destination(
                "dbrow.teleport_jewellery_dueling_emir_s_arena",
                "Emir's Arena",
                CoordGrid(3315, 3235, 0),
            )
            destination(
                "dbrow.teleport_jewellery_dueling_castle_wars",
                "Castle Wars",
                CoordGrid(2440, 3090, 0),
            )
            destination(
                "dbrow.teleport_jewellery_dueling_ferox_enclave",
                "Ferox Enclave",
                CoordGrid(3151, 3636, 0),
            )
            destination(
                "dbrow.teleport_jewellery_dueling_fortis_colosseum",
                "Fortis Colosseum",
                CoordGrid(1793, 3107, 0),
            )

            destination(
                "dbrow.teleport_jewellery_skills_fishing_guild",
                "Fishing Guild",
                CoordGrid(2613, 3389, 0),
            )
            destination(
                "dbrow.teleport_jewellery_skills_mining_guild",
                "Mining Guild",
                CoordGrid(3048, 9763, 0),
            )
            destination(
                "dbrow.teleport_jewellery_skills_crafting_guild",
                "Crafting Guild",
                CoordGrid(2935, 3295, 0),
            )
            destination(
                "dbrow.teleport_jewellery_skills_cooking_guild",
                "Cooking Guild",
                CoordGrid(3143, 3439, 0),
            )
            destination(
                "dbrow.teleport_jewellery_skills_woodcutting_guild",
                "Woodcutting Guild",
                CoordGrid(1661, 3505, 0),
            )
            row("dbrow.teleport_jewellery_skills_farming_guild") {
                column(LABEL, "Farming Guild")
                columnCoord(COORDS, CoordGrid(1248, 3719, 0))
                columnRSCM(BOOST_STAT, "stat.farming")
                column(BOOST_LEVEL, 45)
                columnCoord(BOOST_COORDS, CoordGrid(1248, 3725, 0))
            }

            destination(
                "dbrow.teleport_jewellery_combat_warriors_guild",
                "Warriors' Guild",
                CoordGrid(2882, 3547, 0),
            )
            destination(
                "dbrow.teleport_jewellery_combat_champions_guild",
                "Champions' Guild",
                CoordGrid(3192, 3367, 0),
            )
            destination(
                "dbrow.teleport_jewellery_combat_monastery",
                "Monastery",
                CoordGrid(3052, 3487, 0),
            )
            destination(
                "dbrow.teleport_jewellery_combat_ranging_guild",
                "Ranging Guild",
                CoordGrid(2655, 3443, 0),
            )

            destination(
                "dbrow.teleport_jewellery_burning_chaos_temple",
                "Chaos Temple",
                CoordGrid(3235, 3637, 0),
            )
            destination(
                "dbrow.teleport_jewellery_burning_bandit_camp",
                "Bandit Camp",
                CoordGrid(3038, 3651, 0),
            )
            destination(
                "dbrow.teleport_jewellery_burning_lava_maze",
                "Lava Maze",
                CoordGrid(3027, 3839, 0),
            )

            destination(
                "dbrow.teleport_jewellery_digsite_digsite",
                "Digsite",
                CoordGrid(3340, 3445, 0),
            )
            destination(
                "dbrow.teleport_jewellery_digsite_fossil_island",
                "Fossil Island",
                CoordGrid(3763, 3870, 1),
                quest = "quest_bonevoyage",
            )
            destination(
                "dbrow.teleport_jewellery_digsite_lithkren_dungeon",
                "Lithkren Dungeon",
                CoordGrid(3547, 10456, 0),
                quest = "quest_dragonslayer2",
            )

            destination(
                "dbrow.teleport_jewellery_passage_wizards_tower",
                "Wizards' Tower",
                CoordGrid(3113, 3177, 0),
            )
            destination(
                "dbrow.teleport_jewellery_passage_the_outpost",
                "The Outpost",
                CoordGrid(2428, 3349, 0),
            )
            destination(
                "dbrow.teleport_jewellery_passage_eagles_eyrie",
                "Eagles' Eyrie",
                CoordGrid(3406, 3157, 0),
            )
            destination(
                "dbrow.teleport_jewellery_passage_wyrmscraig",
                "Wyrmscraig",
                CoordGrid(2592, 2220, 0),
            )

            destination(
                "dbrow.teleport_jewellery_wealth_miscellania",
                "Miscellania",
                CoordGrid(2534, 3862, 0),
                quest = "quest_throneofmiscellania",
            )
            destination(
                "dbrow.teleport_jewellery_wealth_grand_exchange",
                "Grand Exchange",
                CoordGrid(3163, 3478, 0),
            )
            destination(
                "dbrow.teleport_jewellery_wealth_falador",
                "Falador",
                CoordGrid(2995, 3375, 0),
            )
            destination(
                "dbrow.teleport_jewellery_wealth_dondakan",
                "Dondakan",
                CoordGrid(2824, 10168, 0),
                quest = "quest_betweenarock",
            )

            destination(
                "dbrow.teleport_jewellery_slayer_stronghold",
                "Stronghold Slayer Cave",
                CoordGrid(2431, 3422, 0),
            )
            destination(
                "dbrow.teleport_jewellery_slayer_slayer_tower",
                "Slayer Tower",
                CoordGrid(3421, 3537, 0),
                quest = "quest_priestinperil",
            )
            destination(
                "dbrow.teleport_jewellery_slayer_fremennik_dungeon",
                "Fremennik Slayer Dungeon",
                CoordGrid(2802, 9999, 0),
            )
            destination(
                "dbrow.teleport_jewellery_slayer_tarns_lair",
                "Tarn's Lair",
                CoordGrid(3185, 4601, 0),
                quest = "quest_hauntedmine",
            )
            destination(
                "dbrow.teleport_jewellery_slayer_dark_beasts",
                "Dark Beasts",
                CoordGrid(2028, 4636, 0),
                quest = "quest_mourningsendpart2",
            )
            destination(
                "dbrow.teleport_jewellery_slayer_wyrmscraig_cavern",
                "Wyrmscraig Cavern",
                CoordGrid(2589, 8614, 0),
                quest = "quest_fallenfromgrace",
            )

            destination(
                "dbrow.teleport_jewellery_xeric_lookout",
                "Xeric's Lookout",
                CoordGrid(1579, 3530, 0),
            )
            destination(
                "dbrow.teleport_jewellery_xeric_glade",
                "Xeric's Glade",
                CoordGrid(1752, 3566, 0),
            )
            destination(
                "dbrow.teleport_jewellery_xeric_inferno",
                "Xeric's Inferno",
                CoordGrid(1504, 3815, 0),
            )
            destination(
                "dbrow.teleport_jewellery_xeric_heart",
                "Xeric's Heart",
                CoordGrid(1644, 3673, 0),
            )
        }

    fun pieces() =
        dbTable("dbtable.teleport_jewellery_pieces", serverOnly = true) {
            column("charged", CHARGED, VarType.OBJ)
            column("spent", SPENT, VarType.OBJ)
            column("remains", REMAINS, VarType.OBJ)
            column("noun", NOUN, VarType.STRING)
            column("unit", UNIT, VarType.STRING)
            column("rub_message", RUB_MESSAGE, VarType.STRING)
            column("empty_message", EMPTY_MESSAGE, VarType.STRING)
            column("last_charge_message", LAST_CHARGE_MESSAGE, VarType.STRING)
            column("wilderness_level", WILDERNESS_LEVEL, VarType.INT)
            column("confirm_wilderness", CONFIRM_WILDERNESS, VarType.BOOLEAN)
            column("destinations", DESTINATIONS, VarType.DBROW)
            column("eternal", ETERNAL, VarType.BOOLEAN)
            column("respawn", RESPAWN, VarType.BOOLEAN)
            column("held_op", HELD_OP, VarType.INT)
            column("worn_menu_op", WORN_MENU_OP, VarType.INT)
            column("worn_subop_op", WORN_SUBOP_OP, VarType.INT)

            row("dbrow.teleport_jewellery_piece_glory") {
                columnRSCM(
                    CHARGED,
                    "obj.amulet_of_glory_6",
                    "obj.amulet_of_glory_5",
                    "obj.amulet_of_glory_4",
                    "obj.amulet_of_glory_3",
                    "obj.amulet_of_glory_2",
                    "obj.amulet_of_glory_1",
                )
                columnRSCM(SPENT, "obj.amulet_of_glory")
                gloryText()
                columnRSCM(DESTINATIONS, GLORY)
            }
            row("dbrow.teleport_jewellery_piece_glory_trimmed") {
                columnRSCM(
                    CHARGED,
                    "obj.trail_amulet_of_glory_6",
                    "obj.trail_amulet_of_glory_5",
                    "obj.trail_amulet_of_glory_4",
                    "obj.trail_amulet_of_glory_3",
                    "obj.trail_amulet_of_glory_2",
                    "obj.trail_amulet_of_glory_1",
                )
                columnRSCM(SPENT, "obj.trail_amulet_of_glory")
                gloryText()
                columnRSCM(DESTINATIONS, GLORY)
            }
            row("dbrow.teleport_jewellery_piece_glory_eternal") {
                columnRSCM(CHARGED, "obj.amulet_of_glory_inf")
                gloryText()
                column(ETERNAL, true)
                columnRSCM(DESTINATIONS, GLORY)
            }
            row("dbrow.teleport_jewellery_piece_games") {
                columnRSCM(
                    CHARGED,
                    "obj.necklace_of_minigames_8",
                    "obj.necklace_of_minigames_7",
                    "obj.necklace_of_minigames_6",
                    "obj.necklace_of_minigames_5",
                    "obj.necklace_of_minigames_4",
                    "obj.necklace_of_minigames_3",
                    "obj.necklace_of_minigames_2",
                    "obj.necklace_of_minigames_1",
                )
                text(
                    noun = "games necklace",
                    unit = USE,
                    rub = "You rub the necklace...",
                    empty = "Your necklace hasn't got any charges left.",
                    last = "Your games necklace crumbles to dust.",
                )
                column(WILDERNESS_LEVEL, STANDARD_WILDERNESS)
                column(HELD_OP, RUB)
                columnRSCM(
                    DESTINATIONS,
                    "dbrow.teleport_jewellery_games_burthorpe",
                    "dbrow.teleport_jewellery_games_barbarian_outpost",
                    "dbrow.teleport_jewellery_games_corporeal_beast",
                    "dbrow.teleport_jewellery_games_tears_of_guthix",
                    "dbrow.teleport_jewellery_games_wintertodt_camp",
                )
            }
            row("dbrow.teleport_jewellery_piece_dueling") {
                columnRSCM(
                    CHARGED,
                    "obj.ring_of_dueling_8",
                    "obj.ring_of_dueling_7",
                    "obj.ring_of_dueling_6",
                    "obj.ring_of_dueling_5",
                    "obj.ring_of_dueling_4",
                    "obj.ring_of_dueling_3",
                    "obj.ring_of_dueling_2",
                    "obj.ring_of_dueling_1",
                )
                text(
                    noun = "ring of dueling",
                    unit = USE,
                    rub = "You rub the ring...",
                    empty = "Your ring hasn't got any charges left.",
                    last = "Your ring of dueling crumbles to dust.",
                )
                column(WILDERNESS_LEVEL, STANDARD_WILDERNESS)
                column(HELD_OP, RUB)
                columnRSCM(
                    DESTINATIONS,
                    "dbrow.teleport_jewellery_dueling_emir_s_arena",
                    "dbrow.teleport_jewellery_dueling_castle_wars",
                    "dbrow.teleport_jewellery_dueling_ferox_enclave",
                    "dbrow.teleport_jewellery_dueling_fortis_colosseum",
                )
            }
            row("dbrow.teleport_jewellery_piece_skills") {
                columnRSCM(
                    CHARGED,
                    "obj.jewl_necklace_of_skills_6",
                    "obj.jewl_necklace_of_skills_5",
                    "obj.jewl_necklace_of_skills_4",
                    "obj.jewl_necklace_of_skills_3",
                    "obj.jewl_necklace_of_skills_2",
                    "obj.jewl_necklace_of_skills_1",
                )
                columnRSCM(SPENT, "obj.jewl_necklace_of_skills")
                column(NOUN, "skills necklace")
                column(UNIT, CHARGE)
                column(
                    EMPTY_MESSAGE,
                    "You will need to recharge your skills necklace before you can use it again.",
                )
                column(LAST_CHARGE_MESSAGE, "You use your skills necklace's last charge.")
                column(WILDERNESS_LEVEL, DRAGONSTONE_WILDERNESS)
                column(HELD_OP, RUB)
                columnRSCM(
                    DESTINATIONS,
                    "dbrow.teleport_jewellery_skills_fishing_guild",
                    "dbrow.teleport_jewellery_skills_mining_guild",
                    "dbrow.teleport_jewellery_skills_crafting_guild",
                    "dbrow.teleport_jewellery_skills_cooking_guild",
                    "dbrow.teleport_jewellery_skills_woodcutting_guild",
                    "dbrow.teleport_jewellery_skills_farming_guild",
                )
            }
            row("dbrow.teleport_jewellery_piece_combat") {
                columnRSCM(
                    CHARGED,
                    "obj.jewl_bracelet_of_combat_6",
                    "obj.jewl_bracelet_of_combat_5",
                    "obj.jewl_bracelet_of_combat_4",
                    "obj.jewl_bracelet_of_combat_3",
                    "obj.jewl_bracelet_of_combat_2",
                    "obj.jewl_bracelet_of_combat_1",
                )
                columnRSCM(SPENT, "obj.jewl_bracelet_of_combat")
                text(
                    noun = "combat bracelet",
                    unit = CHARGE,
                    rub = "You rub the bracelet...",
                    empty =
                        "You will need to recharge your combat bracelet before you can use it " +
                            "again.",
                    last = "You use your combat bracelet's last charge.",
                )
                column(WILDERNESS_LEVEL, DRAGONSTONE_WILDERNESS)
                column(HELD_OP, RUB)
                columnRSCM(
                    DESTINATIONS,
                    "dbrow.teleport_jewellery_combat_warriors_guild",
                    "dbrow.teleport_jewellery_combat_champions_guild",
                    "dbrow.teleport_jewellery_combat_monastery",
                    "dbrow.teleport_jewellery_combat_ranging_guild",
                )
            }
            row("dbrow.teleport_jewellery_piece_burning") {
                columnRSCM(
                    CHARGED,
                    "obj.burning_amulet_5",
                    "obj.burning_amulet_4",
                    "obj.burning_amulet_3",
                    "obj.burning_amulet_2",
                    "obj.burning_amulet_1",
                )
                text(
                    noun = "burning amulet",
                    unit = USE,
                    rub = "You rub the amulet...",
                    empty = "Your amulet hasn't got any charges left.",
                    last = "Your burning amulet crumbles away.",
                )
                column(WILDERNESS_LEVEL, STANDARD_WILDERNESS)
                column(CONFIRM_WILDERNESS, true)
                column(HELD_OP, RUB)
                columnRSCM(
                    DESTINATIONS,
                    "dbrow.teleport_jewellery_burning_chaos_temple",
                    "dbrow.teleport_jewellery_burning_bandit_camp",
                    "dbrow.teleport_jewellery_burning_lava_maze",
                )
            }
            row("dbrow.teleport_jewellery_piece_digsite") {
                columnRSCM(
                    CHARGED,
                    "obj.necklace_of_digsite_5",
                    "obj.necklace_of_digsite_4",
                    "obj.necklace_of_digsite_3",
                    "obj.necklace_of_digsite_2",
                    "obj.necklace_of_digsite_1",
                )
                text(
                    noun = "Digsite Pendant",
                    unit = USE,
                    rub = "You rub the pendant...",
                    empty = "Your pendant hasn't got any charges left.",
                    last = "Your Digsite Pendant crumbles to dust.",
                )
                column(WILDERNESS_LEVEL, STANDARD_WILDERNESS)
                column(HELD_OP, RUB)
                columnRSCM(
                    DESTINATIONS,
                    "dbrow.teleport_jewellery_digsite_digsite",
                    "dbrow.teleport_jewellery_digsite_fossil_island",
                    "dbrow.teleport_jewellery_digsite_lithkren_dungeon",
                )
            }
            row("dbrow.teleport_jewellery_piece_passage") {
                columnRSCM(
                    CHARGED,
                    "obj.necklace_of_passage_5",
                    "obj.necklace_of_passage_4",
                    "obj.necklace_of_passage_3",
                    "obj.necklace_of_passage_2",
                    "obj.necklace_of_passage_1",
                )
                text(
                    noun = "necklace of passage",
                    unit = USE,
                    rub = "You rub the necklace...",
                    empty = "Your necklace hasn't got any charges left.",
                    last = "Your necklace of passage crumbles away.",
                )
                column(WILDERNESS_LEVEL, STANDARD_WILDERNESS)
                column(HELD_OP, RUB)
                columnRSCM(
                    DESTINATIONS,
                    "dbrow.teleport_jewellery_passage_wizards_tower",
                    "dbrow.teleport_jewellery_passage_the_outpost",
                    "dbrow.teleport_jewellery_passage_eagles_eyrie",
                    "dbrow.teleport_jewellery_passage_wyrmscraig",
                )
            }
            row("dbrow.teleport_jewellery_piece_wealth") {
                columnRSCM(
                    CHARGED,
                    "obj.ring_of_wealth_5",
                    "obj.ring_of_wealth_4",
                    "obj.ring_of_wealth_3",
                    "obj.ring_of_wealth_2",
                    "obj.ring_of_wealth_1",
                )
                columnRSCM(SPENT, "obj.ring_of_wealth")
                wealthText()
                columnRSCM(DESTINATIONS, WEALTH)
            }
            row("dbrow.teleport_jewellery_piece_wealth_imbued") {
                columnRSCM(
                    CHARGED,
                    "obj.ring_of_wealth_i5",
                    "obj.ring_of_wealth_i4",
                    "obj.ring_of_wealth_i3",
                    "obj.ring_of_wealth_i2",
                    "obj.ring_of_wealth_i1",
                )
                columnRSCM(SPENT, "obj.ring_of_wealth_i")
                wealthText()
                columnRSCM(DESTINATIONS, WEALTH)
            }
            row("dbrow.teleport_jewellery_piece_slayer") {
                columnRSCM(
                    CHARGED,
                    "obj.slayer_ring_8",
                    "obj.slayer_ring_7",
                    "obj.slayer_ring_6",
                    "obj.slayer_ring_5",
                    "obj.slayer_ring_4",
                    "obj.slayer_ring_3",
                    "obj.slayer_ring_2",
                    "obj.slayer_ring_1",
                )
                columnRSCM(REMAINS, "obj.slayer_gem")
                slayerText()
                columnRSCM(DESTINATIONS, SLAYER)
            }
            row("dbrow.teleport_jewellery_piece_slayer_eternal") {
                columnRSCM(CHARGED, "obj.slayer_ring_eternal")
                slayerText()
                column(ETERNAL, true)
                columnRSCM(DESTINATIONS, SLAYER)
            }
            row("dbrow.teleport_jewellery_piece_returning") {
                columnRSCM(
                    CHARGED,
                    "obj.ring_of_returning_5",
                    "obj.ring_of_returning_4",
                    "obj.ring_of_returning_3",
                    "obj.ring_of_returning_2",
                    "obj.ring_of_returning_1",
                )
                text(
                    noun = "ring of returning",
                    unit = USE,
                    rub = "You rub the ring...",
                    empty = "Your ring hasn't got any charges left.",
                    last = "Your ring of returning crumbles away.",
                )
                column(WILDERNESS_LEVEL, STANDARD_WILDERNESS)
                column(RESPAWN, true)
                column(HELD_OP, RUB)
                column(WORN_MENU_OP, 2)
            }
        }

    private fun DBTableBuilder.destination(
        name: String,
        label: String,
        coords: CoordGrid,
        quest: String? = null,
        varbit: String? = null,
    ) {
        row(name) {
            column(LABEL, label)
            columnCoord(COORDS, coords)
            quest?.let { column(QUEST, it) }
            varbit?.let { columnRSCM(UNLOCK_VARBIT, it) }
        }
    }

    private fun DBRowBuilder.text(
        noun: String,
        unit: String,
        rub: String?,
        empty: String,
        last: String,
    ) {
        column(NOUN, noun)
        column(UNIT, unit)
        rub?.let { column(RUB_MESSAGE, it) }
        column(EMPTY_MESSAGE, empty)
        column(LAST_CHARGE_MESSAGE, last)
    }

    private fun DBRowBuilder.gloryText() {
        text(
            noun = "amulet",
            unit = CHARGE,
            rub = "You rub the amulet...",
            empty = "Your amulet hasn't got any charges left.",
            last = "You use your amulet's last charge.",
        )
        column(WILDERNESS_LEVEL, DRAGONSTONE_WILDERNESS)
        column(HELD_OP, RUB)
    }

    private fun DBRowBuilder.wealthText() {
        text(
            noun = "ring of wealth",
            unit = CHARGE,
            rub = "You rub the ring...",
            empty = "Your ring hasn't got any charges left.",
            last = "You use your ring's last charge.",
        )
        column(WILDERNESS_LEVEL, DRAGONSTONE_WILDERNESS)
        column(HELD_OP, RUB)
    }

    private fun DBRowBuilder.slayerText() {
        text(
            noun = "slayer ring",
            unit = CHARGE,
            rub = null,
            empty = "Your slayer ring hasn't got any charges left.",
            last = "Your slayer ring crumbles to dust.",
        )
        column(WILDERNESS_LEVEL, DRAGONSTONE_WILDERNESS)
        column(HELD_OP, TELEPORT)
        column(WORN_MENU_OP, 3)
        column(WORN_SUBOP_OP, 3)
    }
}
