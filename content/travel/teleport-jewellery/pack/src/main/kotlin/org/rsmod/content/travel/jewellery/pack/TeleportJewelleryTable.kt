package org.rsmod.content.travel.jewellery.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object TeleportJewelleryTable {
    const val PIECE = 0
    const val LABEL = 1
    const val COORDS = 2
    const val QUEST = 3

    fun destinations() =
        dbTable("dbtable.teleport_jewellery_destinations", serverOnly = true) {
            column("piece", PIECE, VarType.STRING)
            column("label", LABEL, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            column("quest", QUEST, VarType.STRING)
            row("dbrow.teleport_jewellery_glory_edgeville") {
                column(PIECE, "glory")
                column(LABEL, "Edgeville")
                columnCoord(COORDS, CoordGrid(3087, 3496, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_glory_karamja") {
                column(PIECE, "glory")
                column(LABEL, "Karamja")
                columnCoord(COORDS, CoordGrid(2918, 3176, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_glory_draynor_village") {
                column(PIECE, "glory")
                column(LABEL, "Draynor Village")
                columnCoord(COORDS, CoordGrid(3105, 3251, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_glory_al_kharid") {
                column(PIECE, "glory")
                column(LABEL, "Al Kharid")
                columnCoord(COORDS, CoordGrid(3293, 3163, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_games_burthorpe") {
                column(PIECE, "games")
                column(LABEL, "Burthorpe")
                columnCoord(COORDS, CoordGrid(2898, 3552, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_games_barbarian_outpost") {
                column(PIECE, "games")
                column(LABEL, "Barbarian Outpost")
                columnCoord(COORDS, CoordGrid(2519, 3572, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_games_corporeal_beast") {
                column(PIECE, "games")
                column(LABEL, "Corporeal Beast")
                columnCoord(COORDS, CoordGrid(2967, 4381, 2))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_games_tears_of_guthix") {
                column(PIECE, "games")
                column(LABEL, "Tears of Guthix")
                columnCoord(COORDS, CoordGrid(3245, 9500, 2))
                column(QUEST, "quest_tearsofguthix")
            }
            row("dbrow.teleport_jewellery_games_wintertodt_camp") {
                column(PIECE, "games")
                column(LABEL, "Wintertodt Camp")
                columnCoord(COORDS, CoordGrid(1627, 3941, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_dueling_emir_s_arena") {
                column(PIECE, "dueling")
                column(LABEL, "Emir's Arena")
                columnCoord(COORDS, CoordGrid(3315, 3235, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_dueling_castle_wars") {
                column(PIECE, "dueling")
                column(LABEL, "Castle Wars")
                columnCoord(COORDS, CoordGrid(2440, 3090, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_dueling_ferox_enclave") {
                column(PIECE, "dueling")
                column(LABEL, "Ferox Enclave")
                columnCoord(COORDS, CoordGrid(3151, 3636, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_dueling_fortis_colosseum") {
                column(PIECE, "dueling")
                column(LABEL, "Fortis Colosseum")
                columnCoord(COORDS, CoordGrid(1793, 3107, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_skills_fishing_guild") {
                column(PIECE, "skills")
                column(LABEL, "Fishing Guild")
                columnCoord(COORDS, CoordGrid(2613, 3389, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_skills_mining_guild") {
                column(PIECE, "skills")
                column(LABEL, "Mining Guild")
                columnCoord(COORDS, CoordGrid(3048, 9763, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_skills_crafting_guild") {
                column(PIECE, "skills")
                column(LABEL, "Crafting Guild")
                columnCoord(COORDS, CoordGrid(2935, 3295, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_skills_cooking_guild") {
                column(PIECE, "skills")
                column(LABEL, "Cooking Guild")
                columnCoord(COORDS, CoordGrid(3143, 3439, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_skills_woodcutting_guild") {
                column(PIECE, "skills")
                column(LABEL, "Woodcutting Guild")
                columnCoord(COORDS, CoordGrid(1661, 3505, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_skills_farming_guild") {
                column(PIECE, "skills")
                column(LABEL, "Farming Guild")
                columnCoord(COORDS, CoordGrid(1248, 3719, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_combat_warriors_guild") {
                column(PIECE, "combat")
                column(LABEL, "Warriors' Guild")
                columnCoord(COORDS, CoordGrid(2882, 3547, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_combat_champions_guild") {
                column(PIECE, "combat")
                column(LABEL, "Champions' Guild")
                columnCoord(COORDS, CoordGrid(3192, 3367, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_combat_monastery") {
                column(PIECE, "combat")
                column(LABEL, "Monastery")
                columnCoord(COORDS, CoordGrid(3052, 3487, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_combat_ranging_guild") {
                column(PIECE, "combat")
                column(LABEL, "Ranging Guild")
                columnCoord(COORDS, CoordGrid(2655, 3443, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_burning_chaos_temple") {
                column(PIECE, "burning")
                column(LABEL, "Chaos Temple")
                columnCoord(COORDS, CoordGrid(3235, 3637, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_burning_bandit_camp") {
                column(PIECE, "burning")
                column(LABEL, "Bandit Camp")
                columnCoord(COORDS, CoordGrid(3038, 3651, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_burning_lava_maze") {
                column(PIECE, "burning")
                column(LABEL, "Lava Maze")
                columnCoord(COORDS, CoordGrid(3027, 3839, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_digsite_digsite") {
                column(PIECE, "digsite")
                column(LABEL, "Digsite")
                columnCoord(COORDS, CoordGrid(3340, 3445, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_digsite_fossil_island") {
                column(PIECE, "digsite")
                column(LABEL, "Fossil Island")
                columnCoord(COORDS, CoordGrid(3763, 3870, 1))
                column(QUEST, "quest_bonevoyage")
            }
            row("dbrow.teleport_jewellery_digsite_lithkren_dungeon") {
                column(PIECE, "digsite")
                column(LABEL, "Lithkren Dungeon")
                columnCoord(COORDS, CoordGrid(3547, 10456, 0))
                column(QUEST, "quest_dragonslayer2")
            }
            row("dbrow.teleport_jewellery_passage_wizards_tower") {
                column(PIECE, "passage")
                column(LABEL, "Wizards' Tower")
                columnCoord(COORDS, CoordGrid(3113, 3177, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_passage_the_outpost") {
                column(PIECE, "passage")
                column(LABEL, "The Outpost")
                columnCoord(COORDS, CoordGrid(2428, 3349, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_passage_eagles_eyrie") {
                column(PIECE, "passage")
                column(LABEL, "Eagles' Eyrie")
                columnCoord(COORDS, CoordGrid(3406, 3157, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_passage_wyrmscraig") {
                column(PIECE, "passage")
                column(LABEL, "Wyrmscraig")
                columnCoord(COORDS, CoordGrid(2592, 2220, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_wealth_miscellania") {
                column(PIECE, "wealth")
                column(LABEL, "Miscellania")
                columnCoord(COORDS, CoordGrid(2534, 3862, 0))
                column(QUEST, "quest_throneofmiscellania")
            }
            row("dbrow.teleport_jewellery_wealth_grand_exchange") {
                column(PIECE, "wealth")
                column(LABEL, "Grand Exchange")
                columnCoord(COORDS, CoordGrid(3163, 3478, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_wealth_falador") {
                column(PIECE, "wealth")
                column(LABEL, "Falador")
                columnCoord(COORDS, CoordGrid(2995, 3375, 0))
                column(QUEST, "")
            }
            row("dbrow.teleport_jewellery_wealth_dondakan") {
                column(PIECE, "wealth")
                column(LABEL, "Dondakan")
                columnCoord(COORDS, CoordGrid(2824, 10168, 0))
                column(QUEST, "quest_betweenarock")
            }
        }
}
