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
//   - Limpwurt seed [main/1/{{#expr:1/({{#var:uncseed}}*137) round 1}}]
//   - Strawberry seed [main/1/{{#expr:1/({{#var:uncseed}}*131) round 1}}]
//   - Marrentill seed [main/1/{{#expr:1/({{#var:uncseed}}*125) round 1}}]
//   - Jangerberry seed [main/1/{{#expr:1/({{#var:uncseed}}*92) round 1}}]
//   - Tarromin seed [main/1/{{#expr:1/({{#var:uncseed}}*85) round 1}}]
//   - Wildblood seed [main/1/{{#expr:1/({{#var:uncseed}}*83) round 1}}]
//   - Watermelon seed [main/1/{{#expr:1/({{#var:uncseed}}*63) round 1}}]
//   - Toadflax seed [main/1/{{#expr:1/({{#var:rareseed}}*216 + {{#var:uncseed}}*27) round 1}}]
//   - Harralander seed [main/1/{{#expr:1/({{#var:uncseed}}*56) round 1}}]
//   - Snape grass seed [main/1/{{#expr:1/({{#var:uncseed}}*40) round 1}}]
//   - Irit seed [main/1/{{#expr:1/({{#var:rareseed}}*148 + {{#var:uncseed}}*18) round 1}}]
//   - Ranarr seed [main/1/{{#expr:1/({{#var:uncseed}}*39) round 1}}]
//   - Belladonna seed [main/1/{{#expr:1/({{#var:rareseed}}*143 + {{#var:uncseed}}*18) round 1}}]
//   - Whiteberry seed [main/1/{{#expr:1/({{#var:uncseed}}*34) round 1}}]
//   - Mushroom spore [main/1/{{#expr:1/({{#var:uncseed}}*29) round 1}}]
//   - Poison ivy seed [main/1/{{#expr:1/({{#var:rareseed}}*101 + {{#var:uncseed}}*13) round 1}}]
//   - Avantoe seed [main/1/{{#expr:1/({{#var:rareseed}}*103 + {{#var:uncseed}}*12) round 1}}]
//   - Cactus seed [main/1/{{#expr:1/({{#var:rareseed}}*96 + {{#var:uncseed}}*12) round 1}}]
//   - Kwuarm seed [main/1/{{#expr:1/({{#var:rareseed}}*69 + {{#var:uncseed}}*9) round 1}}]
//   - Potato cactus seed [main/1/{{#expr:1/({{#var:rareseed}}*70 + {{#var:uncseed}}*8) round 1}}]
//   - Snapdragon seed [main/1/{{#expr:1/({{#var:rareseed}}*46 + {{#var:uncseed}}*5) round 1}}]
//   - Cadantine seed [main/1/{{#expr:1/({{#var:rareseed}}*32 + {{#var:uncseed}}*4) round 1}}]
//   - Lantadyme seed [main/1/{{#expr:1/({{#var:rareseed}}*23 + {{#var:uncseed}}*3) round 1}}]
//   - Dwarf weed seed [main/1/{{#expr:1/({{#var:rareseed}}*14 + {{#var:uncseed}}*2) round 1}}]
//   - Snape grass seed [main/1/{{#expr:1/({{#var:rareseed}}*20) round 1}}]
//   - Torstol seed [main/1/{{#expr:1/({{#var:rareseed}}*9 + {{#var:uncseed}}*1) round 0}}]
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
