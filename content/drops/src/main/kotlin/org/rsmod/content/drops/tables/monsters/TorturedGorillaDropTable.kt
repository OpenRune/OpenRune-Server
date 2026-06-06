package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val torturedGorillaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tortured gorilla Drops",
    npcs = npcs("npc.mm2_tortured_gorilla_1", "npc.mm2_tortured_gorilla_2", "npc.mm2_tortured_gorilla_lab", "npc.mm2_tortured_gorilla_lab_defeated", "npc.mm2_tortured_gorilla_noncombat", "npc.mm2_tortured_gorilla_stronghold"),
    mainTable = rsPlayerWeightedTable(total = 1000) {
        name("Tortured gorilla Drops")
        41 weight "obj.rune_med_helm" count 1
        22 weight "obj.rune_scimitar" count 1
        86 weight "obj.xbows_crossbow_bolts_adamantite" count 1
        76 weight "obj.cosmicrune" count 20..30
        72 weight "obj.earthrune" count 400..600
        69 weight "obj.deathrune" count 10..20
        86 weight "obj.javelin_shaft" count 100..300
        54 weight "obj.cert_adamantite_bar" count 1..2
        53 weight "obj.cert_ruby" count 2..3
        46 weight "obj.rune_javelin_head" count 10
        46 weight "obj.dragon_javelin_head" count 5
        92 weight "obj.shark" count 1
        61 weight "obj.1doseprayerrestore" count 1
        96 weight "obj.coins" count 1000..2000
        1 outOf 3000 separate "obj.zenyte_shard" count 1
        1 outOf 5000 separate rsPlayerWeightedTable {
            1 weight "obj.ballista_limbs" count 1
            1 weight "obj.ballista_spring" count 1
        }
        1 outOf 7500 separate "obj.ballista_frame_light" count 1
        1 outOf 15000 separate rsPlayerWeightedTable {
            1 weight "obj.ballista_frame_heavy" count 1
            1 weight "obj.ballista_rope" count 1
        }

        7 weight SharedDropTables.rareDrop
        93 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/285 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 300 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/1425 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 1500 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
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
