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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val waterfiendDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Waterfiend Drops",
    npcs = npcs("npc.brut_waterfiend", "npc.waterfiend_strongholdcave"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 3000 weight "obj.mist_battlestaff" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Waterfiend Drops")
        6 weight "obj.staff_of_water" count 1
        2 weight "obj.adamant_chainbody" count 1
        2 weight "obj.adamnt_warhammer" count 1
        2 weight "obj.rune_med_helm" count 1
        4 weight "obj.water_battlestaff" count 1
        1 weight "obj.mystic_water_staff" count 1
        1 weight "obj.blue_dragon_vambraces" count 1
        1 weight "obj.rune_full_helm" count 1
        5 weight "obj.xbows_crossbow_bolts_mithril_tipped_sapphire" count 15
        10 weight "obj.mithril_arrow" count 90
        6 weight "obj.waterrune" count 150
        3 weight "obj.bloodrune" count 17
        5 weight "obj.deathrune" count 23
        2 weight "obj.mistrune" count 25..75
        2 weight "obj.mudrune" count 75..100
        2 weight "obj.steamrune" count 40..60
        4 weight "obj.cert_mithril_ore" count 10..20
        3 weight "obj.cert_raw_lobster" count 18
        3 weight "obj.cert_raw_shark" count 8
        3 weight "obj.shark" count 2
        1 weight "obj.cert_mithril_bar" count 10..15
        1 weight "obj.cert_uncut_sapphire" count 3
        1 weight "obj.cert_uncut_emerald" count 3
        1 weight "obj.cert_uncut_ruby" count 3
        1 weight "obj.cert_uncut_diamond" count 3
        15 weight "obj.coins" count 2000..3000
        8 weight "obj.cert_water_orb" count 6..10
        4 weight "obj.cert_vial_water" count 40..50
        4 weight "obj.water_talisman" count 1
        2 weight "obj.oystershell" count 3
        2 weight "obj.cert_seaweed" count 20..30
        2 weight "obj.cert_snape_grass" count 20..30

        9 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
        4 weight SharedDropTables.rareSeed
        3 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: This clue scroll and casket are only dropped when completing a [[Treasure Trails/Full guide/Elite#Cryptic clues
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 24 weight "obj.prif_crystal_shard" count (3..5) condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
