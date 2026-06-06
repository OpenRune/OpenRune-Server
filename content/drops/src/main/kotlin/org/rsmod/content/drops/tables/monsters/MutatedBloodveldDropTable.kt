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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mutatedBloodveldDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mutated Bloodveld Drops",
    npcs = npcs("npc.kourend_bloodveld"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Mutated Bloodveld Drops")
        8 weight "obj.mithril_full_helm" count 1
        6 weight "obj.mithril_battleaxe" count 1
        5 weight "obj.black_med_helm" count 1
        5 weight "obj.mithril_axe" count 1
        3 weight "obj.adamant_knife" count 2
        3 weight "obj.adamant_chainbody" count 1
        3 weight "obj.adamant_scimitar" count 1
        2 weight "obj.mithril_platebody" count 1
        2 weight "obj.rune_med_helm" count 1
        1 weight "obj.black_armoured_boots" count 1
        1 weight "obj.adamant_longsword" count 1
        1 weight "obj.rune_dagger" count 1
        1 weight "obj.rune_battleaxe" count 1
        7 weight "obj.airrune" count 105
        9 weight "obj.firerune" count 75
        13 weight "obj.bloodrune" count 30
        10 weight "obj.bloodrune" count 7
        7 weight "obj.soulrune" count 10
        10 weight "obj.coins" count 350
        3 weight "obj.coins" count 11
        4 weight "obj.bow_string" count 1
        7 weight "obj.gold_ore" count 1
        5 weight "obj.meat_pie" count 1
        5 weight "obj.mithril_bar" count 1
        2 weight "obj.strung_ruby_amulet" count 1

        3 weight SharedDropTables.rareDrop
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: The elite clue scroll and casket are only dropped when completing an elite clue scroll asking you to kill a Bloodveld
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 20 weight "obj.arceuus_corpse_bloodveld" count 1
        1 outOf 24 weight "obj.prif_crystal_shard" count (3..5) condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
