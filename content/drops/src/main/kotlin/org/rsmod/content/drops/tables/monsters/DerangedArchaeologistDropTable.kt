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
public val derangedArchaeologistDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Deranged archaeologist Drops",
    npcs = npcs("npc.fossil_crazy_archaeologist"),
    mainTable = rsPlayerWeightedTable(total = 131) {
        name("Deranged archaeologist Drops")
        3 weight "obj.steel_ring" count 1
        6 weight "obj.black_dragonhide_body" count 1
        3 weight "obj.rune_2h_sword" count 1
        4 weight "obj.rune_sword" count 1
        6 weight "obj.waterrune" count 100
        6 weight "obj.mudrune" count 40
        6 weight "obj.rune_knife" count 25
        1 weight "obj.dragon_arrow" count 60
        4 weight "obj.mcannonball" count 80
        8 weight "obj.cert_unidentified_dwarf_weed" count 4
        7 weight "obj.cert_white_berries" count 10
        5 weight "obj.xbows_crossbow_limbs_runite" count 1
        5 weight "obj.cert_dragonhide_black" count 8
        6 weight "obj.cert_gold_ore" count 10
        5 weight "obj.cert_uncut_diamond" count 5
        6 weight "obj.xbows_bolt_tips_onyx" count 6
        4 weight "obj.anchovie_pizza" count 2
        8 weight "obj.3doseprayerrestore" count 1
        8 weight "obj.potato_cheese" count 3
        8 weight "obj.shark" count 2
        7 weight "obj.crystal_key" count 1
        2 weight "obj.dorgesh_construction_bone" count 1

        6 weight SharedDropTables.rareDrop
        6 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/190 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 200 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
