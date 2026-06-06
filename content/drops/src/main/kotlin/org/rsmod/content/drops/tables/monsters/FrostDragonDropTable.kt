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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val frostDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Frost dragon Drops",
    npcs = npcs("npc.frost_dragon"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 100 weight "obj.dragon_sheet" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 130) {
        name("Frost dragon Drops")
        2 weight "obj.rune_2h_sword" count 1
        2 weight "obj.rune_kiteshield" count 1
        2 weight "obj.rune_longsword" count 1
        2 weight "obj.rune_pickaxe" count 1
        4 weight "obj.adamant_platebody" count 1
        4 weight "obj.adamnt_warhammer" count 1
        5 weight "obj.deathrune" count 20..30
        10 weight "obj.waterrune" count 100..200
        5 weight "obj.waterrune" count 500
        11 weight "obj.airrune" count 100..200
        5 weight "obj.bloodrune" count 10..20
        8 weight "obj.chaosrune" count 60..80
        10 weight "obj.mistrune" count 40..60
        4 weight "obj.naturerune" count 10..20
        2 weight "obj.dragon_cannonball" count 12..20
        4 weight "obj.rune_cannonball" count 20..35
        5 weight "obj.rune_knife" count 10..20
        4 weight "obj.dragon_arrowheads" count 5..10
        3 weight "obj.dragon_bones_superior" count 1
        3 weight "obj.cert_runite_ore" count 1
        3 weight "obj.cert_adamantite_ore" count 1
        2 weight "obj.nails_dragon" count 10..20
        10 weight "obj.coins" count 50..100
        10 weight "obj.coins" count 200..500
        5 weight "obj.apple_pie" count 1

        2 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Rare]
