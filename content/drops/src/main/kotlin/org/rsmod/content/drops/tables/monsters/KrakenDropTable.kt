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
public val krakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kraken Drops",
    npcs = npcs("npc.slayer_kraken_boss", "npc.slayer_kraken_boss_whirlpool"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Kraken Drops")
        3 weight "obj.mystic_water_staff" count 1
        2 weight "obj.rune_warhammer" count 1
        2 weight "obj.rune_longsword" count 1
        1 weight "obj.mystic_robe_top" count 1
        1 weight "obj.mystic_robe_bottom" count 1
        10 weight "obj.waterrune" count 400
        4 weight "obj.mistrune" count 100
        10 weight "obj.chaosrune" count 200
        10 weight "obj.deathrune" count 150
        10 weight "obj.bloodrune" count 60
        7 weight "obj.soulrune" count 50
        3 weight "obj.watermelon_seed" count 24
        1 weight "obj.torstol_seed" count 2
        1 weight "obj.magic_tree_seed" count 1
        3 weight "obj.cert_seaweed" count 125
        4 weight "obj.cert_battlestaff" count 10
        2 weight "obj.cert_stafforb" count 50
        1 weight "obj.cert_diamond" count 8
        3 weight "obj.cert_plank_oak" count 60
        1 weight "obj.runite_bar" count 2
        2 weight "obj.cert_raw_shark" count 50
        2 weight "obj.cert_raw_monkfish" count 100
        2 weight "obj.cert_unidentified_snapdragon" count 6
        15 weight "obj.coins" count 10000..19999
        7 weight "obj.shark" count 5
        4 weight "obj.pirate_boots" count 1
        4 weight "obj.sanfew_salve_4_dose" count 2
        3 weight "obj.edible_seaweed" count 5
        1 weight "obj.harpoon" count 1
        1 weight "obj.bucket_empty" count 1
        1 weight "obj.crystal_key" count 1
        2 weight "obj.digsitesword" count 1
        2 weight "obj.cert_antidote++4" count 2
        1 weight "obj.dragonstone_ring" count 1
        1 outOf 512 separate "obj.tots" count 1
        1 outOf 400 separate "obj.kraken_tentacle" count 1

        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 1000 weight "obj.jar_of_dirt" count 1
        1 outOf 3000 weight "obj.krakenpet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/475 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 500 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
