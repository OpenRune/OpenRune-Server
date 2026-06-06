package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.herbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val aberrantSpectreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Aberrant spectre Drops",
    npcs = npcs("npc.slayer_abberant_spectre_1", "npc.slayer_abberant_spectre_2", "npc.slayer_abberant_spectre_3", "npc.slayer_abberant_spectre_4", "npc.slayer_aberrantspectre_1_strongholdcave", "npc.slayer_aberrantspectre_2_strongholdcave"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Aberrant spectre Drops")
        3 weight "obj.steel_axe" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.lava_battlestaff" count 1
        1 weight "obj.adamant_platelegs" count 1
        1 weight "obj.rune_full_helm" count 1
        1 weight "obj.coins" count 460
        18 weight ringNothing()
        1 outOf 512 separate "obj.mystic_robe_bottom_dark" count 1

        78 weight rsWeightedTable(total = 26) {
            name("Multi-roll herb drop table")
            11 weight herbDropTable
            11 weight doubleRollHerbDropTable
            4 weight tripleRollHerbDropTable
        }
        5 weight SharedDropTables.gem
        19 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Only if you have an [[Treasure Trails/Full guide/Elite#Cryptic clues
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
