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
public val bloodthirstyChokeDevilDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty choke devil Drops",
    npcs = npcs("npc.league_superior_dustdevil"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty choke devil Drops")
        3 weight "obj.adamant_axe" count 1
        2 weight "obj.rune_dagger" count 1
        2 weight "obj.red_dragon_vambraces" count 1
        1 weight "obj.black_dragon_vambraces" count 1
        2 weight "obj.air_battlestaff" count 1
        2 weight "obj.earth_battlestaff" count 1
        1 weight "obj.mystic_air_staff" count 1
        1 weight "obj.mystic_earth_staff" count 1
        1 weight "obj.dragon_dagger" count 1
        10 weight "obj.dustrune" count 200
        10 weight "obj.earthrune" count 300
        10 weight "obj.firerune" count 300
        1 weight "obj.firerune" count 50
        7 weight "obj.chaosrune" count 80
        5 weight "obj.rune_arrow" count 12
        4 weight "obj.soulrune" count 20
        1 weight "obj.soulrune" count 50
        32 weight "obj.coins" count 2000..4000
        2 weight "obj.ugthanki_kebab" count 4
        3 weight "obj.cert_mithril_bar" count 10
        1 weight "obj.cert_adamantite_bar" count 4
        5 outOf 166 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 166 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 166 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        20 outOf 166 separate "obj.cert_white_berries" count 60..150
        30 outOf 166 separate rsPlayerWeightedTable {
            30 weight "obj.kwuarm_seed" count 8..15
            30 weight "obj.ranarr_seed" count 8..15
        }
        16 outOf 166 separate "obj.cert_blue_dragon_scale" count 20..40
        1 outOf 4000 separate "obj.dust_battlestaff" count 1
        1 outOf 32768 separate "obj.dragon_chainbody" count 1

        8 weight SharedDropTables.herb
        8 weight SharedDropTables.gem
        11 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)
