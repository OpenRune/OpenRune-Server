package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val lizardmanShamanDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lizardman shaman Drops",
    npcs = npcs("npc.lizardman_cave_shaman_1", "npc.lizardman_cave_shaman_2", "npc.molch_lizardshaman_1", "npc.zeah_lizardshaman_1", "npc.zeah_lizardshaman_2"),
    mainTable = rsPlayerWeightedTable(total = 500) {
        name("Lizardman shaman Drops")
        18 weight "obj.rune_med_helm" count 1
        17 weight "obj.earth_battlestaff" count 1
        17 weight "obj.mystic_earth_staff" count 1
        16 weight "obj.rune_warhammer" count 1
        12 weight "obj.rune_chainbody" count 1
        10 weight "obj.red_dragon_vambraces" count 1
        25 weight "obj.airrune" count 60..80
        25 weight "obj.chaosrune" count 40..60
        25 weight "obj.deathrune" count 20..30
        25 weight "obj.firerune" count 60..80
        40 weight "obj.xeric_fabric" count 2
        20 weight "obj.cert_coal" count 20..25
        20 weight "obj.cert_iron_ore" count 30..35
        8 weight "obj.cert_runite_ore" count 3..5
        70 weight "obj.coins" count 100..6000
        40 weight "obj.lizardman_fang" count 10..14
        30 weight "obj.potato_chilli+carne" count 2
        2 weight "obj.xeric_talisman_empty" count 1

        20 weight SharedDropTables.rareDrop
        60 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/190 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 200 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/1140 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 1200 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Grimy kwuarm [main/1/{{#expr:1/(5*{{#var:uht}}) round 2}}]
//   - Grimy cadantine [main/1/{{#expr:1/(4*{{#var:uht}}) round 2}}]
//   - Grimy dwarf weed [main/1/{{#expr:1/(4*{{#var:uht}}) round 2}}]
//   - Grimy lantadyme [main/1/{{#expr:1/(3*{{#var:uht}}) round 2}}]
