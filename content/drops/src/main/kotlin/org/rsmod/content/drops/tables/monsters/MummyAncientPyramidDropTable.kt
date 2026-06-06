package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mummyAncientPyramidDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mummy (Ancient Pyramid) Drops",
    npcs = npcs("npc.deserttreasure_mummy_2", "npc.deserttreasure_mummy_2_on_fire", "npc.deserttreasure_mummy_3", "npc.deserttreasure_mummy_3_on_fire", "npc.deserttreasure_mummy_4", "npc.deserttreasure_mummy_4_on_fire", "npc.deserttreasure_mummy_5", "npc.deserttreasure_mummy_5_on_fire"),
    mainTable = rsPlayerWeightedTable(total = 513) {
        name("Mummy (Ancient Pyramid) Drops")
        3 weight "obj.rune_sq_shield" count 1
        3 weight "obj.rune_scimitar" count 1
        1 weight "obj.staff_of_zaros" count 1
        40 weight "obj.gold_ring" count 1
        25 weight "obj.unstrung_gold_amulet" count 1
        20 weight "obj.gold_necklace" count 1
        10 weight "obj.sapphire_necklace" count 1
        10 weight "obj.sapphire_ring" count 1
        10 weight "obj.unstrung_emerald_amulet" count 1
        10 weight "obj.emerald_ring" count 1
        10 weight "obj.ring_of_dueling_1" count 1
        20 weight "obj.coins" count 36
        20 weight "obj.coins" count 95
        20 weight "obj.coins" count 106
        20 weight "obj.coins" count 183
        20 weight "obj.coins" count 222
        17 weight "obj.coins" count 46
        10 weight "obj.coins" count 472
        30 weight "obj.swamp_tar" count 1
        100 weight "obj.gold_ore" count 1
        40 weight "obj.silk" count 1
        44 weight "obj.papyrus" count 1
        3 weight "obj.casket" count 1
        20 weight "obj.gold_bar" count 1
        2 weight ringNothing()

        3 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/487 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 513 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
