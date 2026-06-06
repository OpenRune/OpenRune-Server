package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val talonedWyvernDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Taloned Wyvern Drops",
    npcs = npcs("npc.wyvern_taloned"),
    mainTable = rsPlayerWeightedTable(total = 123) {
        name("Taloned Wyvern Drops")
        4 weight "obj.air_battlestaff" count 1
        3 weight "obj.cert_battlestaff" count 3..5
        2 weight "obj.adamant_battleaxe" count 1
        2 weight "obj.adamant_full_helm" count 1
        2 weight "obj.rune_pickaxe" count 1
        2 weight "obj.adamant_platebody" count 1
        6 weight "obj.adamant_arrow" count 38..42
        4 weight "obj.waterrune" count 50
        4 weight "obj.chaosrune" count 15
        4 weight "obj.lawrune" count 15
        4 weight "obj.deathrune" count 15
        4 weight "obj.bloodrune" count 15
        1 weight "obj.soulrune" count 10
        1 weight "obj.xbows_crossbow_bolts_runite" count 12..30
        2 weight "obj.unidentified_ranarr" count 1
        2 weight "obj.unidentified_torstol" count 1
        2 weight "obj.seaweed_seed" count 12
        8 weight "obj.cert_blankrune_high" count 150
        6 weight "obj.cert_adamantite_bar" count 2..4
        6 weight "obj.cert_teak_logs" count 35
        3 weight "obj.cert_snape_grass" count 10..15
        3 weight "obj.cert_runite_ore" count 1..2
        8 weight "obj.lobster" count 2
        11 weight "obj.coins" count 3000
        7 weight "obj.4doseprayerrestore" count 1
        2 weight "obj.xbows_crossbow_unstrung_adamantite" count 1
        2 weight "obj.fossil_calcite" count 2
        2 weight "obj.fossil_pyrophosphite" count 2
        2 weight "obj.fossil_volcanic_ash" count 20..60
        1 outOf 512 separate "obj.granite_longsword" count 1
        1 outOf 2560 separate "obj.granite_boots" count 1
        14 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 12000 weight "obj.wyvern_visage" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/112 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 118 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Grimy kwuarm [main/1/{{#expr:1/(40*{{#var:herbbase}}) round 1}}]
//   - Grimy cadantine [main/1/{{#expr:1/(32*{{#var:herbbase}}) round 1}}]
//   - Grimy dwarf weed [main/1/{{#expr:1/(32*{{#var:herbbase}}) round 1}}]
//   - Grimy lantadyme [main/1/{{#expr:1/(24*{{#var:herbbase}}) round 1}}]
//   - Ranarr seed [main/1/{{#expr:1/(15*{{#var:seedbase}} + 2/123) round 1}}]
//   - Snapdragon seed [main/1/{{#expr:1/(14*{{#var:seedbase}}) round 1}}]
//   - Torstol seed [main/1/{{#expr:1/(11*{{#var:seedbase}}) round 1}}]
//   - Watermelon seed [main/1/{{#expr:1/(10*{{#var:seedbase}}) round 1}}]
//   - Willow seed [main/1/{{#expr:1/(10*{{#var:seedbase}}) round 1}}]
//   - Mahogany seed [main/1/{{#expr:1/(9*{{#var:seedbase}} + (1/123 * 1/3)) round 1}}]
//   - Maple seed [main/1/{{#expr:1/(9*{{#var:seedbase}}) round 1}}]
//   - Teak seed [main/1/{{#expr:1/(9*{{#var:seedbase}} + (1/123 * 2/3)) round 1}}]
//   - Yew seed [main/1/{{#expr:1/(9*{{#var:seedbase}} + 1/123) round 1}}]
//   - Papaya tree seed [main/1/{{#expr:1/(7*{{#var:seedbase}}) round 1}}]
//   - Magic seed [main/1/{{#expr:1/(6*{{#var:seedbase}}) round 1}}]
//   - Palm tree seed [main/1/{{#expr:1/(5*{{#var:seedbase}}) round 1}}]
//   - Spirit seed [main/1/{{#expr:1/(4*{{#var:seedbase}}) round 1}}]
//   - Dragonfruit tree seed [main/1/{{#expr:1/(3*{{#var:seedbase}}) round 1}}]
//   - Celastrus seed [main/1/{{#expr:1/(2*{{#var:seedbase}}) round 1}}]
//   - Redwood tree seed [main/1/{{#expr:1/(2*{{#var:seedbase}}) round 1}}]
