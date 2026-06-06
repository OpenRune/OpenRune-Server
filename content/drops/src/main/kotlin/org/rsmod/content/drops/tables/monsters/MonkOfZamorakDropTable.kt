package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val monkOfZamorakDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Monk of Zamorak Drops",
    npcs = npcs("npc.chaos_temple_monk1", "npc.chaos_temple_monk2", "npc.chaos_temple_monk_wilderness", "npc.chaosmonk1", "npc.chaosmonk2", "npc.chaosmonk3", "npc.priestperilevilmonk1", "npc.priestperilevilmonk2", "npc.priestperilevilmonk3"),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Monk of Zamorak Drops")
        1 weight "obj.zamrobebottom" count 1
        1 weight "obj.zamrobetop" count 1
        18 weight ringNothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.pipkey_gold" count 1 condition {
            player -> player.isOnQuest("quest_priestinperil")
        }
        1 outOf 6 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
