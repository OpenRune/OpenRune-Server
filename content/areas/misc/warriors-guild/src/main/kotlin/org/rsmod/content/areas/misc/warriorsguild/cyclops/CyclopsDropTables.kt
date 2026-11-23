package org.rsmod.content.areas.misc.warriorsguild.cyclops

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.areas.misc.warriorsguild.Defenders
import org.rsmod.content.areas.misc.warriorsguild.cyclopsRoom
import org.rsmod.content.areas.misc.warriorsguild.cyclopsTier
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.game.entity.Player

/**
 * Only the defender after the best one the killer owned when they walked in can drop, so the seven
 * 2/100 defender lines on the wiki are one line here: 2/100 for that defender, 12/100 nothing.
 */
@field:RegisterDropTable
@JvmField
public val warriorsGuildCyclopsDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Warriors' Guild Cyclops",
        npcs =
            npcs(
                "npc.warguild_cyclops1",
                "npc.warguild_cyclops2",
                "npc.warguild_cyclops3",
                "npc.warguild_cyclops4",
                "npc.warguild_cyclops5",
                "npc.warguild_cyclops6",
            ),
        mainTable =
            rsPlayerWeightedTable(total = 105) {
                name("Warriors' Guild Cyclops")
                16 weight "obj.black_knife" count 4..13
                2 weight "obj.steel_chainbody" count 1
                2 weight "obj.iron_2h_sword" count 1
                2 weight "obj.iron_chainbody" count 1
                2 weight "obj.steel_dagger" count 1
                2 weight "obj.steel_mace" count 1
                2 weight "obj.steel_sword" count 1
                2 weight "obj.steel_battleaxe" count 1
                2 weight "obj.steel_2h_sword" count 1
                2 weight "obj.steel_longsword" count 1
                2 weight "obj.steel_med_helm" count 1
                1 weight "obj.black_2h_sword" count 1
                1 weight "obj.mithril_dagger" count 1
                1 weight "obj.mithril_longsword" count 1
                1 weight "obj.adamant_mace" count 1
                1 weight "obj.black_sword" count 1
                1 weight "obj.black_longsword" count 1
                1 weight "obj.black_dagger" count 1
                1 weight "obj.adamant_2h_sword" count 1
                boosted {
                    2 weight "obj.bronze_parryingdagger" count 1 transformObj { player ->
                        Defenders.drop(player.cyclopsTier)
                    }
                }
                12 weight nothing()
                3 weight SharedDropTables.herb
                1 weight SharedDropTables.seed
                31 weight "obj.coins" count 3..102
                10 weight "obj.coins" count 5..204
                2 weight SharedDropTables.gem
            },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 486 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
                    player.clueScrollTransformObj("obj.trail_clue_hard_map001")
                }
                1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
                1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
            },
    )

@field:RegisterDropTable
@JvmField
public val warriorsGuildBasementCyclopsDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Warriors' Guild Basement Cyclops",
        npcs =
            npcs(
                "npc.warguild_cyclops1_high",
                "npc.warguild_cyclops2_high",
                "npc.warguild_cyclops3_high",
                "npc.warguild_cyclops4_high",
                "npc.warguild_cyclops5_high",
                "npc.warguild_cyclops6_high",
            ),
        preRoll =
            rsPlayerPrerollTable {
                boosted {
                    1 outOf 100 weight "obj.dragon_parryingdagger" count 1 condition { player ->
                        player.cyclopsRoom == CyclopsRooms.BASEMENT
                    }
                }
            },
        mainTable =
            rsPlayerWeightedTable(total = 100) {
                name("Warriors' Guild Basement Cyclops")
                4 weight "obj.adamant_2h_sword" count 1
                6 weight "obj.mithril_dart" count 12
                5 weight "obj.adamant_dagger" count 1
                5 weight "obj.black_full_helm" count 1
                5 weight "obj.black_mace" count 1
                5 weight "obj.mithril_scimitar" count 1
                5 weight "obj.mithril_kiteshield" count 1
                5 weight "obj.steel_platebody" count 1
                7 weight "obj.steel_chainbody" count 1
                4 weight "obj.black_knife" count 22
                4 weight "obj.mithril_platelegs" count 1
                5 weight "obj.black_dagger" count 1
                3 weight "obj.rune_med_helm" count 1
                1 weight "obj.adamant_sq_shield" count 1
                1 weight "obj.rune_full_helm" count 1
                4 weight SharedDropTables.herb
                3 weight SharedDropTables.seed
                11 weight "obj.coins" count 96
                8 weight "obj.coins" count 350..449
                7 weight "obj.lobster" count 1
                2 weight SharedDropTables.rareDrop
            },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 243 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
                    player.clueScrollTransformObj("obj.trail_clue_hard_map001")
                }
                1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
                1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
            },
    )
