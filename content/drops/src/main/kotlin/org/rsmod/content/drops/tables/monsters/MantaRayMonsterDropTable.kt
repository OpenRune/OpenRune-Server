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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mantaRayMonsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Manta ray (monster) Drops",
    npcs = npcs("npc.sailing_manta_ray"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 20 weight "obj.ray_barbs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 99) {
        name("Manta ray (monster) Drops")
        15 weight "obj.raw_mantaray" count 1
        5 weight "obj.cert_bucket_sand" count 10..15
        5 weight "obj.cert_seaweed" count 10..15
        16 outOf 297 separate "obj.coral_pillar_frag" count 1
        8 outOf 297 separate "obj.coral_umbral_frag" count 1
        15 outOf 1980 separate "obj.camphor_seed" count 1
        4 outOf 1980 separate "obj.ironwood_seed" count 1
        1 outOf 1980 separate "obj.rosewood_seed" count 1

        4 weight SharedDropTables.combatHerb
        70 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_manta_ray_skin" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/114 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 120 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Toadflax seed [main/1/{{#expr:1/({{#var:rareseed}}*216) round 1}}]
//   - Irit seed [main/1/{{#expr:1/({{#var:rareseed}}*148) round 1}}]
//   - Belladonna seed [main/1/{{#expr:1/({{#var:rareseed}}*143) round 1}}]
//   - Avantoe seed [main/1/{{#expr:1/({{#var:rareseed}}*103) round 1}}]
//   - Poison ivy seed [main/1/{{#expr:1/({{#var:rareseed}}*101) round 1}}]
//   - Cactus seed [main/1/{{#expr:1/({{#var:rareseed}}*96) round 1}}]
//   - Potato cactus seed [main/1/{{#expr:1/({{#var:rareseed}}*70) round 1}}]
//   - Kwuarm seed [main/1/{{#expr:1/({{#var:rareseed}}*69) round 1}}]
//   - Snapdragon seed [main/1/{{#expr:1/({{#var:rareseed}}*46 + {{#var:treeseed}}*28) round 1}}]
//   - Cadantine seed [main/1/{{#expr:1/({{#var:rareseed}}*32) round 1}}]
//   - Lantadyme seed [main/1/{{#expr:1/({{#var:rareseed}}*23) round 1}}]
//   - Snape grass seed [main/1/{{#expr:1/({{#var:rareseed}}*20) round 1}}]
//   - Dwarf weed seed [main/1/{{#expr:1/({{#var:rareseed}}*14) round 1}}]
//   - Torstol seed [main/1/{{#expr:1/({{#var:rareseed}}*9 + {{#var:treeseed}}*22) round 1}}]
//   - Ranarr seed [main/1/{{#expr:1/({{#var:treeseed}}*30) round 1}}]
//   - Watermelon seed [main/1/{{#expr:1/({{#var:treeseed}}*21) round 1}}]
//   - Willow seed [main/1/{{#expr:1/({{#var:treeseed}}*20) round 1}}]
//   - Mahogany seed [main/1/{{#expr:1/({{#var:treeseed}}*18) round 1}}]
//   - Maple seed [main/1/{{#expr:1/({{#var:treeseed}}*18) round 1}}]
//   - Teak seed [main/1/{{#expr:1/({{#var:treeseed}}*18) round 1}}]
//   - Yew seed [main/1/{{#expr:1/({{#var:treeseed}}*18) round 1}}]
//   - Papaya tree seed [main/1/{{#expr:1/({{#var:treeseed}}*14) round 1}}]
//   - Magic seed [main/1/{{#expr:1/({{#var:treeseed}}*11) round 1}}]
//   - Palm tree seed [main/1/{{#expr:1/({{#var:treeseed}}*10) round 1}}]
//   - Spirit seed [main/1/{{#expr:1/({{#var:treeseed}}*8) round 1}}]
//   - Dragonfruit tree seed [main/1/{{#expr:1/({{#var:treeseed}}*6) round 1}}]
//   - Celastrus seed [main/1/{{#expr:1/({{#var:treeseed}}*4) round 1}}]
//   - Redwood tree seed [main/1/{{#expr:1/({{#var:treeseed}}*4) round 1}}]
//   - Grimy guam leaf [main/1/{{#expr:1/({{#var:hdt}}*32) round 1}}]
//   - Grimy marrentill [main/1/{{#expr:1/({{#var:hdt}}*24) round 1}}]
//   - Grimy tarromin [main/1/{{#expr:1/({{#var:hdt}}*18) round 1}}]
//   - Grimy harralander [main/1/{{#expr:1/({{#var:hdt}}*14) round 1}}]
//   - Grimy ranarr weed [main/1/{{#expr:1/({{#var:hdt}}*11 + {{#var:usefulHdt}}*4) round 1}}]
//   - Grimy irit leaf [main/1/{{#expr:1/({{#var:hdt}}*8) round 1}}]
//   - Grimy avantoe [main/1/{{#expr:1/({{#var:hdt}}*6 + {{#var:usefulHdt}}*5) round 1}}]
//   - Grimy kwuarm [main/1/{{#expr:1/({{#var:hdt}}*5) round 1}}]
//   - Grimy cadantine [main/1/{{#expr:1/({{#var:hdt}}*4) round 1}}]
//   - Grimy lantadyme [main/1/{{#expr:1/({{#var:hdt}}*3) round 1}}]
//   - Grimy dwarf weed [main/1/{{#expr:1/({{#var:hdt}}*3) round 1}}]
//   - Grimy snapdragon [main/1/{{#expr:1/({{#var:usefulHdt}}*4) round 1}}]
//   - Grimy torstol [main/1/{{#expr:1/({{#var:usefulHdt}}*3) round 1}}]
