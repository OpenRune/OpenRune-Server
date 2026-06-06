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
public val nechryaelDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Nechryael Drops",
    npcs = npcs("npc.slayer_nechryael", "npc.slayer_nechryael_strongholdcave"),
    mainTable = rsPlayerWeightedTable(total = 116) {
        name("Nechryael Drops")
        4 weight "obj.adamant_platelegs" count 1
        4 weight "obj.rune_2h_sword" count 1
        3 weight "obj.rune_full_helm" count 1
        2 weight "obj.adamant_kiteshield" count 1
        1 weight "obj.rune_armoured_boots" count 1
        8 weight "obj.chaosrune" count 37
        6 weight "obj.deathrune" count 5
        6 weight "obj.deathrune" count 10
        5 weight "obj.lawrune" count 25..35
        4 weight "obj.bloodrune" count 15..20
        13 weight "obj.coins" count 1000..1499
        11 weight "obj.coins" count 1500..2000
        6 weight "obj.coins" count 2500..2999
        3 weight "obj.coins" count 3000..3500
        3 weight "obj.coins" count 500..999
        1 weight "obj.coins" count 5000
        4 weight "obj.cert_softclay" count 25
        3 weight "obj.tuna" count 1

        1 weight SharedDropTables.rareDrop
        5 weight SharedDropTables.gem
        18 weight SharedDropTables.rareSeed
        5 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
