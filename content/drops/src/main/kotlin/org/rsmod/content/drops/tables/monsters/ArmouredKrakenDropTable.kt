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
public val armouredKrakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Armoured kraken Drops",
    npcs = npcs("npc.sailing_armoured_kraken"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 1280 weight "obj.sailing_boat_large_keel_part_dragon" count 1
        1 outOf 1280 weight "obj.sailing_boat_keel_part_dragon" count 1
        1 outOf 90 weight "obj.dragon_sheet" count 1..2
        1 outOf 500 weight "obj.bottled_storm" count 1
        1 outOf 2000 weight "obj.sailing_paint_inky" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 119) {
        name("Armoured kraken Drops")
        10 weight "obj.deathrune" count 40..65
        10 weight "obj.waterrune" count 400..650
        8 weight "obj.bloodrune" count 25..35
        4 weight "obj.rune_cannonball" count 24..36
        6 weight "obj.battlestaff" count 1
        6 weight "obj.water_battlestaff" count 1
        6 weight "obj.earth_battlestaff" count 1
        5 weight "obj.mystic_water_staff" count 1
        5 weight "obj.mystic_earth_staff" count 1
        2 weight "obj.adamant_platebody" count 1
        1 weight "obj.mystic_robe_bottom" count 1
        12 weight "obj.raw_seaturtle" count 1
        10 weight "obj.coins" count 16000..18500
        8 weight "obj.plank_teak" count 2..4
        4 weight "obj.boat_repair_kit_camphor" count 1
        2 weight "obj.sailing_pirate_shipwreck_salvage" count 1

        8 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        11 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_armoured_kraken_tentacle" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_armoured_kraken_ink_sac" count 1
        1 outOf 152 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Toadflax seed [main/1/{{#expr:1/({{#var:rareseed}}*216 + {{#var:uncseed}}*27) round 1}}]
//   - Irit seed [main/1/{{#expr:1/({{#var:rareseed}}*148 + {{#var:uncseed}}*18) round 1}}]
//   - Belladonna seed [main/1/{{#expr:1/({{#var:rareseed}}*143 + {{#var:uncseed}}*18) round 1}}]
//   - Avantoe seed [main/1/{{#expr:1/({{#var:rareseed}}*103 + {{#var:uncseed}}*12) round 1}}]
//   - Poison ivy seed [main/1/{{#expr:1/({{#var:rareseed}}*101 + {{#var:uncseed}}*13) round 1}}]
//   - Camphor seed [main/1/{{#expr:1/({{#var:sailseed}}*15) round 1}}]
//   - Cactus seed [main/1/{{#expr:1/({{#var:rareseed}}*96 + {{#var:uncseed}}*12) round 1}}]
//   - Potato cactus seed [main/1/{{#expr:1/({{#var:rareseed}}*70 + {{#var:uncseed}}*8) round 1}}]
//   - Kwuarm seed [main/1/{{#expr:1/({{#var:rareseed}}*69 + {{#var:uncseed}}*9) round 1}}]
//   - Snapdragon seed [main/1/{{#expr:1/({{#var:rareseed}}*46 + {{#var:uncseed}}*5) round 1}}]
//   - Limpwurt seed [main/1/{{#expr:1/({{#var:uncseed}}*137) round 1}}]
//   - Strawberry seed [main/1/{{#expr:1/({{#var:uncseed}}*131) round 1}}]
//   - Cadantine seed [main/1/{{#expr:1/({{#var:rareseed}}*32 + {{#var:uncseed}}*4) round 1}}]
//   - Marrentill seed [main/1/{{#expr:1/({{#var:uncseed}}*125) round 1}}]
//   - Ironwood seed [main/1/{{#expr:1/({{#var:sailseed}}*4) round 1}}]
//   - Jangerberry seed [main/1/{{#expr:1/({{#var:uncseed}}*92) round 1}}]
//   - Lantadyme seed [main/1/{{#expr:1/({{#var:rareseed}}*23 + {{#var:uncseed}}*3) round 1}}]
//   - Tarromin seed [main/1/{{#expr:1/({{#var:uncseed}}*85) round 1}}]
//   - Wildblood seed [main/1/{{#expr:1/({{#var:uncseed}}*83) round 1}}]
//   - Snape grass seed [main/1/{{#expr:1/({{#var:rareseed}}*20) round 1}}]
//   - Watermelon seed [main/1/{{#expr:1/({{#var:uncseed}}*63) round 1}}]
//   - Harralander seed [main/1/{{#expr:1/({{#var:uncseed}}*56) round 0}}]
//   - Dwarf weed seed [main/1/{{#expr:1/({{#var:rareseed}}*14 + {{#var:uncseed}}*2) round 0}}]
//   - Snape grass seed [main/1/{{#expr:1/({{#var:uncseed}}*40) round 0}}]
//   - Ranarr seed [main/1/{{#expr:1/({{#var:uncseed}}*39) round 0}}]
//   - Torstol seed [main/1/{{#expr:1/({{#var:rareseed}}*9 + {{#var:uncseed}}*1) round 0}}]
//   - Whiteberry seed [main/1/{{#expr:1/({{#var:uncseed}}*34) round 0}}]
//   - Mushroom spore [main/1/{{#expr:1/({{#var:uncseed}}*29) round 0}}]
//   - Rosewood seed [main/1/{{#expr:1/({{#var:sailseed}}*1) round 0}}]
