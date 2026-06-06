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
public val artioDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Artio Drops",
    npcs = npcs("npc.callisto_singles"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Artio Drops")
        5 weight "obj.dark_crab" count 9
        5 weight "obj.4dose2restore" count 3
        3 weight "obj.rune_2h_sword" count 1
        12 weight "obj.rune_pickaxe" count 1
        7 weight "obj.chaosrune" count 300
        7 weight "obj.deathrune" count 220
        7 weight "obj.bloodrune" count 140
        5 weight "obj.soulrune" count 150
        4 weight "obj.mcannonball" count 190
        4 weight "obj.cert_uncut_ruby" count 17
        3 weight "obj.cert_uncut_diamond" count 7
        2 weight "obj.uncut_dragonstone" count 1
        5 weight "obj.cert_limpwurt_root" count 20
        5 weight "obj.cert_magic_logs" count 60
        6 weight "obj.cert_mahogany_logs" count 200
        3 weight "obj.cert_dragon_bones" count 25
        1 weight "obj.cert_unidentified_toadflax" count 40
        2 weight "obj.cert_coconut" count 30
        3 weight "obj.cert_dragonhide_red" count 55
        21 weight "obj.coins" count 12000
        1 weight "obj.wilderness_fishing_bait" count 300
        4 weight "obj.cert_bucket_supercompost" count 60
        1 weight "obj.yew_seed" count 1
        1 weight "obj.magic_tree_seed" count 1
        5 weight "obj.ranarr_seed" count 3
        5 weight "obj.snapdragon_seed" count 1
        1 weight "obj.palm_tree_seed" count 1
        1 outOf 358 separate rsPlayerWeightedTable {
            1 weight "obj.dragon_2h_sword" count 1
            1 weight "obj.dragon_pickaxe" count 1
        }
        1 outOf 618 separate "obj.wbr_callisto_claws" count 1
        1 outOf 716 separate "obj.heavy_ring" count 1
        1 outOf 912 separate "obj.wbr_voidwaker_hilt" count 1
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
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 2800 weight "obj.callisto_pet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/50 if a ring of wealth (i) is worn.
        1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
