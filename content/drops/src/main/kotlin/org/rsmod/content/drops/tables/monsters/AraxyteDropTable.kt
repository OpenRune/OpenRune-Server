package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.RSWeightedTable
import dtx.rs.npcs
import dtx.rs.rsWeightedTable
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val araxyteDropTable: RSDropTable<Player, DropRollItem> =
    araxyteTable(
        name = "Araxyte (level 96) Drops",
        npcs = npcs("npc.araxyte_normal", "npc.araxyte_normal_bigwander"),
        multiplier = 1,
        clueRate = 128,
        bootsRate = 4000,
        bootsTaskRate = 1600,
    )

@field:RegisterDropTable
@JvmField
public val largeAraxyteDropTable: RSDropTable<Player, DropRollItem> =
    araxyteTable(
        name = "Araxyte (level 146) Drops",
        npcs = npcs("npc.araxyte_large", "npc.araxyte_large_bigwander"),
        multiplier = 2,
        clueRate = 86,
        bootsRate = 2000,
        bootsTaskRate = 800,
    )

private fun araxyteTable(
    name: String,
    npcs: List<String>,
    multiplier: Int,
    clueRate: Int,
    bootsRate: Int,
    bootsTaskRate: Int,
): RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = name,
        npcs = npcs,
        guaranteed = rsPlayerGuaranteedTable { "obj.araxyte_venom_sack" count 1 },
        mainTable =
            rsPlayerWeightedTable(total = 127) {
                name(name)
                5 weight "obj.adamant_longsword" count multiplier
                5 weight "obj.adamant_battleaxe" count multiplier
                3 weight "obj.rune_dagger" count multiplier
                2 weight "obj.rune_med_helm" count multiplier
                2 weight "obj.rune_platelegs" count multiplier
                10 weight "obj.airrune" count (120 * multiplier)..(140 * multiplier)
                10 weight "obj.waterrune" count (120 * multiplier)..(140 * multiplier)
                10 weight "obj.earthrune" count (120 * multiplier)..(140 * multiplier)
                10 weight "obj.firerune" count (120 * multiplier)..(140 * multiplier)
                5 weight "obj.cosmicrune" count (7 * multiplier)..(12 * multiplier)
                5 weight "obj.chaosrune" count (10 * multiplier)..(15 * multiplier)
                5 weight "obj.naturerune" count (15 * multiplier)..(20 * multiplier)
                5 weight "obj.deathrune" count (20 * multiplier)..(25 * multiplier)
                5 weight "obj.lawrune" count (12 * multiplier)..(15 * multiplier)
                5 weight "obj.bloodrune" count (15 * multiplier)..(18 * multiplier)
                5 weight "obj.soulrune" count (9 * multiplier)..(12 * multiplier)
                5 weight "obj.araxyte_venom_sack" count 2 * multiplier
                10 weight "obj.coins" count (800 * multiplier)..(1200 * multiplier)
                10 weight herbs(multiplier)
                10 weight uncommonSeeds(multiplier)
                1 outOf 2000 separate "obj.poh_araxyte_head" count 1
            },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf clueRate weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
                    player.clueScrollTransformObj("obj.trail_clue_hard_map001")
                }
                boosted {
                    1 outOf bootsTaskRate weight "obj.aranea_boots" count 1 killCondition { player, npc, _ ->
                        player.onTaskFor(npc)
                    }
                    1 outOf bootsRate weight "obj.aranea_boots" count 1 killCondition { player, npc, _ ->
                        !player.onTaskFor(npc)
                    }
                }
            },
    )

private fun Player.onTaskFor(npc: Npc): Boolean =
    vars["varp.slayer_count"] > 0 &&
        SlayerTaskManager.isTaskNpc(npc, SlayerTaskCredit.taskKey(this))

private fun herbs(count: Int): RSWeightedTable<Player, DropRollItem> =
    rsWeightedTable(total = 128) {
        name("Araxyte herbs")
        32 weight DropRollItem("obj.unidentified_guam", count)
        24 weight DropRollItem("obj.unidentified_marentill", count)
        18 weight DropRollItem("obj.unidentified_tarromin", count)
        14 weight DropRollItem("obj.unidentified_harralander", count)
        11 weight DropRollItem("obj.unidentified_ranarr", count)
        8 weight DropRollItem("obj.unidentified_irit", count)
        6 weight DropRollItem("obj.unidentified_avantoe", count)
        5 weight DropRollItem("obj.unidentified_kwuarm", count)
        4 weight DropRollItem("obj.unidentified_cadantine", count)
        3 weight DropRollItem("obj.unidentified_lantadyme", count)
        3 weight DropRollItem("obj.unidentified_dwarf_weed", count)
    }

private fun uncommonSeeds(count: Int): RSWeightedTable<Player, DropRollItem> =
    rsWeightedTable(total = 1048) {
        name("Araxyte uncommon seeds")
        137 weight DropRollItem("obj.limpwurt_seed", count)
        131 weight DropRollItem("obj.strawberry_seed", count)
        125 weight DropRollItem("obj.marrentill_seed", count)
        92 weight DropRollItem("obj.jangerberry_bush_seed", count)
        85 weight DropRollItem("obj.tarromin_seed", count)
        83 weight DropRollItem("obj.wildblood_hop_seed", count)
        63 weight DropRollItem("obj.watermelon_seed", count)
        56 weight DropRollItem("obj.harralander_seed", count)
        40 weight DropRollItem("obj.snape_grass_seed", count)
        39 weight DropRollItem("obj.ranarr_seed", count)
        34 weight DropRollItem("obj.whiteberry_bush_seed", count)
        29 weight DropRollItem("obj.mushroom_seed", count)
        27 weight DropRollItem("obj.toadflax_seed", count)
        18 weight DropRollItem("obj.belladonna_seed", count)
        18 weight DropRollItem("obj.irit_seed", count)
        13 weight DropRollItem("obj.poisonivy_bush_seed", count)
        12 weight DropRollItem("obj.avantoe_seed", count)
        12 weight DropRollItem("obj.cactus_seed", count)
        9 weight DropRollItem("obj.kwuarm_seed", count)
        8 weight DropRollItem("obj.potato_cactus_seed", count)
        5 weight DropRollItem("obj.snapdragon_seed", count)
        4 weight DropRollItem("obj.cadantine_seed", count)
        3 weight DropRollItem("obj.lantadyme_seed", count)
        2 weight DropRollItem("obj.dwarf_weed_seed", count)
        1 weight DropRollItem("obj.torstol_seed", count)
        2 weight ringNothing()
    }
