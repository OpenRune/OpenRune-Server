package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val insatiableBloodveldDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Insatiable Bloodveld Drops",
    npcs = npcs("npc.superior_bloodveld"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.arceuus_corpse_bloodveld" count 1
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 138) {
        name("Insatiable Bloodveld Drops")
        4 weight "obj.steel_axe" count 1
        4 weight "obj.steel_full_helm" count 1
        2 weight "obj.steel_scimitar" count 1
        1 weight "obj.black_armoured_boots" count 1
        1 weight "obj.mithril_sq_shield" count 1
        1 weight "obj.mithril_chainbody" count 1
        1 weight "obj.rune_med_helm" count 1
        8 weight "obj.firerune" count 60
        3 weight "obj.bloodrune" count 3
        5 weight "obj.bloodrune" count 10
        1 weight "obj.bloodrune" count 30
        7 weight "obj.coins" count 10
        29 weight "obj.coins" count 40
        30 weight "obj.coins" count 120
        10 weight "obj.coins" count 200
        1 weight "obj.coins" count 450
        10 weight "obj.bones" count 1
        7 weight "obj.big_bones" count 1
        3 weight "obj.big_bones" count 3
        2 weight "obj.gold_ore" count 1
        3 weight "obj.meat_pizza" count 1

        1 weight SharedDropTables.herb
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/24 after unlocking the hard Combat Achievements rewards tier.
        10 outOf 256 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
