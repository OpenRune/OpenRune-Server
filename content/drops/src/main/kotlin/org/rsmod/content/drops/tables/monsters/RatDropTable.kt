package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ratDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Rat Drops",
        npcs = npcs("npc.rat", "npc.rat_indoors"),
        guaranteed =
            rsPlayerGuaranteedTable {
                "obj.rats_tail" count 1 condition { player ->
                    Quest.get("quest_witchspotion")?.isQuestInProgress(player) == true
                }
            },
    )
