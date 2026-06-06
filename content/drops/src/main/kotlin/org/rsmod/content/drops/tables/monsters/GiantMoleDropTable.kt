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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val giantMoleDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant Mole Drops",
    npcs = npcs("npc.mole_giant"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.mole_claw" count 1
        "obj.mole_skin" count 1..3
    },
    mainTable = rsPlayerWeightedTable(total = 131) {
        name("Giant Mole Drops")
        10 weight "obj.adamant_longsword" count 1
        9 weight "obj.mithril_platebody" count 1
        7 weight "obj.amulet_of_strength" count 1
        2 weight "obj.mithril_axe" count 1
        1 weight "obj.mithril_battleaxe" count 1
        1 weight "obj.rune_med_helm" count 1
        20 weight "obj.airrune" count 105
        19 weight "obj.bloodrune" count 15
        11 weight "obj.firerune" count 105
        10 weight "obj.iron_arrow" count 690
        5 weight "obj.lawrune" count 15
        3 weight "obj.deathrune" count 50
        10 weight "obj.cert_yew_logs" count 100
        4 weight "obj.shark" count 4
        3 weight "obj.mithril_bar" count 1
        3 weight "obj.ranarr_seed" count 1
        2 weight "obj.cert_iron_ore" count 100
        1 weight "obj.bigoysterpearls" count 1

        4 weight SharedDropTables.rareDrop
        6 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 50 weight "obj.immaculate_mole_skin" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 3000 weight "obj.molepet" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/475 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 500 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
