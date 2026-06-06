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
public val earthenNaguaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Earthen Nagua Drops",
    npcs = npcs("npc.earth_nagua", "npc.earth_nagua_reinforced"),
    mainTable = rsPlayerWeightedTable(total = 32) {
        name("Earthen Nagua Drops")
        3 weight "obj.chaosrune" count 8..20
        2 weight "obj.naturerune" count 8..15
        2 weight "obj.deathrune" count 8..13
        4 weight "obj.cert_iron_ore" count 8..15
        3 weight "obj.cert_coal" count 8..15
        2 weight "obj.cert_gold_ore" count 4..8
        2 weight "obj.cert_adamantite_ore" count 1..2
        3 weight "obj.cert_softclay" count 5..10
        2 weight "obj.blessed_bone_shard" count 20..40
        1 weight "obj.cert_earth_orb" count 1..2
        1 weight "obj.earth_talisman" count 1
        75 outOf 1264 separate "obj.earthrune" count 10..19
        111 outOf 1264 separate "obj.earthrune" count 20..30
        51 outOf 1264 separate "obj.earthrune" count 31..50

        1 weight SharedDropTables.gem
        6 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Elite clue scrolls and reward caskets are only dropped when completing an elite clue scroll asking you to kill an earthen nagua.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 400 weight "obj.earthbound_tecpatl" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
