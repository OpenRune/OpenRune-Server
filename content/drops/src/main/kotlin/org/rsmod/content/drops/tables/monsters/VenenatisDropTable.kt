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
public val venenatisDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Venenatis Drops",
    npcs = npcs("npc.venenatis"),
    mainTable = rsPlayerWeightedTable(total = 126) {
        name("Venenatis Drops")
        2 weight "obj.cert_mystic_air_staff" count 4
        8 weight "obj.cert_rune_pickaxe" count 5
        3 weight "obj.rune_dart" count 150
        5 weight "obj.rune_knife" count 150
        2 weight "obj.cert_rune_platelegs" count 4
        2 weight "obj.cert_rune_sq_shield" count 4
        2 weight "obj.cert_dragon_dagger" count 6
        7 weight "obj.chaosrune" count 500
        7 weight "obj.deathrune" count 700
        7 weight "obj.bloodrune" count 900
        5 weight "obj.xbows_crossbow_bolts_adamantite_tipped_diamond_enchanted" count 300
        4 weight "obj.mcannonball" count 600
        1 weight "obj.cert_uncut_ruby" count 50
        8 weight "obj.cert_uncut_diamond" count 25
        6 weight "obj.cert_gold_ore" count 675
        5 weight "obj.xbows_bolt_tips_onyx" count 150
        5 weight "obj.cert_magic_logs" count 225
        5 weight "obj.cert_limpwurt_root" count 100
        3 weight "obj.cert_red_spiders_eggs" count 500
        1 weight "obj.cert_unicorn_horn" count 225
        2 weight "obj.cert_uncut_dragonstone" count 5
        1 weight "obj.cert_unidentified_ranarr" count 45
        1 weight "obj.cert_unidentified_snapdragon" count 100
        1 weight "obj.cert_unidentified_toadflax" count 45
        2 weight "obj.cert_battlestaff" count 12
        10 weight "obj.coins" count 50000
        4 weight "obj.cert_bucket_supercompost" count 225
        3 weight "obj.cert_antidote++4" count 20
        5 weight "obj.cert_4dose2restore" count 10
        5 weight "obj.cert_dark_crab" count 50
        1 weight "obj.cert_blighted_anglerfish" count 100
        3 weight "obj.tablet_wildycrabs" count 4
        1 outOf 196 separate "obj.wbr_venenatis_fang" count 1
        1 outOf 256 separate rsPlayerWeightedTable {
            1 weight "obj.dragon_2h_sword" count 1
            1 weight "obj.dragon_pickaxe" count 1
        }
        1 outOf 360 separate "obj.wbr_voidwaker_gem" count 1
        1 outOf 512 separate "obj.sharp_ring" count 1
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
        1 outOf 5 weight "obj.macro_triffidfruit" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 1500 weight "obj.venenatis_pet" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/50 if a ring of wealth (i) is worn.
        1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
