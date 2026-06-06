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
public val demonicGorillaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Demonic gorilla Drops",
    npcs = npcs("npc.mm2_demon_gorilla_1_magic", "npc.mm2_demon_gorilla_1_melee", "npc.mm2_demon_gorilla_1_ranged", "npc.mm2_demon_gorilla_2_magic", "npc.mm2_demon_gorilla_2_melee", "npc.mm2_demon_gorilla_2_ranged", "npc.mm2_demon_gorilla_noncombat"),
    mainTable = rsPlayerWeightedTable(total = 500) {
        name("Demonic gorilla Drops")
        1 weight "obj.ballista_limbs" count 1
        1 weight "obj.ballista_spring" count 1
        35 weight "obj.rune_platelegs" count 1
        35 weight "obj.rune_plateskirt" count 1
        20 weight "obj.rune_chainbody" count 1
        10 weight "obj.dragon_scimitar" count 1
        35 weight "obj.lawrune" count 50..75
        35 weight "obj.deathrune" count 50..75
        25 weight "obj.xbows_crossbow_bolts_runite" count 100..150
        40 weight "obj.3doseprayerrestore" count 2
        35 weight "obj.shark" count 2..3
        25 weight "obj.coins" count 5000..10000
        25 weight "obj.2dosepotionofsaradomin" count 1
        20 weight "obj.cert_adamantite_bar" count 6
        25 weight "obj.javelin_shaft" count 750..1250
        25 weight "obj.rune_javelin_head" count 45..55
        25 weight "obj.dragon_javelin_head" count 27..33
        17 weight "obj.cert_diamond" count 4..6
        15 weight "obj.cert_runite_bar" count 3
        1 outOf 300 separate "obj.zenyte_shard" count 1
        1 outOf 750 separate "obj.ballista_frame_light" count 1
        1 outOf 1500 separate rsPlayerWeightedTable {
            1 weight "obj.ballista_frame_heavy" count 1
            1 weight "obj.ballista_rope" count 1
        }

        5 weight SharedDropTables.rareDrop
        46 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/95 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/475 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 500 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Grimy kwuarm [main/1/{{#expr:1/(40*{{#var:herbbase}}) round 1}}]
//   - Grimy cadantine [main/1/{{#expr:1/(32*{{#var:herbbase}}) round 1}}]
//   - Grimy lantadyme [main/1/{{#expr:1/(24*{{#var:herbbase}}) round 1}}]
//   - Grimy dwarf weed [main/1/{{#expr:1/(32*{{#var:herbbase}}) round 1}}]
