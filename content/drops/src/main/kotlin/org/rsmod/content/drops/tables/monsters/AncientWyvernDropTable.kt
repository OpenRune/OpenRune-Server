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
public val ancientWyvernDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ancient Wyvern Drops",
    npcs = npcs("npc.ancient_wyvern"),
    mainTable = rsPlayerWeightedTable(total = 132) {
        name("Ancient Wyvern Drops")
        4 weight "obj.air_battlestaff" count 2
        3 weight "obj.mystic_air_staff" count 1
        3 weight "obj.rune_pickaxe" count 1
        2 weight "obj.rune_battleaxe" count 1
        2 weight "obj.rune_kiteshield" count 1
        2 weight "obj.rune_full_helm" count 1
        4 weight "obj.cert_battlestaff" count 6
        3 weight "obj.adamant_arrow" count 50..76
        4 weight "obj.naturerune" count 37..43
        4 weight "obj.deathrune" count 47..53
        4 weight "obj.bloodrune" count 27..33
        3 weight "obj.rune_arrow" count 25..50
        1 weight "obj.xbows_crossbow_bolts_runite" count 30..60
        2 weight "obj.cert_unidentified_cadantine" count 3
        2 weight "obj.cert_unidentified_dwarf_weed" count 3
        4 weight "obj.unidentified_torstol" count 1
        4 weight "obj.unidentified_ranarr" count 2..4
        2 weight "obj.seaweed_seed" count 16..24
        1 weight "obj.ranarr_seed" count 2..3
        6 weight "obj.cert_adamantite_bar" count 3
        5 weight "obj.cert_adamantite_ore" count 20
        3 weight "obj.cert_runite_ore" count 2..3
        3 weight "obj.cert_diamond" count 3..5
        4 weight "obj.xbows_bolt_tips_onyx" count 10..15
        6 weight "obj.fossil_volcanic_ash" count 80..120
        5 weight "obj.cert_mahogany_logs" count 25..30
        8 weight "obj.shark" count 5
        5 weight "obj.2dose2combat" count 1
        8 weight "obj.4dose2restore" count 2
        7 weight "obj.coins" count 2000..6000
        3 weight "obj.xbows_crossbow_unstrung_runite" count 1
        3 weight "obj.cert_bucket_supercompost" count 7..10
        1 outOf 600 separate rsPlayerWeightedTable {
            1 weight "obj.granite_longsword" count 1
            1 weight "obj.granite_boots" count 1
        }
        5 outOf 264 separate "obj.cert_unidentified_kwuarm" count 3
        3 outOf 264 separate "obj.cert_unidentified_lantadyme" count 3
        1 outOf 198 separate "obj.magic_tree_seed" count 1
        3 outOf 198 separate "obj.yew_seed" count 1
        1 outOf 99 separate "obj.mahogany_seed" count 1

        2 weight SharedDropTables.rareDrop
        10 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 10000 weight "obj.wyvern_visage" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/332 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 350 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
