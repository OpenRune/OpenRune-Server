package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tormentedDemonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tormented Demon Drops",
    npcs = npcs("npc.tormented_demon_1", "npc.tormented_demon_2", "npc.tormented_demon_quest_1", "npc.tormented_demon_quest_2"),
    mainTable = rsPlayerWeightedTable(total = 51) {
        name("Tormented Demon Drops")
        3 weight "obj.dragon_dagger" count 1
        2 weight "obj.rune_kiteshield" count 1
        3 weight "obj.cert_battlestaff" count 1
        4 weight "obj.rune_platebody" count 1
        4 weight "obj.chaosrune" count 25..100
        2 weight "obj.soulrune" count 50..75
        4 weight "obj.rune_arrow" count 65..125
        4 weight "obj.mantaray" count 1..2
        1 weight "obj.4doseprayerrestore" count 1
        1 weight "obj.2doseprayerrestore" count 2
        2 weight "obj.malicious_ashes" count 2..3
        2 weight "obj.cert_fire_orb" count 5..7
        1 weight "obj.dragon_arrowheads" count 30..40
        499 outOf 250000 separate "obj.bone_claw" count 1
        1 outOf 500 separate "obj.tormented_synapse" count 1
        10 outOf 408 separate "obj.unidentified_kwuarm" count 1
        8 outOf 408 separate rsPlayerWeightedTable {
            8 weight "obj.unidentified_dwarf_weed" count 1
            8 weight "obj.unidentified_cadantine" count 1
        }
        6 outOf 408 separate "obj.unidentified_lantadyme" count 1
        5 outOf 408 separate "obj.unidentified_avantoe" count 1
        4 outOf 408 separate rsPlayerWeightedTable {
            4 weight "obj.unidentified_ranarr" count 1
            4 weight "obj.unidentified_snapdragon" count 1
        }
        3 outOf 408 separate "obj.unidentified_torstol" count 1
        1 outOf 25 separate rsPlayerWeightedTable {
            1 weight "obj.smouldering_gland" count 1
            1 weight "obj.smouldering_pile_of_flesh" count 1
        }
        1 outOf 125 separate "obj.smouldering_heart" count 1
        1 outOf 12 separate "obj.teleportscroll_guthixian_temple" count 2
        29 outOf 255 separate "obj.cert_unstrung_magic_shortbow" count 1
        1 outOf 255 separate "obj.unstrung_magic_longbow" count 1
        18 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/121 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
