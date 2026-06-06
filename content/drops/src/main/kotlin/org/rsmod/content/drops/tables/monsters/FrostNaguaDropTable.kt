package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val frostNaguaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Frost Nagua Drops",
    npcs = npcs("npc.frost_nagua", "npc.frost_nagua_cutscene", "npc.vmq3_ruins_frost_nagua"),
    mainTable = rsPlayerWeightedTable(total = 34) {
        name("Frost Nagua Drops")
        3 weight "obj.waterrune" count 10..30
        3 weight "obj.waterrune" count 30..100
        3 weight "obj.chaosrune" count 10..40
        2 weight "obj.deathrune" count 10..30
        2 weight "obj.naturerune" count 10..20
        3 weight "obj.cert_coal" count 10..15
        3 weight "obj.cert_gold_ore" count 10..20
        3 weight "obj.cert_softclay" count 10..20
        2 weight "obj.cert_mithril_ore" count 5..10
        3 weight "obj.cert_water_orb" count 2..6
        2 weight "obj.1doseprayerrestore" count 1
        1 weight "obj.rune_mace" count 1
        1 weight "obj.water_talisman" count 1
        1 weight "obj.blessed_bone_shard" count 30..50
        1 weight "obj.cert_jug_water" count 10..30

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Elite clue scrolls and reward caskets are only dropped when completing an elite clue scroll asking you to kill a frost nagua.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 10 weight "obj.frozen_tear" count 5..10
        1 outOf 100 weight "obj.pendant_of_ates_empty" count 1
        1 outOf 79 weight "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
        1 outOf 500 weight "obj.glacial_temotli" count 1
        1 outOf 540 weight "obj.varlamore_key_half_1" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Rare]
