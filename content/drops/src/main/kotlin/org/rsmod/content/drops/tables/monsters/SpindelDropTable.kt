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
public val spindelDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Spindel Drops",
    npcs = npcs("npc.venenatis_singles"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Spindel Drops")
        5 weight "obj.dark_crab" count 8
        5 weight "obj.4dose2restore" count 3
        5 weight "obj.rune_knife" count 30
        12 weight "obj.rune_pickaxe" count 1
        3 weight "obj.rune_2h_sword" count 1
        7 weight "obj.chaosrune" count 180
        7 weight "obj.bloodrune" count 150
        7 weight "obj.deathrune" count 220
        4 weight "obj.mcannonball" count 200
        5 weight "obj.xbows_crossbow_bolts_adamantite_tipped_diamond_enchanted" count 60
        8 weight "obj.cert_uncut_diamond" count 7
        6 weight "obj.cert_gold_ore" count 180
        5 weight "obj.cert_magic_logs" count 60
        5 weight "obj.cert_limpwurt_root" count 18
        5 weight "obj.xbows_bolt_tips_onyx" count 35
        3 weight "obj.cert_red_spiders_eggs" count 250
        2 weight "obj.uncut_dragonstone" count 1
        1 weight "obj.cert_uncut_ruby" count 14
        1 weight "obj.cert_unidentified_snapdragon" count 40
        1 weight "obj.cert_unicorn_horn" count 60
        21 weight "obj.coins" count 14000
        4 weight "obj.cert_bucket_supercompost" count 60
        3 weight "obj.cert_antidote++4" count 6
        1 weight "obj.wilderness_fishing_bait" count 200
        1 weight "obj.magic_tree_seed" count 1
        1 weight "obj.palm_tree_seed" count 1
        1 outOf 358 separate rsPlayerWeightedTable {
            1 weight "obj.dragon_2h_sword" count 1
            1 weight "obj.dragon_pickaxe" count 1
        }
        1 outOf 618 separate "obj.wbr_venenatis_fang" count 1
        1 outOf 716 separate "obj.sharp_ring" count 1
        1 outOf 912 separate "obj.wbr_voidwaker_gem" count 1
        5 outOf 54 separate rsPlayerWeightedTable {
            5 weight "obj.blighted_anglerfish" count 5..6
            5 weight "obj.blighted_karambwan" count 5..6
            5 weight "obj.blighted_3dose2restore" count 3..4
            5 weight "obj.blighted_4dose2restore" count 3..4
            5 weight "obj.2doserangerspotion" count 2..3
            5 weight "obj.2dose2combat" count 2..3
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 5 weight "obj.macro_triffidfruit" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 2800 weight "obj.venenatis_pet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/50 if a ring of wealth (i) is worn.
        1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
