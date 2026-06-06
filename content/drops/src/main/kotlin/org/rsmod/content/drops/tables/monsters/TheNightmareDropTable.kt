package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val theNightmareDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "The Nightmare Drops",
    npcs = npcs("npc.nightmare_blast", "npc.nightmare_dying", "npc.nightmare_entry_ready", "npc.nightmare_initial", "npc.nightmare_phase_01", "npc.nightmare_phase_02", "npc.nightmare_phase_03", "npc.nightmare_weak_phase_01", "npc.nightmare_weak_phase_02", "npc.nightmare_weak_phase_03"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("The Nightmare Drops")
        4 weight "obj.cosmicrune" count 11..242
        4 weight "obj.naturerune" count 8..176
        4 weight "obj.deathrune" count 7..176
        4 weight "obj.bloodrune" count 16..440
        4 weight "obj.soulrune" count 8..132
        4 weight "obj.mcannonball" count 6..220
        3 weight "obj.rune_arrow" count 31..547
        4 weight "obj.cert_mithril_ore" count 11..176
        4 weight "obj.cert_coal" count 15..264
        4 weight "obj.cert_gold_ore" count 9..176
        4 weight "obj.cert_adamantite_ore" count 40..95
        4 weight "obj.cert_magic_logs" count 1..55
        4 weight "obj.cert_unidentified_cadantine" count 1..16
        4 weight "obj.cert_unidentified_torstol" count 1..16
        3 weight "obj.snapdragon_seed" count 1..7
        3 weight "obj.cert_uncut_emerald" count 2..44
        3 weight "obj.cert_uncut_ruby" count 27..60
        2 weight "obj.cert_runite_ore" count 1..16
        6 weight "obj.bass" count 1..18
        6 weight "obj.shark" count 1..16
        5 weight "obj.3doseprayerrestore" count 1..10
        5 weight "obj.sanfew_salve_3_dose" count 1..8
        5 weight "obj.3dosepotionofsaradomin" count 1..10
        5 weight "obj.3dosepotionofzamorak" count 1..10
        2 weight "obj.coins" count 2291..43958
        1 outOf 300 separate "obj.nightmare_staff" count 1
        1 outOf 420 separate rsPlayerWeightedTable {
            1 weight "obj.inquisitors_helm" count 1
            1 weight "obj.inquisitors_body" count 1
            1 weight "obj.inquisitors_skirt" count 1
        }
        1 outOf 750 separate "obj.inquisitors_mace" count 1
        1 outOf 960 separate rsPlayerWeightedTable {
            1 weight "obj.eldritch_orb" count 1
            1 weight "obj.harmonised_orb" count 1
            1 weight "obj.volatile_orb" count 1
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The drop rate of the Little Nightmare depends on the amount of players in the fight; 1/800 for 1, 1/1,600 for 2, 1/2,400 for 3, 1/3,200 for 4, and 1/4,000 for 5 and beyond. The team size is counted when the fight starts.
        1 outOf 800 weight "obj.nightmarepet" count 1
        1 outOf 1900 weight "obj.jar_of_dreams" count 1
        1 outOf 180 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
