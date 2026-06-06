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
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val wyrmDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wyrm Drops",
    npcs = npcs("npc.wyrm_dark", "npc.wyrm_light"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 10000 weight "obj.dragon_shortsword" count 1
        1 outOf 10000 weight "obj.dragon_harpoon" count 1
        1 outOf 10000 weight "obj.dragon_knife" count 75..150
        1 outOf 10000 weight "obj.dragon_thrownaxe" count 75..150
    },
    mainTable = rsPlayerWeightedTable(total = 76) {
        name("Wyrm Drops")
        2 weight "obj.adamant_axe" count 1
        3 weight "obj.red_dragonhide_chaps" count 1
        2 weight "obj.adamant_sq_shield" count 1
        2 weight "obj.adamant_battleaxe" count 1
        2 weight "obj.adamant_2h_sword" count 1
        1 weight "obj.earth_battlestaff" count 1
        2 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_battleaxe" count 1
        1 weight "obj.dragon_dagger" count 1
        10 weight "obj.firerune" count 200
        10 weight "obj.earthrune" count 75..150
        5 weight "obj.soulrune" count 15..20
        5 weight "obj.bloodrune" count 25..30
        8 weight "obj.coins" count 950..1450
        7 weight "obj.bass" count 1
        3 weight "obj.cert_blankrune_high" count 200..300
        2 weight "obj.rune_arrowheads" count 8..12
        2 weight "obj.adamant_arrowheads" count 8..12

        4 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        3 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/243 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 256 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
