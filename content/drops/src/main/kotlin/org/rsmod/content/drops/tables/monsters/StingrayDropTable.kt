package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val stingrayDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Stingray Drops",
    npcs = npcs("npc.sailing_stingray"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 30 weight "obj.ray_barbs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Stingray Drops")
        3 weight "obj.cert_bucket_sand" count 10..15
        3 weight "obj.cert_seaweed" count 10..15
        2 weight "obj.weapon_poison+" count 1
        2 weight "obj.weapon_poison++" count 1
        18 outOf 500 separate "obj.coral_elkhorn_frag" count 1..2
        9 outOf 500 separate "obj.coral_pillar_frag" count 1..2
        3 outOf 500 separate "obj.coral_umbral_frag" count 1..2
        15 outOf 400 separate "obj.camphor_seed" count 1
        4 outOf 400 separate "obj.ironwood_seed" count 1
        1 outOf 400 separate "obj.rosewood_seed" count 1

        4 weight SharedDropTables.combatHerb
        86 weight nothing()
        10 outOf 191 separate "obj.limpwurt_seed" count 1
        1 outOf 20 separate "obj.strawberry_seed" count 1
        10 outOf 209 separate "obj.marrentill_seed" count 1
        5 outOf 142 separate "obj.jangerberry_bush_seed" count 1
        5 outOf 154 separate "obj.tarromin_seed" count 1
        2 outOf 63 separate "obj.wildblood_hop_seed" count 1
        2 outOf 83 separate "obj.watermelon_seed" count 1
        1 outOf 45 separate "obj.toadflax_seed" count 1
        10 outOf 467 separate "obj.harralander_seed" count 1
        5 outOf 327 separate "obj.snape_grass_seed" count 1
        2 outOf 133 separate "obj.irit_seed" count 1
        10 outOf 671 separate "obj.ranarr_seed" count 1
        5 outOf 339 separate "obj.belladonna_seed" count 1
        10 outOf 769 separate "obj.whiteberry_bush_seed" count 1
        5 outOf 451 separate "obj.mushroom_seed" count 1
        1 outOf 95 separate "obj.poisonivy_bush_seed" count 1
        2 outOf 195 separate "obj.avantoe_seed" count 1
        10 outOf 1013 separate "obj.cactus_seed" count 1
        10 outOf 1381 separate "obj.kwuarm_seed" count 1
        10 outOf 1447 separate "obj.potato_cactus_seed" count 1
        1 outOf 225 separate "obj.snapdragon_seed" count 1
        5 outOf 1519 separate "obj.cadantine_seed" count 1
        5 outOf 2072 separate "obj.lantadyme_seed" count 1
        10 outOf 6513 separate "obj.dwarf_weed_seed" count 1
        10 outOf 9083 separate "obj.snape_grass_seed" count 3
        1 outOf 1139 separate "obj.torstol_seed" count 1
        10 outOf 143 separate "obj.unidentified_guam" count 1..2
        1 outOf 19 separate "obj.unidentified_marentill" count 1..2
        5 outOf 127 separate "obj.unidentified_tarromin" count 1..2
        10 outOf 327 separate "obj.unidentified_harralander" count 1..2
        5 outOf 188 separate "obj.unidentified_ranarr" count 1..2
        10 outOf 571 separate "obj.unidentified_irit" count 1..2
        2 outOf 123 separate "obj.unidentified_avantoe" count 1..2
        1 outOf 400 separate "obj.unidentified_snapdragon" count 1..2
        10 outOf 5333 separate "obj.unidentified_torstol" count 1..2
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_stingray_skin" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 2 weight "obj.sailing_stingray_fin" count 1
        1 outOf 175 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Grimy kwuarm [main/1/{{#expr:1/({{#var:hdt}}*5) round 1}}]
//   - Grimy cadantine [main/1/{{#expr:1/({{#var:hdt}}*4) round 1}}]
//   - Grimy lantadyme [main/1/{{#expr:1/({{#var:hdt}}*3) round 1}}]
//   - Grimy dwarf weed [main/1/{{#expr:1/({{#var:hdt}}*3) round 1}}]
