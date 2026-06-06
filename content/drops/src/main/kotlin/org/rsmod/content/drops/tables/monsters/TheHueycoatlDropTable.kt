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
public val theHueycoatlDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "The Hueycoatl Drops",
    npcs = npcs("npc.huey_head", "npc.huey_head_defeated", "npc.huey_head_enraged", "npc.huey_head_invulnerable", "npc.huey_head_respawn_placeholder", "npc.huey_tail", "npc.huey_tail_broken"),
    mainTable = rsPlayerWeightedTable(total = 68) {
        name("The Hueycoatl Drops")
        3 weight "obj.cert_rune_mace" count 1..26
        3 weight "obj.cert_rune_scimitar" count 1..20
        2 weight "obj.cert_rune_plateskirt" count 1..20
        3 weight "obj.cert_adamant_platebody" count 1..30
        3 weight "obj.deathrune" count 22..450
        2 weight "obj.earthrune" count 150..3000
        2 weight "obj.cosmicrune" count 20..400
        2 weight "obj.naturerune" count 20..400
        2 weight "obj.lantadyme_seed" count 3..60
        2 weight "obj.huasca_seed" count 1..20
        2 weight "obj.toadflax_seed" count 2..40
        2 weight "obj.torstol_seed" count 1..20
        2 weight "obj.ranarr_seed" count 1..20
        3 weight "obj.avantoe_seed" count 1..20
        3 weight "obj.kwuarm_seed" count 1..20
        5 weight "obj.soiled_page" count 4..80
        3 weight "obj.xbows_crossbow_bolts_adamantite_unfeathered" count 15..300
        3 weight "obj.cert_air_orb" count 4..80
        3 weight "obj.cert_raw_shark" count 9..180
        3 weight "obj.sunfiresplinter" count 17..350
        3 weight "obj.cert_sun_kissed_bone" count 5..100
        3 weight "obj.cert_dragon_bones" count 2..40
        2 weight "obj.cert_adamantite_ore" count 6..120
        3 weight "obj.mcannonball" count 60..1200
        2 weight "obj.rune_dart_tip" count 6..120
        2 weight "obj.cert_limpwurt_root" count 10..200
        1 outOf 105 separate "obj.dragonhunter_wand" count 1
        11 outOf 315 separate "obj.huey_hide" count 3
        1 outOf 90 separate "obj.tome_of_earth_uncharged" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): Each player has around a 1/75 chance of getting the half of a key, regardless of participation.
        1 outOf 75 weight "obj.varlamore_key_half_1" count 1
        // Drops Need Manual (rate): The base rate of 1/400 is scaled by personal contribution to the kill.
        1 outOf 400 weight "obj.hueypet" count 1
        1 outOf 47 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
