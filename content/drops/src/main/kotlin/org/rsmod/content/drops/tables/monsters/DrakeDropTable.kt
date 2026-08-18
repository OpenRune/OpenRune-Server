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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val drakeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Drake Drops",
    npcs = npcs("npc.drake", "npc.drake_death"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 2560 weight "obj.drake_tooth" count 1
        1 outOf 2560 weight "obj.drake_claw" count 1
        1 outOf 10000 rolls rsPlayerWeightedTable {
            1 weight "obj.dragon_thrownaxe" count 100..200
            1 weight "obj.dragon_knife" count 100..200
        }
    },
    mainTable = rsPlayerWeightedTable(total = 93) {
        name("Drake Drops")
        3 weight "obj.rune_full_helm" count 1
        2 weight "obj.red_dragonhide_body" count 1
        1 weight "obj.black_dragon_vambraces" count 1
        1 weight "obj.mystic_earth_staff" count 1
        1 weight "obj.dragon_mace" count 1
        10 weight "obj.firerune" count 100..200
        10 weight "obj.naturerune" count 30..60
        10 weight "obj.lawrune" count 25..50
        10 weight "obj.deathrune" count 20..40
        10 weight "obj.rune_arrow" count 35..65
        2 weight "obj.cert_raw_shark" count 4..6
        2 weight "obj.shark_lure" count 1
        1 weight "obj.cert_raw_anglerfish" count 4..6
        4 weight "obj.coins" count 1000..2000
        1 weight "obj.coins" count 5000..7000
        4 weight "obj.cert_diamond" count 3..6
        4 weight "obj.swordfish" count 1..2

        10 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        1 weight SharedDropTables.rareSeed
        5 weight nothing()
        5 outOf 496 separate "obj.unidentified_avantoe" count 1..3
        5 outOf 496 separate "obj.unidentified_kwuarm" count 1..3
        1 outOf 124 separate "obj.unidentified_ranarr" count 1..3
        1 outOf 124 separate "obj.unidentified_snapdragon" count 1..3
        1 outOf 124 separate "obj.unidentified_cadantine" count 1..3
        1 outOf 124 separate "obj.unidentified_dwarf_weed" count 1..3
        10 outOf 1653 separate "obj.unidentified_lantadyme" count 1..3
        10 outOf 1653 separate "obj.unidentified_torstol" count 1..3
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
