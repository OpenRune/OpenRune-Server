package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val amoxliatlDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Amoxliatl Drops",
    npcs = npcs("npc.amoxliatl"),
    mainTable = rsPlayerWeightedTable(total = 58) {
        name("Amoxliatl Drops")
        2 weight "obj.rune_mace" count 1
        2 weight "obj.rune_pickaxe" count 1
        1 weight "obj.rune_platebody" count 1
        1 weight "obj.rune_platelegs" count 1
        5 weight "obj.waterrune" count 200..400
        4 weight "obj.chaosrune" count 30..60
        4 weight "obj.deathrune" count 20..40
        4 weight "obj.bloodrune" count 15..30
        4 weight "obj.soulrune" count 30..45
        4 weight "obj.naturerune" count 30..60
        5 weight "obj.cert_coal" count 20..30
        5 weight "obj.cert_gold_ore" count 20..30
        4 weight "obj.cert_adamantite_ore" count 5..10
        2 weight "obj.3doseprayerrestore" count 1
        2 weight "obj.cert_runite_ore" count 1
        2 weight "obj.blessed_bone_shard" count 60..100
        2 weight "obj.cert_water_orb" count 10..20
        2 weight "obj.water_talisman" count 1
        1 weight "obj.huasca_seed" count 1
        6 outOf 10 separate "obj.frozen_tear" count 2..4
        3 outOf 10 separate "obj.frozen_tear" count 5..10
        1 outOf 10 separate "obj.frozen_tear" count 10..20
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Guaranteed elite clue scroll and reward casket drops only occur when completing an elite clue scroll asking you to kill a frost nagua.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 25 weight "obj.pendant_of_ates_empty" count 1
        1 outOf 100 weight "obj.glacial_temotli" count 1
        1 outOf 125 weight "obj.varlamore_key_half_1" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 3000 weight "obj.amoxliatlpet" count 1
        1 outOf 190 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
