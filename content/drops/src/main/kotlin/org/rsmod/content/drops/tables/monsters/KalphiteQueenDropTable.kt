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
public val kalphiteQueenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kalphite Queen Drops",
    npcs = npcs("npc.kalphite_flyingqueen", "npc.kalphite_queen", "npc.swan_kalphite_1", "npc.swan_kalphite_2"),
    mainTable = rsPlayerWeightedTable(total = 126) {
        name("Kalphite Queen Drops")
        5 weight "obj.cert_battlestaff" count 10
        4 weight "obj.rune_chainbody" count 1
        4 weight "obj.red_dragonhide_body" count 1
        4 weight "obj.rune_knife_p++" count 25
        2 weight "obj.lava_battlestaff" count 1
        6 weight "obj.deathrune" count 150
        6 weight "obj.bloodrune" count 100
        5 weight "obj.mithril_arrow" count 500
        3 weight "obj.rune_arrow" count 250
        2 weight "obj.cert_unidentified_toadflax" count 25
        2 weight "obj.cert_unidentified_ranarr" count 25
        2 weight "obj.cert_unidentified_snapdragon" count 25
        2 weight "obj.cert_unidentified_torstol" count 25
        4 weight "obj.torstol_seed" count 2
        3 weight "obj.watermelon_seed" count 25
        3 weight "obj.papaya_tree_seed" count 2
        3 weight "obj.palm_tree_seed" count 2
        3 weight "obj.magic_tree_seed" count 2
        5 weight "obj.cert_runite_bar" count 3
        4 weight "obj.cert_bucket_sand" count 100
        4 weight "obj.cert_gold_ore" count 250
        4 weight "obj.cert_magic_logs" count 60
        3 weight "obj.cert_uncut_emerald" count 25
        3 weight "obj.cert_uncut_ruby" count 25
        3 weight "obj.cert_uncut_diamond" count 25
        10 weight "obj.cert_wine_of_zamorak" count 60
        8 weight "obj.cert_cactus_potato" count 100
        5 weight "obj.coins" count 15000..20000
        5 weight "obj.cert_grapes" count 100
        5 weight "obj.cert_weapon_poison++" count 5
        3 weight "obj.cert_cactus_spine" count 10
        1 outOf 9 separate rsPlayerWeightedTable {
            1 weight "obj.monkfish" count 3
            1 weight "obj.shark" count 2
            1 weight "obj.dark_crab" count 2
            1 weight "obj.4dosepotionofsaradomin" count 1
            1 weight "obj.4doseprayerrestore" count 2
            1 weight "obj.4dose2restore" count 1
            1 weight "obj.2dose2combat" count 1
            1 weight "obj.3doserangerspotion" count 1
            1 weight "obj.2dose2antipoison" count 1
        }

        1 weight SharedDropTables.rareDrop
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.arceuus_corpse_kalphite" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 128 weight "obj.poh_trophydrop_kalphitequeen" count 1
        1 outOf 128 weight "obj.dragon_chainbody" count 1
        1 outOf 256 weight "obj.dragon_2h_sword" count 1
        1 outOf 400 weight "obj.dragon_pickaxe" count 1
        1 outOf 2000 weight "obj.jar_of_sand" count 1
        1 outOf 3000 weight "obj.kqpet_walking" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Kq head (tattered) [tertiary/Once]
