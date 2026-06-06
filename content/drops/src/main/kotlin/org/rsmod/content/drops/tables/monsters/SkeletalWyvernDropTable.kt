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
public val skeletalWyvernDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeletal Wyvern Drops",
    npcs = npcs("npc.skeletal_wyvern1", "npc.skeletal_wyvern2", "npc.skeletal_wyvern3", "npc.skeletal_wyvern4"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 512 weight "obj.granite_legs" count 1
        1 outOf 512 weight "obj.dragon_platelegs" count 1
        1 outOf 512 weight "obj.dragon_plateskirt" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Skeletal Wyvern Drops")
        4 weight "obj.earth_battlestaff" count 1
        3 weight "obj.cert_battlestaff" count 10
        3 weight "obj.rune_axe" count 1
        2 weight "obj.rune_battleaxe" count 1
        2 weight "obj.rune_warhammer" count 1
        2 weight "obj.rune_full_helm" count 1
        2 weight "obj.rune_kiteshield" count 1
        6 weight "obj.airrune" count 225
        5 weight "obj.rune_arrow" count 36
        4 weight "obj.waterrune" count 150
        4 weight "obj.chaosrune" count 80
        4 weight "obj.lawrune" count 45
        4 weight "obj.deathrune" count 40
        4 weight "obj.bloodrune" count 25
        3 weight "obj.xbows_crossbow_bolts_adamantite" count 75..99
        3 weight "obj.xbows_crossbow_bolts_runite" count 35..44
        1 weight "obj.soulrune" count 20
        8 weight "obj.cert_blankrune_high" count 250
        6 weight "obj.cert_magic_logs" count 35
        6 weight "obj.cert_adamantite_bar" count 10
        3 weight "obj.cert_iron_ore" count 200
        2 weight "obj.cert_uncut_ruby" count 10
        2 weight "obj.cert_uncut_diamond" count 5
        12 weight "obj.coins" count 300
        8 weight "obj.lobster" count 6
        7 weight "obj.4doseprayerrestore" count 2
        2 weight "obj.cert_stafforb" count 75
        2 weight "obj.xbows_crossbow_unstrung_runite" count 1
        2 weight "obj.ranarr_seed" count 3
        2 weight "obj.snapdragon_seed" count 1

        7 weight SharedDropTables.herb
        3 weight SharedDropTables.rareDrop
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/332 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 350 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
