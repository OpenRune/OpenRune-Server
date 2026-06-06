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
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val chaosElementalDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chaos Elemental Drops",
    npcs = npcs("npc.chaoselemental"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 256 weight "obj.dragon_pickaxe" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Chaos Elemental Drops")
        5 weight "obj.rune_dart" count 100
        4 weight "obj.rune_platelegs" count 1
        4 weight "obj.rune_plateskirt" count 1
        3 weight "obj.rune_2h_sword" count 1
        3 weight "obj.rune_battleaxe" count 1
        3 weight "obj.rune_full_helm" count 1
        3 weight "obj.rune_kiteshield" count 1
        3 weight "obj.mystic_air_staff" count 1
        3 weight "obj.mystic_water_staff" count 1
        3 weight "obj.mystic_earth_staff" count 1
        3 weight "obj.mystic_fire_staff" count 1
        2 weight "obj.dragon_dagger" count 1
        2 weight "obj.dragon_2h_sword" count 1
        2 weight "obj.dragon_platelegs" count 1
        2 weight "obj.dragon_plateskirt" count 1
        8 weight "obj.chaosrune" count 300..500
        8 weight "obj.bloodrune" count 100..250
        5 weight "obj.rune_arrow" count 150
        4 weight "obj.cert_unidentified_ranarr" count 5..8
        4 weight "obj.cert_unidentified_snapdragon" count 5..8
        3 weight "obj.cert_unidentified_avantoe" count 5..8
        3 weight "obj.cert_unidentified_kwuarm" count 5..8
        5 weight "obj.cert_coal" count 75..150
        5 weight "obj.cert_plank_mahogany" count 8..16
        4 weight "obj.cert_runite_bar" count 3..5
        4 weight "obj.cert_adamantite_bar" count 8..12
        7 weight "obj.coins" count 20005..29995
        5 weight "obj.blighted_sack_icebarrage" count 20..40
        4 weight "obj.cert_blighted_karambwan" count 15..25
        4 weight "obj.cert_blighted_anglerfish" count 10..15
        2 weight "obj.tablet_wildycrabs" count 2
        1 outOf 5 separate rsPlayerWeightedTable {
            1 weight "obj.blighted_anglerfish" count 2
            1 weight "obj.blighted_karambwan" count 3
            1 weight "obj.blighted_4dose2restore" count 1
            1 weight "obj.dragon_bones" count 1
            1 weight "obj.1dose2combat" count 1
        }

        8 weight SharedDropTables.rareDrop
    },
    tertiaries = rsPlayerTertiaryTable {
        10 outOf 128 weight "obj.weapon_poison++" count 1
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 300 weight "obj.chaoselepet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/190 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/100 if a ring of wealth (i) is worn.
        1 outOf 200 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
