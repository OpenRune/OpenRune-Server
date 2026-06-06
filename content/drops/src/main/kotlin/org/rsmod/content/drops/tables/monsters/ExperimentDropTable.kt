package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val experimentDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Experiment Drops",
    npcs = npcs("npc.fenk_experiment_1", "npc.fenk_experiment_2", "npc.fenk_experiment_3"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.fenk_mausoleum_key" count 1 condition {
            player -> player.isOnQuest("quest_creatureoffenkenstrain")
        }
    },
    mainTable = rsPlayerWeightedTable(total = 4) {
        name("Experiment Drops")
        1 weight "obj.rag_experiment_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        3 weight nothing()
    },
)
