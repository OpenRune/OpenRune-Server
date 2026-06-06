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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val araxxorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Araxxor Drops",
    npcs = npcs("npc.araxxor"),
    mainTable = rsPlayerWeightedTable(total = 115) {
        name("Araxxor Drops")
        8 weight "obj.cert_rune_kiteshield" count 2
        8 weight "obj.cert_rune_platelegs" count 2
        6 weight "obj.dragon_mace" count 2
        1 weight "obj.cert_rune_2h_sword" count 5
        1 weight "obj.dragon_platelegs" count 2
        5 weight "obj.deathrune" count 250
        2 weight "obj.naturerune" count 80
        1 weight "obj.mudrune" count 100
        1 weight "obj.bloodrune" count 180
        4 weight "obj.yew_seed" count 1
        3 weight "obj.toadflax_seed" count 4
        1 weight "obj.ranarr_seed" count 3
        1 weight "obj.snapdragon_seed" count 3
        1 weight "obj.magic_tree_seed" count 2
        4 weight "obj.cert_coal" count 120
        4 weight "obj.cert_adamantite_ore" count 85
        4 weight "obj.cert_raw_shark" count 21
        3 weight "obj.cert_yew_logs" count 70
        2 weight "obj.cert_runite_ore" count 12
        1 weight "obj.cert_raw_shark" count 100
        1 weight "obj.cert_raw_monkfish" count 120
        1 weight "obj.cert_blankrune_high" count 1200
        8 weight "obj.teleportscroll_spidercave" count 3
        6 weight "obj.cert_earth_orb" count 45
        5 weight "obj.araxyte_venom_sack" count 6
        4 weight "obj.cert_mortmyremushroom" count 24
        4 weight "obj.cert_antidote++3" count 6
        3 weight "obj.cert_wine_of_zamorak" count 8
        2 weight "obj.cert_red_spiders_eggs" count 40
        2 weight "obj.araxyte_venom_sack" count 12
        1 weight "obj.cert_hollow_bark" count 15
        1 outOf 200 separate rsPlayerWeightedTable {
            1 weight "obj.noxious_halberd_part_3" count 1
            1 weight "obj.noxious_halberd_part_1" count 1
            1 weight "obj.noxious_halberd_part_2" count 1
        }
        1 outOf 600 separate "obj.araxyte_fang" count 1
        1 outOf 16 separate rsPlayerWeightedTable {
            1 weight "obj.araxyte_venom_sack" count 1
            1 weight "obj.1dose2combat" count 1
            1 weight "obj.shark" count 2..3
            1 weight "obj.wild_pie" count 2..3
        }
        1 outOf 8 separate "obj.4doseprayerrestore" count 1
        1 outOf 250 separate "obj.poh_araxyte_head" count 1
        1 outOf 1500 separate "obj.jar_of_venom" count 1

        1 weight SharedDropTables.rareDrop
        16 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.araxxor_pet_morph" count 1 condition { player ->
            // Drops Need Manual: Guaranteed reward for defeating Araxxor in under 1:15, provided that the player does not already have one in their possession, or has not already used one on Nid.
             true
        }
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The pet drop rate is doubled if the player chooses to destroy Araxxor's corpse instead of harvesting it.
        1 outOf 3000 weight "obj.araxxorpet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/47 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 50 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
