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
public val corporealBeastDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Corporeal Beast Drops",
    npcs = npcs("npc.corp_beast"),
    mainTable = rsPlayerWeightedTable(total = 512) {
        name("Corporeal Beast Drops")
        18 weight "obj.mystic_robe_top" count 1
        18 weight "obj.mystic_robe_bottom" count 1
        12 weight "obj.mystic_air_staff" count 1
        12 weight "obj.mystic_water_staff" count 1
        12 weight "obj.mystic_earth_staff" count 1
        12 weight "obj.mystic_fire_staff" count 1
        8 weight "obj.spirit_shield" count 1
        32 weight "obj.soulrune" count 250
        24 weight "obj.xbows_crossbow_bolts_runite" count 250
        22 weight "obj.deathrune" count 300
        20 weight "obj.xbows_crossbow_bolts_runite_tipped_onyx_enchanted" count 175
        17 weight "obj.mcannonball" count 2000
        17 weight "obj.adamant_arrow" count 750
        17 weight "obj.lawrune" count 250
        17 weight "obj.cosmicrune" count 500
        21 weight "obj.cert_raw_shark" count 70
        21 weight "obj.cert_blankrune_high" count 2500
        18 weight "obj.cert_adamantite_bar" count 35
        18 weight "obj.cert_dragonhide_green" count 100
        17 weight "obj.cert_adamantite_ore" count 125
        12 weight "obj.cert_runite_ore" count 20
        12 weight "obj.cert_plank_teak" count 100
        12 weight "obj.cert_mahogany_logs" count 150
        12 weight "obj.cert_magic_logs" count 75
        20 weight "obj.potato_tuna+sweetcorn" count 30
        17 weight "obj.cert_white_berries" count 120
        17 weight "obj.cert_desert_goat_horn" count 120
        15 weight "obj.watermelon_seed" count 24
        12 weight "obj.coins" count 20000..50000
        10 weight "obj.cert_antidote++4" count 40
        5 weight "obj.ranarr_seed" count 10
        3 weight "obj.holy_elixir" count 1

        12 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1000 weight "obj.jar_of_spirits" count 1
        1 outOf 5000 weight "obj.corepet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/190 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 200 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
