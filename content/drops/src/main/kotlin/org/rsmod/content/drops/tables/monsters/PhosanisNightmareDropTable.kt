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
public val phosanisNightmareDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Phosani's Nightmare Drops",
    npcs = npcs("npc.nightmare_challenge_blast", "npc.nightmare_challenge_dying", "npc.nightmare_challenge_phase_01", "npc.nightmare_challenge_phase_02", "npc.nightmare_challenge_phase_03", "npc.nightmare_challenge_phase_04", "npc.nightmare_challenge_phase_05", "npc.nightmare_challenge_weak_phase_01", "npc.nightmare_challenge_weak_phase_02", "npc.nightmare_challenge_weak_phase_03", "npc.nightmare_challenge_weak_phase_04"),
    mainTable = rsPlayerWeightedTable(total = 101) {
        name("Phosani's Nightmare Drops")
        4 weight "obj.cosmicrune" count 247..420
        4 weight "obj.naturerune" count 165..305
        4 weight "obj.deathrune" count 165..305
        4 weight "obj.bloodrune" count 343..765
        4 weight "obj.soulrune" count 110..228
        4 weight "obj.mcannonball" count 137..382
        3 weight "obj.rune_arrow" count 412..957
        5 weight "obj.cert_mithril_ore" count 165..305
        4 weight "obj.cert_coal" count 220..458
        4 weight "obj.cert_gold_ore" count 165..305
        4 weight "obj.cert_adamantite_ore" count 40..95
        4 weight "obj.cert_magic_logs" count 40..95
        4 weight "obj.cert_unidentified_cadantine" count 13..26
        4 weight "obj.cert_unidentified_torstol" count 13..26
        3 weight "obj.snapdragon_seed" count 5..10
        3 weight "obj.cert_uncut_emerald" count 33..75
        3 weight "obj.cert_uncut_ruby" count 27..60
        2 weight "obj.cert_runite_ore" count 11..26
        6 weight "obj.bass" count 16..29
        6 weight "obj.shark" count 13..26
        5 weight "obj.3doseprayerrestore" count 8..15
        5 weight "obj.sanfew_salve_3_dose" count 6..12
        5 weight "obj.3dosepotionofsaradomin" count 8..15
        5 weight "obj.3dosepotionofzamorak" count 8..15
        2 weight "obj.coins" count 41417..74500
        69 outOf 35000 separate "obj.nightmare_staff" count 1
        31 outOf 35000 separate "obj.inquisitors_mace" count 1
        1 outOf 700 separate rsPlayerWeightedTable {
            1 weight "obj.inquisitors_helm" count 1
            1 weight "obj.inquisitors_body" count 1
            1 weight "obj.inquisitors_skirt" count 1
        }
        1 outOf 1600 separate rsPlayerWeightedTable {
            1 weight "obj.eldritch_orb" count 1
            1 weight "obj.harmonised_orb" count 1
            1 weight "obj.volatile_orb" count 1
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): In addition to having a 1/25 chance to drop, players will '''always''' receive the tablet on their 25th kill. Players that already have more than 25 kill count, or have managed to lose their slepey tablet without using it, will receive it on their next kill.
        1 outOf 25 weight "obj.slepe_teleport_consumable" count 1
        1 outOf 200 weight "obj.nightmare_challenge_morph" count 1
        1 outOf 1400 weight "obj.nightmarepet" count 1
        1 outOf 4000 weight "obj.jar_of_dreams" count 1
        1 outOf 33 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
