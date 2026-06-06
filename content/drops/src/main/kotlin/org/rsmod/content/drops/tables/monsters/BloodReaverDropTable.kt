package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodReaverDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Blood Reaver Drops",
    npcs = npcs("npc.nex_prison_blood_reaver", "npc.nex_prison_blood_reaver_boss"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Blood Reaver Drops")
        2 weight "obj.airrune" count 150
        7 weight "obj.airrune" count 250
        15 weight "obj.astralrune" count 25
        2 weight "obj.bloodrune" count 15
        3 weight "obj.chaosrune" count 15
        4 weight "obj.mindrune" count 20
        7 weight "obj.mudrune" count 15
        4 weight "obj.naturerune" count 15
        3 weight "obj.unidentified_avantoe" count 1
        3 weight "obj.unidentified_ranarr" count 1
        3 weight "obj.unidentified_snapdragon" count 1
        2 weight "obj.unidentified_torstol" count 1
        2 weight "obj.coins" count 1
        9 weight "obj.coins" count 500
        6 weight "obj.coins" count 1300..1337
        11 weight "obj.1dose1magic" count 1
        2 weight "obj.1dose2defense" count 1
        9 weight "obj.2doseprayerrestore" count 1
        8 weight "obj.cert_adamantite_bar" count 1..4
        1 weight "obj.blood_essence_inactive" count 1
        8 weight "obj.cert_coal" count 1..10
        1 weight "obj.nihil_shard" count 2..7
        7 weight "obj.cactus_potato" count 1
        8 weight "obj.cert_blankrune_high" count 23
        1 outOf 640 separate rsPlayerWeightedTable {
            1 weight "obj.ancient_ceremonial_mask" count 1
            1 weight "obj.ancient_ceremonial_top" count 1
            1 weight "obj.ancient_ceremonial_legs" count 1
            1 weight "obj.ancient_ceremonial_gloves" count 1
            1 weight "obj.ancient_ceremonial_boots" count 1
        }

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/106 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 112 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
