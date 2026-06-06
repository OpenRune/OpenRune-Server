package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val callistoDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Callisto Drops",
    npcs = npcs("npc.callisto"),
    mainTable = rsPlayerWeightedTable(total = 126) {
        name("Callisto Drops")
        2 weight "obj.cert_mystic_earth_staff" count 4
        2 weight "obj.cert_mystic_robe_top" count 4
        2 weight "obj.cert_mystic_robe_bottom" count 4
        8 weight "obj.cert_rune_pickaxe" count 5
        3 weight "obj.cert_rune_2h_sword" count 3
        2 weight "obj.cert_rune_kiteshield" count 4
        2 weight "obj.cert_rune_platebody" count 4
        7 weight "obj.bloodrune" count 500
        7 weight "obj.deathrune" count 700
        7 weight "obj.chaosrune" count 900
        5 weight "obj.soulrune" count 450
        4 weight "obj.mcannonball" count 600
        4 weight "obj.cert_uncut_ruby" count 50
        3 weight "obj.cert_uncut_diamond" count 25
        2 weight "obj.cert_uncut_dragonstone" count 5
        5 weight "obj.cert_limpwurt_root" count 100
        5 weight "obj.cert_magic_logs" count 225
        6 weight "obj.cert_mahogany_logs" count 600
        3 weight "obj.cert_dragonhide_red" count 170
        2 weight "obj.cert_coconut" count 135
        1 weight "obj.cert_unidentified_toadflax" count 100
        1 weight "obj.cert_unidentified_dwarf_weed" count 45
        1 weight "obj.cert_unidentified_ranarr" count 45
        1 weight "obj.cert_unidentified_snapdragon" count 45
        5 weight "obj.ranarr_seed" count 11
        5 weight "obj.snapdragon_seed" count 5
        10 weight "obj.coins" count 50000
        5 weight "obj.cert_dark_crab" count 50
        5 weight "obj.cert_4dose2restore" count 10
        4 weight "obj.cert_bucket_supercompost" count 225
        3 weight "obj.cert_dragon_bones" count 75
        1 weight "obj.cert_blighted_anglerfish" count 100
        3 weight "obj.tablet_wildycrabs" count 4
        1 outOf 196 separate "obj.wbr_callisto_claws" count 1
        1 outOf 256 separate rsPlayerWeightedTable {
            1 weight "obj.dragon_2h_sword" count 1
            1 weight "obj.dragon_pickaxe" count 1
        }
        1 outOf 360 separate "obj.wbr_voidwaker_hilt" count 1
        1 outOf 512 separate "obj.heavy_ring" count 1
        1 outOf 18 separate rsPlayerWeightedTable {
            1 weight "obj.blighted_anglerfish" count 5..6
            1 weight "obj.blighted_karambwan" count 5..6
            1 weight "obj.blighted_3dose2restore" count 3..4
            1 weight "obj.blighted_4dose2restore" count 3..4
            1 weight "obj.2doserangerspotion" count 2..3
            1 weight "obj.2dose2combat" count 2..3
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 1500 weight "obj.callisto_pet" count 1
        2 outOf 10025 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/50 if a ring of wealth (i) is worn.
        1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
