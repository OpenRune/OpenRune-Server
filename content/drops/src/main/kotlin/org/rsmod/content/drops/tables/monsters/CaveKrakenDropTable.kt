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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val caveKrakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cave kraken Drops",
    npcs = npcs("npc.slayer_kraken", "npc.slayer_kraken_sub"),
    mainTable = rsPlayerWeightedTable(total = 200) {
        name("Cave kraken Drops")
        4 weight "obj.staff_of_water" count 1
        7 weight "obj.rune_med_helm" count 1
        4 weight "obj.adamant_spear" count 1
        4 weight "obj.rune_warhammer" count 1
        4 weight "obj.battlestaff" count 1
        4 weight "obj.water_battlestaff" count 1
        2 weight "obj.mystic_water_staff" count 1
        1 weight "obj.tots_uncharged" count 1
        6 weight "obj.steamrune" count 7
        10 weight "obj.waterrune" count 15
        10 weight "obj.waterrune" count 30
        10 weight "obj.waterrune" count 75
        10 weight "obj.firerune" count 30
        16 weight "obj.deathrune" count 30
        16 weight "obj.chaosrune" count 50
        8 weight "obj.bloodrune" count 5
        4 weight "obj.old_boot" count 1
        4 weight "obj.swamp_tar" count 60
        4 weight ringNothing()
        6 weight "obj.cert_seaweed" count 30
        10 weight "obj.coins" count 120..300
        4 weight "obj.cert_raw_lobster" count 3
        4 weight "obj.cert_water_orb" count 2
        4 weight "obj.oystershell" count 1
        6 weight "obj.swordfish" count 2
        6 weight "obj.shark" count 1
        6 weight "obj.cert_antidote++4" count 1
        4 weight "obj.cert_vial_water" count 50
        4 weight "obj.water_talisman" count 1
        1 outOf 240 separate "obj.bucket_empty" count 1
        1 outOf 1200 separate "obj.kraken_tentacle" count 1

        6 weight SharedDropTables.herb
        6 weight SharedDropTables.gem
        6 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/95 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/1140 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 1200 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
