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
public val calvarionDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Calvar'ion Drops",
    npcs = npcs("npc.vetion_2_single", "npc.vetion_single"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Calvar'ion Drops")
        5 weight "obj.dark_crab" count 8
        5 weight "obj.4dose2restore" count 3
        3 weight "obj.staff_of_zaros" count 1
        3 weight "obj.rune_2h_sword" count 1
        12 weight "obj.rune_pickaxe" count 1
        7 weight "obj.chaosrune" count 220
        7 weight "obj.deathrune" count 120
        7 weight "obj.bloodrune" count 180
        4 weight "obj.mcannonball" count 180
        4 weight "obj.cert_uncut_ruby" count 17
        3 weight "obj.cert_uncut_diamond" count 7
        2 weight "obj.uncut_dragonstone" count 1
        6 weight "obj.cert_gold_ore" count 200
        5 weight "obj.cert_limpwurt_root" count 19
        5 weight "obj.cert_magic_logs" count 60
        5 weight "obj.cert_plank_oak" count 220
        3 weight "obj.cert_dragon_bones" count 60
        2 weight "obj.cert_mortmyremushroom" count 120
        5 weight "obj.cert_wine_of_zamorak" count 35
        1 weight "obj.cert_unidentified_ranarr" count 60
        21 weight "obj.coins" count 12000
        1 weight "obj.yew_seed" count 1
        1 weight "obj.magic_tree_seed" count 1
        1 weight "obj.palm_tree_seed" count 1
        4 weight "obj.cert_bucket_supercompost" count 60
        5 weight "obj.cert_sanfew_salve_4_dose" count 6
        1 weight "obj.wilderness_fishing_bait" count 280
        1 outOf 358 separate rsPlayerWeightedTable {
            1 weight "obj.dragon_2h_sword" count 1
            1 weight "obj.dragon_pickaxe" count 1
        }
        1 outOf 618 separate "obj.wbr_vetion_skull" count 1
        1 outOf 716 separate "obj.rotg" count 1
        1 outOf 912 separate "obj.wbr_voidwaker_blade" count 1
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
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 2800 weight "obj.vetion_pet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/50 if a ring of wealth (i) is worn.
        1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
