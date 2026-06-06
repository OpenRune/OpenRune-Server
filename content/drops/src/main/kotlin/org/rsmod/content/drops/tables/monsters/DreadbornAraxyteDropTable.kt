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
public val dreadbornAraxyteDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dreadborn Araxyte Drops",
    npcs = npcs("npc.superior_araxyte"),
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("Dreadborn Araxyte Drops")
        5 weight "obj.adamant_longsword" count 2
        5 weight "obj.adamant_battleaxe" count 2
        3 weight "obj.rune_dagger" count 2
        2 weight "obj.rune_med_helm" count 2
        2 weight "obj.rune_platelegs" count 2
        10 weight "obj.airrune" count 240..280
        10 weight "obj.waterrune" count 240..280
        10 weight "obj.earthrune" count 240..280
        10 weight "obj.firerune" count 240..280
        5 weight "obj.cosmicrune" count 14..24
        5 weight "obj.chaosrune" count 20..30
        5 weight "obj.naturerune" count 30..40
        5 weight "obj.deathrune" count 40..50
        5 weight "obj.lawrune" count 24..30
        5 weight "obj.bloodrune" count 30..36
        5 weight "obj.soulrune" count 18..24
        5 weight "obj.araxyte_venom_sack" count 4
        10 weight "obj.coins" count 1600..2400
        1 outOf 400 separate "obj.aranea_boots" count 1
        1 outOf 2000 separate "obj.poh_araxyte_head" count 1

        10 weight SharedDropTables.herb
        10 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/182 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 192 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
